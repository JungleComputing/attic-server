package ibis.server.poolInfo;

import ibis.util.TypedProperties;

import java.util.Map;
import java.util.TreeMap;

public final class PoolInfoProperties {

    public static final String PREFIX = "ibis.pool.";

    public static final String NAME = PREFIX + "name";

    public static final String SIZE = PREFIX + "size";

    private static final String[][] propertiesList = new String[][] {
            { NAME, null, "Name of the pool" },

            { SIZE, null, "Int: size of the pool" }, };

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
