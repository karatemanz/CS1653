import java.net.ServerSocket;  // The server uses this to bind to a port	
import java.net.Socket;        // Incoming connections are represented as socketsimport java.io.ObjectInputStream;
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
			// Connect to the specified server
			final Socket sock = new Socket(args[0], EchoServer.SERVER_PORT);
			System.out.println("Connected to " + args[0] + " on port " + EchoServer.SERVER_PORT);
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
