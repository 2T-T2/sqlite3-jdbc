package t_panda.jdbc.sqlite;

import t_panda.jdbc.sqlite.event.SQLiteEvent;
import t_panda.jdbc.sqlite.internal.ExceptionMessage;
import t_panda.jdbc.sqlite.internal.SQLiteException;
import t_panda.jdbc.sqlite.internal.bridge.NativePointer;
import t_panda.jdbc.sqlite.internal.bridge.SQLiteStatementBridge;
import t_panda.jdbc.sqlite.internal.bridge.SQLiteSettings;
import t_panda.jdbc.sqlite.internal.bridge.Out;
import t_panda.jdbc.sqlite.internal.SQLiteReturnCode;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLTimeoutException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 静的SQL文を実行し、作成された結果を返すために使用されるオブジェクト
 */
public class SQLiteStatement extends SQLiteStatementBase {
    /**
     * 複数SQLの区切り文字
     */
    public static final String SQL_DELIMITER = ";";

    private final SQLiteConnection conn;
    private final int resultSetType;
    private final int resultSetConcurrency;
    private final int resultSetHoldability;

    private final StringBuilder batchSQL = new StringBuilder();
    private final List<NativePointer<NativePointer.StatementPtr>> keepUnCloseNativePtr = new ArrayList<>();
    private int resultSetMaxRows   =  0;
    private int timeoutSeconds     =  3;
    private int fetchSize          = 10;
    private int fetchDirection     = ResultSet.FETCH_FORWARD;
    private boolean isClosed       = false;

    private SQLiteResultSet currentResultSet = null;
    private int currentUpdateCount = -1;
    private String currentSql = null;
    private String restSql = null;

    private int createResultSetCount = 0;
    private int closedResultSetCount = 0;

    private SQLWarning warning = null;

    SQLiteStatement(SQLiteConnection sqLiteConnection, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        SQLiteResultSet.throwExceptionThenNotSupport(resultSetType, resultSetConcurrency, resultSetHoldability);

        this.conn = sqLiteConnection;
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
        this.resultSetHoldability = resultSetHoldability;

        this.conn.addSQLiteEventListener(this);
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());

        // TODO: 結果セットを返すことが出来ない場合にSQLExceptionをthrow

        // 前回実行分のリソースを開放
        this.closeStatementNativePtr();

        Out<String> dstRestSql = new Out<>();
        this.setStatementNativePtr(NativePointer.createNativeStmtPtr(this.conn.getConnectionNativePointer(), sql, dstRestSql));
        this.restSql = dstRestSql.getAcceptNull();

        return this.createResultSet(
            this.conn,
            this,
            this.currentSql = sql,
            this.restSql,
            this.resultSetType,
            this.resultSetConcurrency,
            this.resultSetHoldability,
            this.resultSetMaxRows,
            this.fetchDirection,
            this.fetchSize,
            this.timeoutSeconds
        );

    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());

        // 前回実行分のリソースを開放
        this.closeStatementNativePtr();

        Out<String> dstRestSql = new Out<>();
        this.setStatementNativePtr(NativePointer.createNativeStmtPtr(this.conn.getConnectionNativePointer(), sql, dstRestSql));
        this.restSql = dstRestSql.getAcceptNull();

        // タイムアウトを設定
        SQLiteSettings.setTimeOut(this.conn.getConnectionNativePointer(), this.timeoutSeconds);

        int updateCount = SQLiteStatementBridge.executeUpdate(this.conn.getConnectionNativePointer(), this.getStatementNativePtr());

        if (this.conn.getAutoCommit())
            this.conn.commit();

        return this.currentUpdateCount = updateCount;
    }

    @Override
    public void close() throws SQLException {
        if (this.isClosed()) return;
        this.closeStatementNativePtr();
        this.closeKeepUnCloseNativePtr();
        this.dispatchCloseEvent(()->new SQLiteEvent(this, this.conn));
        this.conn.removeSQLiteEventListener(this);
        this.removeAllSQLiteEventListener();
        this.isClosed = true;
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getMaxFieldSize"));
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setMaxFieldSize"));
    }

    @Override
    public int getMaxRows() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());
        return this.resultSetMaxRows;
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());
        this.resultSetMaxRows = max;
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setEscapeProcessing"));
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());
        return this.timeoutSeconds;
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());
        if (!(seconds >= 0)) throw new SQLException(ExceptionMessage.ARGUMENT_OVER_MIN.getMessage(1, "seconds", 0));
        this.timeoutSeconds = seconds;
    }

    @Override
    public void cancel() throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("cancel"));
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());
        return this.warning;
    }

    @Override
    public void clearWarnings() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());
        this.warning = null;
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setCursorName"));
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());

        // 前回実行分のリソースを開放
        this.closeStatementNativePtr();

        Out<String> dstRestSql = new Out<>();
        this.setStatementNativePtr(NativePointer.createNativeStmtPtr(this.conn.getConnectionNativePointer(), sql, dstRestSql));
        this.restSql = dstRestSql.getAcceptNull();
        this.currentSql = sql;

        // タイムアウトを設定
        SQLiteSettings.setTimeOut(this.conn.getConnectionNativePointer(), this.timeoutSeconds);

        return SQLiteStatementBridge.execute(this.getStatementNativePtr(), (result) -> {
            if (result.equals(SQLiteReturnCode.SQLITE_BUSY))
                throw new SQLTimeoutException(ExceptionMessage.TIMEOUT.getMessage());

            // 結果セットを返さないSQLを実行した場合
            if (result.equals(SQLiteReturnCode.SQLITE_DONE)) {
                if (this.conn.getAutoCommit())
                    this.conn.commit();
                this.currentResultSet = null;
                this.currentUpdateCount = SQLiteStatementHelper.executeQueryLastUpdateCount(this.conn);
                return false;
            }

            if (!result.isNoError())
                throw new SQLiteException(this.conn.getConnectionNativePointer(), result);

            // 結果セットを返すSQLを実行した場合
            SQLiteStatementBridge.first(this.conn.getConnectionNativePointer(), this.getStatementNativePtr());
            this.currentUpdateCount = -1;
            this.currentResultSet = this.createResultSet(
                this.conn,
                this,
                this.currentSql,
                this.restSql,
                this.resultSetType,
                this.resultSetConcurrency,
                this.resultSetHoldability,
                this.resultSetMaxRows,
                this.fetchDirection,
                this.fetchSize,
                this.timeoutSeconds
            );

            return true;
        });
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());
        return this.currentResultSet;
    }

    @Override
    public int getUpdateCount() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());
        return this.currentUpdateCount;
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());
        return this.getMoreResults(CLOSE_ALL_RESULTS);
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        SQLiteResultSet.throwExceptionThenNotSupport(this.getResultSetType(), this.getResultSetConcurrency(), this.getResultSetHoldability(), direction);
        this.fetchDirection = direction;
    }

    @Override
    public int getFetchDirection() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        return this.fetchDirection;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());
        this.fetchSize = rows;
    }

    @Override
    public int getFetchSize() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());
        return this.fetchSize;
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());
        return this.resultSetConcurrency;
    }

    @Override
    public int getResultSetType() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());
        return this.resultSetType;
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());
        this.batchSQL.append(sql);
        if(!sql.endsWith(SQL_DELIMITER))
            this.batchSQL.append(SQL_DELIMITER);
    }

    @Override
    public void clearBatch() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());
        this.batchSQL.delete(0, this.batchSQL.length());
    }

    @Override
    public int[] executeBatch() throws SQLException {
        //TODO: バッチSQL文字列を一気にネイティブ側に送るべき(一度のやり取りでクエリを実行するための機能のため)
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());

        final List<Integer> updateCounts = new ArrayList<>();
        final String sql = this.batchSQL.toString();

        // 前回実行分のリソースを開放
        this.closeStatementNativePtr();

        // タイムアウトを設定
        SQLiteSettings.setTimeOut(this.conn.getConnectionNativePointer(), this.timeoutSeconds);

        Out<String> dstRestSql = new Out<>();
        this.setStatementNativePtr(NativePointer.createNativeStmtPtr(this.conn.getConnectionNativePointer(), sql, dstRestSql));
        this.restSql = dstRestSql.getAcceptNull();

        while (Objects.isNull(this.restSql)) {
            SQLiteStatementBridge.execute(this.getStatementNativePtr(), (result) -> {
                if (result.equals(SQLiteReturnCode.SQLITE_BUSY))
                    throw new SQLTimeoutException(ExceptionMessage.TIMEOUT.getMessage());
                // 結果セットを返すSQLを実行した場合
                if (result.equals(SQLiteReturnCode.SQLITE_ROW))
                    throw new BatchUpdateException();
                if (!result.equals(SQLiteReturnCode.SQLITE_DONE))
                    throw new SQLiteException(conn.getConnectionNativePointer(), result);
                return null;
            });

            updateCounts.add(SQLiteStatementHelper.executeQueryLastUpdateCount(this.conn));

            this.closeStatementNativePtr();
            this.setStatementNativePtr(NativePointer.createNativeStmtPtr(this.conn.getConnectionNativePointer(), this.restSql, dstRestSql));
            this.restSql = dstRestSql.getAcceptNull();
        }

        this.closeStatementNativePtr();

        return updateCounts.stream().mapToInt(it -> it).toArray();
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());
        return this.conn;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());

        switch (current) {
            case CLOSE_ALL_RESULTS:    // 依存する全ての結果セットをclose
                this.closeStatementNativePtr();
                this.closeKeepUnCloseNativePtr();
                break;
            case CLOSE_CURRENT_RESULT: // 前回実行分のリソースを開放
                this.closeStatementNativePtr();
                break;
            case KEEP_CURRENT_RESULT:
                this.keepUnCloseNativePtr.add(this.getStatementNativePtr());
                break;
            default: throw new SQLException(ExceptionMessage.ILLEGAL_ARGUMENT.getMessage(1, "current", "Statement.CLOSE_ALL_RESULTS, Statement.CLOSE_CURRENT_RESULT, Statement.KEEP_CURRENT_RESULT のいずれか"));
        }

        if (Objects.isNull(this.restSql)) {
            this.currentResultSet = null;
            this.currentUpdateCount = -1;
            return false;
        }

        this.currentSql = this.restSql;
        Out<String> dstRestSql = new Out<>();
        this.setStatementNativePtr(NativePointer.createNativeStmtPtr(this.conn.getConnectionNativePointer(), this.currentSql, dstRestSql));
        this.restSql = dstRestSql.getAcceptNull();

        // タイムアウトを設定
        SQLiteSettings.setTimeOut(this.conn.getConnectionNativePointer(), this.timeoutSeconds);

        return SQLiteStatementBridge.execute(this.getStatementNativePtr(), (result) -> {
            if (result.equals(SQLiteReturnCode.SQLITE_BUSY))
                throw new SQLTimeoutException(ExceptionMessage.TIMEOUT.getMessage());

            // 結果セットを返さないSQLを実行した場合
            if (result.equals(SQLiteReturnCode.SQLITE_DONE)) {
                if (this.conn.getAutoCommit())
                    this.conn.commit();
                this.currentResultSet = null;
                this.currentUpdateCount = SQLiteStatementHelper.executeQueryLastUpdateCount(this.conn);
                return false;
            }

            // 結果セットを返すSQLを実行した場合
            SQLiteStatementBridge.first(this.conn.getConnectionNativePointer(), this.getStatementNativePtr());
            this.currentUpdateCount = -1;
            this.currentResultSet = this.createResultSet(
                this.conn,
                this,
                this.currentSql,
                this.restSql,
                this.resultSetType,
                this.resultSetConcurrency,
                this.resultSetHoldability,
                this.resultSetMaxRows,
                this.fetchDirection,
                this.fetchSize,
                this.timeoutSeconds
            );

            return true;
        });
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getGeneratedKeys"));
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("executeUpdate"));
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("executeUpdate"));
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("executeUpdate"));
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("execute"));
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("execute"));
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("execute"));
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());
        return this.resultSetHoldability;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return this.isClosed;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setPoolable"));
    }

    @Override
    public boolean isPoolable() throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setPoolable"));
    }

    @Override
    public void closeOnCompletion() throws SQLException {
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return this.closedResultSetCount == this.createResultSetCount;
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

    private void closeKeepUnCloseNativePtr() throws SQLException {
        for (var ntvPtr : this.keepUnCloseNativePtr)
            ntvPtr.close();
        this.keepUnCloseNativePtr.clear();
    }

    private SQLiteResultSet createResultSet(SQLiteConnection conn, SQLiteStatement stmt, String currentSql, String restSql, int resultSetType, int resultSetConcurrency, int resultSetHoldability, int resultSetMaxRows, int fetchDirection, int fetchSize, int timeoutSeconds) throws SQLException {
        this.createResultSetCount++;
        return new SQLiteResultSet(
                conn,
                stmt,
                currentSql,
                restSql,
                resultSetType,
                resultSetConcurrency,
                resultSetHoldability,
                resultSetMaxRows,
                fetchDirection,
                fetchSize,
                timeoutSeconds
        );
    }

    @Override
    public void onCommit(SQLiteEvent e) throws SQLException {
        if (!e.getSource().equals(this.conn)) return;
        this.dispatchCommitEvent(()->new SQLiteEvent(this, this.conn));
    }

    @Override
    public void onClose(SQLiteEvent e) throws SQLException {
        if (e.getSource().equals(this.conn)) this.close();

        if (e.getSource() instanceof SQLiteResultSet) {
            this.closedResultSetCount++;
            if (this.isCloseOnCompletion())
                this.closeOnCompletion();
        }
    }

}
