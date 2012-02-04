import java.util.Scanner; // Scanner class required for user input

public class SimpleUI
{
    public static void main(String[] args)
    {
		Scanner console = new Scanner(System.in); // Scanner object for input
		boolean exitKey = false;
		FileClient fc = new FileClient();
		GroupClient gc = new GroupClient();
		
		while (!exitKey)
		{
			System.out.println("Enter 1 to connect to the File Server,\n" +
							   "enter 2 to disconnect from the File Server,\n" +
							   "enter 3 to connect to the Group Server,\n" +
							   "enter 4 to disconnect from the Group Server,\n" +
							   "enter 5 to exit:");
			String inputString = console.nextLine();
			
			switch (Integer.parseInt(inputString))
			{
				case 1:
					System.out.println("1");
					fc.connect("localhost", 4321);
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
}