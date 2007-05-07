package ibis.server.poolInfo;

import java.util.ArrayList;

public final class Pool {
    
    private int size;
    
    private ArrayList<String> hostnames;
    private ArrayList<String> clusterNames;
    
    public Pool() {
        size = -1;
        
        hostnames = new ArrayList<String>();
        clusterNames = new ArrayList<String>();
    }

    public synchronized int join(String hostName, String clusterName, int size) {
        if (size <= 0) {
            return Service.RESULT_INVALID_SIZE;
        }
        
        //set size if unset
        if (this.size == -1) {
            this.size = size;
        }
        
        //pool is of different size
        if (size != this.size) {
            return Service.RESULT_UNEQUAL_SIZE;
        }
        
        //pool is full
        if (hostnames.size() >= size) {
            return Service.RESULT_POOL_CLOSED;
        }
        
        
        int rank = hostnames.size();
        hostnames.add(hostName);
        clusterNames.add(clusterName);
        
        if (hostnames.size() >= size) {
            notifyAll();
        }
        
        //wait for everyone else to join too
        while (hostnames.size() < size) {
            try {
                wait();
            } catch (InterruptedException e) {
                //IGNORE
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
