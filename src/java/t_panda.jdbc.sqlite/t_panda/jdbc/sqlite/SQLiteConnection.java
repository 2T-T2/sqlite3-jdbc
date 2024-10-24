package t_panda.jdbc.sqlite;

import t_panda.jdbc.sqlite.event.SQLiteEventListener;
import t_panda.jdbc.sqlite.event.SQLiteEvent;
import t_panda.jdbc.sqlite.internal.ExceptionMessage;
import t_panda.jdbc.sqlite.internal.bridge.NativePointer;
import t_panda.jdbc.sqlite.internal.bridge.SQLiteConnectionBridge;

import javax.sql.rowset.serial.SerialBlob;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * SQLite データベースのコネクション
 */
public class SQLiteConnection implements Connection {
    private static final int DEFAULT_RESULTSET_TYPE = ResultSet.TYPE_FORWARD_ONLY;
    private static final int DEFAULT_RESULTSET_CONCUR = ResultSet.CONCUR_READ_ONLY;

    private final String filename;
    private final NativePointer<NativePointer.ConnectionPtr> dbConnPtr;

    private boolean autoCommit            = true;
    private boolean isClosed              = false;
    private int transactionIsolationLevel = Connection.TRANSACTION_SERIALIZABLE;
    private int holdability               = ResultSet.HOLD_CURSORS_OVER_COMMIT;
    private SQLWarning warning            = null;

    private final List<SQLiteEventListener> listeners = new ArrayList<>();

    SQLiteConnection(String url) throws SQLException {
        this.filename = url.replace(SQLiteDriver.URI_PREFIX, "");
        if (!url.equals(SQLiteDriver.URI_PREFIX + SQLiteDriver.ON_MEMORY_DATABASE_FILENAME)) {
            if (!Files.exists(Path.of(this.filename)))
                throw new SQLException(ExceptionMessage.DB_ACCESS_ERROR.getMessage(url));
        }

        this.dbConnPtr = NativePointer.createNativeConnPtr(url);
        SQLiteConnectionBridge.begin(this.getConnectionNativePointer());
    }

    @Override
    public Statement createStatement() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        return createStatement(
            DEFAULT_RESULTSET_TYPE
           ,DEFAULT_RESULTSET_CONCUR
           ,holdability
        );
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        return prepareStatement(
             sql
            ,DEFAULT_RESULTSET_TYPE
            ,DEFAULT_RESULTSET_CONCUR
            ,holdability
        );
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_STORED_PROCEDURE.getMessage());
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("nativeSQL"));
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        this.autoCommit = autoCommit;
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        return this.autoCommit;
    }

    @Override
    public void commit() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        SQLiteConnectionBridge.commit(this.getConnectionNativePointer());
        for(var listener : listeners) listener.onCommit(new SQLiteEvent(this, this));
        SQLiteConnectionBridge.begin(this.getConnectionNativePointer());
    }

    @Override
    public void rollback() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        SQLiteConnectionBridge.rollback(this.getConnectionNativePointer());
        SQLiteConnectionBridge.begin(this.getConnectionNativePointer());
    }

    @Override
    public void close() throws SQLException {
        SQLiteConnectionBridge.rollback(this.getConnectionNativePointer());
        this.dbConnPtr.close();
        this.isClosed = true;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return this.isClosed;
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("nativeSQL"));
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("nativeSQL"));
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        return true;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setCatalog"));
    }

    @Override
    public String getCatalog() throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getCatalog"));
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        if (!List.of(TRANSACTION_NONE, TRANSACTION_READ_UNCOMMITTED, TRANSACTION_READ_COMMITTED, TRANSACTION_REPEATABLE_READ, TRANSACTION_SERIALIZABLE).contains(level)) throw new SQLException(ExceptionMessage.NOT_SUPPORT_TRANSACTION_ISOLATION.getMessage());
        if (TRANSACTION_SERIALIZABLE != level) throw new SQLException(ExceptionMessage.NOT_SUPPORT_TRANSACTION_ISOLATION.getMessage());
        this.transactionIsolationLevel = level;
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        return transactionIsolationLevel;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        return this.warning;
    }

    @Override
    public void clearWarnings() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        this.warning = null;
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return createStatement(resultSetType, resultSetConcurrency, holdability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return prepareStatement(sql, resultSetType, resultSetConcurrency, holdability);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_STORED_PROCEDURE.getMessage());
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getTypeMap"));
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getTypeMap"));
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        this.holdability = holdability;
    }

    @Override
    public int getHoldability() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        return this.holdability;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        return new SQLiteSavePoint(null);
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        SQLiteConnectionBridge.setSavepoint(this.getConnectionNativePointer(), name);
        return new SQLiteSavePoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        SQLiteConnectionBridge.rollback(this.getConnectionNativePointer(), savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        SQLiteConnectionBridge.releaseSavepoint(this.getConnectionNativePointer(), savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        SQLiteResultSet.throwExceptionThenNotSupport(resultSetType, resultSetConcurrency, resultSetHoldability);

        return new SQLiteStatement(this, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        SQLiteResultSet.throwExceptionThenNotSupport(resultSetType, resultSetConcurrency, resultSetHoldability);

        return new SQLitePreparedStatement(this, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_STORED_PROCEDURE.getMessage());
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("prepareStatement"));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("prepareStatement"));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("prepareStatement"));
    }

    @Override
    public Clob createClob() throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("createClob"));
    }

    @Override
    public Blob createBlob() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        return new SerialBlob(new byte[] {});
    }

    @Override
    public NClob createNClob() throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("createNClob"));
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("createNClob"));
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        if (timeout < 0) throw new SQLException(ExceptionMessage.ARGUMENT_OVER_MIN.getMessage(1, "timeout", 0));
        // TODO もうちょっとましな実装を。。。
        return !this.isClosed();
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setClientInfo"));
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setClientInfo"));
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getClientInfo"));
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getClientInfo"));
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_DATATYPE.getMessage());
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_DATATYPE.getMessage());
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
    }

    @Override
    public String getSchema() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.DB_ALREADY_CLOSED.getMessage(filename));
        return null;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("abort"));
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setNetworkTimeout"));
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getNetworkTimeout"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this))
            return (T) this;
        else
            throw new SQLException("`" + iface + "`へキャストできないようです.");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    /**
     * SQLiteイベント発生を受け取るリスナを登録します
     * @param listener SQLiteイベント発生を受け取るリスナ
     */
    public void addSQLiteEventListener(SQLiteEventListener listener) {
        this.listeners.add(listener);
    }
    /**
     * SQLiteイベント発生を受け取るリスナの登録を削除します
     * @param listener SQLiteイベント発生を受け取るリスナ
     */
    public void removeSQLiteEventListener(SQLiteEventListener listener) {
        this.listeners.remove(listener);
    }

    NativePointer<NativePointer.ConnectionPtr> getConnectionNativePointer() {
        return this.dbConnPtr;
    }
}
