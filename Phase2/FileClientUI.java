import java.util.Scanner; // Scanner class required for user input
import java.util.List;

public class FileClientUI
{
	FileClient fc = new FileClient();
	
	public boolean launchUI(UserToken token)
	{
		if (fc.connect("localhost", 4321))
		{
			Scanner console = new Scanner(System.in); // Scanner object for input
			boolean exitKey = false;
			List<String> aList;
			
			while (!exitKey)
			{
				/* FileClient methods:
				 * public boolean delete(String filename, UserToken token)
				 * public boolean download(String sourceFile, String destFile, UserToken token)
				 * public List<String> listFiles(UserToken token)
				 * public boolean upload(String sourceFile, String destFile, String group, UserToken token)
				 */
				System.out.println("Enter 1 to list files,\n" +
								   "enter 2 to upload a file to the File Server,\n" +
								   "enter 3 to download a file to the File Server,\n" +
								   "enter 4 to delete a file to the File Server,\n" +
								   "enter 5 to disconnect and exit:");
				String inputString = console.nextLine();
				
				switch (Integer.parseInt(inputString))
				{
					case 1:
						System.out.println("1");
						//aList = fc.listFiles(token);
						// output the list
						System.out.println("listFiles stub");
						break;
					case 2:
						System.out.println("2");
						//fc.upload(sourceFile, destFile, group, token); // returns a boolean
						System.out.println("upload stub");
						break;
					case 3:
						System.out.println("3");
						//fc.download(sourceFile, destFile, token); // returns a boolean
						System.out.println("download stub");
						break;
					case 4:
						//fc.delete(filename, token); // returns a boolean
						System.out.println("delete stub");
						break;
					case 5:
						System.out.println("Disconnecting and exiting...");
						fc.disconnect(); // catch boolean, return its value
						exitKey = true;
						break;
					default:
						exitKey = true;
						break;
				}
			}
			
			return true;
		}
		else // error connecting
		{
			System.out.println("Error connecting to File Server");
			return false;
		}
	}
}