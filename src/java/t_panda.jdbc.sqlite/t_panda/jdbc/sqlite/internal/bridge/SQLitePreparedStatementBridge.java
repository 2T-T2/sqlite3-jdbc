package t_panda.jdbc.sqlite.internal.bridge;

import t_panda.jdbc.sqlite.internal.ExceptionMessage;
import t_panda.jdbc.sqlite.internal.SQLiteException;
import t_panda.jdbc.sqlite.internal.SQLiteReturnCode;

import java.sql.Blob;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;

public class SQLitePreparedStatementBridge {
    public static void resetStatement(NativePointer<NativePointer.ConnectionPtr> connPtr, NativePointer<NativePointer.StatementPtr> stmtPtr) throws SQLException {
        SQLiteReturnCode result = SQLite3Native.sqlite3Reset(stmtPtr);
        if (result.equals(SQLiteReturnCode.SQLITE_BUSY))
            throw new SQLTimeoutException(ExceptionMessage.TIMEOUT.getMessage());

        if (!result.isNoError())
            throw new SQLiteException(connPtr, result);
    }

    public static void setNull(NativePointer<NativePointer.ConnectionPtr> connPtr, NativePointer<NativePointer.StatementPtr> stmtPtr, int parameterIndex) throws SQLException {
        SQLiteReturnCode result = SQLite3Native.sqlite3BindNull(stmtPtr, parameterIndex);
        if (!result.isNoError())
            throw new SQLiteException(connPtr, result);
    }

    public static void setBoolean(NativePointer<NativePointer.ConnectionPtr> connectionNativePointer, NativePointer<NativePointer.StatementPtr> statementNativePtr, int parameterIndex, boolean x) throws SQLException {
        final String val;
        if (x) val = "1";
        else   val = "0";

        SQLiteReturnCode result = SQLite3Native.sqlite3BindText(statementNativePtr, parameterIndex, val);
        if (!result.isNoError())
            throw new SQLiteException(connectionNativePointer, result);
    }

    public static void setByte(NativePointer<NativePointer.ConnectionPtr> connectionNativePointer, NativePointer<NativePointer.StatementPtr> statementNativePtr, int parameterIndex, byte x) throws SQLException {
        SQLiteReturnCode result = SQLite3Native.sqlite3BindInt(statementNativePtr, parameterIndex, x);
        if (!result.isNoError())
            throw new SQLiteException(connectionNativePointer, result);
    }

    public static void setShort(NativePointer<NativePointer.ConnectionPtr> connectionNativePointer, NativePointer<NativePointer.StatementPtr> statementNativePtr, int parameterIndex, short x) throws SQLException {
        SQLiteReturnCode result = SQLite3Native.sqlite3BindInt(statementNativePtr, parameterIndex, x);
        if (!result.isNoError())
            throw new SQLiteException(connectionNativePointer, result);
    }

    public static void setInt(NativePointer<NativePointer.ConnectionPtr> connectionNativePointer, NativePointer<NativePointer.StatementPtr> statementNativePtr, int parameterIndex, int x) throws SQLException {
        SQLiteReturnCode result = SQLite3Native.sqlite3BindInt(statementNativePtr, parameterIndex, x);
        if (!result.isNoError())
            throw new SQLiteException(connectionNativePointer, result);
    }

    public static void setLong(NativePointer<NativePointer.ConnectionPtr> connectionNativePointer, NativePointer<NativePointer.StatementPtr> statementNativePtr, int parameterIndex, long x) throws SQLException {
        SQLiteReturnCode result = SQLite3Native.sqlite3BindInt64(statementNativePtr, parameterIndex, x);
        if (!result.isNoError())
            throw new SQLiteException(connectionNativePointer, result);
    }

    public static void setFloat(NativePointer<NativePointer.ConnectionPtr> connectionNativePointer, NativePointer<NativePointer.StatementPtr> statementNativePtr, int parameterIndex, float x) throws SQLException {
        SQLiteReturnCode result = SQLite3Native.sqlite3BindDouble(statementNativePtr, parameterIndex, x);
        if (!result.isNoError())
            throw new SQLiteException(connectionNativePointer, result);
    }

    public static void setDouble(NativePointer<NativePointer.ConnectionPtr> connectionNativePointer, NativePointer<NativePointer.StatementPtr> statementNativePtr, int parameterIndex, double x) throws SQLException {
        SQLiteReturnCode result = SQLite3Native.sqlite3BindDouble(statementNativePtr, parameterIndex, x);
        if (!result.isNoError())
            throw new SQLiteException(connectionNativePointer, result);
    }

public static void setString(NativePointer<NativePointer.ConnectionPtr> connectionNativePointer, NativePointer<NativePointer.StatementPtr> statementNativePtr, int parameterIndex, String x) throws SQLException {
        SQLiteReturnCode result = SQLite3Native.sqlite3BindText(statementNativePtr, parameterIndex, x);
        if (!result.isNoError())
            throw new SQLiteException(connectionNativePointer, result);
    }

    public static void setBytes(NativePointer<NativePointer.ConnectionPtr> connectionNativePointer, NativePointer<NativePointer.StatementPtr> statementNativePtr, int parameterIndex, byte[] x) throws SQLException {
        SQLiteReturnCode result = SQLite3Native.sqlite3BindBlob(statementNativePtr, parameterIndex, x);
        if (!result.isNoError())
            throw new SQLiteException(connectionNativePointer, result);
    }

    public static void clearParameters(NativePointer<NativePointer.ConnectionPtr> connectionNativePointer, NativePointer<NativePointer.StatementPtr> statementNativePtr) throws SQLException {
        SQLiteReturnCode result = SQLite3Native.sqlite3ClearBindings(statementNativePtr);
        if (!result.isNoError())
            throw new SQLiteException(connectionNativePointer, result);
    }

    public static void setBlob(NativePointer<NativePointer.ConnectionPtr> connectionNativePointer, NativePointer<NativePointer.StatementPtr> statementNativePtr, int parameterIndex, Blob x) throws SQLException {
        byte[] bytes = x.getBytes(0, (int) x.length());
        SQLiteReturnCode result = SQLite3Native.sqlite3BindBlob(statementNativePtr, parameterIndex, bytes);
        if (!result.isNoError())
            throw new SQLiteException(connectionNativePointer, result);
    }
}
