package innlevering2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

public class QuizServer {
	private final static int serverPort = 9876;
	private final static String dbHost = "localhost";
	private final static String dbUser = "root";
	private final static String dbPass = "root";
	private ServerSocket socket;
	private ArrayList<QuizConnection> quizConnections;
	private ArrayList<String> table;

	// Create server socket, get table from database, and start QuizConnection threads
	public QuizServer() {
		quizConnections = new ArrayList<>();
		// Use try-with-resources to make sure resources are closed
		try (ServerSocket socket = new ServerSocket(serverPort);
			 DBHandlerBokliste db = new DBHandlerBokliste(dbHost, dbUser, dbPass))
		{
			// Set the resources in class
			this.socket = socket;
			table = db.getTable();

			// Wait for and create client connections by launching new QuizConnection threads
			System.out.println("Waiting for client connection...");
			while (true) {
				startQuizConnection();
			}
		} catch (IOException | SQLException e) {
			showMessage(e.getMessage());
		}
		// Close all QuizConnection resources
		close();
	}

	// Main application starts a new QuizServer instance to start the server
	public static void main (String[] args) {
		new QuizServer();
	}

	// Simple method for showing messages, currently by printing to console
	private void showMessage (String message) {
		System.out.println(message);
	}

	// Create a new QuizConnection instance and start a new thread
	private void startQuizConnection() {
		QuizConnection quizConnection = new QuizConnection();
		new Thread(quizConnection).start();
		quizConnections.add(quizConnection);
		showMessage("\nConnected with " + quizConnection.connection.getInetAddress().toString());
		showMessage("Number of clients connected: " + quizConnections.size());
	}

	// End a QuizConnection, close its resources by calling close method
	private void endQuizConnection(QuizConnection quizConnection, String cause) {
		try { quizConnection.close(); }
		catch (IOException e) {
			showMessage("Could not end connection with quizConnection \nError message: " + e.getMessage());
		}
		quizConnections.remove(quizConnection);
		showMessage("\nConection to " + quizConnection.connection.getInetAddress().toString()
				+ " has ended: " + cause);
		showMessage("Number of clients connected: " + quizConnections.size());
	}

	// Close all resources left in any QuizConnection
	public void close() {
		try {
			for (QuizConnection quizConnection : quizConnections) {
				quizConnection.close();
			}
		} catch (IOException e) {
			showMessage("An error occurred when closing server:\n" + e.getMessage());
		}
	}

	/* Server Quiz connection with client
	 * This is the server Quiz instance that handles the quiz
	 * Establishes a connection with a client
	 * Sends questions to client and validates answer */
	private class QuizConnection implements Runnable {
		private final static String continueMessage = "ja";
		Socket connection;
		private DataOutputStream output;
		private DataInputStream input;

		// Establish a connection with the client and create resources
		QuizConnection() {
			try /*(Socket connection = socket.accept();
				 DataInputStream input = new DataInputStream(connection.getInputStream());
				 DataOutputStream output = new DataOutputStream(connection.getOutputStream()))*/
			{
				connection = socket.accept();
				input = new DataInputStream(connection.getInputStream());
				output = new DataOutputStream(connection.getOutputStream());
				output.flush();
			} catch (IOException e) {
				System.out.println("Could not start connection: " + e.getMessage());
			}
		}

		// Run the quiz with a client
		@Override
		public void run() {
			Random random = new Random();
			boolean continueQuiz = false;
			String answer;

			// Ask client if he would like to start the quiz
			sendMessage("Vil du delta i forfatter-QUIZ? (ja/nei) ");
			if (readMessage().equals(continueMessage)) {
				continueQuiz = true;
				showMessage("Quiz started with client " + connection.getInetAddress().toString());
			}

			// run the quiz
			while (continueQuiz) {
				// Get title and author from a random book
				int index = random.nextInt(table.size() - 1) + 1;
				String book = table.get(index);
				String[] bookElements = book.split("\\|");
				String bookTitle = bookElements[2];
				String bookAuthor = bookElements[1];

				// send a question to the client
				sendMessage("Hvem har skrevet " + bookTitle + " ");

				// Get the answer from client and verify it is correct
				answer = readMessage();
				if (verifyAuthor(answer, bookAuthor))
					sendMessage("Riktig!" +  "\nVil du fortsette? (ja/nei) ");
				else
					sendMessage("Feil - det er " + bookAuthor + "\nVil du fortsette? (ja/nei) ");

				// Check if client wants to continue the quiz
				if (!readMessage().equalsIgnoreCase(continueMessage)) {
					sendMessage("Takk for at du deltok!");
					continueQuiz = false;
				}
			}
			endQuizConnection(this, "Client ended the quiz");
		}

		// Read message from the client
		private String readMessage () {
			String message = "";
			try {
				message = input.readUTF();
			} catch (IOException e) {
				endQuizConnection(this, "Problem reading message:\n" + e.getMessage());
			}
			return message;
		}

		// Send message to the client
		private void sendMessage (String message) {
			try {
				output.writeUTF(message);
				output.flush();
			} catch (IOException e) {
				endQuizConnection(this, "Problem sending answer:\n" + e.getMessage());
			}
		}

		// Check if the answer (author) was correct
		public boolean verifyAuthor (String answer, String bookAuthor) {
			if (!answer.contains(",") && answer.contains(" ")) {
				String[] answerElements = answer.split(" ");
				answer = answerElements[1] + ", " + answerElements[0];
			}
			return answer.equalsIgnoreCase(bookAuthor);
		}

		// Close all resources in this server-client connection
		public void close() throws IOException {
			connection.close();
			input.close();
			output.close();
		}
	}
}
