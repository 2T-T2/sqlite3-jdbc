package t_panda.jdbc.sqlite.internal;

import java.util.List;

public enum SQLiteReturnCode {
    SQLITE_OK
    , SQLITE_ERROR
    , SQLITE_INTERNAL
    , SQLITE_PERM
    , SQLITE_ABORT
    , SQLITE_BUSY
    , SQLITE_LOCKED
    , SQLITE_NOMEM
    , SQLITE_READONLY
    , SQLITE_INTERRUPT
    , SQLITE_IOERR
    , SQLITE_CORRUPT
    , SQLITE_NOTFOUND
    , SQLITE_FULL
    , SQLITE_CANTOPEN
    , SQLITE_PROTOCOL
    , SQLITE_EMPTY
    , SQLITE_SCHEMA
    , SQLITE_TOOBIG
    , SQLITE_CONSTRAINT
    , SQLITE_MISMATCH
    , SQLITE_MISUSE
    , SQLITE_NOLFS
    , SQLITE_AUTH
    , SQLITE_FORMAT
    , SQLITE_RANGE
    , SQLITE_NOTADB
    , SQLITE_ROW
    , SQLITE_DONE
    ;

    public boolean isNoError() {
        return List.of(
            SQLITE_OK,
            SQLITE_ROW,
            SQLITE_DONE
        ).contains(this);
    }
}
