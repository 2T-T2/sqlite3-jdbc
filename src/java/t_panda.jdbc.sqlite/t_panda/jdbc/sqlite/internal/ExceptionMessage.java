package t_panda.jdbc.sqlite.internal;

import t_panda.jdbc.sqlite.internal.util.Switch;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

public enum ExceptionMessage {
    ARGUMENT_NULL,
    NOT_SUPPORT_LOGGER,
    DB_ACCESS_ERROR,
    DB_CONNECT_ERROR,
    NOT_SUPPORT_STORED_PROCEDURE,
    NOT_SUPPORT_METHOD,
    DB_DISCONNECT_ERROR,
    DB_ALREADY_CLOSED,
    NOT_SUPPORT_TRANSACTION_ISOLATION,
    NOT_SUPPORT_RESULTSET_TYPE,
    NOT_SUPPORT_RESULTSET_CONCURRENCY,
    ARGUMENT_OVER_MIN,
    NOT_SUPPORT_DATATYPE,
    STATEMENT_ALREADY_CLOSED, TIMEOUT, STATEMENT_EXEC_ERROR, CREATE_STATEMNT, ONLY_NO_RESULTSET, ILLEGAL_ARGUMENT, RESULTSET_ALREADY_CLOSED, NOT_FOUND_CLUMN_NAME, TYPE_FORWARD_ONLY, NOT_SUPPORT_FETCH_DIRECTION, CONCUR_READ_ONLY, NOT_SUPPORT_METHOD_THIS_CLASS, NOT_SUPPORTED_JVM_ARCH;

    private final static Properties properties = new Properties();

    static {
        try (var reader = new InputStreamReader(Resource.EXCEPTION_MESSAGE_PROPERTIES.readAsStream(), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getMessage(Object... args) {
        return Switch.<String, String>of(Locale.getDefault().getLanguage())
                .then(Locale.JAPANESE.getLanguage(), () -> String.format(properties.getProperty(this.name() + "_ﾆﾎﾟﾝｺﾞ"), args))
                .then(Locale.ENGLISH.getLanguage(), () -> String.format(properties.getProperty(this.name() + "_english"), args))
                .then(Locale.FRENCH.getLanguage(), () -> String.format(properties.getProperty(this.name() + "_français"), args))
                .then(Locale.CHINESE.getLanguage(), () -> String.format(properties.getProperty(this.name() + "_中文"), args))
                .then(Locale.SIMPLIFIED_CHINESE.getLanguage(), () -> String.format(properties.getProperty(this.name() + "_中文"), args))
                .then(Locale.TRADITIONAL_CHINESE.getLanguage(), () -> String.format(properties.getProperty(this.name() + "_中文"), args))
                .then(Locale.GERMAN.getLanguage(), () -> String.format(properties.getProperty(this.name() + "_Deutsch"), args))
                .then(Locale.ITALIAN.getLanguage(), () -> String.format(properties.getProperty(this.name() + "_Italiano"), args))
                .then(Locale.KOREAN.getLanguage(), () -> String.format(properties.getProperty(this.name() + "_한국어"), args))
                .defaultValue(() -> String.format(properties.getProperty(this.name() + "_ﾆﾎﾟﾝｺﾞ"), args))
                .end()
                ;
    }

}
