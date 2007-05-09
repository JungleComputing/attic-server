package ibis.server.poolInfo;

import ibis.util.TypedProperties;

import java.util.Map;
import java.util.TreeMap;

public final class PoolInfoProperties {
    
    public static final String IBIS_PREFIX = "ibis.";
    
    public static final String HOSTNAME = IBIS_PREFIX + "hostname";
    
    public static final String CLUSTER = IBIS_PREFIX + "cluster";

    public static final String POOL_PREFIX = "ibis.pool.";

    public static final String POOL_NAME = POOL_PREFIX + "name";

    public static final String POOL_SIZE = POOL_PREFIX + "size";

    private static final String[][] propertiesList = new String[][] {
            { POOL_NAME, null, "Name of the pool" },
            

            { POOL_SIZE, null, "Int: size of the pool" },
            
            { HOSTNAME, null, "Hostname to use as identifier. Default is FQDN of this machine"},
            
            { CLUSTER, "unknown", "Cluster of the local machine"},
    
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