package ibis.server.remote;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

import ibis.server.Server;

public class RemoteHandler implements Runnable {

    private static final Logger logger = Logger.getLogger(RemoteHandler.class);

    private final Server server;
    private final DataInputStream in;
    private final DataOutputStream out;

    public RemoteHandler(Server server) {
        this.server = server;
        this.in = new DataInputStream(new BufferedInputStream(System.in));
        this.out = new DataOutputStream(new BufferedOutputStream(System.out));

    }

    private void close() {
        try {
            in.close();
        } catch (IOException e) {
            // IGNORE
        }
        try {
            out.flush();
        } catch (IOException e) {
            // IGNORE
        }
        try {
            out.close();
        } catch (IOException e) {
            // IGNORE
        }
    }

    private void handleAddHubs() throws IOException {
        int size = in.readInt();
        String[] newHubs = new String[size];
        for (int i = 0; i < size; i++) {
            newHubs[i] = in.readUTF();
        }
        server.addHubs(newHubs);
        out.writeByte(Protocol.REPLY_OK);
        out.writeUTF("OK");
        out.flush();
    }

    private void handleGetHubs() throws IOException {
        String[] hubs = server.getHubs();

        out.writeByte(Protocol.REPLY_OK);
        out.writeUTF("OK");

        out.writeInt(hubs.length);
        for (String hub : hubs) {
            out.writeUTF(hub);
        }
        out.flush();
    }

    private void handleGetServiceNames() throws IOException {
        String[] services = server.getServiceNames();

        out.writeByte(Protocol.REPLY_OK);
        out.writeUTF("OK");

        out.writeInt(services.length);
        for (String service : services) {
            out.writeUTF(service);
        }
        out.flush();
    }

    private void handleGetStatistics() throws IOException {
        String serviceName = in.readUTF();
        
        Map<String, String> statistics = server.getStats(serviceName);
        
        out.writeByte(Protocol.REPLY_OK);
        out.writeUTF("OK");
        
        out.writeBoolean(statistics == null);
        
        if (statistics != null) {
            out.writeInt(statistics.size());
            for(Map.Entry<String, String> entry: statistics.entrySet()) {
                out.writeUTF(entry.getKey());
                if (entry.getValue() != null) {
                    out.writeUTF(entry.getValue());
                } else {
                    out.writeUTF(Protocol.NULL_STRING);
                }
            }
        }
        out.flush();
    }

    private void handleEnd() throws IOException {
        long timeout = in.readLong();
        server.end(timeout);
        out.writeByte(Protocol.REPLY_OK);
        out.writeUTF("OK");
        out.flush();
    }

    public void run() {
        logger.debug("running remote handler");

        try {
            byte magic = in.readByte();
            int version = in.readInt();

            if (magic != Protocol.MAGIC) {
                out.writeByte(Protocol.REPLY_ERROR);
                out.writeUTF("wrong magic byte: " + magic + "instead of "
                        + Protocol.MAGIC);
                close();
                return;
            }
            if (version != Protocol.VERSION) {
                out.writeByte(Protocol.REPLY_ERROR);
                out.writeUTF("wrong version: " + version + "instead of "
                        + Protocol.VERSION);
                close();
                return;
            }
            logger.debug("writing OK");
            out.writeByte(Protocol.REPLY_OK);
            out.writeUTF("OK");

            out.writeUTF(server.getLocalAddress());

            out.flush();

            while (true) {
                byte opcode = in.readByte();
                logger.debug("got opcode: " + opcode);

                switch (opcode) {
                case Protocol.OPCODE_ADD_HUBS:
                    handleAddHubs();
                    break;
                case Protocol.OPCODE_GET_HUBS:
                    handleGetHubs();
                    break;
                case Protocol.OPCODE_GET_SERVICE_NAMES:
                    handleGetServiceNames();
                    break;
                case Protocol.OPCODE_GET_STATISTICS:
                    handleGetStatistics();
                    break;
                case Protocol.OPCODE_END:
                    handleEnd();
                    close();
                    return;
                default:
                    throw new IOException("unknown opcode: " + opcode);
                }
            }

        } catch (IOException e) {
            logger.error("error on handling remote request, ending server", e);
            server.end(-1);
            close();
        }
    }

}
