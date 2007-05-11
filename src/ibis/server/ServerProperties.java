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

    public static final String PRINT_EVENTS = PREFIX + "print.events";

    public static final String PRINT_STATS = PREFIX + "print.stats";
    
    public static final String LOG_LEVEL = PREFIX + "log.level";

    // client side properties

    public static final String ADDRESS = PREFIX + "address";

    private static final String[][] propertiesList = new String[][] {
            { HUB_ADDRESSES, null, "Comma seperated list of hubs." },

            { START_HUB, "true", "Boolean: if true, also start a hub at the server" },
            
            { PORT, "8888", "Port which the server binds to" },

            { IMPL_PATH, null, "Path used to find service implementations" },

            { PRINT_EVENTS, "false",
                    "Boolean: if true, events of services are printed to standard out." },
            { PRINT_STATS, "false",
                    "Boolean: if true, statistics are printed to standard out regularly." },
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
