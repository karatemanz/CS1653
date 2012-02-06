import java.util.Scanner; // Scanner class required for user input
import java.util.List;

public class GroupClientUI
{
	GroupClient gc = new GroupClient();
	
	public boolean launchUI(UserToken token)
	{
		if (gc.connect("localhost", 8765))
		{
			Scanner console = new Scanner(System.in); // Scanner object for input
			boolean exitKey = false;
			List<String> aList;
			
			while (!exitKey)
			{
				/* GroupClient methods:
				 * public UserToken getToken(String username)
				 * public boolean createUser(String username, UserToken token)
				 * public boolean deleteUser(String username, UserToken token)
				 * public boolean createGroup(String groupname, UserToken token)
				 * public boolean deleteGroup(String groupname, UserToken token)
				 * public List<String> listMembers(String group, UserToken token)
				 * public boolean addUserToGroup(String username, String groupname, UserToken token)
				 * public boolean deleteUserFromGroup(String username, String groupname, UserToken token)
				 */
				
				// need to determine whether user has ADMIN rights or not, then
				// send to appropriate menu - only ADMIN can create/delete user
				System.out.print("Enter 1 to create a user,\n" +
								 "enter 2 to delete a user,\n" +
								 "enter 3 to create a group,\n" +
								 "enter 4 to delete a group,\n" +
								 "enter 5 to list the members of a group,\n" +
								 "enter 6 to add a user to a group,\n" +
								 "enter 7 to delete a user from a group,\n" +
								 "enter 8 to disconnect from Group Server:\n> ");
				String inputString = console.nextLine();
				
				switch (Integer.parseInt(inputString))
				{
					case 1:
						System.out.println("1");
						System.out.println("createUser() stub");
						break;
					case 2:
						System.out.println("2");
						System.out.println("deleteUser() stub");
						break;
					case 3:
						System.out.println("3");
						System.out.println("createGroup() stub");
						break;
					case 4:
						System.out.println("4");
						System.out.println("deleteGroup() stub");
						break;
					case 5:
						System.out.println("5");
						System.out.println("listMembers() stub");
						break;
					case 6:
						System.out.println("6");
						System.out.println("addUserToGroup() stub");
						break;
					case 7:
						System.out.println("7");
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
}