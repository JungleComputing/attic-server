package ibis.server.remote;

public class Protocol {

    public static final byte MAGIC = 53;
    
    public static final byte VERSION = 2;
    
    public static final byte OPCODE_ADD_HUBS = 1;

    public static final byte OPCODE_GET_HUBS = 2;

    public static final byte OPCODE_GET_SERVICE_NAMES = 3;
    
    public static final byte OPCODE_GET_STATISTICS = 4;
    
    public static final byte OPCODE_END = 5;

    public static final byte REPLY_OK = 1;

    public static final byte REPLY_ERROR = 2;
    
    //string representation of null (since writeUTF does not send null)
    public static final String NULL_STRING = "<<<null>>>";
    
}
