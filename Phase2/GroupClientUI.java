import java.util.Scanner; // Scanner class required for user input
import java.util.List;

public class GroupClientUI
{
	GroupClient gc = new GroupClient();
	
	public boolean launchUI(UserToken token, String serverName, int portNumber)
	{
		if (gc.connect(serverName, portNumber))
		{
			Scanner console = new Scanner(System.in); // Scanner object for input
			String userName = token.getSubject();
			String aUserName, aGroupName;
			boolean exitKey = false;
			List<String> aList;
			
			while (!exitKey)
			{
				System.out.print("Enter 1 to create a user,\n" +
								 "enter 2 to delete a user,\n" +
								 "enter 3 to create a group,\n" +
								 "enter 4 to delete a group,\n" +
								 "enter 5 to list the members of a group,\n" +
								 "enter 6 to add a user to a group,\n" +
								 "enter 7 to delete a user from a group,\n" +
								 "enter 8 to disconnect from Group Server:\n" +
								 userName + "> ");
				String inputString = console.nextLine();
				
				switch (Integer.parseInt(inputString))
				{
					case 1:
						if (token.getGroups().contains("ADMIN"))
						{
							aUserName = getNonEmptyString("Enter the username to be added: ", 32);
							if (gc.createUser(aUserName, token))
							{
								System.out.println("Added " + aUserName + " to the User List.");
							}
							else
							{
								System.out.println("Error adding user - name already exists.");
							}
						}
						else
						{
							System.out.println("Forbidden operation. You must be an ADMIN to create a user.");
						}
						break;
					case 2:
						if (token.getGroups().contains("ADMIN"))
						{
							aUserName = getNonEmptyString("Enter the username to be deleted: ", 32);
							if (gc.deleteUser(aUserName, token))
							{
								System.out.println("Deleted " + aUserName + " from the User List.");
							}
							else
							{
								System.out.println("Error deleting user - unknown username.");
							}
						}
						else
						{
							System.out.println("Forbidden operation. You must be an ADMIN to delete a user.");
						}
						break;
					case 3:
						aGroupName = getNonEmptyString("Enter the group name to be created: ", 64);
						if (gc.createGroup(aGroupName, token))
						{
							System.out.println("Added the group " + aGroupName + " to your Group List.");
						}
						else
						{
							System.out.println("Error creating group - group name already exists.");
						}
						break;
					case 4:
						aGroupName = getNonEmptyString("Enter the group name to be deleted: ", 64);
						if (gc.deleteGroup(aGroupName, token))
						{
							System.out.println("Deleted the group " + aGroupName + " from your Group List.");
						}
						else
						{
							System.out.println("Forbidden operation. You must be the owner of a group to delete it.");
						}
						break;
					case 5:
						aGroupName = getNonEmptyString("Enter the group name: ", 64);
						aList = gc.listMembers(aGroupName, token);
						if (aList != null)
						{
							for (String s: aList)
							{
								System.out.println(s);
							}
						}
						else
						{
							System.out.println("Error. You are not a member of group " +
											   aGroupName + ".");
						}
						break;
					case 6:
						aUserName = getNonEmptyString("Enter the username: ", 32);
						aGroupName = getNonEmptyString("Enter the group name: ", 64);
						if (gc.addUserToGroup(aUserName, aGroupName, token))
						{
							System.out.println("Added " + aUserName + " to group " + aGroupName + ".");
						}
						else
						{
							System.out.println("Error adding user to group - check username and group name.");
						}
						break;
					case 7:
						aUserName = getNonEmptyString("Enter the username: ", 32);
						aGroupName = getNonEmptyString("Enter the group name: ", 64);
						if (gc.deleteUserFromGroup(aUserName, aGroupName, token))
						{
							System.out.println("Added " + aUserName + " to group " + aGroupName + ".");
						}
						else
						{
							System.out.println("Error deleting user from group - check that the user is member of that group.");
						}
						break;
					case 8:
						System.out.println("Disconnecting from Group Server...");
						gc.disconnect();
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
			System.out.println("Error connecting to Group Server");
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