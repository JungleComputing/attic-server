package ibis.server;

import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public final class ServerProperties {

    public static final String PREFIX = "ibis.server.";

    public static final String NAME = PREFIX + "name";

    public static final String HUBS = PREFIX + "hubs";

    public static final String PORT = PREFIX + "port";

    public static final String IMPL_PATH = PREFIX + "impl.path";

    public static final String EVENTS = PREFIX + "events";

    public static final String STATS = PREFIX + "stats";

    public static final String WARN = PREFIX + "warn";

    public static final String DEBUG = PREFIX + "debug";

    // client side properties

    public static final String ADDRESS = PREFIX + "address";

    public static final String DISCOVER = PREFIX + "discover";

    public static final String AUTOSTART = PREFIX + "autostart";

    private static final String[][] propertiesList = new String[][] {
            { NAME, null, "Name of the Ibis server, and all its hubs" },

            { HUBS, null, "Comma seperated list of addition hubs to connect to" },

            { PORT, "8888", "Port which the server binds to" },

            { IMPL_PATH, null, "Path used to find service implementations" },

            { EVENTS, "false",
                    "Boolean: if true, events of services are printed to the log." },
            { STATS, "false",
                    "Boolean: if true, statistics are printed to the log regularly." },
            { ADDRESS, null,
                    "Address of the server, or, if applicable, the local hub." },
            { DISCOVER, "false",
                    "Boolean: if true, try to locate the server using UDP if it " +
                    "was not specified using the ibis.server.address property." },
            { AUTOSTART, "false",
                    "Boolean: if true, a server will be automatically created " +
                    "if the server should be running on the local machine," +
                    " but is not." },

    };
    
    public static Properties getHardcodedProperties() {
        Properties properties = new Properties();

        for (String[] element : propertiesList) {
            if (element[1] != null) {
                properties.setProperty(element[0], element[1]);
            }
        }

        return properties;
    }
    
    public static Map<String, String> getDescriptions() {
        Map<String, String> result = new TreeMap<String, String>();

        for (String[] element : propertiesList) {
            result.put(element[0], element[2]);
        }

        return result;
    }

}
