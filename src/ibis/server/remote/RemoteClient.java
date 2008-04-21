package ibis.server.remote;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.log4j.Logger;

public class RemoteClient {
	
    private static final Logger logger = Logger.getLogger(RemoteClient.class);

	private final DataInputStream in;

	private final DataOutputStream out;

	private final PipedOutputStream pipeOut;

	private final PipedInputStream pipeIn;

	private boolean initialized = false;

	/**
	 * Connect to the server with the given in and output stream
	 * 
	 * @throws IOException
	 */
	public RemoteClient() throws IOException {
		pipeOut = new PipedOutputStream();
		in = new DataInputStream(new BufferedInputStream(new PipedInputStream(
				pipeOut)));

		pipeIn = new PipedInputStream();
		out = new DataOutputStream(new BufferedOutputStream(
				new PipedOutputStream(pipeIn)));

	}

	public synchronized void init() throws IOException {
		if (initialized) {
			return;
		}

		out.writeByte(Protocol.MAGIC);
		out.writeInt(Protocol.VERSION);
		out.flush();
		
		logger.debug("written magic/version, waiting for reply");

		byte reply = in.readByte();
		String message = in.readUTF();
		if (reply != Protocol.REPLY_OK) {
			throw new IOException(message);
		}

		initialized = true;
	}

	/**
	 * Returns the stream data from the server for this client can be written
	 * to.
	 * 
	 * @return the stream data from the server for this client can be written
	 *         to.
	 */
	public OutputStream getOutputStream() {
		return pipeOut;
	}

	/**
	 * Returns the stream which this client writes data for the server process
	 * to.
	 * 
	 * @return
	 */
	public InputStream getInputStream() {
		return pipeIn;
	}

	public synchronized void addHubs(String... hubs) throws IOException {
		init();

		out.writeByte(Protocol.OPCODE_ADD_HUBS);
		out.writeInt(hubs.length);
		for (String hub : hubs) {
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
		init();

		out.writeByte(Protocol.OPCODE_GET_HUBS);
		out.flush();

		byte reply = in.readByte();
		String message = in.readUTF();

		if (reply != Protocol.REPLY_OK) {
			throw new IOException(message);
		}

		int size = in.readInt();
		String[] result = new String[size];
		for (int i = 0; i < size; i++) {
			result[i] = in.readUTF();
		}

		return result;
	}

	public synchronized String getLocalAddress() throws IOException {
		init();

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
		init();

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
