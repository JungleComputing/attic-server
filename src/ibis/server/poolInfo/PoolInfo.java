/* $Id: PoolInfo.java 5075 2007-02-22 16:43:45Z ceriel $ */

package ibis.server.poolInfo;

import ibis.server.Client;
import ibis.smartsockets.virtual.VirtualSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.TypedProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * The <code>PoolInfo</code> class provides a utility for finding out
 * information about the nodes involved in a closed-world run.
 * 
 * The <code>PoolInfo</code> class depends on the ibis.pool.size property,
 * which must contain the total number of hosts involved in the run. This
 * property can be set either as a system property, or in the ibis.properties
 * config file.
 */
public class PoolInfo {

    public static final int CONNECTION_TIMEOUT = 5000;

    private final String poolName;

    private final int size;

    private final int rank;

    private final String[] hostnames;

    private final String[] clusters;

    PoolInfo(String hostname, String clusterName, Properties properties)
            throws Exception {

        TypedProperties typedProperties = PoolInfoProperties
                .getHardcodedProperties();

        typedProperties.loadConfig("ibis.properties", "ibis.properties.file");

        typedProperties.addProperties(properties);

        size = typedProperties.getIntProperty(PoolInfoProperties.SIZE, 0);
        if (size <= 0) {
            throw new Exception("invalid or unknown pool size: " + size);
        }

        poolName = typedProperties.getProperty(PoolInfoProperties.NAME);
        if (poolName == null) {
            throw new Exception("Required property " + PoolInfoProperties.NAME
                    + " not set");
        }

        VirtualSocketFactory factory = Client.getFactory(typedProperties);
        VirtualSocketAddress serviceAddress = Client.getServiceAddress(
                Service.VIRTUAL_PORT, typedProperties);

        VirtualSocket socket = factory.createClientSocket(serviceAddress,
                CONNECTION_TIMEOUT, null);
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                socket.getOutputStream()));
        DataInputStream in = new DataInputStream(new BufferedInputStream(socket
                .getInputStream()));

        out.writeUTF(poolName);
        out.writeUTF(hostname);
        out.writeUTF(clusterName);
        out.writeInt(size);

        out.flush();
        
        rank = in.readInt();

        if (rank == Service.RESULT_INVALID_SIZE) {
            throw new Exception("Server: invalid size: " + size);
        } else if (rank == Service.RESULT_POOL_CLOSED) {
            throw new Exception("Server: cannot join pool " + poolName
                    + " : pool already closed");
        } else if (rank == Service.RESULT_UNEQUAL_SIZE) {
            throw new Exception("Server: cannot join pool " + poolName
                    + " : pool exists with different size");
        } else if (rank < 0) {
            throw new Exception("Unknown result: " + rank);
        }

        hostnames = new String[size];
        for (int i = 0; i < size; i++) {
            hostnames[i] = in.readUTF();
        }
        clusters = new String[size];
        for (int i = 0; i < size; i++) {
            clusters[i] = in.readUTF();
        }

        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            // IGNORE
        }

    }

    public String poolName() {
        return poolName;
    }

    /**
     * Returns the number of nodes in the pool.
     * 
     * @return the total number of nodes.
     */
    public int size() {
        return size;
    }

    /**
     * Returns the rank number in the pool of the current host.
     * 
     * @return the rank number.
     */
    public int rank() {
        return rank;
    }

    /**
     * Returns the name of the current host.
     * 
     * @return the name of the current host.
     */
    public String hostName() {
        return hostnames[rank];
    }

    /**
     * Returns the cluster name for the current host.
     * 
     * @return the cluster name.
     */
    public String clusterName() {
        return clusters[rank];
    }

    /**
     * Returns the cluster name for the host specified by the rank number.
     * 
     * @param rank
     *            the rank number.
     * @return the cluster name.
     */
    public String clusterName(int rank) {
        return clusters[rank];
    }

    /**
     * Returns an array of cluster names, one for each host involved in the run.
     * 
     * @return the cluster names
     */
    public String[] clusterNames() {
        return clusters.clone();
    }

    /**
     * Returns the name of the host with the given rank.
     * 
     * @param rank
     *            the rank number.
     * @return the name of the host with the given rank.
     */
    public String hostName(int rank) {
        return hostnames[rank];
    }

    /**
     * Returns an array of hostnames of the hosts.
     * 
     * @return an array of hostnames of the hosts.
     */
    public String[] hostNames() {
        return hostnames.clone();
    }

    /**
     * Returns a string representation of the information in this
     * <code>PoolInfo</code>.
     * 
     * @return a string representation.
     */
    public String toString() {
        String result = "pool info: size = " + size + "; my rank is " + rank
                + "; host list:\n";
        for (int i = 0; i < hostnames.length; i++) {
            result += i + ": address = " + hostnames[i] + " cluster = "
                    + clusters[i] + "\n";
        }
        return result;
    }
    
    public static void main(String[] args) {
        String host = args[0];
        String cluster = args[1];
        
        PoolInfo info;
        try {
            info = new PoolInfo(host, cluster, null);
        } catch (Exception e) {
           e.printStackTrace(System.err);
           return;
        }
        
        System.err.println(info.toString());
    }
}
