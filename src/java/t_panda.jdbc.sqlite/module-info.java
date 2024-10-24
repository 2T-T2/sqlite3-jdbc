/**
 * SQLite の jdbc モジュール
 */
module t_panda.jdbc.sqlite {
    requires transitive java.sql;
    requires transitive java.sql.rowset;
    opens t_panda.jdbc.sqlite;
    exports t_panda.jdbc.sqlite;
    opens t_panda.jdbc.sqlite.event;
    exports t_panda.jdbc.sqlite.event;
}
