/* This thread does all the work. It communicates with the client through Envelopes.
 * 
 */
import java.lang.Thread;
import java.net.Socket;
import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class GroupThread extends Thread 
{
	private final Socket socket;
	private GroupServer my_gs;
	private Key sessionKeyEnc;
	private Key sessionKeyAuth;
	private PrivateKey privateKey;
	private int sequence;
	
	public GroupThread(Socket _socket, GroupServer _gs, PrivateKey _pk)
	{
		socket = _socket;
		my_gs = _gs;
		privateKey = _pk;
	}
	
	public void run() {
		boolean proceed = true;

		try {
			//Announces connection and opens object streams
			System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
			final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			
			do {
				Envelope message = (Envelope)input.readObject();
				System.out.println("Request received: " + message.getMessage());
				Envelope response;
				
				// parse through publicly accessible messages
				if (message.getMessage().equals("GETPUBKEY")) { // Client wants the public key
					response = new Envelope("OK");
					response.addObject(my_gs.getServerPublicKey());
					output.writeObject(response);
				}
				else if (message.getMessage().equals("KCG")) { // Client wants a session key
					// Decrypt sealed object with private key
					SealedObject sealedObject = (SealedObject)message.getObjContents().get(0);
					String algo = sealedObject.getAlgorithm();
					Cipher cipher = Cipher.getInstance(algo);
					cipher.init(Cipher.DECRYPT_MODE, privateKey);
					// Get KeyPack challenge/key combo from sealedObject
					KeyPack kcg = (KeyPack)sealedObject.getObject(cipher);
					int challenge = kcg.getChallenge();
					sessionKeyEnc = kcg.getSecretKey();
					sessionKeyAuth = kcg.getHmacKey();
					// Get IV from message
					byte IVarray[] = (byte[])message.getObjContents().get(1);
					
					// Encryption of challenge response and sequence number
					Cipher theCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
					challenge += 1;
					SecureRandom seqRand = new SecureRandom();
					sequence = seqRand.nextInt(Integer.MAX_VALUE / 2);
					
					// Respond to the client
					response = new Envelope("OK");
					response.addObject(challenge);
					output.writeObject(encryptEnv(response));
				}
				else if (message.getMessage().equals("ENV")) { // encrypted Envelope
					// decrypt contents of encrypted Envelope and pass to branches below
					message = decryptEnv(message);
					System.out.println("ENV: " + message.getMessage());
					
					if (message.getMessage().equals("GET")) // Client wants a token
					{
						String username = (String)message.getObjContents().get(0); // Get the username
						char[] password = (char[])message.getObjContents().get(1);
						
						if (username == null) {
							response = new Envelope("FAIL");
							response.addObject(null);
							output.writeObject(encryptEnv(response));
						}
						else {
							Token yourToken = createToken(username, password); // Create a token
							// Respond to the client. On error, the client will receive a null token
							response = new Envelope("OK");
							response.addObject(yourToken);
							output.writeObject(encryptEnv(response));
						}
					}
					else if (message.getMessage().equals("GETGT")) // Client wants a group token for the File Server
					{
						Token token = (Token)message.getObjContents().get(0);
						String groupName = (String)message.getObjContents().get(1);
						String address = (String)message.getObjContents().get(2);
						int port = (Integer)message.getObjContents().get(3);
						
						if (token == null) {
							response = new Envelope("FAIL");
							response.addObject(null);
							output.writeObject(encryptEnv(response));
						}
						else {
							Token yourToken = createGroupToken(token, groupName, address, port);
							response = new Envelope("OK");
							response.addObject(yourToken);
							output.writeObject(encryptEnv(response));
						}
					}
					else if (message.getMessage().equals("GETKEYS")) // Client wants a group's file keys
					{
						Token token = (Token)message.getObjContents().get(0);
						if (token == null) {
							response = new Envelope("FAIL");
							response.addObject(null);
							output.writeObject(encryptEnv(response));
						}
						else {
							ArrayList<Key> keys = new ArrayList<Key>();
							keys = my_gs.keyTable.getKeys(token.getGroups().get(0));
							response = new Envelope("OK");
							response.addObject(keys);
							output.writeObject(encryptEnv(response));
						}
					}
					else if (message.getMessage().equals("CUSER")) //Client wants to create a user
					{
						if (message.getObjContents().size() < 3) {
							response = new Envelope("FAIL");
						}
						else {
							response = new Envelope("FAIL");
							
							if (message.getObjContents().get(0) != null &&
								message.getObjContents().get(1) != null &&
								message.getObjContents().get(2) != null) {
								
								String username = (String)message.getObjContents().get(0); // Extract the username
								char[] password = (char[])message.getObjContents().get(1); // Extract the password
								Token yourToken = (Token)message.getObjContents().get(2); // Extract the token
								
								// Check token's authenticity
								if (authToken(yourToken)) {
									if (createUser(username, password, yourToken)) {
										response = new Envelope("OK"); // Success
									}
								}
							}
						}
						output.writeObject(encryptEnv(response));
					}
					else if (message.getMessage().equals("DUSER")) { //Client wants to delete a user
						if (message.getObjContents().size() < 2) {
							response = new Envelope("FAIL");
						}
						else
						{
							response = new Envelope("FAIL");
							
							if (message.getObjContents().get(0) != null)
							{
								if (message.getObjContents().get(1) != null) {
									String username = (String)message.getObjContents().get(0); // Extract the username
									Token yourToken = (Token)message.getObjContents().get(1); // Extract the token
									
									// Check token's authenticity
									if (authToken(yourToken)) {
										if(deleteUser(username, yourToken)) {
											response = new Envelope("OK"); //Success
										}
									}
								}
							}
						}
						output.writeObject(encryptEnv(response));
					}
					else if (message.getMessage().equals("CGROUP")) { // Client wants to create a group
						if (message.getObjContents().size() < 2) {
							response = new Envelope("FAIL");
						}
						else {
							response = new Envelope("FAIL");
							
							if (message.getObjContents().get(0) != null) {
								if (message.getObjContents().get(1) != null) {
									String groupname = (String)message.getObjContents().get(0); // Extract the group name
									Token yourToken = (Token)message.getObjContents().get(1); // Extract the token
									
									// Check token's authenticity
									if (authToken(yourToken)) {
										if (createGroup(groupname, yourToken)) {
											response = new Envelope("OK"); // Success
										}
									}
								}
							}
						}						
						output.writeObject(encryptEnv(response));
					}
					else if (message.getMessage().equals("DGROUP")) { // Client wants to delete a group
						if (message.getObjContents().size() < 2) {
							response = new Envelope("FAIL");
						}
						else {
							response = new Envelope("FAIL");
							
							if (message.getObjContents().get(0) != null) {
								if (message.getObjContents().get(1) != null) {
									String groupname = (String)message.getObjContents().get(0); // Extract the group name
									Token yourToken = (Token)message.getObjContents().get(1); // Extract the token
									
									// Check token's authenticity
									if (authToken(yourToken)) {
										if (deleteGroup(groupname, yourToken)) {
											response = new Envelope("OK"); // Success
										}
									}
								}
							}
						}						
						output.writeObject(encryptEnv(response));
					}
					else if (message.getMessage().equals("LMEMBERS")) { //Client wants a list of members in a group
						String groupName = (String)message.getObjContents().get(0); //Get the groupName
						Token yourToken = (Token)message.getObjContents().get(1); //Extract the token
						
						if (groupName == null) {
							response = new Envelope("FAIL");
							response.addObject(null);
							output.writeObject(encryptEnv(response));
						}
						else {
							// Check token's authenticity
							if (authToken(yourToken)) {
								List<String> memberList = listMembers(groupName, yourToken);
								// Respond to the client. On error, the client will receive a null List
								response = new Envelope("OK");
								response.addObject(memberList);
								output.writeObject(encryptEnv(response));
							}
						}
					}
					else if(message.getMessage().equals("AUSERTOGROUP")) { // Client wants to add user to a group
						if (message.getObjContents().size() < 3) {
							response = new Envelope("FAIL");
						}
						else {
							response = new Envelope("FAIL");
							
							if (message.getObjContents().get(0) != null) {
								if (message.getObjContents().get(1) != null) {
									if (message.getObjContents().get(1) != null) {
										String username = (String)message.getObjContents().get(0); // Extract the username
										String groupname = (String)message.getObjContents().get(1); // Extract the group name
										Token yourToken = (Token)message.getObjContents().get(2); // Extract the token
										
										// Check token's authenticity
										if (authToken(yourToken)) {
											if (addUserToGroup(username, groupname, yourToken)) {
												response = new Envelope("OK"); // Success
											}
										}
									}
								}
							}
						}
						output.writeObject(encryptEnv(response));
					}
					else if (message.getMessage().equals("RUSERFROMGROUP")) { //Client wants to remove user from a group
						if (message.getObjContents().size() < 3) {
							response = new Envelope("FAIL");
						}
						else {
							response = new Envelope("FAIL");
							
							if(message.getObjContents().get(0) != null) {
								if(message.getObjContents().get(1) != null) {
									if (message.getObjContents().get(1) != null) {
										String username = (String)message.getObjContents().get(0); // Extract the username
										String groupname = (String)message.getObjContents().get(1); // Extract the group name
										Token yourToken = (Token)message.getObjContents().get(2); // Extract the token
										
										// Check token's authenticity
										if (authToken(yourToken)) {
											if (deleteUserFromGroup(username, groupname, yourToken)) {
												response = new Envelope("OK"); // Success
											}
										}
									}
								}
							}
						}
						output.writeObject(encryptEnv(response));
					}
					else if (message.getMessage().equals("DISCONNECT")) { // Client wants to disconnect
						socket.close(); // Close the socket
						proceed = false; // End this communication loop
					}
					else {
						response = new Envelope("FAIL"); //Server does not understand client request
						output.writeObject(encryptEnv(response));
					}
				}
				else if (message.getMessage().equals("DISCONNECT")) { // Client wants to disconnect
					socket.close(); // Close the socket
					proceed = false; // End this communication loop
				}
				else {  // Server does not understand client request
					response = new Envelope("FAIL");
					output.writeObject(encryptEnv(response));
					proceed = false;
				}
			} while (proceed);	
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}
	
	//Method to create tokens
	private Token createToken(String username, char[] password) {
		//Check that user exists
		if (my_gs.userList.checkUser(username)) {
			// verify password
			if (my_gs.comparePasswordHash(username, password)) {
				// Issue a new token with server's name, user's name, and user's groups
				Token yourToken = new Token(my_gs.name, username, my_gs.userList.getUserGroups(username));

				return my_gs.getSignedToken(yourToken);
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
	}
	
	// Method to create a signed token with a single group and the File Server ID set
	private Token createGroupToken(Token aToken, String groupName, String address, int port) {
		Token fsToken = new Token(aToken.getIssuer(),
								  aToken.getSubject(),
								  aToken.getGroups(),
								  address, port);
		fsToken.setGroup(groupName);
		return my_gs.getSignedToken(fsToken);								  
	}
	
	//Method to create a user
	private boolean createUser(String username, char[] password, UserToken yourToken)
	{
		String requester = yourToken.getSubject();
		
		//Check if requester exists
		if(my_gs.userList.checkUser(requester)) {
			//Get the user's groups
			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);
			//requester needs to be an administrator
			if (temp.contains("ADMIN")) {
				//Does user already exist?
				if (my_gs.userList.checkUser(username)) {
					return false; //User already exists
				}
				else {
					my_gs.userList.addUser(username);
					my_gs.userList.setUserHash(username, my_gs.getHash(password));
					return true;
				}
			}
			else {
				return false; //requester not an administrator
			}
		}
		else {
			return false; //requester does not exist
		}
	}
	
	//Method to delete a user
	private boolean deleteUser(String username, UserToken yourToken) {
		String requester = yourToken.getSubject();
		String aUser;
		ArrayList<String> groupList = new ArrayList<String>();
		
		//Does requester exist?
		if(my_gs.userList.checkUser(requester))
		{
			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);
			//requester needs to be an administer
			if(temp.contains("ADMIN"))
			{
				//Does user exist?
				if(my_gs.userList.checkUser(username))
				{
					//If groups are owned, they must be deleted
					ArrayList<String> deleteOwnedGroup = new ArrayList<String>();
					
					//Make a hard copy of the user's ownership list
					for(int index = 0; index < my_gs.userList.getUserOwnership(username).size(); index++)
					{
						deleteOwnedGroup.add(my_gs.userList.getUserOwnership(username).get(index));
					}
										
					//Delete owned groups
					for(int index = 0; index < deleteOwnedGroup.size(); index++)
					{
						//Use the delete group method. Token must be created for this action
						deleteGroup(deleteOwnedGroup.get(index), new Token(my_gs.name, username, deleteOwnedGroup));
					}
					
					//Delete the user from the user list
					my_gs.userList.deleteUser(username);
					
					return true;	
				}
				else {
					return false; //User does not exist
				}
			}
			else {
				return false; //requester is not an administer
			}
		}
		else {
			return false; //requester does not exist
		}
	}
	
	private boolean createGroup(final String groupname, final UserToken yourToken)
	{
		String requester = yourToken.getSubject();
		String otherUser;
		
		//Check if requester exists
		if(my_gs.userList.checkUser(requester))
		{
			// loop through users - if group name already owned, cannot create
			for (Enumeration<String> usernameList = my_gs.userList.getUsernames(); usernameList.hasMoreElements();)
			{
				otherUser = (usernameList.nextElement());
				
				if (my_gs.userList.getUserOwnership(otherUser).contains(groupname))
				{
					return false; // group name is taken
				}
			}
			
			// add group to ownership
			my_gs.userList.addOwnership(requester, groupname);
			
			// add group to groups
			my_gs.userList.addGroup(requester, groupname);
			
			// Add group and new key to KeyTable
			try {
				KeyGenerator keyGen = KeyGenerator.getInstance("AES", "BC");
				keyGen.init(128);
				my_gs.keyTable.addGroup(groupname, keyGen.generateKey());
			}
			catch (Exception ke) {
				System.out.println("Error creating Group File Key for Create New Group");
			}

			return true;
		}
		else {
			return false; //requester does not exist
		}
	}
	
	private boolean deleteGroup(String groupname, UserToken yourToken)
	{
		String requester = yourToken.getSubject();
		String otherUser;
		
		//Check if requester exists
		if(my_gs.userList.checkUser(requester))
		{
			// Check if requester owns group
			ArrayList<String> deleteOwnedGroup = new ArrayList<String>();
			
			for(int index = 0; index < my_gs.userList.getUserOwnership(requester).size(); index++)
			{
				deleteOwnedGroup.add(my_gs.userList.getUserOwnership(requester).get(index));
			}

			if (deleteOwnedGroup.contains(groupname))
			{
				// go through other users, remove this group from their list
				for (Enumeration<String> usernameList = my_gs.userList.getUsernames(); usernameList.hasMoreElements();)
				{
					otherUser = (usernameList.nextElement());
					
					for (int index = 0; index < deleteOwnedGroup.size(); index++)
					{
						if (my_gs.userList.getUserGroups(otherUser).contains(groupname));
						{
							// delete this group from the user's group list
							my_gs.userList.removeGroup(otherUser, groupname);
						}
					}
				}
				
				// remove this group from owner's lists
				my_gs.userList.removeGroup(requester, groupname);
				my_gs.userList.removeOwnership(requester, groupname);
				
				// remove the group and its keys from the KeyTable
				my_gs.keyTable.removeGroup(groupname);
				
				return true;
			}
			else {
				return false; // requester does not own this group
			}
		}		
		else {
			return false; //requester does not exist
		}
	}
	
	private List<String> listMembers(String group, UserToken yourToken)
	{
		String requester = yourToken.getSubject();
		String aUser = new String();
		List<String> aList = new ArrayList<String>();
		
		//Does requester exist?
		if(my_gs.userList.checkUser(requester)) {
			// Requester needs to the owner of the group requested
			if(my_gs.userList.getUserOwnership(requester).contains(group))
			{
				// get a list of usernames, loop through each
				for (Enumeration<String> usernameList = my_gs.userList.getUsernames(); usernameList.hasMoreElements();)
				{
					aUser = (usernameList.nextElement());
					
					if (my_gs.userList.getUserGroups(aUser).contains(group))
					{
						aList.add(aUser);
					}
				}
				return aList;
			}
			else {
				return null; // requester does not own this group
			}
		}
		else {
			return null; //requester does not exist
		}
	}
	
	private boolean addUserToGroup(String username, String groupname, UserToken yourToken)
	{
		String requester = yourToken.getSubject();
		
		// Do requester and user exist?
		if(my_gs.userList.checkUser(requester) && my_gs.userList.checkUser(username))
		{
			// Does requester have ownership of group?
			if (my_gs.userList.getUserOwnership(requester).contains(groupname))
			{
				// Is user already in the group?
				if (!my_gs.userList.getUserGroups(username).contains(groupname))
				{
					my_gs.userList.addGroup(username, groupname);
					return true;
				}
				
				return false;
			}
			else {
				return false; //requester does not have ownership
			}
		}
		else {
			return false; //requester does not exist
		}
	}
	
	private boolean deleteUserFromGroup(String username, String groupname, UserToken yourToken)
	{
		String requester = yourToken.getSubject();
		
		// Does the requester exist?
		if(my_gs.userList.checkUser(requester))
		{
			// Does the requester have ownsership of group?
			if (my_gs.userList.getUserOwnership(requester).contains(groupname))
			{
				// Does the user exist?
				if(my_gs.userList.checkUser(username)) {
					// Is the user a member of the group?
					if (my_gs.userList.getUserGroups(username).contains(groupname)) {
						my_gs.userList.removeGroup(username, groupname);
						
						// Add new key to group's entry in KeyTable
						try {
							KeyGenerator keyGen = KeyGenerator.getInstance("AES", "BC");
							keyGen.init(128);
							my_gs.keyTable.addKey(groupname, keyGen.generateKey());
						}
						catch (Exception ke) {
							System.out.println("Error creating Group File Key after Delete User from Group");
						}
						
						return true;
					}
					else {
						return false; // username is not in group
					}
				}
				else {
					return false; // can't delete nonexistent user
				}
			}
			else {
				return false; //requester does not have ownership
			}
		}
		else {
			return false; //requester does not exist
		}
	}
	
	private Envelope decryptEnv(Envelope msg) {
		try {
			// Decrypt Envelope contents
			SealedObject inCipher = (SealedObject)msg.getObjContents().get(0);
			byte[] IVarray = (byte[])msg.getObjContents().get(1);
			byte[] hmac = (byte[])msg.getObjContents().get(2);
			String algo = inCipher.getAlgorithm();
			Cipher envCipher = Cipher.getInstance(algo);
			envCipher.init(Cipher.DECRYPT_MODE, sessionKeyEnc, new IvParameterSpec(IVarray));
			
			// check HMAC
			Mac mac = Mac.getInstance("HmacSHA1", "BC");
			mac.init(sessionKeyAuth);
			mac.update(getBytes(inCipher));
			if (!Arrays.equals(mac.doFinal(), hmac)) {
				System.out.println("Secure Message HMAC FAIL");
				return new Envelope("HMACFAIL");
			}
			
			Envelope reply = (Envelope)inCipher.getObject(envCipher);
			// check sequence
			if ((Integer)reply.getObjContents().get(0) == sequence + 1) {
				sequence += 2;
				return (Envelope)reply.getObjContents().get(1);
			}
			else {
				System.out.println("Secure Message sequence FAIL.");
				return new Envelope("SEQFAIL");
			}
		}
		catch (Exception e) {
			System.out.println("Error: " + e);
			e.printStackTrace();
		}
		return null;
	}
	
	private Envelope encryptEnv(Envelope msg) {
		try {
			Envelope seqMsg = new Envelope("SEQMSG");
			seqMsg.addObject(sequence);
			seqMsg.addObject(msg);

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			SecureRandom IV = new SecureRandom();
			byte IVarray[] = new byte[16];
			IV.nextBytes(IVarray);
			cipher.init(Cipher.ENCRYPT_MODE, sessionKeyEnc, new IvParameterSpec(IVarray));
			SealedObject so = new SealedObject(seqMsg, cipher);
			
			// Do the HMAC
			Mac mac = Mac.getInstance("HmacSHA1", "BC");
			mac.init(sessionKeyAuth);
			mac.update(getBytes(so));

			// Put together the envelope
			Envelope encryptedMsg = new Envelope("ENV");
			encryptedMsg.addObject(so);
			encryptedMsg.addObject(IVarray);
			encryptedMsg.addObject(mac.doFinal());
			return encryptedMsg;
		}
		catch (Exception e) {
			System.out.println("Error: " + e);
			e.printStackTrace();
		}
		return null;
	}

	// found at http://www.javafaq.nu/java-article236.html
	public byte[] getBytes(Object obj) throws java.io.IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
		ObjectOutputStream oos = new ObjectOutputStream(bos); 
		oos.writeObject(obj);
		oos.flush(); 
		oos.close(); 
		bos.close();
		return bos.toByteArray();
	}
	
	public boolean authToken(Token aToken) {
		try {
			// Signature verification
			Signature signed = Signature.getInstance("SHA1WithRSA", "BC");
			signed.initVerify(my_gs.getServerPublicKey());
			signed.update(aToken.getContents().getBytes());
			if (signed.verify(aToken.getSignature())) {
				// RSA Signature verified
				return true;
			}
			else {
				// RSA Signature bad
				return false;
			}
		}
		catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		return false;
	}
}
