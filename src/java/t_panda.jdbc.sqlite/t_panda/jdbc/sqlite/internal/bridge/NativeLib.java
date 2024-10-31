package t_panda.jdbc.sqlite.internal.bridge;

import t_panda.jdbc.sqlite.SQLiteDriver;
import t_panda.jdbc.sqlite.internal.ExceptionMessage;

import java.io.File;
import java.nio.file.Path;

/**
 * ネイティブライブラリに係る処理のクラス
 */
public class NativeLib {
    /** ネイティブライブラリ名 */
    private static final String NATIVE_LIB_NAME_FMT_SQLITE = "t_panda.jdbc.sqlite_%d.%s-%s.%s";
    /** バージョン */
    private static final String VERSION = "0.0.0.0";

    /**
     * ネイティブライブラリの絶対パスを取得します。<br>
     * ドライバの配置ディレクトリ + ネイティブライブラリファイル名を返却します。
     * @return ネイティブライブラリの絶対パスを取得します。
     */
    public static Path getNativeLibAbsolutePath() {
        return SQLiteDriver.DRIVER_JAR_LOCATION.getParent().resolve(String.format(
            NATIVE_LIB_NAME_FMT_SQLITE
           ,getJvmArch()
           ,getSystemFileExtension()
           ,VERSION
           ,getSystemFileExtension()
        )).toAbsolutePath().normalize();
    }

    public static String getSystemFileExtension() {
        final String os = System.getProperty("os.name", "").toLowerCase();
        if (false) {}
        else if (os.startsWith("win"))   return "dll";
        else if (os.startsWith("mac"))   return "jnilib";
        else if (os.startsWith("linux")) return "so";

        if (File.pathSeparatorChar == ';')
            return "dll";

        return "so";
    }

    public static int getJvmArch() {
        String os = System.getProperty( "sun.arch.data.model" ) ;
        if( os != null && !(os = os.trim()).isEmpty()) {
            if( "32".equals( os ) ) {
                return 32 ;
            }
            else if( "64".equals( os ) ) {
                return 64 ;
            }
        }
        os = System.getProperty( "os.arch" ) ;
        if( os == null || (os = os.trim()).isEmpty()) {
            throw new RuntimeException(ExceptionMessage.NOT_SUPPORTED_JVM_ARCH.getMessage());
        }
        if( os.endsWith( "86" ) ) {
            return 32 ;
        }
        else if( os.endsWith( "64" ) ) {
            return 64 ;
        }
        return 32 ;
    }
}
