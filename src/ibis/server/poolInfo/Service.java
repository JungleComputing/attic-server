package ibis.server.poolInfo;

import ibis.server.ServerProperties;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocket;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.Log;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public final class Service implements ibis.server.Service, Runnable {

    public static final int VIRTUAL_PORT = 301;

    public static final int RESULT_INVALID_SIZE = -1;

    public static final int RESULT_POOL_CLOSED = -2;
    
    public static final int RESULT_UNEQUAL_SIZE = -3;

    private static final Logger logger = Logger.getLogger(Service.class);

    private final VirtualServerSocket serverSocket;

    private final Map<String, Pool> pools;
    
    private final boolean events;

    public Service(TypedProperties properties, VirtualSocketFactory factory)
            throws IOException {
        pools = new HashMap<String, Pool>();

        serverSocket = factory.createServerSocket(VIRTUAL_PORT, 0, null);

        ThreadPool.createNew(this, "PoolInfoService");
        
        events = properties.getBooleanProperty(ServerProperties.LOG_EVENTS);
        
        Level level = Level.toLevel(properties
                .getProperty(ServerProperties.LOG_LEVEL, "INFO"));
        Log.initLog4J(logger, level);

        logger.info("Started pool info" + " service on virtual port "
                + VIRTUAL_PORT);
    }

    public void end(boolean waitUntilIdle) {
        // TODO Auto-generated method stub

    }

    private synchronized Pool getPool(String poolName) {
        Pool pool = pools.get(poolName);

        if (pool == null) {
            pool = new Pool();
            pools.put(poolName, pool);
        }

        return pool;
    }

    public void run() {
        while (true) {

            VirtualSocket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                logger.error("could not accept socket, stopping service", e);
                return;
            }

            // handle next connection in new thread;
            ThreadPool.createNew(this, "PoolInfoService");

            try {

                DataOutputStream out = new DataOutputStream(
                        new BufferedOutputStream(socket.getOutputStream()));
                DataInputStream in = new DataInputStream(
                        new BufferedInputStream(socket.getInputStream()));

                String poolName = in.readUTF();
                String hostName = in.readUTF();
                String clusterName = in.readUTF();
                int size = in.readInt();

                Pool pool = getPool(poolName);
                
                if (events) {
                    logger.info(hostName + " in cluster " + clusterName + " is joining pool " + poolName);
                }

                // blocks until pool is complete
                int rank = pool.join(hostName, clusterName, size);

                out.writeInt(rank);
                if (rank >= 0) {
                    String[] hostnames = pool.getHostnames();
                    for (int i = 0; i < hostnames.length; i++) {
                        out.writeUTF(hostnames[i]);
                    }
                    String[] clusters = pool.getClusterNames();
                    for (int i = 0; i < clusters.length; i++) {
                        out.writeUTF(clusters[i]);
                    }
                }
                out.flush();
                out.close();
                in.close();
                socket.close();
            } catch (Exception e) {
                logger.error("error on handling PoolInfo request", e);
            }
        }

    }

}
