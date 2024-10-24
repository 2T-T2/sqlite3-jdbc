package t_panda.jdbc.sqlite.internal.bridge;

import t_panda.jdbc.sqlite.SQLiteDriver;
import t_panda.jdbc.sqlite.event.NativePointerEventListener;
import t_panda.jdbc.sqlite.internal.ExceptionMessage;
import t_panda.jdbc.sqlite.internal.SQLiteException;
import t_panda.jdbc.sqlite.internal.SQLiteReturnCode;

import java.sql.SQLException;

public interface NativePointer<T> extends AutoCloseable {
    /**
     * 保持しているポインタをバイト配列で返却する。
     * {@link #close() close}メソッドを呼び出し後はnullを返却する。
     * @return 保持しているポインタのバイト配列表現
     */
    byte[] getAsByteArray();
    boolean isClosed();
    void addNativePointerEventListener(NativePointerEventListener listener);
    void removeNativePointerEventListener(NativePointerEventListener listener);
    void removeAllNativePointerEventListener();

    @Override
    void close() throws SQLException;

    class StatementPtr {}
    class ConnectionPtr {}

    static NativePointer<StatementPtr> createNativeStmtPtr(NativePointer<ConnectionPtr> conn, String sql, Out<String> dstRestSql) throws SQLException {
        Out<NativePointer<StatementPtr>> dstStmtPtr = new Out<>();
        SQLiteReturnCode result = SQLite3Native.sqlite3Prepare_v2(conn, sql, dstStmtPtr, dstRestSql);
        if (!result.isNoError())
            throw new SQLiteException(conn,result);

        return dstStmtPtr.getOrElseThrow(() ->new SQLException(ExceptionMessage.CREATE_STATEMNT.getMessage()));
    }

    static NativePointer<ConnectionPtr> createNativeConnPtr(String url) throws SQLException {
        Out<NativePointer<ConnectionPtr>> dstConnPtr = new Out<>();
        var r = SQLite3Native.sqlite3Open(url.replace(SQLiteDriver.URI_PREFIX, ""), dstConnPtr);
        if (!r.isNoError())
            throw new SQLException(r + ExceptionMessage.DB_CONNECT_ERROR.getMessage(url));

        return dstConnPtr.getOrElseThrow(() ->new SQLException(ExceptionMessage.DB_CONNECT_ERROR.getMessage(url)));
    }
}
