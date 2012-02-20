import java.util.Scanner; // Scanner class required for user input
import java.util.List; // to create test UserToken
import java.util.Arrays; // to create test UserToken

public class MainUI
{
    public static void main(String[] args) {
		if (args.length == 2) {
			try {
				String groupServer = args[0];
				int groupPort = Integer.parseInt(args[1]);
				loginMenu(groupServer, groupPort);
			}
			catch (NumberFormatException e) {
				System.out.printf("Error parsing port number " + args[1] +
								  " for Group Server - please retry.");
			}
		}
		else {
			loginMenu("localhost", 8765); // default to localhost
		}
	}
	
	public static void loginMenu(String groupServerAddress, int groupServerPort)
	{
		FileClient fc = new FileClient();
		GroupClient gc = new GroupClient();
		Scanner console = new Scanner(System.in); // Scanner object for input

		String inputString;
		int menuChoice;
		boolean exitKey = false;
		boolean hasToken = false;
		String userName = new String();
		UserToken userToken = null;
		String serverAddress;
		int portNumber;
		
		while (!exitKey)
		{
			System.out.print("Enter 1 to login,\nenter 2 to exit...\n> ");
			inputString = console.nextLine();
			
			try
			{
				menuChoice = Integer.parseInt(inputString);
			}
			catch(Exception e)
			{
				menuChoice = -1;
			}
			
			if (menuChoice == 1)
			{
				System.out.print("Enter your username to login...\n> ");
				userName = console.nextLine();

				// connect to group server and get token
				gc.connect(groupServerAddress, groupServerPort);
				if (gc.isConnected()) // check that server is running
				{
					userToken = gc.getToken(userName);
					if (userToken == null) // no login for that name
					{
						System.out.println("Username not recognized. Contact Admin.");
						gc.disconnect();
					}
					else // has a valid token, can disconnect from gc
					{
						hasToken = true;
						gc.disconnect();
					}
				}
				else
				{
					System.out.println("Error - Group Server not reached at given address.");
				}
			}
			else if (menuChoice == 2)
			{
				System.out.println("Exiting...");
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
								 "enter 3 to logout...\n" +
								 userName + "> ");
				inputString = console.nextLine();
				
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
						// prompt user for server address, port
						System.out.print("Please enter the IP address of the File Server...\n" +
										 userName + "> ");
						serverAddress = console.nextLine();
						System.out.print("Please enter the port number of the File Server...\n" +
										 userName + "> ");
						portNumber = Integer.parseInt(console.nextLine());
						System.out.println("Connecting to File Server at " +
										   serverAddress + " port " +
										   portNumber + "...");
						FileClientUI fcu = new FileClientUI();
						fcu.launchUI(userToken, serverAddress, portNumber);
						break;
					case 2:
						System.out.println("Connecting to Group Server...");
						GroupClientUI gcu = new GroupClientUI();
						gcu.launchUI(userToken, groupServerAddress, groupServerPort);
						hasToken = false;
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