package innlevering2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectToDB {
    private Connection con;

    // Connect to database using DriverManager.getConnection
    public ConnectToDB (String serverName, String databaseName,
                        String user, String password) throws SQLException {
        con = DriverManager.getConnection("jdbc:mysql://" +
                serverName + "/" + databaseName, user, password);
    }

    // Close the database connection
    public void close() throws SQLException {
        con.close();
    }

    // Return the database connection
    public Connection getConnection() {
        return con;
    }
}
