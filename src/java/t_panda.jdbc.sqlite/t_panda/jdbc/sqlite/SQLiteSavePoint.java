package t_panda.jdbc.sqlite;

import java.sql.SQLException;
import java.sql.Savepoint;

/**
 * Connection.rollbackメソッドから参照される現在のトランザクション内のポイントであるセーブポイントの表現
 */
public class SQLiteSavePoint implements Savepoint {
    private final String name;

    SQLiteSavePoint(String name) {
        this.name = name;
    }

    @Override
    public int getSavepointId() throws SQLException {
        return 0;
    }

    @Override
    public String getSavepointName() throws SQLException {
        return this.name;
    }
}
