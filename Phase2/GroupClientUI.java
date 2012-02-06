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
						System.out.println("1");
						aList = token.getGroups();
						if (aList.contains("ADMIN"))
						{
							System.out.println("ADMIN");
							// public boolean createUser(String username, UserToken token)
							System.out.println("createUser() stub");
						}
						else
						{
							System.out.println("Forbidden operation. You must be an ADMIN to create a user");
						}
						break;
					case 2:
						System.out.println("2");
						aList = token.getGroups();
						if (aList.contains("ADMIN"))
						{
							System.out.println("ADMIN");
							// public boolean deleteUser(String username, UserToken token)
							System.out.println("deleteUser() stub");
						}
						else
						{
							System.out.println("Forbidden operation. You must be an ADMIN to delete a user");
						}
						break;
					case 3:
						System.out.println("3"); // don't allow duplicate name (also covers ADMIN)
						// public boolean createGroup(String groupname, UserToken token)
						System.out.println("createGroup() stub");
						break;
					case 4:
						System.out.println("4");
						// public boolean deleteGroup(String groupname, UserToken token)
						System.out.println("deleteGroup() stub");
						break;
					case 5:
						System.out.println("5");
						// public List<String> listMembers(String group, UserToken token)
						aGroupName = getNonEmptyString("Enter the group name: ", 64);
						gc.listMembers(aGroupName, token);
//						System.out.println("listMembers() stub");
						break;
					case 6:
						System.out.println("6");
						// public boolean addUserToGroup(String username, String groupname, UserToken token)
						System.out.println("addUserToGroup() stub");
						break;
					case 7:
						System.out.println("7");
						// public boolean deleteUserFromGroup(String username, String groupname, UserToken token)
						System.out.println("deleteUserFromGroup() stub");
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