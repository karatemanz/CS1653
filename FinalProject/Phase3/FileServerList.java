import java.util.*;
import java.security.*;

	public class FileServerList implements java.io.Serializable {
		private static final long serialVersionUID = 5634179927600343803L;
		public ArrayList<FileServerID> fileServerList;

		public FileServerList() {
			fileServerList = new ArrayList<FileServerID>();
		}

		public synchronized void addServer(FileServerID fsid) {
			fileServerList.add(fsid);
		}

		public synchronized boolean hasServer(FileServerID fsid) {
			if (fileServerList.contains(fsid)) {
				return true;
			}
			else {
				return false;
			}
		}
	}

	class FileServerID implements java.io.Serializable {
		private static final long serialVersionUID = -3998215986699986336L;
		public String address;
		public int port;
		public PublicKey key;
		
		public FileServerID(String _address, int _port, PublicKey _key) {
			address = _address;
			port = _port;
			key = _key;
		}
	}

