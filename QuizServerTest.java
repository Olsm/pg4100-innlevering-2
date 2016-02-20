package innlevering2;

import org.junit.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class QuizServerTest {
	private final static String serverAddress = "localhost";
	private final static int serverPort = 9876;
	static Thread serverThread;
	Socket connection;
	DataOutputStream output;
	DataInputStream input;

	@BeforeClass
	public static void setUpServer() {
		serverThread = new Thread(QuizServer::new);
		serverThread.start();
	}

	@Before
	public void setUpClient() {
		try {
			connection = new Socket(serverAddress, serverPort);
			output = new DataOutputStream(connection.getOutputStream());
			input = new DataInputStream(connection.getInputStream());
			output.flush();
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	// Helper method for starting a quiz and returning first message
	private String startQuiz() throws IOException {
		// Start the quiz and get first message
		readMessage();
		writeMessage("ja");
		return readMessage();
	}

	private String readMessage() throws IOException {
		return input.readUTF();
	}

	private void writeMessage(String message) throws IOException {
		output.writeUTF(message);
	}

	@Test
	public void testQuizWelcomeMessage () {
		try {
			String message = readMessage();
			assertEquals("Vil du delta i forfatter-QUIZ? (ja/nei) ", message);
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testDoNotParticipate() {
		try {
			readMessage();
			writeMessage("nei");
			readMessage();
			fail("connection was not ended");
		} catch (EOFException e) {
			// Success, connection was ended
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testQuestionFormat() {
		try {
			String message = startQuiz();

			// Verify first message is a question about book author
			assertTrue(message.startsWith("Hvem har skrevet "));
			assertTrue(message.endsWith("? "));
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCorrectAnswer() {
		try (DBHandlerBokliste db = new DBHandlerBokliste("localhost", "root", "root")){
			String message = startQuiz();

			// Find correct answer
			ArrayList<String> bookList = db.getTable();
			String bookTitle = message.substring(message.lastIndexOf("skrevet") + 8, message.indexOf("?"));
			String author = "";
			for (String s : bookList) {
				if (s.contains(bookTitle)) {
					author = s.substring(s.indexOf("|") + 1, s.indexOf("|" + bookTitle));
					break;
				}
				if (bookList.indexOf(s) == bookList.size())
					fail("Could not find author in database table");
			}

			// Send answer and verify validation is correct
			writeMessage(author);
			assertEquals("Riktig!\nVil du fortsette? (ja/nei) ", readMessage());
		} catch (IOException | SQLException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testIncorrectAnswer() {
		try {
			startQuiz();

			// Send an incorrect answer
			writeMessage("incorrect");

			// Verify correct validation of incorrect answer
			String message = readMessage();
			assertTrue(message.startsWith("Feil - det er "));
			assertTrue(message.endsWith("Vil du fortsette? (ja/nei) "));
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testParticipateAndEndQuiz() {
		try {
			startQuiz();

			// Participate by sending an answer
			writeMessage("answer");

			// End the quiz
			readMessage();
			writeMessage("nei");
			assertEquals("Takk for at du deltok!", readMessage());

			// Verify quiz has ended
			readMessage();
			fail("connection was not ended");
		} catch (EOFException e) {
			// Success, connection was ended
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@After
	public void tearDown() {
		try {
			input.close();
			output.close();
			connection.close();
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}
}