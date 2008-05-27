package ibis.server.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import ibis.server.Server;

public class RemoteHandler implements Runnable {

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

    private String[] readLine() throws IOException {
        String line = in.readLine();

        if (line == null) {
            return null;
        }

        String[] result = line.split(" ");

        if (!result[0].equals(Protocol.CLIENT_SAYS)) {
            throw new IOException("line did not start with "
                    + Protocol.CLIENT_SAYS + " as expected. Line: " + line);
        }

        return result;
    }

    public RemoteHandler(Server server) {
        this.server = server;
    }

    private void handleGetLocalAddress() throws IOException {
        println("OK " + server.getLocalAddress());
    }

    private void handleAddHub(String[] command) throws IOException {
        if (command.length < 3) {
            println("hub not given");
            return;
        }

        String hub = command[2];

        server.addHubs(hub);

        println("OK");
    }

    private void handleGetHubs() throws IOException {
        String[] hubs = server.getHubs();

        String reply = "OK " + hubs.length;

        for (String hub : hubs) {
            reply = reply + " " + hub;
        }
        println(reply);
    }

    private void handleGetServiceNames() throws IOException {
        String[] services = server.getServiceNames();

        String reply = "OK" + services.length;
        for (String service : services) {
            reply = reply + " " + service;
        }
        println(reply);
    }

    private void handleGetStatistics(String[] command) throws IOException {
        if (command.length < 3) {
            println("service name not given");
            return;
        }

        String serviceName = command[2];

        Map<String, String> statistics = server.getStats(serviceName);

        if (statistics == null) {
            println("Could not find service: " + serviceName);
            return;
        }

        String reply = "OK " + statistics.size();

        for (Map.Entry<String, String> entry : statistics.entrySet()) {
            reply += " " + entry.getKey();
            reply += " " + entry.getValue(); // may print "null"
        }

        println(reply);
    }

    private void handleEnd(String[] command) throws IOException {
        if (command.length < 3) {
            println("timeout not given");
            return;
        }

        String timeoutString = command[2];

        try {
            long timeout = Long.parseLong(timeoutString);

            server.end(timeout);

            println("OK");
        } catch (NumberFormatException e) {
            throw new IOException("error parsing long: " + e);
        }
    }

    public void run() {
        System.err.println("starting remote handler");

        while (true) {
            try {
                String[] command = readLine();

                if (command == null) {
                    System.err.println("input stream closed, stopping server");
                    server.end(-1);
                    return;
                }

                if (command.length < 2) {
                    println("command not given");
                } else {

                    System.err.println("got command: " + command[0]);
                    if (command[1].equals(Protocol.OPCODE_GET_LOCAL_ADDRESS)) {
                        handleGetLocalAddress();
                    } else if (command[1].equals(Protocol.OPCODE_ADD_HUB)) {
                        handleAddHub(command);
                    } else if (command[1].equals(Protocol.OPCODE_GET_HUBS)) {
                        handleGetHubs();
                    } else if (command[1].equals(Protocol.OPCODE_GET_SERVICE_NAMES)) {
                        handleGetServiceNames();
                    } else if (command[1].equals(Protocol.OPCODE_GET_STATISTICS)) {
                        handleGetStatistics(command);
                    } else if (command[1].equals(Protocol.OPCODE_END)) {
                        handleEnd(command);
                        return;
                    } else {
                        System.err.println("unknown command: " + command[1]);
                    }
                }
            } catch (Exception e) {
                System.err
                        .println("error on handling remote request (ignoring)");
                e.printStackTrace(System.err);
            }

        }
    }
}
