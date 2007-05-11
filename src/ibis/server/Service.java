package ibis.server;


/**
 * Interface for an Ibis Service. Any service which
 *  want to be automatiscally started by the ibis-server, needs to implement
 *  this interface. It should also have a constructor: 
 *  Service(TypedProperties properties, VirtualSocketFactory factory)
 */
public interface Service {
    
    
    /**
     * Called when the server stops.
     */
    void end(boolean waitUntilIdle);
    
}
