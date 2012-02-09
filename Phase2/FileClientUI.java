import java.util.Scanner; // Scanner class required for user input
import java.util.List;

public class FileClientUI
{
	FileClient fc = new FileClient();
	
	public boolean launchUI(UserToken token, String serverAddress, int portNumber)
	{
		if (fc.connect(serverAddress, portNumber))
		{
			Scanner console = new Scanner(System.in); // Scanner object for input
			String userName = token.getSubject();
			boolean exitKey = false;
			List<String> aList;
			String groupName;
			String currentGroup = new String();
			String userPrompt;
			String sourceFileName;
			String destFileName;
			
			while (!exitKey)
			{
				/* FileClient methods:
				 * public boolean delete(String filename, UserToken token)
				 * public boolean download(String sourceFile, String destFile, UserToken token)
				 * public List<String> listFiles(UserToken token)
				 * public boolean upload(String sourceFile, String destFile, String group, UserToken token)
				 */
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
								 "enter 5 to download a file to the File Server,\n" +
								 "enter 6 to delete a file to the File Server,\n" +
								 "enter 0 to disconnect from File Server...\n" +
								 userPrompt + "> ");
				String inputString = console.nextLine();
				
				switch (Integer.parseInt(inputString))
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
//						groupName = getNonEmptyString("Enter the group name to change to...\n" + userPrompt + "> ", 64);
//						aList = fc.changeGroup(groupName, token);
//						if (aList != null)
//						{
//							for (String s: aList)
//							{
//								System.out.println("Changed to group " + s + ".");
//								currentGroup = s;
//							}
//						}
//						else
//						{
//							System.out.println("Error - user has no groups. Please add groups in Group Server.");
//						}
						System.out.println("changeGroup stub");
					case 3:
						//aList = fc.listFiles(token);
						System.out.println("listFiles stub");
						aList = fc.listFiles(token);
						if (aList != null)
						{
							for (String s: aList)
							{
								System.out.println(s);
							}
						}
						break;

					case 4:
						//fc.upload(sourceFile, destFile, group, token); // returns a boolean
						System.out.println("upload stub");
						break;
					case 5:
						//fc.download(sourceFile, destFile, token); // returns a boolean
						System.out.println("download stub");
						break;
					case 6:
						//fc.delete(filename, token); // returns a boolean
						System.out.println("delete stub");
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
}