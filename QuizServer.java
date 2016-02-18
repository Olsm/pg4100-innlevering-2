package innlevering2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
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

	public QuizServer() {
		quizConnections = new ArrayList<>();
		try (ServerSocket socket = new ServerSocket(serverPort);
			 DBHandlerBokliste db = new DBHandlerBokliste(dbHost, dbUser, dbPass)) {
			this.socket = socket;
			table = db.getTable();

			System.out.println("Waiting for client connection...");
			while (true) {
				QuizConnection quizConnection = new QuizConnection();
				new Thread(quizConnection).start();
				quizConnections.add(quizConnection);
				showMessage("Connected with " + quizConnection.connection.getInetAddress().toString());
				showMessage("Number of clients connected: " + quizConnections.size());
			}
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}
	}

	public static void main (String[] args) {
		new QuizServer();
	}

	private void showMessage (String message) throws IOException {
		System.out.println(message);
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
				System.out.println("Error: " + e.getMessage());
			}
		}

		@Override
		public void run() {
			try {
				runQuiz();
			} catch (EOFException e) {
				System.out.println("QuizServer closed the connection");
			} catch (IOException e) {
				System.out.println("QuizConnection problem: " + e.getMessage());
				e.printStackTrace();
			}
		}

		private void runQuiz() throws IOException {
			Random rnd = new Random();
			boolean continueQuiz = false;
			String answer;

			sendMessage("Vil du delta i forfatter-QUIZ? (ja/nei) ");
			if (readMessage().equals(continueMessage))
				continueQuiz = true;

			while (continueQuiz) {
				int index = rnd.nextInt(table.size() - 1) + 1;
				String book = table.get(index);
				String[] bookElements = book.split("\\|");
				String bookTitle = bookElements[2];
				String bookAuthor = bookElements[1];

				sendMessage("Hvem har skrevet " + bookTitle + " ");
				answer = readMessage();
				if (answer.equalsIgnoreCase(bookAuthor))
					sendMessage("Riktig!" +  "\nVil du fortsette? (ja/nei) ");
				else
					sendMessage("Feil - det er " + bookAuthor + "\nVil du fortsette? (ja/nei) ");

				if (!readMessage().equalsIgnoreCase(continueMessage)) {
					sendMessage("Takk for at du deltok!");
					continueQuiz = false;
				}
			}
		}

		private String readMessage () throws IOException {
			return input.readUTF();
		}

		private void sendMessage (String message) throws IOException {
			output.writeUTF(message);
			output.flush();
		}

		public void setConnection(Socket connection) {
			this.connection = connection;
		}

		public void setOutput(DataOutputStream output) {
			this.output = output;
		}

		public void setInput(DataInputStream input) {
			this.input = input;
		}
	}
}
