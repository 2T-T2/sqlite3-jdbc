package t_panda.jdbc.sqlite.internal.bridge;

import t_panda.jdbc.sqlite.internal.ExceptionMessage;
import t_panda.jdbc.sqlite.internal.SQLiteException;
import t_panda.jdbc.sqlite.internal.SQLiteReturnCode;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Savepoint;

public class SQLiteConnectionBridge {
    public static void begin(NativePointer<NativePointer.ConnectionPtr> conn) throws SQLException {
        try (var commitStmtPtr = NativePointer.createNativeStmtPtr(conn, "begin", null)) {
            SQLiteReturnCode result = SQLite3Native.sqlite3Step(commitStmtPtr);
            if (result.equals(SQLiteReturnCode.SQLITE_BUSY))
                throw new SQLTimeoutException(ExceptionMessage.TIMEOUT.getMessage());
            if (!result.isNoError())
                throw new SQLiteException(conn, result);
        }
    }

    public static void commit(NativePointer<NativePointer.ConnectionPtr> conn) throws SQLException {
        try (var commitStmtPtr = NativePointer.createNativeStmtPtr(conn, "commit", null)) {
            SQLiteReturnCode result = SQLite3Native.sqlite3Step(commitStmtPtr);
            if (result.equals(SQLiteReturnCode.SQLITE_BUSY))
                throw new SQLTimeoutException(ExceptionMessage.TIMEOUT.getMessage());
            if (!result.isNoError())
                throw new SQLiteException(conn, result);
        }
    }

    public static void rollback(NativePointer<NativePointer.ConnectionPtr> conn) throws SQLException {
        try (var commitStmtPtr = NativePointer.createNativeStmtPtr(conn, "rollback", null)) {
            SQLiteReturnCode result = SQLite3Native.sqlite3Step(commitStmtPtr);
            if (result.equals(SQLiteReturnCode.SQLITE_BUSY))
                throw new SQLTimeoutException(ExceptionMessage.TIMEOUT.getMessage());
            if (!result.isNoError())
                throw new SQLiteException(conn, result);
        }
    }

    public static void rollback(NativePointer<NativePointer.ConnectionPtr> conn, Savepoint sp) throws SQLException {
        try (var commitStmtPtr = NativePointer.createNativeStmtPtr(conn, "rollback to " + sp.getSavepointName(), null)) {
            SQLiteReturnCode result = SQLite3Native.sqlite3Step(commitStmtPtr);
            if (result.equals(SQLiteReturnCode.SQLITE_BUSY))
                throw new SQLTimeoutException(ExceptionMessage.TIMEOUT.getMessage());
            if (!result.isNoError())
                throw new SQLiteException(conn, result);
        }
    }

    public static void setSavepoint(NativePointer<NativePointer.ConnectionPtr> conn, String name) throws SQLException{
        try (var commitStmtPtr = NativePointer.createNativeStmtPtr(conn, "savepoint " + name, null)) {
            SQLiteReturnCode result = SQLite3Native.sqlite3Step(commitStmtPtr);
            if (result.equals(SQLiteReturnCode.SQLITE_BUSY))
                throw new SQLTimeoutException(ExceptionMessage.TIMEOUT.getMessage());
            if (!result.isNoError())
                throw new SQLiteException(conn, result);
        }
    }

    public static void releaseSavepoint(NativePointer<NativePointer.ConnectionPtr> conn, Savepoint sp) throws SQLException {
        try (var commitStmtPtr = NativePointer.createNativeStmtPtr(conn, "release " + sp.getSavepointName(), null)) {
            SQLiteReturnCode result = SQLite3Native.sqlite3Step(commitStmtPtr);
            if (result.equals(SQLiteReturnCode.SQLITE_BUSY))
                throw new SQLTimeoutException(ExceptionMessage.TIMEOUT.getMessage());
            if (!result.isNoError())
                throw new SQLiteException(conn, result);
        }
    }
}
