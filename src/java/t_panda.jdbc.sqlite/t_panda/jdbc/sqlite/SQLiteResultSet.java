package t_panda.jdbc.sqlite;

import t_panda.jdbc.sqlite.event.NativePointerEvent;
import t_panda.jdbc.sqlite.event.NativePointerEventListener;
import t_panda.jdbc.sqlite.event.SQLiteEvent;
import t_panda.jdbc.sqlite.event.SQLiteEventListener;
import t_panda.jdbc.sqlite.internal.ExceptionMessage;
import t_panda.jdbc.sqlite.internal.bridge.NativePointer;
import t_panda.jdbc.sqlite.internal.bridge.SQLiteResultSetBridge;
import t_panda.jdbc.sqlite.internal.bridge.SQLiteSettings;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * データベースの結果セットを表すデータの表
 */
public class SQLiteResultSet implements ResultSet, SQLiteEventListener, NativePointerEventListener {
    private final NativePointer<NativePointer.StatementPtr> stmtPtr;

    private final SQLiteConnection conn;
    private final SQLiteStatementBase stmt;
    private final int type;
    private final int concurrency;
    private final int holdability;
    private final int maxRows;
    private final int timeoutSeconds;
    private final SQLiteResultSetMetaData metaData;
    private final int maxRowCount;

    // TODO: フェッチサイズ
    private int fetchSize;
    private int fetchDirection;
    private boolean isClosed = false;
    private final List<SQLiteEventListener> sqLiteEventListeners = new ArrayList<>();
    private int lastGetColumnIndex = -1;
    private SQLWarning warning = null;

    private int currentRowCount = 0;
    private boolean rowUpdated = false;
    private boolean rowInserted = false;
    private boolean rowDeleted = false;

    SQLiteResultSet(SQLiteConnection conn, SQLiteStatementBase stmt, String allSql, String restSql, int resultSetType, int resultSetConcurrency, int resultSetHoldability, int resultSetMaxRows, int fetchDirection, int fetchSize,int timeoutSeconds) throws SQLException {
        this.conn = conn;
        this.stmt = stmt;
        this.type = resultSetType;
        this.concurrency = resultSetConcurrency;
        this.holdability = resultSetHoldability;
        this.maxRows = resultSetMaxRows;
        this.fetchDirection = fetchDirection;
        this.fetchSize = fetchSize;
        this.timeoutSeconds = timeoutSeconds;

        this.stmtPtr = stmt.getStatementNativePtr();
        this.metaData = new SQLiteResultSetMetaData(this.stmtPtr, this.getConcurrency());
        this.maxRowCount = SQLiteResultSetBridge.getMaxRowCount(
            this.conn.getConnectionNativePointer(),
            (restSql != null) ? allSql.replaceAll(restSql, "") : allSql
        );

        stmt.addSQLiteEventListener(this);
        this.addSQLiteEventListener(stmt);
        stmtPtr.addNativePointerEventListener(this);
    }

    static void throwExceptionThenNotSupport(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        if (!List.of(ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE).contains(resultSetType)) throw new SQLException(ExceptionMessage.NOT_SUPPORT_RESULTSET_TYPE.getMessage());
        if (resultSetType == ResultSet.TYPE_SCROLL_SENSITIVE) throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_RESULTSET_TYPE.getMessage());

        if (!List.of(ResultSet.CONCUR_READ_ONLY, ResultSet.CONCUR_UPDATABLE).contains(resultSetConcurrency)) throw new SQLException(ExceptionMessage.NOT_SUPPORT_RESULTSET_CONCURRENCY.getMessage());
        if (resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_RESULTSET_CONCURRENCY.getMessage());

        if (!List.of(ResultSet.HOLD_CURSORS_OVER_COMMIT, ResultSet.CLOSE_CURSORS_AT_COMMIT).contains(resultSetHoldability)) throw new SQLException(ExceptionMessage.NOT_SUPPORT_RESULTSET_CONCURRENCY.getMessage());
    }
    static void throwExceptionThenNotSupport(int resultSetType, int resultSetConcurrency, int resultSetHoldability, int fetchDirection) throws SQLException {
        if (!List.of(ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE).contains(resultSetType)) throw new SQLException(ExceptionMessage.NOT_SUPPORT_RESULTSET_TYPE.getMessage());
        if (resultSetType == ResultSet.TYPE_SCROLL_SENSITIVE) throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_RESULTSET_TYPE.getMessage());

        if (!List.of(ResultSet.CONCUR_READ_ONLY, ResultSet.CONCUR_UPDATABLE).contains(resultSetConcurrency)) throw new SQLException(ExceptionMessage.NOT_SUPPORT_RESULTSET_CONCURRENCY.getMessage());
        if (resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_RESULTSET_CONCURRENCY.getMessage());

        if (!List.of(ResultSet.HOLD_CURSORS_OVER_COMMIT, ResultSet.CLOSE_CURSORS_AT_COMMIT).contains(resultSetHoldability)) throw new SQLException(ExceptionMessage.NOT_SUPPORT_RESULTSET_CONCURRENCY.getMessage());

        if (!List.of(ResultSet.FETCH_FORWARD, ResultSet.FETCH_REVERSE, ResultSet.FETCH_UNKNOWN).contains(fetchDirection)) throw new SQLException(ExceptionMessage.NOT_SUPPORT_FETCH_DIRECTION.getMessage());
        if (resultSetType == ResultSet.TYPE_FORWARD_ONLY && fetchDirection != ResultSet.FETCH_FORWARD) throw new SQLException(ExceptionMessage.NOT_SUPPORT_FETCH_DIRECTION.getMessage());
    }

    @Override
    public boolean next() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.maxRows != 0 && this.currentRowCount == this.maxRows) return false;
        this.currentRowCount++;
        SQLiteSettings.setTimeOut(this.conn.getConnectionNativePointer(), this.timeoutSeconds);
        boolean hasNext = SQLiteResultSetBridge.next(this.conn.getConnectionNativePointer(), this.stmtPtr);
        if (this.conn.getAutoCommit() && this.getType() == ResultSet.TYPE_FORWARD_ONLY && !hasNext) this.conn.commit();
        return hasNext;
    }

    @Override
    public void close() throws SQLException {
        if (this.isClosed()) return;
        for (var listener : this.sqLiteEventListeners) listener.onClose(new SQLiteEvent(this, this.conn));
        this.stmt.removeSQLiteEventListener(this);
        this.stmtPtr.removeNativePointerEventListener(this);
        this.removeSQLiteEventListener(this.stmt);
        this.isClosed = true;
    }

    @Override
    public boolean wasNull() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.lastGetColumnIndex == -1)
            return SQLiteResultSetBridge.isNull(this.stmtPtr, this.lastGetColumnIndex);

        return false;
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
        this.lastGetColumnIndex = columnIndex;
        return SQLiteResultSetBridge.getString(this.stmtPtr, columnIndex);
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
        this.lastGetColumnIndex = columnIndex;
        return SQLiteResultSetBridge.getBoolean(this.stmtPtr, columnIndex);
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
        this.lastGetColumnIndex = columnIndex;
        return SQLiteResultSetBridge.getByte(this.stmtPtr, columnIndex);
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
        this.lastGetColumnIndex = columnIndex;
        return SQLiteResultSetBridge.getShort(this.stmtPtr, columnIndex);
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
        this.lastGetColumnIndex = columnIndex;
        return SQLiteResultSetBridge.getInt(this.stmtPtr, columnIndex);
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
        this.lastGetColumnIndex = columnIndex;
        return SQLiteResultSetBridge.getLong(this.stmtPtr, columnIndex);
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
        this.lastGetColumnIndex = columnIndex;
        return SQLiteResultSetBridge.getFloat(this.stmtPtr, columnIndex);
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
        this.lastGetColumnIndex = columnIndex;
        return SQLiteResultSetBridge.getDouble(this.stmtPtr, columnIndex);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getBigDecimal"));
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
        this.lastGetColumnIndex = columnIndex;
        return SQLiteResultSetBridge.getBytes(this.stmtPtr, columnIndex);
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
        this.lastGetColumnIndex = columnIndex;
        return new Date(Timestamp.valueOf(SQLiteResultSetBridge.getString(this.stmtPtr, columnIndex)).getTime());
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
        this.lastGetColumnIndex = columnIndex;
        return new Time(Timestamp.valueOf(SQLiteResultSetBridge.getString(this.stmtPtr, columnIndex)).getTime());
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
        this.lastGetColumnIndex = columnIndex;
        return Timestamp.valueOf(SQLiteResultSetBridge.getString(this.stmtPtr, columnIndex));
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getAsciiStream"));
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getUnicodeStream"));
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getBinaryStream"));
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        return getString(this.findColumn(columnLabel));
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        return getBoolean(this.findColumn(columnLabel));
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        return getByte(this.findColumn(columnLabel));
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        return getShort(this.findColumn(columnLabel));
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        return getInt(this.findColumn(columnLabel));
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        return getLong(this.findColumn(columnLabel));
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        return getFloat(this.findColumn(columnLabel));
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        return getDouble(this.findColumn(columnLabel));
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        return getBigDecimal(this.findColumn(columnLabel));
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        return getBytes(this.findColumn(columnLabel));
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        return getDate(this.findColumn(columnLabel));
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        return getTime(this.findColumn(columnLabel));
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        return getTimestamp(this.findColumn(columnLabel));
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        return getAsciiStream(this.findColumn(columnLabel));
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        return getUnicodeStream(this.findColumn(columnLabel));
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        return getBinaryStream(this.findColumn(columnLabel));
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return this.warning;
    }

    @Override
    public void clearWarnings() throws SQLException {
        this.warning = null;
    }

    @Override
    public String getCursorName() throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getCursorName"));
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return this.metaData;
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getObject"));
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        return this.getObject(this.findColumn(columnLabel));
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        int idx = this.metaData.getColumnIndexOf(columnLabel);
        if (idx == -1) throw new SQLException(ExceptionMessage.NOT_FOUND_CLUMN_NAME.getMessage());
        return idx + 1;
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getCharacterStream"));
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getCharacterStream"));
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
        double val = SQLiteResultSetBridge.getDouble(this.stmtPtr, columnIndex);
        return BigDecimal.valueOf(val);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        return this.getBigDecimal(this.findColumn(columnLabel));
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        return this.currentRowCount == 0;
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        return this.currentRowCount == this.maxRowCount;
    }

    @Override
    public boolean isFirst() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        return this.currentRowCount == 1;
    }

    @Override
    public boolean isLast() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        return this.currentRowCount == this.maxRowCount - 1;
    }

    @Override
    public void beforeFirst() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getType() == TYPE_FORWARD_ONLY) throw new SQLException(ExceptionMessage.TYPE_FORWARD_ONLY.getMessage());
        SQLiteResultSetBridge.beforeFirst(this.conn.getConnectionNativePointer(), this.stmtPtr);
        this.currentRowCount = 0;
    }

    @Override
    public void afterLast() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getType() == TYPE_FORWARD_ONLY) throw new SQLException(ExceptionMessage.TYPE_FORWARD_ONLY.getMessage());
        while (this.next());
    }

    @Override
    public boolean first() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getType() == TYPE_FORWARD_ONLY) throw new SQLException(ExceptionMessage.TYPE_FORWARD_ONLY.getMessage());
        this.beforeFirst();
        return this.next();
    }

    @Override
    public boolean last() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getType() == TYPE_FORWARD_ONLY) throw new SQLException(ExceptionMessage.TYPE_FORWARD_ONLY.getMessage());
        return this.absolute(-1);
    }

    @Override
    public int getRow() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        return this.currentRowCount;
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getType() == TYPE_FORWARD_ONLY) throw new SQLException(ExceptionMessage.TYPE_FORWARD_ONLY.getMessage());
        row = Math.min(Math.max((row + maxRowCount) % maxRowCount, 0), maxRowCount);
        this.beforeFirst();
        boolean b = true;
        for (int i = 0; i < row && b; i++)
            b = this.next();
        this.currentRowCount = row;
        return b;
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getType() == TYPE_FORWARD_ONLY) throw new SQLException(ExceptionMessage.TYPE_FORWARD_ONLY.getMessage());
        return this.absolute(this.currentRowCount + rows);
    }

    @Override
    public boolean previous() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getType() == TYPE_FORWARD_ONLY) throw new SQLException(ExceptionMessage.TYPE_FORWARD_ONLY.getMessage());
        return this.relative(-1);
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        throwExceptionThenNotSupport(this.getType(), this.getConcurrency(), this.getHoldability(), direction);
        this.fetchDirection = direction;
    }

    @Override
    public int getFetchDirection() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        return this.fetchDirection;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        this.fetchSize = rows;
    }

    @Override
    public int getFetchSize() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        return this.fetchSize;
    }

    @Override
    public int getType() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        return this.type;
    }

    @Override
    public int getConcurrency() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        return this.concurrency;
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        return this.rowUpdated;
    }

    @Override
    public boolean rowInserted() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        return this.rowInserted;
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        return this.rowDeleted;
    }

    @Override
    public void updateNull(int columnIndex) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateNull"));
    }

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateBoolean"));
    }

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateByte"));
    }

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateShort"));
    }

    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateInt"));
    }

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateLong"));
    }

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateFloat"));
    }

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateDouble"));
    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateBigDecimal"));
    }

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateString"));
    }

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateBytes"));
    }

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateDate"));
    }

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateTime"));
    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateTimestamp"));
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateAsciiStream"));
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateBinaryStream"));
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateCharacterStream"));
    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateObject"));
    }

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateObject"));
    }

    @Override
    public void updateNull(String columnLabel) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateNull"));
    }

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateBoolean"));
    }

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateByte"));
    }

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateShort"));
    }

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateInt"));
    }

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateLong"));
    }

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateFloat"));
    }

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateDouble"));
    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateBigDecimal"));
    }

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateString"));
    }

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateBytes"));
    }

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateDate"));
    }

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateTime"));
    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateTimestamp"));
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateAsciiStream"));
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateBinaryStream"));
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateCharacterStream"));
    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateObject"));
    }

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateObject"));
    }


    @Override
    public void insertRow() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        this.rowInserted = true;
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("insertRow"));
    }

    @Override
    public void updateRow() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        this.rowUpdated = true;
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateRow"));
    }

    @Override
    public void deleteRow() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        this.rowDeleted = true;
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("deleteRow"));
    }

    @Override
    public void refreshRow() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("refreshRow"));
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("cancelRowUpdates"));
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("moveToInsertRow"));
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("moveToCurrentRow"));
    }

    @Override
    public Statement getStatement() throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        return this.stmt;
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getObject"));
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getObject"));
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
        this.lastGetColumnIndex = columnIndex;
        return SQLiteResultSetBridge.getBlob(this.stmtPtr, columnIndex);
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getClob"));
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getArray"));
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        return this.getObject(this.findColumn(columnLabel), map);
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        return this.getRef(this.findColumn(columnLabel));
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        return this.getBlob(this.findColumn(columnLabel));
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        return this.getClob(this.findColumn(columnLabel));
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        return this.getArray(this.findColumn(columnLabel));
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
        this.lastGetColumnIndex = columnIndex;
        cal.setTime(getDate(columnIndex));
        return new Date(cal.getTimeInMillis());
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        return this.getDate(this.findColumn(columnLabel), cal);
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
        this.lastGetColumnIndex = columnIndex;
        cal.setTime(getDate(columnIndex));
        return new Time(cal.getTimeInMillis());
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        return this.getTime(this.findColumn(columnLabel), cal);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
        this.lastGetColumnIndex = columnIndex;
        cal.setTime(getDate(columnIndex));
        return new Timestamp(cal.getTimeInMillis());
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        return this.getTimestamp(this.findColumn(columnLabel), cal);
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        try {
            if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
            if (columnIndex <= 0 || columnIndex > this.metaData.getColumnCount()) throw new SQLException(new ArrayIndexOutOfBoundsException());
            this.lastGetColumnIndex = columnIndex;
            String urlStr = SQLiteResultSetBridge.getString(this.stmtPtr, columnIndex);
            if (urlStr == null) return null;

            return new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        return this.getURL(this.findColumn(columnLabel));
    }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateRef"));
    }

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateRef"));
    }

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateBlob"));
    }

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateBlob"));
    }

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateClob"));
    }

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateClob"));
    }

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateArray"));
    }

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateArray"));
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getRowId"));
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getRowId"));
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateRowId"));
    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateRowId"));
    }

    @Override
    public int getHoldability() throws SQLException {
        return this.holdability;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return this.isClosed || this.getStatementNativePtr().isClosed();
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateNString"));
    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        this.updateNString(this.findColumn(columnLabel), nString);
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateNClob"));
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        this.updateNClob(this.findColumn(columnLabel), nClob);
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getNClob"));
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        return this.getNClob(this.findColumn(columnLabel));
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getSQLXML"));
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        return this.getSQLXML(this.findColumn(columnLabel));
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateSQLXML"));
    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        this.updateSQLXML(this.findColumn(columnLabel), xmlObject);
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getNString"));
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        return this.getNString(this.findColumn(columnLabel));
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getNCharacterStream"));
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        return this.getNCharacterStream(this.findColumn(columnLabel));
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateNCharacterStream"));
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        this.updateNCharacterStream(this.findColumn(columnLabel), reader, length);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateAsciiStream"));
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateBinaryStream"));
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateCharacterStream"));
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        this.updateAsciiStream(this.findColumn(columnLabel), x, length);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        this.updateBinaryStream(this.findColumn(columnLabel), x, length);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        this.updateCharacterStream(this.findColumn(columnLabel), reader, length);
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateBlob"));
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        this.updateBlob(this.findColumn(columnLabel), inputStream, length);
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateClob"));
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        this.updateClob(this.findColumn(columnLabel), reader, length);
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateNClob"));
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        this.updateNClob(this.findColumn(columnLabel), reader, length);
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateNCharacterStream"));
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        this.updateNCharacterStream(this.findColumn(columnLabel), reader);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateAsciiStream"));
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateBinaryStream"));
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateCharacterStream"));
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        this.updateAsciiStream(this.findColumn(columnLabel), x);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        this.updateBinaryStream(this.findColumn(columnLabel), x);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        this.updateCharacterStream(this.findColumn(columnLabel), reader);
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateBlob"));
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        this.updateBlob(this.findColumn(columnLabel), inputStream);
    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateClob"));
    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        this.updateClob(this.findColumn(columnLabel), reader);
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        if (this.isClosed()) throw new SQLException(ExceptionMessage.RESULTSET_ALREADY_CLOSED.getMessage());
        if (this.getConcurrency() == ResultSet.CONCUR_READ_ONLY) throw new SQLException(ExceptionMessage.CONCUR_READ_ONLY.getMessage());
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("updateNClob"));
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        this.updateNClob(this.findColumn(columnLabel), reader);
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getObject"));
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        return this.getObject(this.findColumn(columnLabel), type);
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

    @Override
    public void onCommit(SQLiteEvent e) throws SQLException {
        if (!e.getConnection().equals(this.conn)) return;

        if (this.holdability == ResultSet.CLOSE_CURSORS_AT_COMMIT)
            this.close();
    }

    @Override
    public void onClose(SQLiteEvent e) throws SQLException {
        if (e.getSource().equals(this.stmt)) this.close();
    }

    @Override
    public void onCloseNativePointer(NativePointerEvent e) throws SQLException {
        this.close();
    }

    /**
     * SQLiteイベント発生を受け取るリスナを登録します
     * @param listener SQLiteイベント発生を受け取るリスナ
     */
    public void addSQLiteEventListener(SQLiteEventListener listener) {
        this.sqLiteEventListeners.add(listener);
    }
    /**
     * SQLiteイベント発生を受け取るリスナの登録を削除します
     * @param listener SQLiteイベント発生を受け取るリスナ
     */
    public void removeSQLiteEventListener(SQLiteEventListener listener) {
        this.sqLiteEventListeners.remove(listener);
    }

    NativePointer<NativePointer.StatementPtr> getStatementNativePtr() {
        return this.stmtPtr;
    }
}
