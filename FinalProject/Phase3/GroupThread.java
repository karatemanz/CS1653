/* This thread does all the work. It communicates with the client through Envelopes.
 * 
 */
import java.lang.Thread;
import java.net.Socket;
import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class GroupThread extends Thread 
{
	private final Socket socket;
	private GroupServer my_gs;
	
	public GroupThread(Socket _socket, GroupServer _gs)
	{
		socket = _socket;
		my_gs = _gs;
	}
	
	public void run()
	{
		boolean proceed = true;

		try
		{
			//Announces connection and opens object streams
			System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
			final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			
			do
			{
				Envelope message = (Envelope)input.readObject();
				System.out.println("Request received: " + message.getMessage());
				Envelope response;
				
				if (message.getMessage().equals("KCG")) { // Client wants a session key
					// decrypt sealed object with private key
					SealedObject sealedObject = (SealedObject)message.getObjContents().get(0);
					String algo = sealedObject.getAlgorithm();
					Cipher cipher = Cipher.getInstance(algo);
					cipher.init(Cipher.DECRYPT_MODE, my_gs.getPrivateKey());
					// get KeyPack
					KeyPack kcg = (KeyPack)sealedObject.getObject(cipher);
					
					int challenge = kcg.getChallenge();
					Key sharedKey = kcg.getSecretKey();
					
					System.out.println(sharedKey.getEncoded());
					byte ouch[] = sharedKey.getEncoded();
					for (int i = 0; i < ouch.length; i++) {
						System.out.print(ouch[i]);
					}
					System.out.println();

					// encrypt something
					byte IVseed[] = {13, 91, 101, 37};
					SecureRandom IV = new SecureRandom(IVseed);
					
					Cipher theCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");

					// Encryption
					challenge += 1;
					System.out.println(challenge);
					byte plaintext[] = new byte[4];
					plaintext[0] = (byte)(challenge >> 24);
					plaintext[1] = (byte)(challenge >> 16);
					plaintext[2] = (byte)(challenge >> 8);
					plaintext[3] = (byte)(challenge /*>> 0*/);
					theCipher.init(Cipher.ENCRYPT_MODE, sharedKey, IV);			
					byte[] cipherText = theCipher.doFinal(plaintext);
					
//					System.out.println("getIV(): " + theCipher.getIV());

//					byte IVseed[] = {kcg[0], kcg[1], kcg[2], kcg[3]};
//					SecureRandom IV = new SecureRandom(IVseed);
					
					// set shared key to global
					
					// Respond to the client
					response = new Envelope("OK");
					response.addObject(cipherText);
					response.addObject(theCipher.getIV());
					output.writeObject(response);

				}
				else if (message.getMessage().equals("GET"))//Client wants a token
				{
					String username = (String)message.getObjContents().get(0); //Get the username
					char[] password = (char[])message.getObjContents().get(1);
					
					if(username == null)
					{
						response = new Envelope("FAIL");
						response.addObject(null);
						output.writeObject(response);
					}
					else
					{
						UserToken yourToken = createToken(username, password); //Create a token
						//Respond to the client. On error, the client will receive a null token
						response = new Envelope("OK");
						response.addObject(yourToken);
						output.writeObject(response);
					}
				}
				else if(message.getMessage().equals("CUSER")) //Client wants to create a user
				{
					if (message.getObjContents().size() < 3) {
						response = new Envelope("FAIL");
					}
					else {
						response = new Envelope("FAIL");
						
						if (message.getObjContents().get(0) != null &&
							message.getObjContents().get(1) != null &&
							message.getObjContents().get(2) != null) {

							String username = (String)message.getObjContents().get(0); //Extract the username
							char[] password = (char[])message.getObjContents().get(1); //Extract the password
							UserToken yourToken = (UserToken)message.getObjContents().get(2); //Extract the token
							
							if (createUser(username, password, yourToken)) {
								response = new Envelope("OK"); //Success
							}
						}
					}
					
					output.writeObject(response);
				}
				else if(message.getMessage().equals("DUSER")) //Client wants to delete a user
				{
					if(message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");
						
						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								String username = (String)message.getObjContents().get(0); //Extract the username
								UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token
								
								if(deleteUser(username, yourToken))
								{
									response = new Envelope("OK"); //Success
								}
							}
						}
					}
					
					output.writeObject(response);
				}
				else if(message.getMessage().equals("CGROUP")) //Client wants to create a group
				{
					if(message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");
						
						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								String groupname = (String)message.getObjContents().get(0); //Extract the group name
								UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token
								
								if(createGroup(groupname, yourToken))
								{
									response = new Envelope("OK"); //Success
								}
							}
						}
					}
					
					output.writeObject(response);
				}
				else if(message.getMessage().equals("DGROUP")) //Client wants to delete a group
				{
					if(message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");
						
						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								String groupname = (String)message.getObjContents().get(0); //Extract the group name
								UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token
								
								if(deleteGroup(groupname, yourToken))
								{
									response = new Envelope("OK"); //Success
								}
							}
						}
					}
					
					output.writeObject(response);
				}
				else if(message.getMessage().equals("LMEMBERS")) //Client wants a list of members in a group
				{
					String groupName = (String)message.getObjContents().get(0); //Get the groupName
					UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token
					
					if(groupName == null)
					{
						response = new Envelope("FAIL");
						response.addObject(null);
						output.writeObject(response);
					}
					else
					{
						List<String> memberList = listMembers(groupName, yourToken); //Create a token
						//Respond to the client. On error, the client will receive a null List
						response = new Envelope("OK");
						response.addObject(memberList);
						output.writeObject(response);
					}
				}
				else if(message.getMessage().equals("AUSERTOGROUP")) //Client wants to add user to a group
				{
					if(message.getObjContents().size() < 3)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");
						
						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								if(message.getObjContents().get(1) != null)
								{
									String username = (String)message.getObjContents().get(0); //Extract the username
									String groupname = (String)message.getObjContents().get(1); //Extract the group name
									UserToken yourToken = (UserToken)message.getObjContents().get(2); //Extract the token
									
									if(addUserToGroup(username, groupname, yourToken))
									{
										response = new Envelope("OK"); //Success
									}
								}
							}
						}
					}
					
					output.writeObject(response);
				}
				else if(message.getMessage().equals("RUSERFROMGROUP")) //Client wants to remove user from a group
				{
					if(message.getObjContents().size() < 3)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");
						
						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								if(message.getObjContents().get(1) != null)
								{
									String username = (String)message.getObjContents().get(0); //Extract the username
									String groupname = (String)message.getObjContents().get(1); //Extract the group name
									UserToken yourToken = (UserToken)message.getObjContents().get(2); //Extract the token
									
									if(deleteUserFromGroup(username, groupname, yourToken))
									{
										response = new Envelope("OK"); //Success
									}
								}
							}
						}
					}
					
					output.writeObject(response);
				}
				else if (message.getMessage().equals("GETPUBKEY")) { // Client wants the public key
					response = new Envelope("OK");
					response.addObject(my_gs.getServerPublicKey());
					output.writeObject(response);
				}
				else if(message.getMessage().equals("DISCONNECT")) //Client wants to disconnect
				{
					socket.close(); //Close the socket
					proceed = false; //End this communication loop
				}
				else
				{
					response = new Envelope("FAIL"); //Server does not understand client request
					output.writeObject(response);
				}
			}while(proceed);	
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}
	
	//Method to create tokens
	private Token createToken(String username, char[] password) 
	{
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
		else
		{
			return null;
		}
	}
	
	
	//Method to create a user
	private boolean createUser(String username, char[] password, UserToken yourToken)
	{
		String requester = yourToken.getSubject();
		
		//Check if requester exists
		if(my_gs.userList.checkUser(requester))
		{
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
			else
			{
				return false; //requester not an administrator
			}
		}
		else
		{
			return false; //requester does not exist
		}
	}
	
	//Method to delete a user
	private boolean deleteUser(String username, UserToken yourToken)
	{
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
				else
				{
					return false; //User does not exist
				}
			}
			else
			{
				return false; //requester is not an administer
			}
		}
		else
		{
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
			
			return true;
		}
		else
		{
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
				
				// remove this group from owner's list
				my_gs.userList.removeGroup(requester, groupname);
				
				return true;
			}
			else
			{
				return false; // requester does not own this group
			}
		}		
		else
		{
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
			else
			{
				return null; // requester does not own this group
			}
		}
		else
		{
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
			else
			{
				return false; //requester does not have ownership
			}
		}
		else
		{
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
			else
			{
				return false; //requester does not have ownership
			}
		}
		else
		{
			return false; //requester does not exist
		}
	}
}
