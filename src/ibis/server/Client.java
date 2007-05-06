package ibis.server;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

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
    private static final Logger logger = Logger.getLogger(Client.class);

    private static VirtualSocketFactory factory = null;

    private static DirectSocketAddress createAddressFromString(
            String serverString, int defaultPort) throws IOException {
        if (serverString == null) {
            throw new IOException("serverString undefined");
        }

        // maybe it is a DirectSocketAddress?
        try {
            return DirectSocketAddress.getByAddress(serverString);
        } catch (IllegalArgumentException e) {
            logger.debug("could not create server address", e);
        }

        try {
            return DirectSocketAddress.getByAddress(serverString, defaultPort);
        } catch (Exception e) {
            logger.debug("could not create server address", e);
        }

        throw new IOException(
                "could not create server address from given string: "
                        + serverString);
    }

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

    public static synchronized VirtualSocketFactory getFactory(
            Properties properties) throws InitializationException {
        if (properties == null) {
            properties = new Properties();
        }
        if (factory == null) {
            Properties smartProperties = new Properties();
            smartProperties.put(SmartSocketsProperties.DISCOVERY_ALLOWED,
                    "false");
            String server = properties.getProperty(ServerProperties.ADDRESS);
            if (server != null) {
                String hubs = properties
                        .getProperty(ServerProperties.HUB_ADDRESSES);
                if (hubs == null) {
                    hubs = server;
                } else {
                    hubs = hubs + "," + server;
                }
                smartProperties.put(SmartSocketsProperties.HUB_ADDRESSES, hubs);
            }

            factory = VirtualSocketFactory.createSocketFactory(smartProperties,
                    true);
        }

        return factory;
    }

}
