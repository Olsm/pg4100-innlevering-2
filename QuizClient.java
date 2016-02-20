package innlevering2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class QuizClient {
	private final static String serverAddress = "localhost";
	private final static int serverPort = 9876;
	private final static String endMessage = "Takk for at du deltok!";
	private Scanner in;
	private Socket connection;
	private DataOutputStream output;
	private DataInputStream input;

	// Connect to the server, create resources and start the client
	public QuizClient() {
		// Use try-with-resources to make sure resources are closed
		try (Scanner in = new Scanner(System.in);
			 Socket connection = new Socket(serverAddress, serverPort);
			 DataOutputStream output = new DataOutputStream(connection.getOutputStream());
			 DataInputStream input = new DataInputStream(connection.getInputStream()))
		{
			// Set the resources in class variables for easy access by other methods
			this.in = in;
			this.connection = connection;
			this.output = output;
			this.input = input;
			output.flush();

			// Start the client
			System.out.println("Tilkoblet forfatter-QUIZ server");
			runClient();
		} catch (EOFException e) {
			showMessage("Tilkobling til server avsluttet");
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

	// Main method, simply start a new instance of the client
	public static void main(String[] args) {
		new QuizClient();
	}

	// Get and show message from server and reply with message from console
	private void runClient() throws IOException {
		String message;
		do {
			message = readMessage();
			showMessage(message);
			if (!message.equals(endMessage) && in.hasNextLine())
				sendMessage(in.nextLine());
		}
		while (!message.equals(endMessage));
	}

	// Simple method for showing messages, currently by printing to console
	private void showMessage(String message) {
		System.out.print("\n" + message);
	}

	// Send a message to the server
	public void sendMessage (String message) throws IOException {
		output.writeUTF(message);
		output.flush();
	}

	// Read message from server
	public String readMessage () throws IOException {
		return input.readUTF();
	}
}
