package t_panda.jdbc.sqlite;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class SQLiteStatementHelper {
    public static int executeQueryLastUpdateCount(SQLiteConnection conn) throws SQLException {
        try (
            Statement updCountStmt = conn.createStatement();
            ResultSet updCountResultSet = updCountStmt.executeQuery("select changes()")
        ) {
            updCountResultSet.next();
            return updCountResultSet.getInt(1);
        }
    }
}
