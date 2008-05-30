package ibis.server.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

// import org.apache.log4j.Logger;

public class RemoteClient {

    // private static final Logger logger = Logger.getLogger(RemoteClient.class);

    private final BufferedReader in;

    private final PrintStream out;

    private final PipedOutputStream pipeOut;

    private final PipedInputStream pipeIn;

    private String serverAddress = null;

    /**
     * Connect to the server with the given in and output stream
     * 
     * @throws IOException
     */
    public RemoteClient() throws IOException {
        pipeOut = new PipedOutputStream();
        in = new BufferedReader(new InputStreamReader(new PipedInputStream(
                pipeOut)));

        pipeIn = new PipedInputStream();
        out = new PrintStream(new PipedOutputStream(pipeIn));

    }
    
    private void println(String line) {
        out.println(Protocol.CLIENT_SAYS + line);
        out.flush();
    }
    
    private String readLine() throws IOException {
        String line = in.readLine();

        while (line != null && !line.startsWith(Protocol.SERVER_SAYS)) {
            System.err.println("client ignoring line: " + line);
            line = in.readLine();
        }

        return line.substring(Protocol.SERVER_SAYS.length());
    }

    private int readSizeIntLine() throws IOException {
        try {
            String string = readLine();

            if (string == null) {
                throw new IOException("could not read int, got EOF");
            }

            int result = Integer.parseInt(string);
            
            if (result < 0 || result > 15000) {
                throw new IOException("invalid number: " + result);
            }
            
            return result;
        } catch (NumberFormatException e) {
            throw new IOException("parse error on reading int: " + e);
        }
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
    
    public synchronized void addHubs(String... hubs) throws IOException {
        for(String hub: hubs) {
            addHub(hub);
        }
    }
            

    /**
     * Tell the server about some hubs
     */
    public synchronized void addHub(String hub) throws IOException {
        println(Protocol.OPCODE_ADD_HUB);
        
        println(hub);
        
        String status = readLine();
        
        if (status == null || !status.equals("OK")) {
            throw new IOException("Status not OK: " + status);
        }
    }

    /**
     * Returns the addresses of all hubs known to this server
     */
    public synchronized String[] getHubs() throws IOException {
        println(Protocol.OPCODE_GET_HUBS);

        String status = readLine();
        
        if (status == null || !status.equals("OK")) {
            throw new IOException("Status not OK: " + status);
        }
        
        int size = readSizeIntLine();
        String[] result = new String[size];
        for (int i = 0; i < size; i++) {
            result[i] = readLine();
        }

        return result;
    }

    /**
     * Returns the local address of the server as a string
     */
    public synchronized String getLocalAddress() throws IOException {
        if (serverAddress == null) {
            println(Protocol.OPCODE_GET_LOCAL_ADDRESS);
            String status = readLine();
            if (status == null || !status.equals("OK")) {
                throw new IOException("wrong status: " + status);
            }

            serverAddress = readLine();
        }

        return serverAddress;
    }

    /**
     * Returns the names of all services currently in this server
     * 
     * @throws IOException
     *             in case of trouble
     */
    public synchronized String[] getServiceNames() throws IOException {
        println(Protocol.OPCODE_GET_SERVICE_NAMES);

        String status = readLine();

        if (status == null || !status.equals("OK")) {
            throw new IOException("wrong status: " + status);
        }

        try {
            int nrOfServices = readSizeIntLine();

            String[] result = new String[nrOfServices];

            for (int i = 0; i < result.length; i++) {
                result[i] = readLine();
            }

            return result;
        } catch (NumberFormatException e) {
            throw new IOException("could not parse result into integer: " + e);
        }
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
    public synchronized Map<String, String> getStats(String serviceName)
            throws IOException {

        println(Protocol.OPCODE_GET_STATISTICS);
        println(serviceName);
        
        String status = readLine();
        if (status == null || !status.equals("OK")) {
            throw new IOException("wrong status: " + status);
        }

        int size = readSizeIntLine();
        
        Map<String, String> result = new HashMap<String, String>();
        for (int i = 0; i < size; i++) {
            String key = readLine();
            String value = readLine();
            if (value.equals("null")) {
                value = null;
            }

            result.put(key, value);
        }

        return result;

    }

    public synchronized void end(long timeout) throws IOException {
        println(Protocol.OPCODE_END);
        println("" + timeout);

        String status = readLine();
        
        if (status == null) {
            //end may cause null, but that's ok
            return;
        }
        
        if (!status.equals("OK")) {
            throw new IOException("wrong status: " + status);
        }
        
        in.close();
        
        out.close();
    }
}
