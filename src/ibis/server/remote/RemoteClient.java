package ibis.server.remote;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class RemoteClient {

    private static final Logger logger = Logger.getLogger(RemoteClient.class);

    private final DataInputStream in;

    private final DataOutputStream out;

    private final PipedOutputStream pipeOut;

    private final PipedInputStream pipeIn;

    private boolean initialized = false;

    private String serverAddress = null;

    /**
     * Connect to the server with the given in and output stream
     * 
     * @throws IOException
     */
    public RemoteClient() throws IOException {
        pipeOut = new PipedOutputStream();
        in = new DataInputStream(new BufferedInputStream(new PipedInputStream(
                pipeOut)));

        pipeIn = new PipedInputStream();
        out = new DataOutputStream(new BufferedOutputStream(
                new PipedOutputStream(pipeIn)));

    }

    public synchronized void init() throws IOException {
        if (initialized) {
            return;
        }

        out.writeByte(Protocol.MAGIC);
        out.writeInt(Protocol.VERSION);
        out.flush();

        logger.debug("written magic/version, waiting for reply");

        byte reply = in.readByte();
        String message = in.readUTF();
        if (reply != Protocol.REPLY_OK) {
            throw new IOException(message);
        }

        serverAddress = in.readUTF();

        initialized = true;
    }

    /**
     * Returns the stream data from the server for this client can be written
     * to.
     * 
     * @return the stream data from the server for this client can be written
     *         to.
     */
    public OutputStream getOutputStream() {
        return pipeOut;
    }

    /**
     * Returns the stream which this client writes data for the server process
     * to.
     * 
     * @return the stream which this client writes data for the server process
     *         to.
     */
    public InputStream getInputStream() {
        return pipeIn;
    }

    /**
     * Tell the server about some hubs
     */
    public synchronized void addHubs(String... hubs) throws IOException {
        init();

        out.writeByte(Protocol.OPCODE_ADD_HUBS);
        out.writeInt(hubs.length);
        for (String hub : hubs) {
            out.writeUTF(hub);
        }
        out.flush();
        byte reply = in.readByte();
        String message = in.readUTF();

        if (reply != Protocol.REPLY_OK) {
            throw new IOException(message);
        }
    }

    /**
     * Returns the addresses of all hubs known to this server
     */
    public synchronized String[] getHubs() throws IOException {
        init();

        out.writeByte(Protocol.OPCODE_GET_HUBS);
        out.flush();

        byte reply = in.readByte();
        String message = in.readUTF();

        if (reply != Protocol.REPLY_OK) {
            throw new IOException(message);
        }

        int size = in.readInt();
        String[] result = new String[size];
        for (int i = 0; i < size; i++) {
            result[i] = in.readUTF();
        }

        return result;
    }

    /**
     * Returns the local address of the server as a string
     */
    public synchronized String getLocalAddress() throws IOException {
        init();

        return serverAddress;
    }

    /**
     * Returns the names of all services currently in this server
     * 
     * @throws IOException
     *             in case of trouble
     */
    public String[] getServiceNames() throws IOException {
        init();

        out.writeByte(Protocol.OPCODE_GET_SERVICE_NAMES);
        out.flush();

        byte reply = in.readByte();
        String message = in.readUTF();

        if (reply != Protocol.REPLY_OK) {
            throw new IOException(message);
        }

        int size = in.readInt();
        String[] result = new String[size];
        for (int i = 0; i < size; i++) {
            result[i] = in.readUTF();
        }

        return result;

    }

    /**
     * Function to retrieve statistics for a given service
     * 
     * @param serviceName
     *            Name of service to get statistics of
     * 
     * @return statistics for given service, or null if service exist.
     * @throws IOException
     *             in case of trouble.
     */
    public Map<String, String> getStats(String serviceName) throws IOException {
        init();

        out.writeByte(Protocol.OPCODE_GET_STATISTICS);
        out.writeUTF(serviceName);
        out.flush();

        byte reply = in.readByte();
        String message = in.readUTF();

        if (reply != Protocol.REPLY_OK) {
            throw new IOException(message);
        }

        boolean resultIsNull = in.readBoolean();

        if (resultIsNull) {
            return null;
        }

        int size = in.readInt();
        Map<String, String> result = new HashMap<String, String>();
        for (int i = 0; i < size; i++) {
            String key = in.readUTF();
            String value = in.readUTF();

            // readUTF does not send null, so do this trick
            if (value.equals(Protocol.NULL_STRING)) {
                value = null;
            }
            result.put(key, value);
        }

        return result;

    }

    public synchronized void end(long timeout) throws IOException {
        init();

        out.writeByte(Protocol.OPCODE_END);
        out.writeLong(timeout);
        out.flush();

        byte reply = in.readByte();
        String message = in.readUTF();

        if (reply != Protocol.REPLY_OK) {
            throw new IOException(message);
        }
        in.close();
        out.close();
    }
}
