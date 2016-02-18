package innlevering2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

public class DBHandlerBokliste implements AutoCloseable {
	private static final String dbName = "pg4100innlevering2";
	private ConnectToDB db;
	private PreparedStatement pstmtGetTable;
	private PreparedStatement pstmtGetRow;
	
	public DBHandlerBokliste (String host, String user, String password) throws SQLException {
		// Connect to database local host and get connection.
		db = new ConnectToDB (host, dbName, user, password);
		Connection con = db.getConnection();
		
		//prepare the sql statements
		pstmtGetTable = con.prepareStatement("SELECT * FROM `bokliste`");
		pstmtGetRow = con.prepareStatement("SELECT * FROM `bokliste` WHERE `forfatter` = ? AND `tittel` = ?");
	}
	
	// Close PreparedStatements and the database connection.
	public void close() throws SQLException {
		pstmtGetTable.close();
		pstmtGetRow.close();
		db.close();
	}
	
	// Get all rows in table and return arraylist of strings
	public ArrayList<String> getTable() throws SQLException {
		ArrayList<String> table = new ArrayList<>();
		ResultSet rs = executeSQLSelect (pstmtGetTable, null, null, null); 
		
		ResultSetMetaData rsmd = rs.getMetaData();
		table.add (rsmd.getColumnName(1) + "|" + rsmd.getColumnName(2) + "|" 
				+ rsmd.getColumnName(3));
		
		while (rs.next()) {
			table.add(rs.getString("isbn") + "|" + rs.getString ("forfatter") 
					+ "|" + rs.getString ("tittel"));
		}
		rs.close();
		return table;
	}
	
	// Private method to execute SQL select statements
	private ResultSet executeSQLSelect (PreparedStatement pstmt, 
			String arg1, String arg2, String arg3) throws SQLException {
		prepareSQL(pstmt, arg1, arg2, arg3);
		return pstmt.executeQuery();	// Execute SQL select and return result
	}
	
	// Private method to prepare SQL statements
	private PreparedStatement prepareSQL (PreparedStatement pstmt, 
			String arg1, String arg2, String arg3) throws SQLException {
		
		// Set string for arguments only if they are not null
		if (arg1 != null) 
			pstmt.setString(1, arg1);
		if (arg2 != null)
			pstmt.setString(2, arg2);
		if (arg3 != null)
			pstmt.setString(3, arg3);
		
		return pstmt;	// Return the prepared SQL PreparedStatement
	}
}
