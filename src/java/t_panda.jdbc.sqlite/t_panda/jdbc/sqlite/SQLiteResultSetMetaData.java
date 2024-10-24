package t_panda.jdbc.sqlite;

import t_panda.jdbc.sqlite.internal.ExceptionMessage;
import t_panda.jdbc.sqlite.internal.bridge.NativePointer;
import t_panda.jdbc.sqlite.internal.bridge.SQLiteResultSetBridge;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;

class SQLiteResultSetMetaData implements ResultSetMetaData {
    private final String[] columnNames;
    private final int concurrency;

    SQLiteResultSetMetaData(NativePointer<NativePointer.StatementPtr> stmtPtr, int concurrency) throws SQLException {
        this.columnNames = SQLiteResultSetBridge.getColumnNames(stmtPtr);
        this.concurrency = concurrency;
    }

    @Override
    public int getColumnCount() throws SQLException {
        return columnNames.length;
    }

    @Override
    public boolean isAutoIncrement(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isCaseSensitive(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isSearchable(int column) throws SQLException {
        return true;
    }

    @Override
    public boolean isCurrency(int column) throws SQLException {
        return false;
    }

    @Override
    public int isNullable(int column) throws SQLException {
        // TODO 取得法を考える
        return columnNullableUnknown;
    }

    @Override
    public boolean isSigned(int column) throws SQLException {
        return true;
    }

    @Override
    public int getColumnDisplaySize(int column) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getColumnDisplaySize"));
    }

    @Override
    public String getColumnLabel(int column) throws SQLException {
        return this.columnNames[column];
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        return this.columnNames[column];
    }

    @Override
    public String getSchemaName(int column) throws SQLException {
        // SQLiteにスキーマの概念はない
        return "";
    }

    @Override
    public int getPrecision(int column) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getPrecision"));
    }

    @Override
    public int getScale(int column) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getScale"));
    }

    @Override
    public String getTableName(int column) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getTableName"));
    }

    @Override
    public String getCatalogName(int column) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getCatalogName"));
    }

    @Override
    public int getColumnType(int column) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getColumnType"));
    }

    @Override
    public String getColumnTypeName(int column) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getColumnTypeName"));
    }

    @Override
    public boolean isReadOnly(int column) throws SQLException {
        return concurrency == ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public boolean isWritable(int column) throws SQLException {
        return concurrency == ResultSet.CONCUR_UPDATABLE;
    }

    @Override
    public boolean isDefinitelyWritable(int column) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("isDefinitelyWritable"));
    }

    @Override
    public String getColumnClassName(int column) throws SQLException {
        throw new UnsupportedOperationException(ExceptionMessage.NOT_SUPPORT_METHOD.getMessage("getColumnClassName"));
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

    int getColumnIndexOf(String str) {
        return Arrays.asList(this.columnNames).indexOf(str);
    }
}
