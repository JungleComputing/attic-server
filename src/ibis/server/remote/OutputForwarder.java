package ibis.server.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class OutputForwarder implements Runnable {

    private final InputStream in;
    private final OutputStream out;

    public OutputForwarder(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    public void run() {
        byte[] buffer = new byte[1024];

        try {
            while (true) {
                int read;
                read = in.read(buffer);

                if (read == -1) {
                    return;
                }

                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            // IGNORE
        }
    }

}
