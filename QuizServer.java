package innlevering2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

public class QuizServer implements AutoCloseable {
	private final static int serverPort = 9876;
	private final static String dbHost = "localhost";
	private final static String dbUser = "root";
	private final static String dbPass = "root";
	private ServerSocket socket;
	private ArrayList<QuizConnection> quizConnections;
	private ArrayList<String> table;

	public QuizServer() {
		quizConnections = new ArrayList<>();
		try (ServerSocket socket = new ServerSocket(serverPort);
			 DBHandlerBokliste db = new DBHandlerBokliste(dbHost, dbUser, dbPass))
		{
			this.socket = socket;
			table = db.getTable();

			System.out.println("Waiting for client connection...");
			while (true) {
				startQuizConnection();
			}
		} catch (IOException | SQLException e) {
			showMessage(e.getMessage());
			System.exit(1);
		}
	}

	public static void main (String[] args) {
		new QuizServer();
	}

	private void showMessage (String message) {
		System.out.println(message);
	}

	private void startQuizConnection() {
		QuizConnection quizConnection = new QuizConnection();
		new Thread(quizConnection).start();
		quizConnections.add(quizConnection);
		showMessage("\nConnected with " + quizConnection.connection.getInetAddress().toString());
		showMessage("Number of clients connected: " + quizConnections.size());
	}

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

	@Override
	public void close() throws IOException {
		System.out.println("servertest");
		for (QuizConnection quizConnection : quizConnections) {
			quizConnection.close();
		}
		if (socket != null)
			socket.close();
	}

	private class QuizConnection implements Runnable {
		private final static String continueMessage = "ja";
		Socket connection;
		private DataOutputStream output;
		private DataInputStream input;

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

		@Override
		public void run() {
			Random rnd = new Random();
			boolean continueQuiz = false;
			String answer;

			sendMessage("Vil du delta i forfatter-QUIZ? (ja/nei) ");
			if (readMessage().equals(continueMessage)) {
				continueQuiz = true;
				showMessage("Quiz started with client " + connection.getInetAddress().toString());
			}

			while (continueQuiz) {
				int index = rnd.nextInt(table.size() - 1) + 1;
				String book = table.get(index);
				String[] bookElements = book.split("\\|");
				String bookTitle = bookElements[2];
				String bookAuthor = bookElements[1];

				sendMessage("Hvem har skrevet " + bookTitle + " ");
				answer = readMessage();
				if (verifyAuthor(answer, bookAuthor))
					sendMessage("Riktig!" +  "\nVil du fortsette? (ja/nei) ");
				else
					sendMessage("Feil - det er " + bookAuthor + "\nVil du fortsette? (ja/nei) ");

				if (!readMessage().equalsIgnoreCase(continueMessage)) {
					sendMessage("Takk for at du deltok!");
					continueQuiz = false;
				}
			}
			endQuizConnection(this, "Client ended the quiz");
		}

		private String readMessage () {
			String message = "";
			try {
				message = input.readUTF();
			} catch (IOException e) {
				endQuizConnection(this, "Problem reading message:\n" + e.getMessage());
				e.printStackTrace();
			}
			return message;
		}

		private void sendMessage (String message) {
			try {
				output.writeUTF(message);
				output.flush();
			} catch (IOException e) {
				endQuizConnection(this, "Problem sending answer:\n" + e.getMessage());
			}
		}

		public boolean verifyAuthor (String answer, String bookAuthor) {
			if (!answer.contains(",") && answer.contains(" ")) {
				String[] answerElements = answer.split(" ");
				answer = answerElements[1] + ", " + answerElements[0];
			}
			return answer.equalsIgnoreCase(bookAuthor);
		}

		public void close() throws IOException {
			connection.close();
			input.close();
			output.close();
		}
	}
}
