package t_panda.jdbc.sqlite.internal.bridge;

public class SQLiteError {
    public static String getErrorLastMessage(NativePointer<NativePointer.ConnectionPtr> conn) {
        return SQLite3Native.sqlite3Errmsg(conn);
    }

    public static int getErrorCode(NativePointer<NativePointer.ConnectionPtr> conn) {
        return SQLite3Native.sqlite3ExtendedErrcode(conn);
    }
}
