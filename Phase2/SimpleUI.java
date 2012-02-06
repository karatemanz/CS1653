import java.util.Scanner; // Scanner class required for user input
import java.util.List; // to create test UserToken
import java.util.Arrays; // to create test UserToken

public class SimpleUI
{
    public static void main(String[] args)
    {
		Scanner console = new Scanner(System.in); // Scanner object for input
		String inputString;
		String userName;
		UserToken userToken;
		boolean exitKey = false;
		boolean hasToken = false;
		FileClient fc = new FileClient();
		GroupClient gc = new GroupClient();
		
		// make a test Token to send to FileClientUI
		String[] testGroups = {"this", "is", "the", "test", "group", "list"};  
		List<String> testList = Arrays.asList(testGroups);
		
		UserToken testToken = new Token("TestServer", "TestUser", testList);
		
		/* From phase 2 description: Once a user obtains a token from the group
		 * server, they can log into one or more file servers to upload,
		 * download or delete files.
		 * So, we should force user to go to Group Server and get their token
		 * before accessing the File Server. Can this be done from the File
		 * Server UI without being overly complex? Or just do it from here
		 * before presenting any other options? */
		
		// (1) get token by connecting to group server via group client
		// (2) upon valid token retrieval, allow formal connection to group server
		System.out.print("Enter your username to login...\n> ");
		userName = console.nextLine();		
		
		// connect to group server and get token
		gc.connect("localhost", 8765);
		if (gc.isConnected()) // check that server is running)
		{
			userToken = gc.getToken(userName);
			if (userToken == null) // no login for that name
			{
				System.out.println("Username not recognized. Contact Admin.");
				exitKey = true;
			}
			else
			{
				System.out.println(userToken.getSubject());
				System.out.println(userToken.getIssuer());
				testList = userToken.getGroups();
				for (String huh : testList)
				{ System.out.println(huh); }
			}
		}
		else
		{
			System.out.println("Error - Group Server not running. Contact Admin.");
			exitKey = true;
		}
		
		// get connection to either File Server or Group Server
		while (!exitKey)
		{
			System.out.print("Main menu:\n" +
							 "Enter 1 to connect to the File Server,\n" +
							 "enter 2 to connect to the Group Server,\n" +
							 "enter 3 to exit...\n> ");
			inputString = console.nextLine();
			
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
					//fc.disconnect();
					System.out.println(gc.isConnected());
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