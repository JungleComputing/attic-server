package ibis.server.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.log4j.Logger;

import ibis.server.Server;

public class RemoteHandler implements Runnable {

    private static final Logger logger = Logger.getLogger(RemoteHandler.class);

    private final Server server;

    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    private void println(String string) {
        System.out.println(Protocol.SERVER_SAYS + string);
        System.out.flush();
    }

    private void println(int value) {
        System.out.println(Protocol.SERVER_SAYS + value);
        System.out.flush();
    }

    private String readLine() throws IOException {
        String line = in.readLine();

        while (line != null && !line.startsWith(Protocol.CLIENT_SAYS)) {
            System.err.println("server ignoring line: " + line);
            line = in.readLine();
        }

        return line.substring(Protocol.CLIENT_SAYS.length());
    }

    private int readIntLine() throws IOException {
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

    public RemoteHandler(Server server) {
        this.server = server;
    }

    private void handleGetLocalAddress() throws IOException {
        println("OK");
        println(server.getLocalAddress());
    }

    private void handleAddHub() throws IOException {
        String hub = readLine();

        server.addHubs(hub);

        println("OK");
    }

    private void handleGetHubs() throws IOException {
        String[] hubs = server.getHubs();

        println("OK");
        println(hubs.length);
        for (String hub : hubs) {
            println(hub);
        }
    }

    private void handleGetServiceNames() throws IOException {
        String[] services = server.getServiceNames();

        println("OK");
        println(services.length);
        for (String service : services) {
            println(service);
        }
    }

    private void handleGetStatistics() throws IOException {
        String serviceName = readLine();

        Map<String, String> statistics = server.getStats(serviceName);

        if (statistics == null) {
            println("Could not find service: " + serviceName);
            return;
        }

        println("OK");

        println(statistics.size());
        for (Map.Entry<String, String> entry : statistics.entrySet()) {
            println(entry.getKey());
            println(entry.getValue()); // may print "null"
        }
    }

    private void handleEnd() throws IOException {
        String timeoutString = readLine();

        try {
            long timeout = Long.parseLong(timeoutString);

            server.end(timeout);

            println("OK");
        } catch (NumberFormatException e) {
            throw new IOException("error parsing long: " + e);
        }
    }

    public void run() {
        logger.debug("running remote handler");

        while (true) {
            try {
                String opcode = readLine();

                if (opcode == null) {
                    System.err.println("input stream closed, stopping server");
                    server.end(-1);
                    return;
                }

                logger.debug("got opcode: " + opcode);
                if (opcode.equals(Protocol.OPCODE_GET_LOCAL_ADDRESS)) {
                    handleGetLocalAddress();
                } else if (opcode.equals(Protocol.OPCODE_ADD_HUB)) {
                    handleAddHub();
                } else if (opcode.equals(Protocol.OPCODE_GET_HUBS)) {
                    handleGetHubs();
                } else if (opcode.equals(Protocol.OPCODE_GET_SERVICE_NAMES)) {
                    handleGetServiceNames();
                } else if (opcode.equals(Protocol.OPCODE_GET_STATISTICS)) {
                    handleGetStatistics();
                } else if (opcode.equals(Protocol.OPCODE_END)) {
                    handleEnd();
                    return;
                } else {
                    System.err.println("unknown opcode: " + opcode);
                }
            } catch (Exception e) {
                logger.error("error on handling remote request (ignoring)", e);
            }

        }
    }
}
