/* FileClient provides all the client functionality regarding the file server */

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.math.BigInteger;

public class FileClient extends Client implements FileClientInterface {
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
			message = new Envelope("KCF");
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
			// If successful response, return public key
			if(response.getMessage().equals("OK")) {
				return (PublicKey)response.getObjContents().get(0);
			}
			return null;
		}
		catch(Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}

	public Envelope secureMsg (Envelope message) {
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
	
	public boolean delete(String filename, Token token) {
		String remotePath;
		if (filename.charAt(0)=='/') {
			remotePath = filename.substring(1);
		}
		else {
			remotePath = filename;
		}
		Envelope env = new Envelope("DELETEF"); // Success
	    env.addObject(remotePath);
	    env.addObject(token.getGroups().get(0)); // group
	    env.addObject(token);
	    try {
			env = secureMsg(env);

			if (checkResponse(env.getMessage())) {
				System.out.printf("File %s deleted successfully\n", filename);				
			}
			else {
				System.out.printf("Error deleting file %s (%s)\n", filename, env.getMessage());
				return false;
			}			
		}
		catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}	    	
		return true;
	}

	public boolean download(String sourceFile, String destFile, Token token, ArrayList<Key> keys) {
		if (sourceFile.charAt(0) == '/') {
			sourceFile = sourceFile.substring(1);
		}

		File file = new File(destFile);
		try {
			if (!file.exists()) {
				file.createNewFile();
				FileOutputStream fos = new FileOutputStream(file);
				int keyVersion;
				
				Envelope env = new Envelope("DOWNLOADF"); // Success
				env.addObject(sourceFile);
				env.addObject(token.getGroups().get(0)); // group
				env.addObject(token);

				env = secureMsg(env);
				
				if (env.getMessage().equals("KEYVERSION")) {
					keyVersion = (Integer)env.getObjContents().get(0);
					env = secureMsg(new Envelope("DOWNLOADF"));
				}
				else {
					System.out.printf("Error retrieving file key version for %s (%s)\n", sourceFile, env.getMessage());
					file.delete();
					return false;
				}

				Cipher bufCipher = Cipher.getInstance("AES/CTR/NoPadding", "BC");
				byte[] IVarray = Arrays.copyOf(getBytes(token.getGroups().get(0)), 16);

				while (env.getMessage().compareTo("CHUNK")==0) {
					// decrypt chunk
					bufCipher.init(Cipher.DECRYPT_MODE, keys.get(keyVersion), new IvParameterSpec(IVarray));
					byte[] plainText = bufCipher.doFinal((byte[])env.getObjContents().get(0));
					
					fos.write(plainText, 0, (Integer)env.getObjContents().get(1));
					System.out.printf(".");
					env = new Envelope("DOWNLOADF"); // Success
					
					env = secureMsg(env);
				}										
				fos.close();
				
				if (env.getMessage().compareTo("EOF")==0) {
					fos.close();
					System.out.printf("\nTransfer successful file %s\n", sourceFile);
					env = new Envelope("OK"); //Success

					// send envelope, don't need response so don't use secureMsg
					try {
						Envelope seqMsg = new Envelope("SEQMSG");
						seqMsg.addObject(sequence);
						seqMsg.addObject(env);
						
						// Encrypt original Envelope
						Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
						SecureRandom IV = new SecureRandom();
						IVarray = new byte[16];
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
					}
					catch(Exception e) {
						System.out.println("Error: " + e);
						e.printStackTrace();
					}

				}
				else {
					System.out.printf("Error reading file %s (%s)\n", sourceFile, env.getMessage());
					file.delete();
					return false;
				}
			}    
			 
			else {
				System.out.printf("Error couldn't create file %s\n", destFile);
				return false;
			}
		}
		catch (IOException e1) {
			
			System.out.printf("Error couldn't create file %s\n", destFile);
			return false;
		}
		catch (Exception e1) {
			e1.printStackTrace();
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public List<String> listFiles(Token token) {
		try {
			Envelope message = null, env = null;
			message = new Envelope("LFILES");
			message.addObject(token); // Add requester's token

			env = secureMsg(message);
			
			if (checkResponse(env.getMessage())) {
				return (List<String>)env.getObjContents().get(0); // This cast creates compiler warnings. Sorry.
			}
			
			return null;
		}
		catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}

	public boolean upload(String sourceFile, String destFile, Token token, ArrayList<Key> keys) {
		if (destFile.charAt(0) != '/') {
			 destFile = "/" + destFile;
		}
		
		try {
			Envelope message = null, env = null;
			message = new Envelope("UPLOADF");
			message.addObject(destFile);
			message.addObject(token.getGroups().get(0)); // group
			message.addObject(token);
			message.addObject(keys.size() - 1); // most recent key version

			FileInputStream fis = new FileInputStream(sourceFile);

			env = secureMsg(message);

			if (env.getMessage().equals("READY")) { 
				System.out.printf("Meta data upload successful\n");
			}
			else {				
				System.out.printf("Upload failed: %s\n", env.getMessage());
				return false;
			}
			
			Cipher bufCipher = Cipher.getInstance("AES/CTR/NoPadding", "BC");
			// get 128-bit IV from group name
			byte[] IVarray = Arrays.copyOf(getBytes(token.getGroups().get(0)), 16);
			int keyVersion = keys.size() - 1; // most recent key version

			do {
				byte[] buf = new byte[4096];
				if (env.getMessage().compareTo("READY") != 0) {
					System.out.printf("Server error: %s\n", env.getMessage());
					return false;
				}
				message = new Envelope("CHUNK");
				int n = fis.read(buf); // can throw an IOException
				if (n > 0) {
					System.out.printf(".");
				}
				else if (n < 0) {
					System.out.println("Read error");
					return false;
				}

				// encrypt buf
				bufCipher.init(Cipher.ENCRYPT_MODE, keys.get(keyVersion), new IvParameterSpec(IVarray));
				byte[] cipherText = bufCipher.doFinal(buf);
				
				message.addObject(cipherText);
				message.addObject(new Integer(n));

				env = secureMsg(message);
			}
			while (fis.available() > 0);		 
					 
			if (env.getMessage().compareTo("READY") == 0) { 
				message = new Envelope("EOF");
				
				env = secureMsg(message);
				
				if (checkResponse(env.getMessage())) {
					System.out.printf("\nFile data upload successful\n");
				}
				else {
					 System.out.printf("\nUpload failed: %s\n", env.getMessage());
					 return false;
				}
			}
			else {
				 System.out.printf("Upload failed: %s\n", env.getMessage());
				 return false;
			}
		}
		catch(Exception e1) {
			System.err.println("Error: " + e1.getMessage());
			e1.printStackTrace(System.err);
			return false;
		}
		return true;
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
