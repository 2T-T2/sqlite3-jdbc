package t_panda.jdbc.sqlite.internal.bridge;

import t_panda.jdbc.sqlite.internal.SQLiteException;
import t_panda.jdbc.sqlite.internal.SQLiteReturnCode;

import java.sql.SQLException;

public class SQLiteSettings {
    public static void setTimeOut(NativePointer<NativePointer.ConnectionPtr> conn, int sec) throws SQLException {
        SQLiteReturnCode rc = SQLite3Native.sqlite3BusyTimeout(conn, (sec != 0) ? sec * 1000 : Integer.MAX_VALUE);
        if (!rc.isNoError())
            throw new SQLiteException(conn, rc);
    }
}
