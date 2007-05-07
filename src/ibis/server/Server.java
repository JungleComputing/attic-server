package ibis.server;

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
import ibis.smartsockets.virtual.VirtualSocketFactory;

public final class Server {

    private final VirtualSocketFactory virtualSocketFactory;

    private final ArrayList<Service> services;

    public Server(Properties properties, boolean addDefaultConfigProperties) throws Exception {

        // load properties from config files and such
        TypedProperties typedProperties = ServerProperties
                .getHardcodedProperties();
        
        if (addDefaultConfigProperties) {
            typedProperties.loadDefaultConfigProperties();
        }
  
        typedProperties.addProperties(properties);

        //Init ibis.server logger
        Logger logger = Logger.getLogger("ibis.server");
        Level level = Level.toLevel(typedProperties
                .getProperty(ServerProperties.LOG_LEVEL));
        Log.initLog4J(logger, level);

        if (logger.isInfoEnabled()) {
            TypedProperties serverProperties = typedProperties.filter("ibis.server");
            logger.info("Settings for server:\n" + serverProperties);
        }

        // create the virtual socket factory
        Properties smartProperties = new Properties();

        smartProperties.put(SmartSocketsProperties.DIRECT_PORT, typedProperties
                .getProperty(ServerProperties.PORT));

        String hubs = typedProperties
                .getProperty(ServerProperties.HUB_ADDRESSES);
        if (hubs != null) {
            smartProperties.put(SmartSocketsProperties.HUB_ADDRESSES, hubs);
        }

        if (typedProperties.getBooleanProperty(ServerProperties.START_HUB)) {
            smartProperties.put(SmartSocketsProperties.START_HUB, "true");
            smartProperties.put(SmartSocketsProperties.HUB_DELEGATE, "true");
        }

        virtualSocketFactory = VirtualSocketFactory.createSocketFactory(
                smartProperties, true);

        // Obtain a list of Services
        String implPath = typedProperties
                .getProperty(ServerProperties.IMPL_PATH);
        ClassLister clstr = ClassLister.getClassLister(implPath);
        List<Class> compnts = clstr.getClassList("Ibis-Service", Service.class);
        Class[] serviceClassList = compnts.toArray(new Class[compnts.size()]);

        services = new ArrayList<Service>();

        for (int i = 0; i < serviceClassList.length; i++) {
            try {
                Service service = (Service) serviceClassList[i].getConstructor(
                        new Class[] { TypedProperties.class,
                                VirtualSocketFactory.class }).newInstance(
                        new Object[] { typedProperties, virtualSocketFactory });
                logger.debug("created new service: " + service);
                services.add(service);
            } catch (Throwable e) {
                logger.warn("Could not create service " + serviceClassList[i]
                        + ":", e);
            }
        }
    }

    /**
     * Returns the local address of this server as a string
     */
    public String getLocalAddress() {
        return virtualSocketFactory.getVirtualAddressAsString();
    }

    public String toString() {
        return "Ibis server on " + getLocalAddress();
    }

    /**
     * Stops all services
     */
    public void end() {
        for (Service service : services) {
            service.end();
        }
    }

    private static void printUsage(PrintStream out) {
        out.println("Start a server for Ibis.");
        out.println();
        out.println("USAGE: ibis-server [OPTIONS]");
        out.println();
        out.println("--no-hub\t\t\tDo not start a hub");
        out.println("--hub-addresses HUB[,HUB]\tAdditional hubs to connect to");
        out.println("--port PORT\t\t\tPort used for the server");

        out
                .println("PROPERTY=VALUE\t\t\tSet a property, as if it was set in a configuration");
        out.println("\t\t\t\tfile or as a System property.");
        out.println("Output Options:");
        out.println("--events\t\t\tPrint events");
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
            server.end();
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
            } else if (args[i].equalsIgnoreCase("--hub-addresses")) {
                i++;
                properties.setProperty(ServerProperties.HUB_ADDRESSES, args[i]);
            } else if (args[i].equalsIgnoreCase("--port")) {
                i++;
                properties.put(ServerProperties.PORT, args[i]);
            } else if (args[i].equalsIgnoreCase("--events")) {
                properties.setProperty(ServerProperties.EVENTS, "true");
            } else if (args[i].equalsIgnoreCase("--stats")) {
                properties.setProperty(ServerProperties.STATS, "true");
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
            System.out.println("Started " + server.toString());
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

        synchronized (server) {
            try {
                server.wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }

        // run server until completion, then exit
        // server.run();
    }

}
