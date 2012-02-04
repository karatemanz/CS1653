import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public abstract class Client {

	/* protected keyword is like private but subclasses have access
	 * Socket and input/output streams
	 */
	protected Socket sock;
	protected ObjectOutputStream output;
	protected ObjectInputStream input;

	public boolean connect(final String server, final int port) {
		System.out.println("attempting to connect");

		/* TODO: Write this method */
		try{
			// This is basically just listens for new client connections
			final ServerSocket serverSock = new ServerSocket(SERVER_PORT);
			
			// A simple infinite loop to accept connections
			Socket sock = null;
			EchoThread thread = null;
			while(true){
				sock = serverSock.accept();     // Accept an incoming connection
				thread = new EchoThread(sock);  // Create a thread to handle this connection
				thread.start();                 // Fork the thread
			}                                   // Loop to work on new connections while this
			// the accept()ed connection is handled
		}
		catch(Exception e){
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}

	public boolean isConnected() {
		if (sock == null || !sock.isConnected()) {
			return false;
		}
		else {
			return true;
		}
	}

	public void disconnect()	 {
		if (isConnected()) {
			try
			{
				Envelope message = new Envelope("DISCONNECT");
				output.writeObject(message);
			}
			catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
			}
		}
	}
}
