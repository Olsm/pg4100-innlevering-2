package innlevering2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class QuizClient {
	private final static String serverAdress = "localhost"; //10.21.24.192
	private final static int serverPort = 9876;	// 1342
	private final static String endMessage = "Takk for at du deltok!";
	private Scanner in;
	private Socket connection;
	private DataOutputStream output;
	private DataInputStream input;

	public QuizClient() {

		try (Scanner in = new Scanner(System.in);
			 Socket connection = new Socket(serverAdress, serverPort);
			 DataOutputStream output = new DataOutputStream(connection.getOutputStream());
			 DataInputStream input = new DataInputStream(connection.getInputStream())) {
			/*in = new Scanner(System.in);
			connection = new Socket(serverAdress, serverPort);
			output = new DataOutputStream(connection.getOutputStream());
			input = new DataInputStream(connection.getInputStream());*/
			this.in = in;
			this.connection = connection;
			this.output = output;
			this.input = input;
			output.flush();

			System.out.println("Tilkoblet forfatter-QUIZ server");
			runClient();
		} catch (EOFException e) {
			showMessage("Tilkobling til server avsluttet");
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		new QuizClient();
	}

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

	private void showMessage(String message) {
		System.out.print("\n" + message);
	}

	public void sendMessage (String message) throws IOException {
		output.writeUTF(message);
		output.flush();
	}

	public String readMessage () throws IOException {
		return input.readUTF();
	}
}
