package innlevering2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuizServer {
	private final static int serverPort = 9876;
	private final static String dbHost = "localhost";
	private final static String dbUser = "root";
	private final static String dbPass = "root";
	private ExecutorService threadExecutor;
	private ServerSocket socket;
	private List<QuizConnection> quizConnections;
	private ArrayList<String> table;

	// Create server socket, get table from database, and start QuizConnection threads
	public QuizServer() {
		quizConnections = Collections.synchronizedList(new ArrayList<>());
		// Use try-with-resources to make sure resources are closed
		try (ServerSocket socket = new ServerSocket(serverPort);
			 DBHandlerBokliste db = new DBHandlerBokliste(dbHost, dbUser, dbPass))
		{
			// Set the resources in class
			this.socket = socket;
			threadExecutor = Executors.newCachedThreadPool();
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
		threadExecutor.execute(quizConnection);
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
		threadExecutor.shutdown();
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
				sendMessage("Hvem har skrevet " + bookTitle + "? ");

				// Get the answer from client and verify it is correct
				answer = readMessage();
				if (verifyAuthor(answer, bookAuthor.replaceAll(" ", "")))
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
		private boolean verifyAuthor(String answer, String bookAuthor) {
			boolean isCorrect = false;

			bookAuthor = bookAuthor.replaceAll(" ", "");
			bookAuthor = bookAuthor.replaceAll(",", "");
			bookAuthor = bookAuthor.replaceAll("\\.", "");
			answer = answer.replaceAll("\\s+", " ");
			answer = answer.replaceAll("\\.", "");

			if (!answer.contains(",") && answer.contains(" ")) {
				answer = answer.replace(" ", ",");
			}

			if (answer.contains(",")) {
				answer = answer.replaceAll(" ", "");
				String[] answerElements = answer.split(",");
				isCorrect = bookAuthor.equalsIgnoreCase(answerElements[1] + answerElements[0]);
				if (!isCorrect) isCorrect = bookAuthor.equalsIgnoreCase(answerElements[0] + answerElements[1]);
			}

			return isCorrect;
		}

		// Close all resources in this server-client connection
		public void close() throws IOException {
			connection.close();
			input.close();
			output.close();
		}
	}
}
