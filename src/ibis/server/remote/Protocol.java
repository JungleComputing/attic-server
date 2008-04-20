package ibis.server.remote;

public class Protocol {

    public static final byte MAGIC = 53;
    
    public static final byte VERSION = 1;
    
    public static final byte OPCODE_ADD_HUBS = 1;

    public static final byte OPCODE_GET_HUBS = 2;

    public static final byte OPCODE_GET_LOCAL_ADDRESS = 3;

    public static final byte OPCODE_END = 4;

    public static final byte REPLY_OK = 1;

    public static final byte REPLY_ERROR = 2;
    
}
