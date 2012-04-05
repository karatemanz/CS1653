/* Implements the GroupClient Interface */

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.io.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.math.BigInteger;

public class GroupClient extends Client implements GroupClientInterface {
	private Key sessionKeyEnc;
	private Key sessionKeyAuth;
	private int sequence;
	
	public boolean getSessionKeys() {
		Security.addProvider(new BouncyCastleProvider());
		try {
			// create symmetric shared key for this session
			Cipher sharedCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			KeyGenerator keyGen = KeyGenerator.getInstance("AES", "BC");
			SecureRandom rand = new SecureRandom();
			byte b[] = new byte[20];
			rand.nextBytes(b);
			keyGen.init(128, rand);
			sessionKeyEnc = keyGen.generateKey();
			
			// create authentication key for HMAC
			keyGen = KeyGenerator.getInstance("HmacSHA1", "BC");
			b = new byte[20];
			rand.nextBytes(b);
			keyGen.init(128, rand);
			sessionKeyAuth = keyGen.generateKey();

			// get challenge from same generator as key
			int challenge = (Integer)rand.nextInt();
			
			KeyPack keyPack = new KeyPack(challenge, sessionKeyEnc, sessionKeyAuth);
			
			// create an object for use as IV
			byte IVarray[] = new byte[16];
			SecureRandom IV = new SecureRandom();
			IV.nextBytes(IVarray);
			
			// encrypt key and challenge with Group Client's public key
			Envelope message = null, ciphertext = null, response = null;
			Cipher msgCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
			msgCipher.init(Cipher.ENCRYPT_MODE, getPubKey());
			SealedObject outCipher = new SealedObject(keyPack, msgCipher);
			
			// send it to the server with IV array
			message = new Envelope("KCG");
			message.addObject(outCipher);
			message.addObject(IVarray);
			output.writeObject(message);
			// get the response from the server
			response = (Envelope)input.readObject();

			// decrypt and verify challenge + 1, sequence, HMAC
			if (response.getMessage().equals("ENV")) {
				// so, iv array, hmac
				SealedObject env = (SealedObject)response.getObjContents().get(0);
				IVarray = (byte[])response.getObjContents().get(1);
				byte[] hmac = (byte[])response.getObjContents().get(2);
				String algo = env.getAlgorithm();
				Cipher envCipher = Cipher.getInstance(algo);
				envCipher.init(Cipher.DECRYPT_MODE, sessionKeyEnc, new IvParameterSpec(IVarray));
				Envelope seqMsg = (Envelope)env.getObject(envCipher);
				Envelope reply = (Envelope)seqMsg.getObjContents().get(1);
				
				// check HMAC
				Mac mac = Mac.getInstance("HmacSHA1", "BC");
				mac.init(sessionKeyAuth);
				mac.update(getBytes(env));
				if (!Arrays.equals(mac.doFinal(), hmac)) {
					System.out.println("Session Key creation HMAC inequality");
					return false;
				}

				// check challenge, set sequence
				if ((Integer)reply.getObjContents().get(0) == challenge + 1) {
					sequence = (Integer)seqMsg.getObjContents().get(0) + 1;
					return true;
				}
				else {
					System.out.println("Session Key challenge response failed.");
				}
			}
		}
		catch(Exception e) {
			System.out.println("Error: " + e);
			e.printStackTrace();
		}
		return false;
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

	public Token getToken(String username, char[] password) {
		try {
			Token token = null;
			Envelope message = null, response = null;
		 		 	
			// Tell the server to return a token.
			message = new Envelope("GET");
			message.addObject(username); // Add username string
			message.addObject(password); // Add user's password
		
			// Get the response from the server
			response = secureMsg(message);
			
			System.out.println(response.getMessage());
			// Successful response
			if (checkResponse(response.getMessage())) {
				// If there is a token in the Envelope, return it 
				ArrayList<Object> temp = null;
				temp = response.getObjContents();
				
				if(temp.size() == 1) {
					token = (Token)temp.get(0);
					return token;
				}
			}
			
			return null;
		}
		catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}

	public Token getGroupToken(Token aToken, String groupName, String address, int port) {
		try {
			Token token = null;
			Envelope message = null, response = null;
			
			// Tell the server to return a group token for use in a File Server
			message = new Envelope("GETGT");
			message.addObject(aToken);
			message.addObject(groupName);
			message.addObject(address);
			message.addObject(port);
			
			// Get the response from the server
			response = secureMsg(message);
			
			System.out.println(response.getMessage());
			// Successful response
			if (checkResponse(response.getMessage())) {
				// If there is a token in the Envelope, return it 
				ArrayList<Object> temp = null;
				temp = response.getObjContents();
				
				if(temp.size() == 1) {
					token = (Token)temp.get(0);
					return token;
				}
			}
			
			return null;
		}
		catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Key> getGroupKeys(Token aToken) {
		try {
			ArrayList<Key> keys = null;
			Envelope message = null, response = null;

			// Tell the server to return a group token for use in a File Server
			message = new Envelope("GETKEYS");
			message.addObject(aToken);
			
			// Get the response from the server
			response = secureMsg(message);
			
			System.out.println(response.getMessage());
			// Successful response
			if (checkResponse(response.getMessage())) {
				// If there is a token in the Envelope, return it 
				ArrayList<Object> temp = null;
				temp = response.getObjContents();
				
				if (temp.size() == 1) {
					keys = (ArrayList<Key>)temp.get(0);
					return keys;
				}
			}
			
			return null;
		}
		catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}

	public boolean createUser(String username, char[] password, Token token) {
		try {
			Envelope message = null, response = null;
			//Tell the server to create a user
			message = new Envelope("CUSER");
			message.addObject(username); //Add user name string
			message.addObject(password); //Add user password
			message.addObject(token); //Add the requester's token
			 
			 response = secureMsg(message);
			
			//If server indicates success, return true
			if (checkResponse(response.getMessage())) {
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
	 
	public boolean deleteUser(String username, Token token)
	{
		try {
			Envelope message = null, response = null;
		 
			//Tell the server to delete a user
			message = new Envelope("DUSER");
			message.addObject(username); //Add user name
			message.addObject(token);  //Add requester's token
			
			response = secureMsg(message);
			
			//If server indicates success, return true
			if (checkResponse(response.getMessage())) {
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
	 
	public boolean createGroup(String groupname, Token token)
	{
		try {
			Envelope message = null, response = null;
			//Tell the server to create a group
			message = new Envelope("CGROUP");
			message.addObject(groupname); //Add the group name string
			message.addObject(token); //Add the requester's token
			
			response = secureMsg(message);
			
			//If server indicates success, return true
			if (checkResponse(response.getMessage())) {
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
	 
	public boolean deleteGroup(String groupname, Token token) {
		try {
			Envelope message = null, response = null;
			//Tell the server to delete a group
			message = new Envelope("DGROUP");
			message.addObject(groupname); //Add group name string
			message.addObject(token); //Add requester's token
			
			response = secureMsg(message);
			//If server indicates success, return true
			if (checkResponse(response.getMessage())) {
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
	 
	@SuppressWarnings("unchecked")
	public List<String> listMembers(String group, Token token) {
		try {
			Envelope message = null, response = null;
			//Tell the server to return the member list
			message = new Envelope("LMEMBERS");
			message.addObject(group); //Add group name string
			message.addObject(token); //Add requester's token

			response = secureMsg(message);
			 
			// If server indicates success, return the member list
			if (checkResponse(response.getMessage())) {
				return (List<String>)response.getObjContents().get(0); //This cast creates compiler warnings. Sorry.
			}
				
			return null;
		}
		catch(Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}
	 
	public boolean addUserToGroup(String username, String groupname, Token token) {
		try {
			Envelope message = null, response = null;
			//Tell the server to add a user to the group
			message = new Envelope("AUSERTOGROUP");
			message.addObject(username); //Add user name string
			message.addObject(groupname); //Add group name string
			message.addObject(token); //Add requester's token
			
			response = secureMsg(message);
			//If server indicates success, return true
			if (checkResponse(response.getMessage())) {
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
	 
	public boolean deleteUserFromGroup(String username, String groupname, Token token) {
		try {
			Envelope message = null, response = null;
			//Tell the server to remove a user from the group
			message = new Envelope("RUSERFROMGROUP");
			message.addObject(username); //Add user name string
			message.addObject(groupname); //Add group name string
			message.addObject(token); //Add requester's token
			
			response = secureMsg(message);
			//If server indicates success, return true
			if (checkResponse(response.getMessage())) {
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

	public Envelope secureMsg(Envelope message) {
		try {
			Envelope seqMsg = new Envelope("SEQMSG");
			seqMsg.addObject(sequence);
			seqMsg.addObject(message);
			
			// Encrypt original Envelope
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			SecureRandom IV = new SecureRandom();
			byte IVarray[] = new byte[16];
			IV.nextBytes(IVarray);
			cipher.init(Cipher.ENCRYPT_MODE, sessionKeyEnc, new IvParameterSpec(IVarray));
			SealedObject outCipher = new SealedObject(seqMsg, cipher);
			
			// Do the HMAC
			Mac mac = Mac.getInstance("HmacSHA1", "BC");
			mac.init(sessionKeyAuth);
			mac.update(getBytes(outCipher));

			// Create new Envelope with encrypted data, IV, and HMAC
			Envelope cipherMsg = new Envelope("ENV");
			Envelope encResponse = null;
			cipherMsg.addObject(outCipher);
			cipherMsg.addObject(IVarray);
			cipherMsg.addObject(mac.doFinal());
			output.writeObject(cipherMsg);
			
			// Get and decrypt response
			encResponse = (Envelope)input.readObject();
			if (encResponse.getMessage().equals("ENV")) {
				// Decrypt Envelope contents
				SealedObject inCipher = (SealedObject)encResponse.getObjContents().get(0);
				IVarray = (byte[])encResponse.getObjContents().get(1);
				byte[] hmac = (byte[])encResponse.getObjContents().get(2);
				String algo = inCipher.getAlgorithm();
				Cipher envCipher = Cipher.getInstance(algo);
				envCipher.init(Cipher.DECRYPT_MODE, sessionKeyEnc, new IvParameterSpec(IVarray));
				
				// check HMAC
				mac = Mac.getInstance("HmacSHA1", "BC");
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
		}
		catch(Exception e) {
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

	public boolean checkResponse(String resp) {
		if (resp.equals("OK")) {
			return true;
		}
		else if (resp.equals("HMACFAIL")) {
			System.out.println("Secure Message HMAC FAIL");
			return false;
		}
		else if (resp.equals("SEQFAIL")) {
			System.out.println("Secure Message sequence FAIL.");
			return false;
		}
		else {
			return false;
		}
	}
}
