import java.util.Scanner; // Scanner class required for user input
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.security.*;

public class FileClientUI
{
	FileClient fc = new FileClient();
	
	public boolean launchUI(UserToken token, String serverAddress, int portNumber)
	{
		if (fc.connect(serverAddress, portNumber))
		{
			Scanner console = new Scanner(System.in); // Scanner object for input
			String userName = token.getSubject();
			int menuChoice = 0;
			boolean exitKey = false;
			final int MAXUSERLENGTH = 32;
			final int MAXGROUPLENGTH = 32;
			final int MAXPATHLENGTH = 256;
			List<String> aList;
			String groupName;
			String currentGroup = new String();
			String userPrompt;
			String sourceFileName;
			String destFileName;
			String fsFile = "FileServerList.bin";
			ObjectInputStream ois;
			FileServerID thisFS = new FileServerID(serverAddress, portNumber, fc.getKey());

			// determine whether or not this server has been used before
			try {
				FileInputStream fis = new FileInputStream(fsFile);
				ois = new ObjectInputStream(fis);
				FileServerList fsList = (FileServerList)ois.readObject();
				
				if (fsList.hasServer(thisFS)) {
					// we're cool
				}
				else {
					// ask if we need to add
					System.out.println("This File Server's identity has not been recorded previously...");
					System.out.println("Address: " + serverAddress);
					System.out.println("Port: " + serverAddress);
					System.out.println("Public Key: " + fc.getKey().getEncoded());
					
					// if yes, add to fsList, save fsList to file
					
					// if no, exitKey = true;
				}
			}
			catch(FileNotFoundException e) {
				System.out.println("File Server List Does Not Exist. Creating " + fsFile + "...");
				ObjectOutputStream oos;
				try {
					FileServerList fsl = new FileServerList();
					oos = new ObjectOutputStream(new FileOutputStream(fsFile));
					oos.writeObject(fsl);
					oos.close();
				}
				catch(Exception ee) {
					System.err.println("Error writing to " + fsFile + ".");
					ee.printStackTrace(System.err);
					System.exit(-1);
				}

			}
			catch(IOException e) {
				System.out.println("Error reading from " + fsFile);
				System.exit(-1);
			}
			catch(ClassNotFoundException e) {
				System.out.println("Error reading from UserList file");
				System.exit(-1);
			}
			
			while (!exitKey) {
				if (currentGroup.length() > 0)
				{
					userPrompt = userName + "/" + currentGroup;
				}
				else
				{
					userPrompt = userName;
				}
				System.out.print("Enter 1 to list the groups you belong to,\n" +
								 "enter 2 to change the current group to traverse,\n" +
								 "enter 3 to list files,\n" +
								 "enter 4 to upload a file to the File Server,\n" +
								 "enter 5 to download a file from the File Server,\n" +
								 "enter 6 to delete a file from the File Server,\n" +
								 "enter 0 to disconnect from File Server...\n" +
								 userPrompt + "> ");
				String inputString = console.nextLine();
				
				try
				{
					menuChoice = Integer.parseInt(inputString);
				}
				catch(Exception e)
				{
					menuChoice = -1;
				}
				
				switch (menuChoice)
				{
					case 1:
						aList = fc.listGroups(token);
						if (aList != null)
						{
							for (String s: aList)
							{
								System.out.println(s);
							}
						}
						else
						{
							System.out.println("Error - user has no groups. Please add groups in Group Server.");
						}
						break;
					case 2:
						groupName = getNonEmptyString("Enter the group name to change to...\n> ", MAXGROUPLENGTH);
						aList = fc.changeGroup(groupName, token);
						if (aList != null)
						{
							for (String s: aList)
							{
								System.out.println("Changed to group " + s + ".");
								currentGroup = s;
							}
						}
						else
						{
							System.out.println("Error - please add groups to Group Server.");
						}
						break;
					case 3:
						aList = fc.listFiles(token);
						if (aList != null && aList.size() != 0)
						{
							for (String s: aList)
							{
								System.out.println(s);
							}
						}
						else
						{
							System.out.println("No files present.");
						}
						break;
					case 4:
						if (currentGroup.length() > 0)
						{
							sourceFileName = getNonEmptyString("Enter source file path...\n> ", MAXPATHLENGTH);
							destFileName = getNonEmptyString("Enter destination file path...\n> ", MAXPATHLENGTH);
							if (fc.upload(sourceFileName, destFileName, currentGroup, token))
							{
								System.out.println(destFileName + " successfully uploaded to group " + currentGroup + ".");
							}
							else
							{
								System.out.println("Error uploading " + destFileName + " to File Server.");
							}
						}
						else
						{
							System.out.println("You must pick a group for your workspace (option 2).");
						}
						break;
					case 5:
						if (currentGroup.length() > 0)
						{
							sourceFileName = getNonEmptyString("Enter source file path...\n> ", MAXPATHLENGTH);
							destFileName = getNonEmptyString("Enter destination file path...\n> ", MAXPATHLENGTH);
							if (fc.download(sourceFileName, destFileName, currentGroup, token))
							{
								System.out.println(destFileName + " successfully downloaded.");
							}
						}
						else
						{
							System.out.println("You must pick a group for your workspace (option 2).");
						}
						break;
					case 6:
						if (currentGroup.length() > 0)
						{
							sourceFileName = getNonEmptyString("Enter filename to delete...\n> ", MAXPATHLENGTH);
							if (fc.delete(sourceFileName, currentGroup, token))
							{
								System.out.println(sourceFileName + " successfully deleted.");
							}
						}
						else
						{
							System.out.println("You must pick a group for your workspace (option 2).");
						}
						break;
					case 0:
						System.out.println("Disconnecting from File Server...");
						fc.disconnect();
						exitKey = true;
						break;
					default:
						System.out.println("Unknown command. Please try again.");
						break;
				}
			}
			
			return true;
		}
		else // error connecting
		{
			System.out.println("Error connecting to File Server at " +
							   serverAddress + " port " + portNumber + ".");
			return false;
		}
	}
	
	public static String getNonEmptyString(String prompt, int maxLength)
	{
		String str = "";
		Scanner scan = new Scanner(System.in);
		
		System.out.print(prompt);        
		
		while (str.length() == 0)
		{
			str = scan.nextLine();
			
			if (str.length() == 0)
			{
				System.out.print(prompt);
			}
			else if (str.length() > maxLength)
			{
				System.out.println("Maximum length allowed is " + maxLength + " characters. Please re-enter.");
				System.out.print(prompt);
				str = "";
			}
		}
		
		return str;
	}
	
	public class FileServerID {
		public String address;
		public int port;
		public PublicKey key;
		
		public FileServerID(String _address, int _port, PublicKey _key) {
			address = _address;
			port = _port;
			key = _key;
		}
	}
	
	public class FileServerList implements java.io.Serializable {
		private static final long serialVersionUID = 5634179927600343803L;
		public ArrayList<FileServerID> fileServerList;
		
		public FileServerList() {
			fileServerList = new ArrayList<FileServerID>();
		}
		
		public void addServer(FileServerID fsid) {
			fileServerList.add(fsid);
		}
		
		public boolean hasServer(FileServerID fsid) {
			if (fileServerList.contains(fsid)) {
				return true;
			}
			else {
				return false;
			}
		}
	}

}