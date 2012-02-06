import java.util.Scanner; // Scanner class required for user input
import java.util.List; // to create test UserToken
import java.util.Arrays; // to create test UserToken

public class SimpleUI
{
	FileClient fc = new FileClient();
	GroupClient gc = new GroupClient();
	Scanner console = new Scanner(System.in); // Scanner object for input

    public static void main(String[] args)
    {
//		Scanner console = new Scanner(System.in); // Scanner object for input
//		String inputString;
//		String userName;
//		UserToken userToken;
//		boolean exitKey = false;
//		boolean hasToken = false;
		
		// make a test Token to send to FileClientUI
//		String[] testGroups = {"this", "is", "the", "test", "group", "list"};  
//		List<String> testList = Arrays.asList(testGroups);
//		
//		UserToken testToken = new Token("TestServer", "TestUser", testList);
		
		/* From phase 2 description: Once a user obtains a token from the group
		 * server, they can log into one or more file servers to upload,
		 * download or delete files.
		 * So, we should force user to go to Group Server and get their token
		 * before accessing the File Server. Can this be done from the File
		 * Server UI without being overly complex? Or just do it from here
		 * before presenting any other options? */
		
		// (1) get token by connecting to group server via group client
		// (2) upon valid token retrieval, allow formal connection to group server
//			else // test UserToken methods
//			{
//				System.out.println(userToken.getSubject());
//				System.out.println(userToken.getIssuer());
//				testList = userToken.getGroups();
//				for (String huh : testList)
//				{ System.out.println(huh); }
//			}
		
		// get connection to either File Server or Group Server
	}
	
	public void loginMenu()
	{
		String loginString;
		int loginNumber;
		boolean doExit = false;
		String userName;
		UserToken userToken;
		
		while (!doExit)
		{
			System.out.println("Enter 1 to login,\nenter 2 to exit...\n> ");
			loginString = console.nextLine();

			System.out.print("Enter your username to login...\n> ");
			userName = console.nextLine();
			loginNumber = Integer.parseInt(loginString);
			
			if (loginNumber == 1)
			{
				// connect to group server and get token
				gc.connect("localhost", 8765);
				if (gc.isConnected()) // check that server is running)
				{
					userToken = gc.getToken(userName);
					if (userToken == null) // no login for that name
					{
						System.out.println("Username not recognized. Contact Admin.");
						gc.disconnect();
						doExit = true;
					}
				}
				else
				{
					System.out.println("Error - Group Server not running. Contact Admin.");
					doExit = true;
				}
			}
			else if (loginNumber == 2)
			{
				System.out.println("Exiting...");
				doExit = true;
			}
			else
			{
				System.out.println("Unknown command. Please try again.");
			}
		}
	}
	
	public void serverMenu(UserToken theToken)
	{
		boolean exitKey = false;
		String inputString;
		while (!exitKey)
		{
			System.out.print("Main menu:\n" +
							 "Enter 1 to connect to the File Server,\n" +
							 "enter 2 to connect to the Group Server,\n" +
							 "enter 3 to logout...\n> ");
			inputString = console.nextLine();
			
			switch (Integer.parseInt(inputString))
			{
				case 1:
					System.out.println("Connecting to File Server");
					FileClientUI fcu = new FileClientUI();
					fcu.launchUI(theToken);
					break;
				case 2:
					System.out.println("Connecting to Group Server...");
					GroupClientUI gcu = new GroupClientUI();
					gcu.launchUI(theToken);
					break;
				case 3:
					System.out.println("Logging out...");
					exitKey = true;
					break;
				default:
					System.out.println("Unknown command. Please try again.");
					break;
			}
		}		
	}
}