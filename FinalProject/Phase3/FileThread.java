/* File worker thread handles the business of uploading, downloading, and removing files for clients with valid tokens */

import java.lang.Thread;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.*;
import javax.crypto.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class FileThread extends Thread
{
	private final Socket socket;
	public final PublicKey fsKey;
	public final PublicKey gsKey;

	public FileThread(Socket _socket, PublicKey _fsKey, PublicKey _gsKey)
	{
		socket = _socket;
		fsKey = _fsKey;
		gsKey = _gsKey;
	}

	public void run()
	{
		boolean proceed = true;
		Security.addProvider(new BouncyCastleProvider());

		try
		{
			System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
			final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			Envelope response;

			do
			{
				Envelope e = (Envelope)input.readObject();
				System.out.println("Request received: " + e.getMessage());

				if(e.getMessage().equals("LGROUPS"))
				{
					if(e.getObjContents().size() < 1)
					{
						response = new Envelope("FAIL-BADCONTENTS");
					}
					else
					{
						if(e.getObjContents().get(0) == null) {
							response = new Envelope("FAIL-BADTOKEN");
						}
						else {
							Token yourToken = (Token)e.getObjContents().get(0); //Extract token
							if (authToken(yourToken)) {
								List<String> groupList = yourToken.getGroups(); // get groups
								response = new Envelope("OK"); //Success
								response.addObject(groupList);
							}
							else {
								response = new Envelope("FAIL-BADTOKENAUTH");
							}
						}
					}
					output.writeObject(response);
				}
				if(e.getMessage().equals("CGROUP"))
				{
					if(e.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL-BADCONTENTS");
					}
					else
					{
						if(e.getObjContents().get(0) == null) {
							response = new Envelope("FAIL-BADGROUP");
						}
						if(e.getObjContents().get(1) == null) {
							response = new Envelope("FAIL-BADTOKEN");
						}
						else {
							String changeGroup = (String)e.getObjContents().get(0); //Extract group
							Token yourToken = (Token)e.getObjContents().get(1); //Extract token
							if (authToken(yourToken)) {
								// check that it is a valid group
								if (yourToken.getGroups().contains(changeGroup))
								{
									response = new Envelope("OK"); //Success
									List<String> changeGroupList = new ArrayList<String>();
									changeGroupList.add(changeGroup);
									
									response.addObject(changeGroupList);
								}
								else
								{
									response = new Envelope("FAIL-BADGROUP");
								}
							}
							else {
								response = new Envelope("FAIL-BADTOKENAUTH");
							}
						}
					}
					output.writeObject(response);
				}

				// Handler to list files that this user is allowed to see
				if(e.getMessage().equals("LFILES"))
				{
					if(e.getObjContents().size() < 1)
					{
						response = new Envelope("FAIL-BADCONTENTS");
					}
					else
					{
						if(e.getObjContents().get(0) == null) {
							response = new Envelope("FAIL-BADTOKEN");
						}
						else
						{
							Token yourToken = (Token)e.getObjContents().get(0); //Extract token
							if (authToken(yourToken)) {
								String username = yourToken.getSubject();
								String outputStr;
								List<ShareFile> fullFileList = FileServer.fileList.getFiles();
								List<String> userFileList = new ArrayList<String>();
								if (fullFileList != null)
								{
									for (ShareFile sf: fullFileList)
									{
										if (yourToken.getGroups().contains(sf.getGroup()))
										{
											userFileList.add(sf.getPath() + "\t(" + sf.getOwner() + "/" + sf.getGroup() + ")");
										}
									}
								}

								response = new Envelope("OK"); //Success
								response.addObject(userFileList);
							}
							else {
								response = new Envelope("FAIL-BADTOKENAUTH");
							}
						}
					}
					output.writeObject(response);
				}
				if(e.getMessage().equals("UPLOADF"))
				{

					if(e.getObjContents().size() < 3)
					{
						response = new Envelope("FAIL-BADCONTENTS");
					}
					else
					{
						if(e.getObjContents().get(0) == null) {
							response = new Envelope("FAIL-BADPATH");
						}
						if(e.getObjContents().get(1) == null) {
							response = new Envelope("FAIL-BADGROUP");
						}
						if(e.getObjContents().get(2) == null) {
							response = new Envelope("FAIL-BADTOKEN");
						}
						else {
							String remotePath = (String)e.getObjContents().get(0);
							String group = (String)e.getObjContents().get(1);
							Token yourToken = (Token)e.getObjContents().get(2); //Extract token
							if (authToken(yourToken)) {
								if (FileServer.fileList.checkFile(remotePath)) {
									System.out.printf("Error: file already exists at %s\n", remotePath);
									response = new Envelope("FAIL-FILEEXISTS"); //Success
								}
								else if (!yourToken.getGroups().contains(group)) {
									System.out.printf("Error: user missing valid token for group %s\n", group);
									response = new Envelope("FAIL-UNAUTHORIZED"); //Success
								}
								else  {
									File file = new File("shared_files/"+remotePath.replace('/', '_'));
									file.createNewFile();
									FileOutputStream fos = new FileOutputStream(file);
									System.out.printf("Successfully created file %s\n", remotePath.replace('/', '_'));

									response = new Envelope("READY"); //Success
									output.writeObject(response);

									e = (Envelope)input.readObject();
									while (e.getMessage().compareTo("CHUNK")==0) {
										fos.write((byte[])e.getObjContents().get(0), 0, (Integer)e.getObjContents().get(1));
										response = new Envelope("READY"); //Success
										output.writeObject(response);
										e = (Envelope)input.readObject();
									}

									if(e.getMessage().compareTo("EOF")==0) {
										System.out.printf("Transfer successful file %s\n", remotePath);
										FileServer.fileList.addFile(yourToken.getSubject(), group, remotePath);
										response = new Envelope("OK"); //Success
									}
									else {
										System.out.printf("Error reading file %s from client\n", remotePath);
										response = new Envelope("ERROR-TRANSFER"); //Success
									}
									fos.close();
								}
							}
							else {
								response = new Envelope("FAIL-BADTOKENAUTH");
							}
						}
					}

					output.writeObject(response);
				}
				else if (e.getMessage().compareTo("DOWNLOADF")==0) {

					String remotePath = (String)e.getObjContents().get(0);
					String group = (String)e.getObjContents().get(1);
					Token t = (Token)e.getObjContents().get(2);
					if (authToken(t)) {
						ShareFile sf = FileServer.fileList.getFile("/"+remotePath);
						if (sf == null) {
							System.out.printf("Error: File %s doesn't exist\n", remotePath);
							e = new Envelope("ERROR_FILEMISSING");
							output.writeObject(e);

						}
						else if (!sf.getGroup().equals(group)){
							System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
							e = new Envelope("ERROR_GROUP_PERMISSION");
							output.writeObject(e);
						}
						else {

							try
							{
								File f = new File("shared_files/_"+remotePath.replace('/', '_'));
							if (!f.exists()) {
								System.out.printf("Error file %s missing from disk\n", "_"+remotePath.replace('/', '_'));
								e = new Envelope("ERROR_NOTONDISK");
								output.writeObject(e);

							}
							else {
								FileInputStream fis = new FileInputStream(f);

								do {
									byte[] buf = new byte[4096];
									if (e.getMessage().compareTo("DOWNLOADF")!=0) {
										System.out.printf("Server error: %s\n", e.getMessage());
										break;
									}
									e = new Envelope("CHUNK");
									int n = fis.read(buf); //can throw an IOException
									if (n > 0) {
										System.out.printf(".");
									} else if (n < 0) {
										System.out.println("Read error");

									}


									e.addObject(buf);
									e.addObject(new Integer(n));

									output.writeObject(e);

									e = (Envelope)input.readObject();


								}
								while (fis.available()>0);

								//If server indicates success, return the member list
								if(e.getMessage().compareTo("DOWNLOADF")==0)
								{

									e = new Envelope("EOF");
									output.writeObject(e);

									e = (Envelope)input.readObject();
									if(e.getMessage().compareTo("OK")==0) {
										System.out.printf("File data upload successful\n");
									}
									else {

										System.out.printf("Upload failed: %s\n", e.getMessage());

									}

								}
								else {

									System.out.printf("Upload failed: %s\n", e.getMessage());

								}
							}
							}
							catch(Exception e1)
							{
								System.err.println("Error: " + e.getMessage());
								e1.printStackTrace(System.err);

							}
						}
					}
					else {
						e = new Envelope("FAIL-BADTOKENAUTH");
						output.writeObject(e);
					}
				}
				else if (e.getMessage().compareTo("DELETEF")==0) {

					String remotePath = (String)e.getObjContents().get(0);
					String group = (String)e.getObjContents().get(1);
					Token t = (Token)e.getObjContents().get(2);
					if (authToken(t)) {
						ShareFile sf = FileServer.fileList.getFile("/"+remotePath);
						if (sf == null) {
							System.out.printf("Error: File %s doesn't exist\n", remotePath);
							e = new Envelope("ERROR_DOESNTEXIST");
						}
						else if (!sf.getGroup().equals(group)){
							System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
							e = new Envelope("ERROR_GROUP_PERMISSION");
						}
						else {

							try
							{


								File f = new File("shared_files/"+"_"+remotePath.replace('/', '_'));

								if (!f.exists()) {
									System.out.printf("Error file %s missing from disk\n", "_"+remotePath.replace('/', '_'));
									e = new Envelope("ERROR_FILEMISSING");
								}
								else if (f.delete()) {
									System.out.printf("File %s deleted from disk\n", "_"+remotePath.replace('/', '_'));
									FileServer.fileList.removeFile("/"+remotePath);
									e = new Envelope("OK");
								}
								else {
									System.out.printf("Error deleting file %s from disk\n", "_"+remotePath.replace('/', '_'));
									e = new Envelope("ERROR_DELETE");
								}


							}
							catch(Exception e1)
							{
								System.err.println("Error: " + e1.getMessage());
								e1.printStackTrace(System.err);
								e = new Envelope(e1.getMessage());
							}
						}
					output.writeObject(e);
					}
					else {
						e = new Envelope("FAIL-BADTOKENAUTH");
						output.writeObject(e);
					}
				}
				else if (e.getMessage().compareTo("GETPUBKEY") == 0) { // Client wants the public key
					response = new Envelope("OK");
					response.addObject(fsKey);
					output.writeObject(response);
				}
				else if(e.getMessage().equals("DISCONNECT"))
				{
					socket.close();
					proceed = false;
				}
			} while(proceed);
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}

	public boolean authToken(Token aToken) {
		try {
			// Signature verification
			Signature signed = Signature.getInstance("SHA1WithRSA", "BC");
			signed.initVerify(gsKey);
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
