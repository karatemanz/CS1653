/* This list represents the file keys for groups on the server */
import java.util.*;
import java.security.*;
import javax.crypto.*;

public class KeyTable implements java.io.Serializable {
	
	private static final long serialVersionUID = 6700343803563417992L;
	private Hashtable<String, ArrayList<Key>> keyList = new Hashtable<String, ArrayList<Key>>();
	
	public synchronized void addGroup(String groupName, Key firstKey) {
		ArrayList<Key> list = new ArrayList<Key>();
		list.add(firstKey);
		keyList.put(groupName, list);
	}
	
	public synchronized void addKey(String groupName, Key newKey) {
		ArrayList<Key> list = keyList.get(groupName);
		list.add(newKey);
		keyList.put(groupName, list);
	}
	
	public synchronized ArrayList<Key> getKeys(String groupName) {
		return keyList.get(groupName);
	}
	
	public synchronized void removeGroup(String groupName) {
		keyList.remove(groupName);
	}
}	
