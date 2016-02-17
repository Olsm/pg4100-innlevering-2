package innlevering2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client {
	Socket connection;
	DataOutputStream output;
	DataInputStream input;

	public Client(String address, int port) throws IOException {
		connection = new Socket(address, port);
		output = new DataOutputStream(connection.getOutputStream());
		input = new DataInputStream(connection.getInputStream());
	}

}
