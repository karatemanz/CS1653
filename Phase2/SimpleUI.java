import java.util.Scanner; // Scanner class required for user input
import java.util.List; // to create test UserToken
import java.util.Arrays; // to create test UserToken

public class SimpleUI
{
    public static void main(String[] args)
    {
		Scanner console = new Scanner(System.in); // Scanner object for input
		boolean exitKey = false;
		FileClient fc = new FileClient();
		GroupClient gc = new GroupClient();
		
		// make a test Token to send to FileClientUI
		String[] testGroups = {"this", "is", "the", "test", "group", "list"};  
		List<String> testList = Arrays.asList(testGroups);
		
		UserToken testToken = new Token("TestServer", "TestUser", testList);
		
		while (!exitKey)
		{
			System.out.print("Enter 1 to connect to the File Server,\n" +
							 "enter 2 to disconnect from the File Server,\n" +
							 "enter 3 to connect to the Group Server,\n" +
							 "enter 4 to disconnect from the Group Server,\n" +
							 "enter 5 to exit:\n>");
			String inputString = console.nextLine();
			
			switch (Integer.parseInt(inputString))
			{
				case 1:
					System.out.println("1");
					//fc.connect("localhost", 4321);
					FileClientUI fcu = new FileClientUI();
					fcu.launchUI(testToken);
					break;
				case 2:
					System.out.println("2");
					fc.disconnect();
					break;
				case 3:
					System.out.println("3");
					gc.connect("localhost", 8765);
					break;
				case 4:
					System.out.println("4");
					gc.disconnect();
					break;
				case 5:
					System.out.println("Exiting");
					exitKey = true;
					break;
				default:
					exitKey = true;
					break;
			}
		}
	}
	
	/* FileClient methods:
	 * public boolean delete(String filename, UserToken token)
	 * public boolean download(String sourceFile, String destFile, UserToken token)
	 * public List<String> listFiles(UserToken token)
	 * public boolean upload(String sourceFile, String destFile, String group, UserToken token)
	 */
	
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
}