package ibis.server;

import ibis.ipl.IbisProperties;

import ibis.util.ClassLister;
import ibis.util.Log;
import ibis.util.TypedProperties;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ibis.smartsockets.SmartSocketsProperties;
import ibis.smartsockets.direct.DirectSocketAddress;
import ibis.smartsockets.hub.Hub;
import ibis.smartsockets.virtual.VirtualSocketFactory;

public final class Server {

    private final VirtualSocketFactory virtualSocketFactory;

    private final Hub hub;

    private final DirectSocketAddress address;

    private final ArrayList<Service> services;

    private final boolean hubOnly;

    public Server(Properties properties, boolean addDefaultConfigProperties)
            throws Exception {
        services = new ArrayList<Service>();

        // load properties from config files and such
        TypedProperties typedProperties = ServerProperties
                .getHardcodedProperties();

        if (addDefaultConfigProperties) {
            typedProperties.addProperties(IbisProperties.getDefaultProperties());
        }

        typedProperties.addProperties(properties);

        // Init ibis.server logger
        Logger logger = Logger.getLogger("ibis.server");
        Level level = Level.toLevel(typedProperties
                .getProperty(ServerProperties.LOG_LEVEL));
        Log.initLog4J(logger, level);

        if (logger.isDebugEnabled()) {
            TypedProperties serverProperties = typedProperties
                    .filter("ibis.server");
            logger.debug("Settings for server:\n" + serverProperties);
        }

        // create the virtual socket factory
        ibis.smartsockets.util.TypedProperties smartProperties = new ibis.smartsockets.util.TypedProperties();

        String hubs = typedProperties
                .getProperty(ServerProperties.HUB_ADDRESSES);
        if (hubs != null) {
            smartProperties.put(SmartSocketsProperties.HUB_ADDRESSES, hubs);
        }

        hubOnly = typedProperties.getBooleanProperty(ServerProperties.HUB_ONLY);

        if (hubOnly) {
            virtualSocketFactory = null;

            smartProperties.put(SmartSocketsProperties.HUB_PORT,
                    typedProperties.getProperty(ServerProperties.PORT));

            hub = new Hub(smartProperties);
            address = hub.getHubAddress();

        } else {
            hub = null;

            smartProperties.put(SmartSocketsProperties.DIRECT_PORT,
                    typedProperties.getProperty(ServerProperties.PORT));

            if (typedProperties.getBooleanProperty(ServerProperties.START_HUB)) {
                smartProperties.put(SmartSocketsProperties.START_HUB, "true");
                smartProperties
                        .put(SmartSocketsProperties.HUB_DELEGATE, "true");
            }

            virtualSocketFactory = VirtualSocketFactory.createSocketFactory(
                    smartProperties, true);
            address = virtualSocketFactory.getLocalHost();

            // Obtain a list of Services
            String implPath = typedProperties
                    .getProperty(ServerProperties.IMPL_PATH);
            ClassLister clstr = ClassLister.getClassLister(implPath);
            List<Class> compnts = clstr.getClassList("Ibis-Service",
                    Service.class);
            Class[] serviceClassList = compnts
                    .toArray(new Class[compnts.size()]);

            for (int i = 0; i < serviceClassList.length; i++) {
                try {
                    Service service = (Service) serviceClassList[i]
                            .getConstructor(
                                    new Class[] { TypedProperties.class,
                                            VirtualSocketFactory.class })
                            .newInstance(
                                    new Object[] { typedProperties,
                                            virtualSocketFactory });
                    services.add(service);
                } catch (Throwable e) {
                    logger.warn("Could not create service "
                            + serviceClassList[i] + ":", e);
                }
            }
        }
    }

    /**
     * Returns the local address of this server as a string
     */
    public String getLocalAddress() {
        return address.toString();
    }

    public DirectSocketAddress[] getHubs() {
        if (hubOnly) {
            return hub.knownHubs();
        } else {
            return virtualSocketFactory.getKnownHubs();
        }
    }

    public String toString() {
        if (hubOnly) {
            return "Hub running on " + getLocalAddress();
        }

        String message = "Ibis server running on " + getLocalAddress()
                + "\nList of Services:";

        for (Service service : services) {
            message += "\n    " + service.toString();
        }

        return message;

    }

    /**
     * Stops all services
     */
    public void end(boolean waitUntilIdle) {
        for (Service service : services) {
            service.end(waitUntilIdle);
        }
    }

    private static void printUsage(PrintStream out) {
        out.println("Start a server for Ibis.");
        out.println();
        out.println("USAGE: ibis-server [OPTIONS]");
        out.println();
        out.println("--no-hub\t\t\tDo not start a hub");
        out
                .println("--hub-only\t\t\tOnly start a hub, not the rest of the server");
        out.println("--hub-addresses HUB[,HUB]\tAdditional hubs to connect to");
        out.println("--port PORT\t\t\tPort used for the server");
        out.println();
        out
                .println("PROPERTY=VALUE\t\t\tSet a property, as if it was set in a configuration");
        out.println("\t\t\t\tfile or as a System property.");
        out.println("Output Options:");
        out.println("--events\t\t\tPrint events");
        out.println("--errors\t\t\tPrint details of errors (such as stacktraces)");
        out.println("--stats\t\t\t\tPrint statistics once in a while");
        out.println("--warn\t\t\t\tOnly print warnings and errors, "
                + "no status messages or events or statistics");
        out.println("--debug\t\t\t\tPrint debug output.");
        out.println("--help | -h | /?\t\tThis message.");
    }

    private static class Shutdown extends Thread {
        private final Server server;

        Shutdown(Server server) {
            this.server = server;
        }

        public void run() {
            server.end(false);
        }
    }

    /**
     * Run the ibis server
     */
    public static void main(String[] args) {
        Properties properties = new Properties();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--no-hub")) {
                properties.setProperty(ServerProperties.START_HUB, "false");
            } else if (args[i].equalsIgnoreCase("--hub-only")) {
                properties.setProperty(ServerProperties.HUB_ONLY, "true");
            } else if (args[i].equalsIgnoreCase("--hub-addresses")) {
                i++;
                properties.setProperty(ServerProperties.HUB_ADDRESSES, args[i]);
            } else if (args[i].equalsIgnoreCase("--port")) {
                i++;
                properties.put(ServerProperties.PORT, args[i]);
            } else if (args[i].equalsIgnoreCase("--events")) {
                properties.setProperty(ServerProperties.PRINT_EVENTS, "true");
            } else if (args[i].equalsIgnoreCase("--errors")) {
                properties.setProperty(ServerProperties.PRINT_ERRORS, "true");
            } else if (args[i].equalsIgnoreCase("--stats")) {
                properties.setProperty(ServerProperties.PRINT_STATS, "true");
            } else if (args[i].equalsIgnoreCase("--warn")) {
                properties.setProperty(ServerProperties.LOG_LEVEL, "WARN");
            } else if (args[i].equalsIgnoreCase("--debug")) {
                properties.setProperty(ServerProperties.LOG_LEVEL, "DEBUG");
            } else if (args[i].equalsIgnoreCase("--help")
                    || args[i].equalsIgnoreCase("-h")
                    || args[i].equalsIgnoreCase("/?")) {
                printUsage(System.out);
                System.exit(0);
            } else if (args[i].contains("=")) {
                String[] parts = args[i].split("=", 2);
                properties.setProperty(parts[0], parts[1]);
            } else {
                System.err.println("Unknown argument: " + args[i]);
                printUsage(System.err);
                System.exit(1);
            }
        }

        Server server = null;
        try {
            server = new Server(properties, true);
            System.out.println(server.toString());
        } catch (Throwable t) {
            System.err.println("Could not start Server: " + t);
            System.exit(1);
        }

        // register shutdown hook
        try {
            Runtime.getRuntime().addShutdownHook(new Shutdown(server));
        } catch (Exception e) {
            // IGNORE
        }

        String knownHubs = null;
        while (true) {
            DirectSocketAddress[] hubs = server.getHubs();
            //FIXME: remove if smartsockets promises to not return null ;)
            if (hubs == null) {
                hubs = new DirectSocketAddress[0];
            }

            if (hubs.length != 0) {
                String newKnownHubs = hubs[0].toString();
                for (int i = 1; i < hubs.length; i++) {
                    newKnownHubs += "," + hubs[i].toString();
                }

                if (!newKnownHubs.equals(knownHubs)) {
                    knownHubs = newKnownHubs;
                    System.out.println("Known hubs now: " + knownHubs);
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

}
