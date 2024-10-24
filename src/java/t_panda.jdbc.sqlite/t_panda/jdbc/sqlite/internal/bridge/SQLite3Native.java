package t_panda.jdbc.sqlite.internal.bridge;

import t_panda.jdbc.sqlite.event.NativePointerEvent;
import t_panda.jdbc.sqlite.event.NativePointerEventListener;
import t_panda.jdbc.sqlite.internal.ExceptionMessage;
import t_panda.jdbc.sqlite.internal.SQLiteException;
import t_panda.jdbc.sqlite.internal.SQLiteReturnCode;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class SQLite3Native {
    private static native SQLiteReturnCode sqlite3_open(String url, Out<byte[]> dstDbConnPtr);
    static SQLiteReturnCode sqlite3Open(String url, Out<NativePointer<NativePointer.ConnectionPtr>> dstDbConnPtr) {
        Out<byte[]> dstConnPtrAsBytes = new Out<>();
        SQLiteReturnCode result = sqlite3_open(url, dstConnPtrAsBytes);
        dstDbConnPtr.set(new NativePointer<>() {
            private final List<NativePointerEventListener> listeners = new ArrayList<>();
            boolean isClosed = false;

            @Override
            public byte[] getAsByteArray() {
                return dstConnPtrAsBytes.getAcceptNull();
            }

            @Override
            public boolean isClosed() {
                return isClosed;
            }

            @Override
            public void addNativePointerEventListener(NativePointerEventListener listener) {
                this.listeners.add(listener);
            }

            @Override
            public void removeNativePointerEventListener(NativePointerEventListener listener) {
                this.listeners.remove(listener);
            }

            @Override
            public void removeAllNativePointerEventListener() {
                this.listeners.clear();
            }

            @Override
            public void close() throws SQLException {
                if (!sqlite3Close_v2(this).isNoError()) {
                    throw new SQLException(ExceptionMessage.DB_CONNECT_ERROR.getMessage(url));
                }
                for (var listener : this.listeners) listener.onCloseNativePointer(new NativePointerEvent(this));
                this.removeAllNativePointerEventListener();
                dstConnPtrAsBytes.set(null);
                isClosed = true;
            }
        });
        return result;
    }

    private static native SQLiteReturnCode sqlite3_close_v2(byte[] dbConnPtr);
    static SQLiteReturnCode sqlite3Close_v2(NativePointer<NativePointer.ConnectionPtr> conn) {
        return sqlite3_close_v2(conn.getAsByteArray());
    }

    private static native SQLiteReturnCode sqlite3_prepare_v2(byte[] dbConnPtr, String sql, Out<byte[]> dstStmtPtr, Out<String> dstRestSql);
    static SQLiteReturnCode sqlite3Prepare_v2(NativePointer<NativePointer.ConnectionPtr> conn, String sql, Out<NativePointer<NativePointer.StatementPtr>> dstStmtPtr, Out<String> dstRestSql) {
        Out<byte[]> dstStmtPtrAsBytes = new Out<>();
        SQLiteReturnCode result = sqlite3_prepare_v2(conn.getAsByteArray(), sql, dstStmtPtrAsBytes, dstRestSql);
        dstStmtPtr.set(new NativePointer<>() {
            private final List<NativePointerEventListener> listeners = new ArrayList<>();
            boolean isClosed = false;

            @Override
            public byte[] getAsByteArray() {
                return dstStmtPtrAsBytes.getAcceptNull();
            }

            @Override
            public boolean isClosed() {
                return isClosed;
            }

            @Override
            public void addNativePointerEventListener(NativePointerEventListener listener) {
                this.listeners.add(listener);
            }

            @Override
            public void removeNativePointerEventListener(NativePointerEventListener listener) {
                this.listeners.remove(listener);
            }

            @Override
            public void removeAllNativePointerEventListener() {
                this.listeners.clear();
            }

            @Override
            public void close() throws SQLException {
                SQLiteReturnCode rc = sqlite3Finalize(this);
                if (!rc.isNoError()) {
                    throw new SQLiteException(conn, rc);
                }
                for (var listener : this.listeners) listener.onCloseNativePointer(new NativePointerEvent(this));
                dstStmtPtrAsBytes.set(null);
                isClosed = true;
            }
        });
        return result;
    }

    private static native int sqlite3_extended_errcode(byte[] dbConnPtr);
    static int sqlite3ExtendedErrcode(NativePointer<NativePointer.ConnectionPtr> conn) {
        return sqlite3_extended_errcode(conn.getAsByteArray());
    }

    private static native String sqlite3_errmsg(byte[] dbConnPtr);
    static String sqlite3Errmsg(NativePointer<NativePointer.ConnectionPtr> conn) {
        return sqlite3_errmsg(conn.getAsByteArray());
    }

    private static native SQLiteReturnCode sqlite3_step(byte[] stmtPtr);
    static SQLiteReturnCode sqlite3Step(NativePointer<NativePointer.StatementPtr> stmt) {
        return sqlite3_step(stmt.getAsByteArray());
    }

    private static native SQLiteReturnCode sqlite3_finalize(byte[] stmtPtr);
    static SQLiteReturnCode sqlite3Finalize(NativePointer<NativePointer.StatementPtr> sqLiteStatement) {
        return sqlite3_finalize(sqLiteStatement.getAsByteArray());
    }

    private static native SQLiteReturnCode sqlite3_busy_timeout(byte[] dbConnPtr, int second);
    static SQLiteReturnCode sqlite3BusyTimeout(NativePointer<NativePointer.ConnectionPtr> conn, int second) {
        return  sqlite3_busy_timeout(conn.getAsByteArray(), second);
    }

    private static native SQLiteReturnCode sqlite3_reset(byte[] stmtPtr);
    static SQLiteReturnCode sqlite3Reset(NativePointer<NativePointer.StatementPtr> sqLiteStatement) {
        return sqlite3_reset(sqLiteStatement.getAsByteArray());
    }

    private static native int sqlite3_column_int(byte[] stmtPtr, int i);
    static int sqlite3ColumnInt(NativePointer<NativePointer.StatementPtr> stmtPtr, int i) {
        return sqlite3_column_int(stmtPtr.getAsByteArray(), i - 1);
    }

    private static native String sqlite3_column_name(byte[] stmtPtr, int i);
    static String sqlite3ColumnName(NativePointer<NativePointer.StatementPtr> stmtPtr, int i) {
        return sqlite3_column_name(stmtPtr.getAsByteArray(), i - 1);
    }

    private static native int sqlite3_column_count(byte[] stmtPtr);
    static int sqlite3ColumnCount(NativePointer<NativePointer.StatementPtr> stmtPtr) {
        return sqlite3_column_count(stmtPtr.getAsByteArray());
    }

    private static native String sqlite3_column_text(byte[] stmtPtr, int columnIndex);
    static String sqlite3ColumnText(NativePointer<NativePointer.StatementPtr> stmtPtr, int columnIndex) {
        return sqlite3_column_text(stmtPtr.getAsByteArray(), columnIndex - 1);
    }

    private native static long sqlite3_column_int64(byte[] stmtPtr, int columnIndex);
    static long sqlite3ColumnInt64(NativePointer<NativePointer.StatementPtr> stmtPtr, int columnIndex) {
        return sqlite3_column_int64(stmtPtr.getAsByteArray(), columnIndex - 1);
    }

    private native static double sqlite3_column_double(byte[] stmtPtr, int columnIndex);
    static double sqlite3ColumnDouble(NativePointer<NativePointer.StatementPtr> stmtPtr, int columnIndex) {
        return sqlite3_column_double(stmtPtr.getAsByteArray(), columnIndex - 1);
    }

    private static native boolean sqlite3_column_isNull(byte[] stmtPtr, int lastGetColumnIndex);
    static boolean sqlite3ColumnIsNull(NativePointer<NativePointer.StatementPtr> stmtPtr, int lastGetColumnIndex) {
        /* memo c実装
        sqlite3_value* value = sqlite3_column_value(statement, index);
        return sqlite3_value_type(value) == SQLITE_NULL;
        */
        return sqlite3_column_isNull(stmtPtr.getAsByteArray(), lastGetColumnIndex - 1);
    }

    private static native boolean sqlite3_column_boolean(byte[] stmtPtr, int columnIndex);
    static boolean sqlite3ColumnBoolean(NativePointer<NativePointer.StatementPtr> stmtPtr, int columnIndex) {
        /* memo c実装
		sqlite3_value* value = sqlite3_column_value(statement, index);
        switch (sqlite3_value_type(value)) {
            case SQLITE_NULL   : return false; break;
			case SQLITE_INTEGER: return sqlite3_value_int(value) != 0; break;
			case SQLITE_FLOAT  : return sqlite3_value_double(value) != 0.0; break;
			case SQLITE_TEXT   : return sqlite3_value_text(value) != "0"; break;
			case SQLITE_BLOB   : return true; break;
        }
        return false;
        */
        return sqlite3_column_boolean(stmtPtr.getAsByteArray(), columnIndex - 1);
    }

    private native static byte[] sqlite3_column_bytes(byte[] stmtPtr, int columnIndex);
    static byte[] sqlite3ColumnBytes(NativePointer<NativePointer.StatementPtr> stmtPtr, int columnIndex) {
        /* memo c実装
		sqlite3_value* value = sqlite3_column_value(statement, index);
        switch (sqlite3_value_type(value)) {
            case SQLITE_NULL   : return nullptr; break;
			case SQLITE_INTEGER: return sqlite3_value_int(value) != 0; break;
			case SQLITE_FLOAT  : return sqlite3_value_double(value) != 0.0; break;
			case SQLITE_TEXT   : return sqlite3_value_text(value) != "0"; break;
			case SQLITE_BLOB   : return true; break;
        }
        return false;
        */
        return sqlite3_column_bytes(stmtPtr.getAsByteArray(), columnIndex - 1);
    }

    private static native byte[] sqlite3_column_blob(byte[] stmtPtr, int columnIndex);
    static byte[] sqlite3ColumnBlob(NativePointer<NativePointer.StatementPtr> stmtPtr, int columnIndex) {
        return sqlite3_column_blob(stmtPtr.getAsByteArray(), columnIndex - 1);
    }

    private static native SQLiteReturnCode sqlite3_bind_null(byte[] stmtPtr, int parameterIndex);
    static SQLiteReturnCode sqlite3BindNull(NativePointer<NativePointer.StatementPtr> stmtPtr, int parameterIndex) {
        return sqlite3_bind_null(stmtPtr.getAsByteArray(), parameterIndex);
    }
    private static native  SQLiteReturnCode sqlite3_bind_blob ( byte[] stmtPtr, int parameterIndex, byte[] value );
    static SQLiteReturnCode sqlite3BindBlob(NativePointer<NativePointer.StatementPtr> stmtPtr, int parameterIndex, byte[] value ) { return sqlite3_bind_blob(stmtPtr.getAsByteArray(), parameterIndex, value); }
    private static native  SQLiteReturnCode sqlite3_bind_double ( byte[] stmtPtr, int parameterIndex, double value );
    static SQLiteReturnCode sqlite3BindDouble(NativePointer<NativePointer.StatementPtr> stmtPtr, int parameterIndex, double value ) { return sqlite3_bind_double(stmtPtr.getAsByteArray(), parameterIndex, value); }
    private static native  SQLiteReturnCode sqlite3_bind_int ( byte[] stmtPtr, int parameterIndex, int value );
    static SQLiteReturnCode sqlite3BindInt(NativePointer<NativePointer.StatementPtr> stmtPtr, int parameterIndex, int value ) { return sqlite3_bind_int(stmtPtr.getAsByteArray(), parameterIndex, value); }
    private static native  SQLiteReturnCode sqlite3_bind_int64 ( byte[] stmtPtr, int parameterIndex, long value );
    static SQLiteReturnCode sqlite3BindInt64(NativePointer<NativePointer.StatementPtr> stmtPtr, int parameterIndex, long value ) { return sqlite3_bind_int64(stmtPtr.getAsByteArray(), parameterIndex, value); }
    private static native  SQLiteReturnCode sqlite3_bind_text ( byte[] stmtPtr, int parameterIndex, String value );
    static SQLiteReturnCode sqlite3BindText(NativePointer<NativePointer.StatementPtr> stmtPtr, int parameterIndex, String value ) { return sqlite3_bind_text(stmtPtr.getAsByteArray(), parameterIndex, value); }

    private static native SQLiteReturnCode sqlite3_clear_bindings(byte[] stmtPtr);
    static SQLiteReturnCode sqlite3ClearBindings(NativePointer<NativePointer.StatementPtr> stmtPtr) {
        return sqlite3_clear_bindings(stmtPtr.getAsByteArray());
    }
}
