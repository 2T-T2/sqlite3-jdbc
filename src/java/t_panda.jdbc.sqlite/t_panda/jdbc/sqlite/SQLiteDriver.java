package t_panda.jdbc.sqlite;

import t_panda.jdbc.sqlite.internal.ExceptionMessage;
import t_panda.jdbc.sqlite.internal.bridge.NativeLib;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * SQLiteのJDBCドライバクラス
 */
public class SQLiteDriver implements Driver {
    /** SQLiteDriver が認識するURIのプレフィックス */
    public static final String URI_PREFIX = "jdbc:sqlite://";
    /** インメモリデータベースを使用する場合 */
    public static final String ON_MEMORY_DATABASE_FILENAME = ":memory:";
    /** ドライバのJarの配置位置 */
    public static final Path DRIVER_JAR_LOCATION;

    static {
        try {
            if (!isSupported()) throw new Exception("ドライバはこの環境はサポートしていません。");
            DRIVER_JAR_LOCATION = Paths.get(SQLiteDriver.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!NativeLib.getNativeLibAbsolutePath().toFile().exists())
                throw new FileNotFoundException(NativeLib.getNativeLibAbsolutePath().normalize().toString());
            System.load(NativeLib.getNativeLibAbsolutePath().toString());
            java.sql.DriverManager.registerDriver(new SQLiteDriver());

        } catch (URISyntaxException e) {
            throw new RuntimeException("ドライバの配置位置を取得できませんでした。\n" + e);

        } catch (SQLException e) {
            throw new RuntimeException("ドライバの登録 もしくは、ドライバのインスタンス化に失敗しました。\n" + e);

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (Objects.isNull(url))
            throw new SQLException(ExceptionMessage.ARGUMENT_NULL.getMessage(1, "url"));
        if (!acceptsURL(url))
            return null;
        return new SQLiteConnection(url);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        if (Objects.isNull(url))
            throw new SQLException(ExceptionMessage.ARGUMENT_NULL.getMessage(1, "url"));
        if (!url.startsWith(URI_PREFIX))
            return false;
        if (url.replace(URI_PREFIX, "").equals(ON_MEMORY_DATABASE_FILENAME) )
            return true;
        if (!Files.exists(Paths.get(url.replace(URI_PREFIX, ""))))
            throw new SQLException(ExceptionMessage.DB_ACCESS_ERROR.getMessage(url));
        return true;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        throw new UnsupportedOperationException("TODO: 実装");
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException(ExceptionMessage.NOT_SUPPORT_LOGGER.getMessage(this.getClass().getName()));
    }

    /**
     * このドライバがサポートする環境かどうかを判定します
     * @return このドライバがサポートする環境かどうか
     */
    public static boolean isSupported() {
        final String os = System.getProperty("os.name", "").toLowerCase();

        if (false) {}
        else if (os.startsWith("win") || File.pathSeparatorChar == ';') return NativeLib.getJvmArch() == 64;

        return false;
    }
}
