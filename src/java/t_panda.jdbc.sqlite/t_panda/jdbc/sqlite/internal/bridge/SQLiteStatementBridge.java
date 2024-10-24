package t_panda.jdbc.sqlite.internal.bridge;

import t_panda.jdbc.sqlite.internal.SQLiteException;
import t_panda.jdbc.sqlite.internal.ExceptionMessage;
import t_panda.jdbc.sqlite.internal.SQLiteReturnCode;
import t_panda.jdbc.sqlite.internal.util.function.ThrowableFunction;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;

public class SQLiteStatementBridge {

    public static int executeUpdate(NativePointer<NativePointer.ConnectionPtr> conn, NativePointer<NativePointer.StatementPtr> stmtPtr) throws SQLException {
        SQLiteReturnCode result = SQLite3Native.sqlite3Step(stmtPtr);
        if (result.equals(SQLiteReturnCode.SQLITE_BUSY))
            throw new SQLTimeoutException(ExceptionMessage.TIMEOUT.getMessage());
        if (result.equals(SQLiteReturnCode.SQLITE_ROW))
            throw new SQLException(ExceptionMessage.ONLY_NO_RESULTSET.getMessage());
        if (!result.equals(SQLiteReturnCode.SQLITE_DONE))
            throw new SQLiteException(conn, result);
        if (!result.isNoError())
            throw new SQLiteException(conn, result);

        try (var updCountStmt = NativePointer.createNativeStmtPtr(conn, "select changes()", null)) {
            result = SQLite3Native.sqlite3Step(updCountStmt);
            if (result.equals(SQLiteReturnCode.SQLITE_BUSY))
                throw new SQLTimeoutException(ExceptionMessage.TIMEOUT.getMessage());
            if (!result.equals(SQLiteReturnCode.SQLITE_ROW))
                throw new SQLiteException(conn, result);
            if (!result.isNoError())
                throw new SQLiteException(conn, result);
            return SQLite3Native.sqlite3ColumnInt(updCountStmt, 0);
        }
    }

    public static <X extends SQLException, T> T execute(NativePointer<NativePointer.StatementPtr> stmtPtr, ThrowableFunction<SQLiteReturnCode, T, X> function) throws X {
        return function.apply(SQLite3Native.sqlite3Step(stmtPtr));
    }

    public static void first(NativePointer<NativePointer.ConnectionPtr> conn, NativePointer<NativePointer.StatementPtr> stmtPtr) throws SQLiteException {
        SQLiteReturnCode result = SQLite3Native.sqlite3Reset(stmtPtr);  // step取り消し
        if (!result.isNoError())
            throw new SQLiteException(conn, result);
    }

}
