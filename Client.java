package innlevering2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
	private final static String serverAdress = "localhost";
	private final static int serverPort = 1234;
	private final static String endMessage = "Takk for at du deltok!";
	private Scanner in;
	private Socket connection;
	private DataOutputStream output;
	private DataInputStream input;

	public Client () {
		try (Scanner in = new Scanner(System.in);
			 Socket connection = new Socket(serverAdress, serverPort);
			 DataOutputStream output = new DataOutputStream(connection.getOutputStream());
			 DataInputStream input = new DataInputStream(connection.getInputStream()))
		{
			output.flush();

			setIn(in);
			setConnection(connection);
			setOutput(output);
			setInput(input);

			runClient();
		} catch (EOFException e) {
			System.out.println("Server closed the connection");
		} catch (IOException e) {
			System.out.println("Connection problem: " + e.getMessage());
		}
	}

	public static void main(String[] args) {
		new Client();
	}

	private void runClient() throws IOException {
		String message;
		do {
			message = readMessage();
			showMessage(message);
			if (in.hasNextLine())
				sendMessage(in.nextLine());
		}
		while (!message.equals(endMessage));
	}

	private void showMessage(String message) {
		System.out.println(message);
	}

	public void sendMessage (String message) throws IOException {
		output.writeUTF(message);
		output.flush();
	}

	public String readMessage () throws IOException {
		return input.readUTF();
	}

	private void setIn(Scanner in) {
		this.in = in;
	}

	private void setConnection(Socket connection) {
		this.connection = connection;
	}

	private void setOutput(DataOutputStream output) {
		this.output = output;
	}

	private void setInput(DataInputStream input) {
		this.input = input;
	}
}
