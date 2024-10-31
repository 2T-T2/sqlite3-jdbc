package t_panda.jdbc.sqlite;

import t_panda.jdbc.sqlite.event.SQLiteEvent;
import t_panda.jdbc.sqlite.internal.ExceptionMessage;
import t_panda.jdbc.sqlite.internal.SQLiteException;
import t_panda.jdbc.sqlite.internal.SQLiteReturnCode;
import t_panda.jdbc.sqlite.internal.bridge.NativePointer;
import t_panda.jdbc.sqlite.internal.bridge.Out;
import t_panda.jdbc.sqlite.internal.bridge.SQLitePreparedStatementBridge;
import t_panda.jdbc.sqlite.internal.bridge.SQLiteSettings;
import t_panda.jdbc.sqlite.internal.bridge.SQLiteStatementBridge;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLTimeoutException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

/**
 * プリコンパイルされたSQL文を表すオブジェクト
 */
public class SQLitePreparedStatement extends SQLiteStatementBase implements PreparedStatement {
    private final SQLiteConnection conn;
    private final String originalSql;
    private final int resultSetType;
    private final int resultSetConcurrency;
    private final int resultSetHoldability;

    private SQLiteResultSetMetaData metaData;

    private String currentSql;
    private String restSql;

    private SQLiteResultSet currentResultSet = null;
    private int currentUpdateCount = -1;
    private int resultSetMaxRows   =  0;
    private int timeoutSeconds     =  3;
    private int fetchSize          = 10;
    private int fetchDirection     = ResultSet.FETCH_FORWARD;
    private boolean isClosed       = false;

    private final Queue<Map<Integer, Map.Entry<Class<?>, Object>>> batBindParam = new ArrayDeque<>();
    private final Map<Integer, Map.Entry<Class<?>, Object>> bindParam = new HashMap<>();
    private SQLWarning warning;

    private final List<NativePointer<NativePointer.StatementPtr>> keepUnCloseNativePtr = new ArrayList<>();
    private int closedResultSetCount = 0;
    private int createResultSetCount = 0;

    SQLitePreparedStatement(SQLiteConnection sqLiteConnection, String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        this.conn = sqLiteConnection;
        this.originalSql = sql;
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
        this.resultSetHoldability = resultSetHoldability;

        this.currentSql = sql;
        Out<String> restSql = new Out<>();
        this.setStatementNativePtr(NativePointer.createNativeStmtPtr(this.conn.getConnectionNativePointer(), this.originalSql, restSql));
        this.restSql = restSql.getAcceptNull();
        this.metaData = new SQLiteResultSetMetaData(this.getStatementNativePtr(), this.getResultSetConcurrency());
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());

        // TODO: 結果セットを返すことが出来ない場合にSQLExceptionをthrow

        SQLitePreparedStatementBridge.resetStatement(this.conn.getConnectionNativePointer(), this.getStatementNativePtr());
        if (!Objects.isNull(this.currentResultSet)) this.currentResultSet.close();

        this.currentResultSet = this.createResultSet(
            this.conn,
            this,
            this.currentSql,
            restSql,
            this.resultSetType,
            this.resultSetConcurrency,
            this.resultSetHoldability,
            this.resultSetMaxRows,
            this.fetchDirection,
            this.fetchSize,
            this.timeoutSeconds
        );
        SQLitePreparedStatementBridge.resetStatement(this.conn.getConnectionNativePointer(), this.getStatementNativePtr());

        return this.currentResultSet;
    }

    @Override
    public int executeUpdate() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());

        // タイムアウトを設定
        SQLiteSettings.setTimeOut(this.conn.getConnectionNativePointer(), this.timeoutSeconds);

        SQLitePreparedStatementBridge.resetStatement(this.conn.getConnectionNativePointer(), this.getStatementNativePtr());
        int updateCount = SQLiteStatementBridge.executeUpdate(this.conn.getConnectionNativePointer(), this.getStatementNativePtr());
        SQLitePreparedStatementBridge.resetStatement(this.conn.getConnectionNativePointer(), this.getStatementNativePtr());

        if (this.conn.getAutoCommit())
            this.conn.commit();

        return updateCount;
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        bindParam.put(parameterIndex, Map.entry(Void.class, sqlType));
        SQLitePreparedStatementBridge.setNull(this.conn.getConnectionNativePointer(), this.getStatementNativePtr(), parameterIndex);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        bindParam.put(parameterIndex, Map.entry(boolean.class, x));
        SQLitePreparedStatementBridge.setBoolean(this.conn.getConnectionNativePointer(), this.getStatementNativePtr(), parameterIndex, x);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        bindParam.put(parameterIndex, Map.entry(byte.class, x));
        SQLitePreparedStatementBridge.setByte(this.conn.getConnectionNativePointer(), this.getStatementNativePtr(), parameterIndex, x);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        bindParam.put(parameterIndex, Map.entry(short.class, x));
        SQLitePreparedStatementBridge.setShort(this.conn.getConnectionNativePointer(), this.getStatementNativePtr(), parameterIndex, x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        bindParam.put(parameterIndex, Map.entry(int.class, x));
        SQLitePreparedStatementBridge.setInt(this.conn.getConnectionNativePointer(), this.getStatementNativePtr(), parameterIndex, x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        bindParam.put(parameterIndex, Map.entry(long.class, x));
        SQLitePreparedStatementBridge.setLong(this.conn.getConnectionNativePointer(), this.getStatementNativePtr(), parameterIndex, x);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        bindParam.put(parameterIndex, Map.entry(float.class, x));
        SQLitePreparedStatementBridge.setFloat(this.conn.getConnectionNativePointer(), this.getStatementNativePtr(), parameterIndex, x);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        bindParam.put(parameterIndex, Map.entry(double.class, x));
        SQLitePreparedStatementBridge.setDouble(this.conn.getConnectionNativePointer(), this.getStatementNativePtr(), parameterIndex, x);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        bindParam.put(parameterIndex, Map.entry(x.getClass(), x));
        SQLitePreparedStatementBridge.setDouble(this.conn.getConnectionNativePointer(), this.getStatementNativePtr(), parameterIndex, x.doubleValue());
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        bindParam.put(parameterIndex, Map.entry(x.getClass(), x));
        SQLitePreparedStatementBridge.setString(this.conn.getConnectionNativePointer(), this.getStatementNativePtr(), parameterIndex, x);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        bindParam.put(parameterIndex, Map.entry(x.getClass(), x));
        SQLitePreparedStatementBridge.setBytes(this.conn.getConnectionNativePointer(), this.getStatementNativePtr(), parameterIndex, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        bindParam.put(parameterIndex, Map.entry(x.getClass(), x));
        SQLitePreparedStatementBridge.setString(this.conn.getConnectionNativePointer(), this.getStatementNativePtr(), parameterIndex, new Timestamp(x.getTime()).toString());
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        bindParam.put(parameterIndex, Map.entry(x.getClass(), x));
        SQLitePreparedStatementBridge.setString(this.conn.getConnectionNativePointer(), this.getStatementNativePtr(), parameterIndex, new Timestamp(x.getTime()).toString());
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        bindParam.put(parameterIndex, Map.entry(x.getClass(), x));
        SQLitePreparedStatementBridge.setString(this.conn.getConnectionNativePointer(), this.getStatementNativePtr(), parameterIndex, x.toString());
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setAsciiStream"));
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setUnicodeStream"));
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setBinaryStream"));
    }

    @Override
    public void clearParameters() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        bindParam.clear();
        SQLitePreparedStatementBridge.clearParameters(this.conn.getConnectionNativePointer(), this.getStatementNativePtr());
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setObject"));
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setObject"));
    }

    @Override
    public boolean execute() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());

        this.closeStatementNativePtr();

        Out<String> dstRestSql = new Out<>();
        this.setStatementNativePtr(NativePointer.createNativeStmtPtr(this.conn.getConnectionNativePointer(), this.originalSql, dstRestSql));
        this.metaData = new SQLiteResultSetMetaData(this.getStatementNativePtr(), this.getResultSetConcurrency());
        this.restSql = dstRestSql.getAcceptNull();
        this.currentSql = originalSql;

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
    public void addBatch() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());

        this.batBindParam.add(new HashMap<>() {{
            putAll(bindParam);
        }});
        this.bindParam.clear();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setCharacterStream"));
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setRef"));
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        bindParam.put(parameterIndex, Map.entry(x.getClass(), x));
        SQLitePreparedStatementBridge.setBlob(this.conn.getConnectionNativePointer(), this.getStatementNativePtr(), parameterIndex, x);
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setClob"));
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setArray"));
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        return this.metaData;
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        cal.setTime(x);
        this.setDate(parameterIndex, new Date(cal.getTimeInMillis()));
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        cal.setTime(x);
        this.setTime(parameterIndex, new Time(cal.getTimeInMillis()));
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        cal.setTime(x);
        this.setTimestamp(parameterIndex, new Timestamp(cal.getTimeInMillis()));
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setNull"));
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        bindParam.put(parameterIndex, Map.entry(x.getClass(), x));
        SQLitePreparedStatementBridge.setString(this.conn.getConnectionNativePointer(), this.getStatementNativePtr(), parameterIndex, x.toString());
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        throw new SQLException(ExceptionMessage.NOT_SUPPORT_METHOD_THIS_CLASS.getMessage());
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setRowId"));
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setNString"));
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setNCharacterStream"));
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setNClob"));
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setClob"));
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        try {
            this.setBlob(parameterIndex, new SerialBlob(inputStream.readNBytes((int)length)));
        } catch (IOException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setNClob"));
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setSQLXML"));
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setObject"));
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setAsciiStream"));
    }


    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        try {
            if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
            bindParam.put(parameterIndex, Map.entry(x.getClass(), x));
            SQLitePreparedStatementBridge.setBlob(this.conn.getConnectionNativePointer(), this.getStatementNativePtr(), parameterIndex, new SerialBlob(x.readNBytes((int)length)));
        } catch (IOException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setCharacterStream"));
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setAsciiStream"));
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setBinaryStream"));
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setCharacterStream"));
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setNCharacterStream"));
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setClob"));
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        try {
            this.setBlob(parameterIndex, new SerialBlob(inputStream.readAllBytes()));
        } catch (IOException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("setNClob"));
    }
    // ↓ Statement より //
    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        throw new SQLException(ExceptionMessage.NOT_SUPPORT_METHOD_THIS_CLASS.getMessage());
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        throw new SQLException(ExceptionMessage.NOT_SUPPORT_METHOD_THIS_CLASS.getMessage());
    }

    @Override
    public void close() throws SQLException {
        if (this.isClosed()) return;
        if (!Objects.isNull(this.getStatementNativePtr())) this.closeStatementNativePtr();
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
        throw new SQLException(ExceptionMessage.NOT_SUPPORT_METHOD_THIS_CLASS.getMessage());
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
        throw new SQLException(ExceptionMessage.NOT_SUPPORT_METHOD_THIS_CLASS.getMessage());
    }

    @Override
    public void clearBatch() throws SQLException {
        // addBatch(String) は実行不可能のため
    }

    @Override
    public int[] executeBatch() throws SQLException {
        //TODO: バッチパラメーターを一気にネイティブ側に送るべき(一度のやり取りでクエリを実行するための機能のため)
        if (this.isClosed()) throw new SQLException(ExceptionMessage.STATEMENT_ALREADY_CLOSED.getMessage());

        // タイムアウトを設定
        SQLiteSettings.setTimeOut(this.conn.getConnectionNativePointer(), this.timeoutSeconds);

        final List<Integer> updateCounts = new ArrayList<>();

        SQLitePreparedStatementBridge.resetStatement(this.conn.getConnectionNativePointer(), this.getStatementNativePtr());
        this.clearParameters();

        while (!this.batBindParam.isEmpty()) {
            for (var entry : this.batBindParam.poll().entrySet())
                this.setParam(entry.getKey(), entry.getValue());

            updateCounts.add(SQLiteStatementBridge.executeUpdate(this.conn.getConnectionNativePointer(), this.getStatementNativePtr()));

            if (this.conn.getAutoCommit())
                this.conn.commit();

            SQLitePreparedStatementBridge.resetStatement(this.conn.getConnectionNativePointer(), this.getStatementNativePtr());
            this.clearParameters();
        }

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

        if (Objects.isNull(this.restSql) || this.restSql.split(";")[0].isBlank()) {
            this.currentResultSet = null;
            this.currentUpdateCount = -1;
            return false;
        }

        this.currentSql = this.restSql;
        Out<String> dstRestSql = new Out<>();
        this.setStatementNativePtr(NativePointer.createNativeStmtPtr(this.conn.getConnectionNativePointer(), this.currentSql, dstRestSql));
        this.metaData = new SQLiteResultSetMetaData(this.getStatementNativePtr(), this.getResultSetConcurrency());
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
        throw new SQLException(ExceptionMessage.NOT_SUPPORT_METHOD_THIS_CLASS.getMessage());
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLException(ExceptionMessage.NOT_SUPPORT_METHOD_THIS_CLASS.getMessage());
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        throw new SQLException(ExceptionMessage.NOT_SUPPORT_METHOD_THIS_CLASS.getMessage());
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLException(ExceptionMessage.NOT_SUPPORT_METHOD_THIS_CLASS.getMessage());
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLException(ExceptionMessage.NOT_SUPPORT_METHOD_THIS_CLASS.getMessage());
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        throw new SQLException(ExceptionMessage.NOT_SUPPORT_METHOD_THIS_CLASS.getMessage());
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

    // ↓ SQLiteEventListener の実装
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

    // ↓ private
    private SQLiteResultSet createResultSet(SQLiteConnection conn, SQLitePreparedStatement stmt, String currentSql, String restSql, int resultSetType, int resultSetConcurrency, int resultSetHoldability, int resultSetMaxRows, int fetchDirection, int fetchSize, int timeoutSeconds) throws SQLException {
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

    private void setParam(Integer key, Map.Entry<Class<?>, Object> value) throws SQLException {
        if (false) {}
        else if (value.getKey().equals(Void.class))    this.setNull(key, (int)value.getValue());
        else if (value.getKey().equals(boolean.class)) this.setBoolean(key, (boolean)value.getValue());
        else if (value.getKey().equals(byte.class))    this.setByte(key, (byte)value.getValue());
        else if (value.getKey().equals(short.class))   this.setShort(key, (short)value.getValue());
        else if (value.getKey().equals(int.class))     this.setInt(key, (int)value.getValue());
        else if (value.getKey().equals(long.class))    this.setLong(key, (long)value.getValue());
        else if (value.getKey().equals(float.class))   this.setFloat(key, (float)value.getValue());
        else if (value.getKey().equals(double.class))  this.setDouble(key, (double)value.getValue());
        else if (value.getKey().equals(byte[].class))  this.setBytes(key, (byte[]) value.getValue());
        else if (String.class.isAssignableFrom(value.getKey()))      this.setString(key, (String)value.getValue());
        else if (BigDecimal.class.isAssignableFrom(value.getKey()))  this.setBigDecimal(key, (BigDecimal)value.getValue());
        else if (Date.class.isAssignableFrom(value.getKey()))        this.setDate(key, (Date) value.getValue());
        else if (Time.class.isAssignableFrom(value.getKey()))        this.setTime(key, (Time) value.getValue());
        else if (Timestamp.class.isAssignableFrom(value.getKey()))   this.setTimestamp(key, (Timestamp) value.getValue());
        else if (Blob.class.isAssignableFrom(value.getKey()))        this.setBlob(key, (Blob) value.getValue());
        else if (URL.class.isAssignableFrom(value.getKey()))         this.setURL(key, (URL) value.getValue());
        else if (InputStream.class.isAssignableFrom(value.getKey())) this.setBinaryStream(key, (InputStream) value.getValue());
    }

    private void closeKeepUnCloseNativePtr() throws SQLException {
        for (var ntvPtr : this.keepUnCloseNativePtr)
            ntvPtr.close();
        this.keepUnCloseNativePtr.clear();
    }

}
