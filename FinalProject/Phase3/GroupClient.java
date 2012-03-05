/* Implements the GroupClient Interface */

import java.util.ArrayList;
import java.util.List;
import java.io.ObjectInputStream;
import java.security.*;
import javax.crypto.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.math.BigInteger;

public class GroupClient extends Client implements GroupClientInterface {
	public Key getSharedKey() {
		try {
			// create symmetric shared key
			Cipher sharedCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			KeyGenerator keyGenAES = KeyGenerator.getInstance("AES", "BC");
			SecureRandom rand = new SecureRandom();
			byte b[] = new byte[20];
			rand.nextBytes(b);
			keyGenAES.init(128, rand);
			Key sharedKey = keyGenAES.generateKey();
			
			// get challenge from same generator as key - may want to change
			int challenge = (Integer)rand.nextInt();
			
			// create an object for use as IV
			byte IVseed[] = {13, 91, 101, 37};
			SecureRandom IV = new SecureRandom(IVseed);

			// get Group Server's public key
			PublicKey groupPubKey = getPubKey();
			
			// encrypt key and challenge with Group Client's public key
			Envelope message = null, ciphertext = null, response = null;
			ciphertext = new Envelope("CHAL");
			message.addObject(challenge);
			message.addObject(sharedKey);
			Cipher envCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
			envCipher.init(Cipher.ENCRYPT_MODE, groupPubKey);
			SealedObject sealedObject = new SealedObject(ciphertext, envCipher);			
			
			// send it to the server
			message = new Envelope("KCG");
			message.addObject(IVseed); // add the IVseed for AES encrypt/decrypt
			message.addObject(sealedObject);
			output.writeObject(message);

			// Get the response from the server
			response = (Envelope)input.readObject();

			// decrypt and verify challenge value + 1 was returned
			if(response.getMessage().equals("OK")) {
				ArrayList<Object> temp = null;
				byte[] challResp = null;
				temp = response.getObjContents();
				if (temp.size() == 1) {
					challResp = (byte[])temp.get(0);
				}
				// decrypt challenge
				AlgorithmParameters algoPara = sharedCipher.getParameters();
				sharedCipher.init(Cipher.DECRYPT_MODE, sharedKey, algoPara, IV);
				byte[] plainText = sharedCipher.doFinal(challResp);
				if (new BigInteger(plainText).intValue() == challenge + 1) {
					return sharedKey;
				}
				else {
					System.out.println(challenge);
					System.out.println(new BigInteger(plainText).intValue());
				}
			}
			return null;
		}
		catch(Exception e) {
			System.out.println("Error: " + e);
			e.printStackTrace();
		}
		
		return null;
	}
	
	public PublicKey getPubKey() {
		try {
			Envelope message = null, response = null;			
			// Tell the server to return its public key.
			message = new Envelope("GETPUBKEY");
			output.writeObject(message);
			// Get the response from the server
			response = (Envelope)input.readObject();
			// Successful response
			if(response.getMessage().equals("OK"))
			{
				//If there is a public key in the Envelope, return it 
				ArrayList<Object> temp = null;
				temp = response.getObjContents();
				
				if(temp.size() == 1) {
					return (PublicKey)temp.get(0);
				}
			}
			return null;
		}
		catch(Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}

	 public UserToken getToken(String username, char[] password) {
		try {
			UserToken token = null;
			Envelope message = null, response = null;
		 		 	
			//Tell the server to return a token.
			message = new Envelope("GET");
			message.addObject(username); //Add user name string
			message.addObject(password); //Add user name string
			output.writeObject(message);
		
			//Get the response from the server
			response = (Envelope)input.readObject();
			
			//Successful response
			if(response.getMessage().equals("OK"))
			{
				//If there is a token in the Envelope, return it 
				ArrayList<Object> temp = null;
				temp = response.getObjContents();
				
				if(temp.size() == 1)
				{
					token = (UserToken)temp.get(0);
					return token;
				}
			}
			
			return null;
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
		
	 }
	 
	 public boolean createUser(String username, char[] password, UserToken token) {
		 try {
			Envelope message = null, response = null;
			//Tell the server to create a user
			message = new Envelope("CUSER");
			message.addObject(username); //Add user name string
			message.addObject(password); //Add user password
			message.addObject(token); //Add the requester's token
			output.writeObject(message);
		
			response = (Envelope)input.readObject();
			
			//If server indicates success, return true
			if(response.getMessage().equals("OK")) {
				return true;
			}
			
			return false;
		}
		catch(Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}
	 
	 public boolean deleteUser(String username, UserToken token)
	 {
		 try
			{
				Envelope message = null, response = null;
			 
				//Tell the server to delete a user
				message = new Envelope("DUSER");
				message.addObject(username); //Add user name
				message.addObject(token);  //Add requester's token
				output.writeObject(message);
			
				response = (Envelope)input.readObject();
				
				//If server indicates success, return true
				if(response.getMessage().equals("OK"))
				{
					return true;
				}
				
				return false;
			}
			catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
				return false;
			}
	 }
	 
	 public boolean createGroup(String groupname, UserToken token)
	 {
		 try
			{
				Envelope message = null, response = null;
				//Tell the server to create a group
				message = new Envelope("CGROUP");
				message.addObject(groupname); //Add the group name string
				message.addObject(token); //Add the requester's token
				output.writeObject(message); 
			
				response = (Envelope)input.readObject();
				
				//If server indicates success, return true
				if(response.getMessage().equals("OK"))
				{
					return true;
				}
				
				return false;
			}
			catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
				return false;
			}
	 }
	 
	 public boolean deleteGroup(String groupname, UserToken token)
	 {
		 try
			{
				Envelope message = null, response = null;
				//Tell the server to delete a group
				message = new Envelope("DGROUP");
				message.addObject(groupname); //Add group name string
				message.addObject(token); //Add requester's token
				output.writeObject(message); 
			
				response = (Envelope)input.readObject();
				//If server indicates success, return true
				if(response.getMessage().equals("OK"))
				{
					return true;
				}
				
				return false;
			}
			catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
				return false;
			}
	 }
	 
	 @SuppressWarnings("unchecked")
	public List<String> listMembers(String group, UserToken token)
	 {
		 try
		 {
			 Envelope message = null, response = null;
			 //Tell the server to return the member list
			 message = new Envelope("LMEMBERS");
			 message.addObject(group); //Add group name string
			 message.addObject(token); //Add requester's token
			 output.writeObject(message); 
			 
			 response = (Envelope)input.readObject();
			 
			 //If server indicates success, return the member list
			 if(response.getMessage().equals("OK"))
			 { 
				return (List<String>)response.getObjContents().get(0); //This cast creates compiler warnings. Sorry.
			 }
				
			 return null;
			 
		 }
		 catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
				return null;
			}
	 }
	 
	 public boolean addUserToGroup(String username, String groupname, UserToken token)
	 {
		 try
			{
				Envelope message = null, response = null;
				//Tell the server to add a user to the group
				message = new Envelope("AUSERTOGROUP");
				message.addObject(username); //Add user name string
				message.addObject(groupname); //Add group name string
				message.addObject(token); //Add requester's token
				output.writeObject(message); 
			
				response = (Envelope)input.readObject();
				//If server indicates success, return true
				if(response.getMessage().equals("OK"))
				{
					return true;
				}
				
				return false;
			}
			catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
				return false;
			}
	 }
	 
	 public boolean deleteUserFromGroup(String username, String groupname, UserToken token)
	 {
		 try
			{
				Envelope message = null, response = null;
				//Tell the server to remove a user from the group
				message = new Envelope("RUSERFROMGROUP");
				message.addObject(username); //Add user name string
				message.addObject(groupname); //Add group name string
				message.addObject(token); //Add requester's token
				output.writeObject(message);
			
				response = (Envelope)input.readObject();
				//If server indicates success, return true
				if(response.getMessage().equals("OK"))
				{
					return true;
				}
				
				return false;
			}
			catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
				return false;
			}
	 }

}
