package t_panda.jdbc.sqlite.internal;

import t_panda.jdbc.sqlite.internal.bridge.NativePointer;
import t_panda.jdbc.sqlite.internal.bridge.SQLiteError;

import java.io.Serial;
import java.sql.SQLException;

public class SQLiteException extends SQLException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int errorCode;
    private final String message;

    public SQLiteException(NativePointer<NativePointer.ConnectionPtr> conn, SQLiteReturnCode rc) {
        this.errorCode = SQLiteError.getErrorCode(conn);
        this.message = String.format("%s(%d) %s", rc.name(), this.getErrorCode(), SQLiteError.getErrorLastMessage(conn));
    }

    @Override
    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
