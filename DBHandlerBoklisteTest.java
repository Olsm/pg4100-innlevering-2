package innlevering2;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class DBHandlerBoklisteTest {

	DBHandlerBokliste dbHB;

	@Before
	public void setUp() {
		try {
			dbHB = new DBHandlerBokliste("localhost", "root", "root");
		} catch (SQLException e) {
			fail("SQLException: " + e.getMessage());
		}
	}

	@After
	public void tearDown() {
		try {
			dbHB.close();
		} catch (SQLException e) {
			fail("SQLException: " + e.getMessage());
		}
	}

	@Test
	public void testGetTable() {
		try {
			ArrayList<String> table = dbHB.getTable();
			assertEquals("ISBN|forfatter|tittel", table.get(0));
			assertTrue(table.contains("0-099-30278-0|DIAMOND, JARED|GUNS, GERMS AND STEEL"));
		} catch (SQLException e) {
			fail("SQLException: " + e.getMessage());
		}
	}
}