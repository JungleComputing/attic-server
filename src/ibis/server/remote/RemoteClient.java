package ibis.server.remote;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RemoteClient {
    
    private final DataInputStream in;
    private final DataOutputStream out;

    /**
     * Connect to the server with the given in and output stream
     * @throws IOException 
     */
    public RemoteClient(OutputStream stdin, InputStream stdout, InputStream stderr) throws IOException {
        this.in = new DataInputStream(new BufferedInputStream(stdout));
        this.out = new DataOutputStream(new BufferedOutputStream(stdin));
        
        new OutputForwarder(stderr, System.err);
        
        out.writeByte(Protocol.MAGIC);
        out.writeInt(Protocol.VERSION);
        out.flush();
        
        byte reply = in.readByte();
        String message = in.readUTF();
        if (reply != Protocol.REPLY_OK) {
            throw new IOException(message);
        }
    }

    public synchronized void addHubs(String... hubs) throws IOException {
        out.writeByte(Protocol.OPCODE_ADD_HUBS);
        out.writeInt(hubs.length);
        for (String hub: hubs) {
            out.writeUTF(hub);
        }
        out.flush();
        byte reply = in.readByte();
        String message = in.readUTF();
        
        if (reply != Protocol.REPLY_OK) {
            throw new IOException(message);
        }
    }

    public synchronized String[] getHubs() throws IOException {
        out.writeByte(Protocol.OPCODE_GET_HUBS);
        out.flush();
        
        byte reply = in.readByte();
        String message = in.readUTF();
        
        if (reply != Protocol.REPLY_OK) {
            throw new IOException(message);
        }

        int size = in.readInt();
        String[] result = new String[size];
        for(int i = 0; i < size; i++) {
            result[i] = in.readUTF();
        }
        
        return result;
    }

    public synchronized String getLocalAddress() throws IOException {
        out.writeByte(Protocol.OPCODE_GET_LOCAL_ADDRESS);
        out.flush();
        
        byte reply = in.readByte();
        String message = in.readUTF();
        
        if (reply != Protocol.REPLY_OK) {
            throw new IOException(message);
        }
        
        return in.readUTF();
    }
    
    public synchronized void end(boolean waitUntilIdle) throws IOException {
        out.writeByte(Protocol.OPCODE_END);
        out.writeBoolean(waitUntilIdle);
        out.flush();
        
        byte reply = in.readByte();
        String message = in.readUTF();
        
        if (reply != Protocol.REPLY_OK) {
            throw new IOException(message);
        }
        in.close();
        out.close();
    }
}
