package ibis.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ibis.smartsockets.SmartSocketsProperties;
import ibis.smartsockets.direct.DirectSocketAddress;
import ibis.smartsockets.virtual.InitializationException;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.TypedProperties;

/**
 * Convenience class to retrieve information on the server, and create a
 * suitable VirtualSocketFactory.
 */
public class Client {
    private static VirtualSocketFactory defaultFactory = null;

    private static Map<String, VirtualSocketFactory> factories = new HashMap<String, VirtualSocketFactory>();

    private Client() {
        // DO NOT USE
    }

    private static DirectSocketAddress createAddressFromString(
            String serverString, int defaultPort) throws IOException {
        if (serverString == null) {
            throw new IOException("serverString undefined");
        }

        // maybe it is a DirectSocketAddress?
        try {
            return DirectSocketAddress.getByAddress(serverString);
        } catch (IllegalArgumentException e) {
            // IGNORE
        }

        try {
            return DirectSocketAddress.getByAddress(serverString, defaultPort);
        } catch (Exception e) {
            // IGNORE
        }

        throw new IOException(
                "could not create server address from given string: "
                        + serverString);
    }

    /**
     * Get the address of a service running on a given port
     * 
     * @param port
     *            the port the service is running on
     * @param properties
     *            object containing any server properties needed (such as the
     *            servers address)
     */
    public static VirtualSocketAddress getServiceAddress(int port,
            Properties properties) throws IOException {
        TypedProperties typedProperties = ServerProperties
                .getHardcodedProperties();
        typedProperties.addProperties(properties);

        String serverAddressString = typedProperties
                .getProperty(ServerProperties.ADDRESS);
        if (serverAddressString == null) {
            throw new IOException(ServerProperties.ADDRESS
                    + " undefined, cannot locate server");
        }

        int defaultPort = typedProperties.getIntProperty(ServerProperties.PORT);

        DirectSocketAddress serverMachine = createAddressFromString(
                serverAddressString, defaultPort);

        return new VirtualSocketAddress(serverMachine, port);
    }

    public static synchronized VirtualSocketFactory getFactory(Properties p)
            throws InitializationException, IOException {
        TypedProperties typedProperties = ServerProperties
                .getHardcodedProperties();
        typedProperties.addProperties(p);

        String hubs = typedProperties
                .getProperty(ServerProperties.HUB_ADDRESSES);

        String server = typedProperties.getProperty(ServerProperties.ADDRESS);
        if (server != null) {
            DirectSocketAddress serverAddress = createAddressFromString(server,
                    typedProperties.getIntProperty(ServerProperties.PORT));
            if (hubs == null) {
                hubs = serverAddress.toString();
            } else {
                hubs = hubs + "," + serverAddress.toString();
            }
        }

        if (hubs == null) {
            // return the default factory

            if (defaultFactory == null) {
                Properties smartProperties = new Properties();
                smartProperties.put(SmartSocketsProperties.DISCOVERY_ALLOWED,
                        "false");
                defaultFactory = VirtualSocketFactory.createSocketFactory(
                        smartProperties, true);
            }
            return defaultFactory;
        }

        VirtualSocketFactory factory = factories.get(hubs);

        if (factory == null) {
            // return a factory for the specified "hubs" string
            Properties smartProperties = new Properties();
            smartProperties.put(SmartSocketsProperties.DISCOVERY_ALLOWED,
                    "false");
            smartProperties.put(SmartSocketsProperties.HUB_ADDRESSES, hubs);

            factory = VirtualSocketFactory.createSocketFactory(smartProperties,
                    true);

            factories.put(hubs, factory);
        }

        return factory;
    }

}
