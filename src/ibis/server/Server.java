package ibis.server;

import ibis.util.ClassLister;
import ibis.util.Log;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import smartsockets.virtual.InitializationException;
import smartsockets.virtual.VirtualSocketFactory;

public final class Server {

    // NOT the name of this class but the entire registry package instead
    private static final Logger logger = Logger.getLogger("ibis.server");

    private final VirtualSocketFactory virtualSocketFactory;

    private final ArrayList<Service> services;

    public Server(Properties properties) throws Exception {

        // create the virtual socket factory

        Properties smartProperties = new Properties();
        smartProperties.setProperty(smartsockets.Properties.DIRECT_PORT,
                properties.getProperty(ServerProperties.PORT));

        virtualSocketFactory = VirtualSocketFactory.createSocketFactory(
                smartProperties, true);

        // Obtain a list of Services
        String implPath = properties.getProperty(ServerProperties.IMPL_PATH);
        ClassLister clstr = ClassLister.getClassLister(implPath);
        List<Class> compnts = clstr.getClassList("Ibis-Service", Service.class);
        Class[] serviceClassList = compnts.toArray(new Class[compnts.size()]);

        services = new ArrayList<Service>();

        for (int i = 0; i < serviceClassList.length; i++) {
            try {
                Service service = (Service) serviceClassList[i].getConstructor(
                        new Class[] { Properties.class,
                                VirtualSocketFactory.class }).newInstance(
                        new Object[] { properties, virtualSocketFactory });
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

    private static void printUsage(PrintStream out) {
        out.println("Start a server for Ibis.");
        out.println();
        out.println("USAGE: ibis-server [OPTIONS]");
        out.println();
        out.println("--name NAME\t\t\tSet the name of this server and its hub");
        out.println("--hubs HUB[,HUB]\t\tAdditional hubs to connect to");
        out.println("--port PORT\t\t\tPort used for the server");

        out
                .println("PROPERTY=VALUE\t\tSet a property, as if it was set in a configuration");
        out.println("\t\t\tfile or as a System property.");
        out.println();
        out
                .println("--events\t\t\tPrint events such as new pools/joins/leaves/etc");
        out.println("--stats\t\t\tPrint statistics once in a while");
        out.println("--warn\t\t\tOnly print warnings and errors, "
                + "no status messages or events or statistics");
        out.println("--debug\t\t\tPrint debug output.");
        out.println("--help | -h | /?\tThis message.");
    }

    /**
     * Run the ibis server
     */
    public static void main(String[] args) {
        Properties properties = new Properties();

        // add an appender to this package if needed
        Log.initLog4J(logger);
        Level logLevel = Level.INFO;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--name")) {
                i++;
                properties.setProperty(ServerProperties.NAME, args[i]);
            } else if (args[i].equalsIgnoreCase("--hubs")) {
                i++;
                properties.setProperty(ServerProperties.HUBS, args[i]);
            } else if (args[i].equalsIgnoreCase("--port")) {
                i++;
                properties.put(ServerProperties.PORT, args[i]);
            } else if (args[i].equalsIgnoreCase("--events")) {
                properties.setProperty(ServerProperties.EVENTS, "true");
            } else if (args[i].equalsIgnoreCase("--stats")) {
                properties.setProperty(ServerProperties.STATS, "true");

            } else if (args[i].equalsIgnoreCase("--warn")) {
                logLevel = Level.WARN;
            } else if (args[i].equalsIgnoreCase("--debug")) {
                logLevel = Level.DEBUG;
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

        logger.setLevel(logLevel);

        Server server = null;
        try {
            server = new Server(properties);
            logger.info("Started " + server.toString());
        } catch (Throwable t) {
            logger.error("Could not start Server", t);
            System.exit(1);
        }
        
        // run server until completion, then exit
      //  server.run();
    }

}
