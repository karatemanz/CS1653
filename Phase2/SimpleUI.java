import java.util.Scanner; // Scanner class required for user input
import java.util.List; // to create test UserToken
import java.util.Arrays; // to create test UserToken

public class SimpleUI
{
    public static void main(String[] args)
    {
		loginMenu();
	}
	
	public static void loginMenu()
	{
		FileClient fc = new FileClient();
		GroupClient gc = new GroupClient();
		Scanner console = new Scanner(System.in); // Scanner object for input

		String inputString;
		int menuChoice;
		boolean exitKey = false;
		boolean hasToken = false;
		String userName;
		UserToken userToken = null;
		
		while (!exitKey)
		{
			System.out.print("Enter 1 to login,\nenter 2 to exit...\n> ");
			inputString = console.nextLine();
			
			if (menuChoice == 1)
			{
				System.out.print("Enter your username to login...\n> ");
				userName = console.nextLine();
				menuChoice = Integer.parseInt(inputString);

				// connect to group server and get token
				gc.connect("localhost", 8765);
				if (gc.isConnected()) // check that server is running
				{
					userToken = gc.getToken(userName);
					if (userToken == null) // no login for that name
					{
						System.out.println("Username not recognized. Contact Admin.");
						gc.disconnect();
						exitKey = true;
					}
					else // has a valid token
					{
						hasToken = true;
					}
				}
				else
				{
					System.out.println("Error - Group Server not running. Contact Admin.");
					exitKey = true;
				}
			}
			else if (menuChoice == 2)
			{
				System.out.println("Exiting...");
				if (gc.isConnected())
				{
					gc.disconnect();
				}
				exitKey = true;
			}
			else
			{
				System.out.println("Unknown command. Please try again.");
			}
			
			while (hasToken)
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
						fcu.launchUI(userToken);
						break;
					case 2:
						System.out.println("Connecting to Group Server...");
						GroupClientUI gcu = new GroupClientUI();
						gcu.launchUI(userToken);
						break;
					case 3:
						System.out.println("Logging out...");
						hasToken = false;
						userToken = null;
						break;
					default:
						System.out.println("Unknown command. Please try again.");
						break;
				}
			}
		}
	}
}