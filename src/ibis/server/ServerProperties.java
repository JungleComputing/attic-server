package ibis.server;

import ibis.util.TypedProperties;

import java.util.Map;
import java.util.TreeMap;

public final class ServerProperties {

    public static final String PREFIX = "ibis.server.";

    public static final String HUB_ADDRESSES = PREFIX + "hub.addresses";
    
    public static final String START_HUB = PREFIX + "start.hub";

    public static final String PORT = PREFIX + "port";

    public static final String IMPL_PATH = PREFIX + "impl.path";

    public static final String LOG_EVENTS = PREFIX + "log.events";

    public static final String LOG_STATS = PREFIX + "log.stats";
    
    public static final String LOG_LEVEL = PREFIX + "log.level";

    // client side properties

    public static final String ADDRESS = PREFIX + "address";

    private static final String[][] propertiesList = new String[][] {
            { HUB_ADDRESSES, null, "Comma seperated list of hubs." },

            { START_HUB, "true", "Boolean: if true, also start a hub at the server" },
            
            { PORT, "8888", "Port which the server binds to" },

            { IMPL_PATH, null, "Path used to find service implementations" },

            { LOG_EVENTS, "false",
                    "Boolean: if true, events of services are printed to the log." },
            { LOG_STATS, "false",
                    "Boolean: if true, statistics are printed to the log regularly." },
            { LOG_LEVEL, "INFO",
                    "determines level of verbosity (TRACE,DEBUG,INFO,WARN,ERROR or FATAL) of the server log" },
            { ADDRESS, null,
                    "Address of the server" },

    };
    
    public static TypedProperties getHardcodedProperties() {
        TypedProperties properties = new TypedProperties();

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
