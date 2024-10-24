package t_panda.jdbc.sqlite.internal.bridge;

import t_panda.jdbc.sqlite.internal.ExceptionMessage;
import t_panda.jdbc.sqlite.internal.SQLiteException;
import t_panda.jdbc.sqlite.internal.SQLiteReturnCode;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;

public class SQLiteResultSetBridge {
    public static boolean next(NativePointer<NativePointer.ConnectionPtr> connPtr, NativePointer<NativePointer.StatementPtr> stmtPtr) throws SQLException {
        SQLiteReturnCode result = SQLite3Native.sqlite3Step(stmtPtr);
        if (result.equals(SQLiteReturnCode.SQLITE_BUSY))
            throw new SQLTimeoutException(ExceptionMessage.TIMEOUT.getMessage());

        if (!result.isNoError())
            throw new SQLiteException(connPtr, result);

        return !result.equals(SQLiteReturnCode.SQLITE_DONE);
    }

    public static boolean isNull(NativePointer<NativePointer.StatementPtr> stmtPtr, int lastGetColumnIndex) {
        return SQLite3Native.sqlite3ColumnIsNull(stmtPtr, lastGetColumnIndex);
    }

    public static String[] getColumnNames(NativePointer<NativePointer.StatementPtr> stmtPtr) {
        String[] columnNames = new String[SQLite3Native.sqlite3ColumnCount(stmtPtr)];
        for (int i = 1; i <= columnNames.length; i++) {
            columnNames[i-1] = SQLite3Native.sqlite3ColumnName(stmtPtr, i);
        }
        return columnNames;
    }

    public static String getString(NativePointer<NativePointer.StatementPtr> stmtPtr, int columnIndex) {
        return SQLite3Native.sqlite3ColumnText(stmtPtr, columnIndex);
    }

    public static boolean getBoolean(NativePointer<NativePointer.StatementPtr> stmtPtr, int columnIndex) {
        return SQLite3Native.sqlite3ColumnBoolean(stmtPtr, columnIndex);
    }

    public static byte getByte(NativePointer<NativePointer.StatementPtr> stmtPtr, int columnIndex) {
        return (byte)SQLite3Native.sqlite3ColumnInt(stmtPtr, columnIndex);
    }

    public static short getShort(NativePointer<NativePointer.StatementPtr> stmtPtr, int columnIndex) {
        return (short) SQLite3Native.sqlite3ColumnInt(stmtPtr, columnIndex);
    }

    public static int getInt(NativePointer<NativePointer.StatementPtr> stmtPtr, int columnIndex) {
        return SQLite3Native.sqlite3ColumnInt(stmtPtr, columnIndex);
    }

    public static long getLong(NativePointer<NativePointer.StatementPtr> stmtPtr, int columnIndex) {
        return SQLite3Native.sqlite3ColumnInt64(stmtPtr, columnIndex);
    }

    public static float getFloat(NativePointer<NativePointer.StatementPtr> stmtPtr, int columnIndex) {
        return (float) SQLite3Native.sqlite3ColumnDouble(stmtPtr, columnIndex);
    }

    public static double getDouble(NativePointer<NativePointer.StatementPtr> stmtPtr, int columnIndex) {
        return SQLite3Native.sqlite3ColumnDouble(stmtPtr, columnIndex);
    }

    public static byte[] getBytes(NativePointer<NativePointer.StatementPtr> stmtPtr, int columnIndex) {
        return SQLite3Native.sqlite3ColumnBytes(stmtPtr, columnIndex);
    }

    public static void beforeFirst(NativePointer<NativePointer.ConnectionPtr> conn, NativePointer<NativePointer.StatementPtr> stmtPtr) throws SQLException {
        SQLiteReturnCode rc = SQLite3Native.sqlite3Reset(stmtPtr);
        if (!rc.isNoError()) throw new SQLiteException(conn, rc);
    }

    public static int getMaxRowCount(NativePointer<NativePointer.ConnectionPtr> conn, String sql) throws SQLException {
        try (var maxRowCountStmt = NativePointer.createNativeStmtPtr(conn, "select count(1) from (" + sql + ") A", null)) {
            SQLiteReturnCode result = SQLite3Native.sqlite3Step(maxRowCountStmt);
            if (result.equals(SQLiteReturnCode.SQLITE_BUSY))
                throw new SQLTimeoutException(ExceptionMessage.TIMEOUT.getMessage());
            if (!result.equals(SQLiteReturnCode.SQLITE_ROW))
                throw new SQLiteException(conn, result);
            if (!result.isNoError())
                throw new SQLiteException(conn, result);
            return SQLite3Native.sqlite3ColumnInt(maxRowCountStmt, 0);
        }

    }

    public static Blob getBlob(NativePointer<NativePointer.StatementPtr> stmtPtr, int columnIndex) throws SQLException {
        return new SerialBlob(SQLite3Native.sqlite3ColumnBlob(stmtPtr, columnIndex));
    }
}
