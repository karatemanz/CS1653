import java.util.Scanner; // Scanner class required for user input
import java.util.List;
import java.util.ArrayList;
import java.security.*;
import java.io.Console;

public class MainUI {
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
	
	public static void loginMenu(String groupServerAddress, int groupServerPort) {
		FileClient fc = new FileClient();
		GroupClient gc = new GroupClient();
		Scanner scan = new Scanner(System.in); // Scanner object for input
		Console console = System.console(); // for password input

		String inputString;
		int menuChoice;
		boolean exitKey = false;
		boolean hasToken = false;
		String userName = new String();
		UserToken userToken = null;
		String groupName;
		String serverAddress;
		int portNumber;
		
		// connect to group client for login and tokens
		gc.connect(groupServerAddress, groupServerPort);

		while (!exitKey) {
			System.out.print("Enter 1 to login,\nenter 2 to exit...\n> ");
			inputString = scan.nextLine();
			
			try {
				menuChoice = Integer.parseInt(inputString);
			}
			catch(Exception e) {
				menuChoice = -1;
			}
			
			if (menuChoice == 1) {
				System.out.print("Enter your username to login...\n> ");
				userName = scan.nextLine();
				char pwArray[] = console.readPassword("Enter your password...\n> ");
				
				if (gc.isConnected()) { // check that server is running
					// get session key
					if (gc.getSessionKeys()) {
						System.out.println("Session key obtained. Connection to server encrypted.");
					}
					else {
						System.out.println("Error while obtaining session key from Group Server. Exiting.");
						break;
					}

					userToken = gc.getToken(userName, pwArray);
					if (userToken == null) { // no login for that name
						System.out.println("Username/password combination not recognized.");
					}
					else { // has a valid token
						hasToken = true;
						System.out.println("Password accepted. Welcome, " + userName);
					}
				}
				else {
					System.out.println("Error - Group Server not reached at given address.");
				}
			}
			else if (menuChoice == 2) {
				System.out.println("Exiting...");
				exitKey = true;
			}
			else {
				System.out.println("Unknown command. Please try again.");
			}
			
			while (hasToken) {
				System.out.print("Main menu:\n" +
								 "Enter 1 to connect to the File Server,\n" +
								 "enter 2 to connect to the Group Server,\n" +
								 "enter 3 to logout...\n" +
								 userName + "> ");
				inputString = scan.nextLine();
				
				try {
					menuChoice = Integer.parseInt(inputString);
				}
				catch(Exception e) {
					menuChoice = -1;
				}

				switch (menuChoice) {
					case 1:
						// prompt user for group name, server address, port
						System.out.print("Please enter the group to use File Server...\n" +
										 userName + "> ");
						groupName = scan.nextLine();
						System.out.print("Please enter the IP address of the File Server...\n" +
										 userName + "> ");
						serverAddress = scan.nextLine();
						System.out.print("Please enter the port number of the File Server...\n" +
										 userName + "> ");
						portNumber = Integer.parseInt(scan.nextLine());
						userToken = gc.getGroupToken(userToken, groupName, serverAddress, portNumber);
						ArrayList<Key> keys = new ArrayList<Key>();
						keys = gc.getGroupKeys(userToken);
						System.out.println("Connecting to File Server at " +
										   serverAddress + " port " +
										   portNumber + "...");
						FileClientUI fcu = new FileClientUI();
						fcu.launchUI(userToken, keys, serverAddress, portNumber);
						keys = null; // clear keys from memory
						hasToken = false;
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
		
		// we're done with the server, close connection
		gc.disconnect();
	}
}