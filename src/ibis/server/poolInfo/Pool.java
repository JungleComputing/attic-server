package ibis.server.poolInfo;

import java.util.ArrayList;

public final class Pool {

    private final String name;

    private final int size;

    private final boolean printEvents;

    private ArrayList<String> hostnames;

    private ArrayList<String> clusterNames;

    public Pool(String name, int size, boolean printEvents) {
        this.name = name;
        this.size = size;
        this.printEvents = printEvents;

        hostnames = new ArrayList<String>();
        clusterNames = new ArrayList<String>();

        if (printEvents) {
            System.out.println("PoolInfo: created new pool \"" + name
                    + "\" of size " + size);
        }
    }

    public synchronized int join(String hostName, String clusterName, int size) {
        if (size <= 0) {
            return Service.RESULT_INVALID_SIZE;
        }

        // pool is of different size
        if (size != this.size) {
            return Service.RESULT_UNEQUAL_SIZE;
        }

        // pool is full
        if (hostnames.size() >= size) {
            return Service.RESULT_POOL_CLOSED;
        }

        int rank = hostnames.size();
        hostnames.add(hostName);
        clusterNames.add(clusterName);

        if (hostnames.size() >= size) {
            notifyAll();
        }
        
        if (printEvents) {
            System.out.println("PoolInfo: \"" + hostName + "@" + clusterName
                    + "\" joins pool \"" + name + "\", rank " + rank + " of " + this.size);
        }

        // wait for everyone else to join too
        while (hostnames.size() < size) {
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }

        return rank;
    }

    public synchronized String[] getHostnames() {
        return hostnames.toArray(new String[0]);
    }

    public synchronized String[] getClusterNames() {
        return clusterNames.toArray(new String[0]);
    }

}
