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
        // TODO Auto-generated method stub
        return 0;
    }

    public synchronized String[] getHostnames() {
        return hostnames.toArray(new String[0]);
    }

    public synchronized String[] getClusterNames() {
        return clusterNames.toArray(new String[0]);
    }

}
