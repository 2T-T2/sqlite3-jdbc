package t_panda.jdbc.sqlite.event;

import t_panda.jdbc.sqlite.SQLiteConnection;

/**
 * SQLite のイベントを通達するときのイベントオブジェクト
 */
public class SQLiteEvent {
    /** このイベントが発生したデータベースコネクション */
    private final SQLiteConnection conn;
    private final Object source;

    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @param conn このイベントが発生したデータベースコネクション
     * @throws IllegalArgumentException if source is null
     */
    public SQLiteEvent(Object source, SQLiteConnection conn) {
        this.source = source;
        this.conn = conn;
    }

    /**
     * このイベントが発生したデータベースコネクション を取得します
     * @return このイベントが発生したデータベースコネクション
     */
    public SQLiteConnection getConnection() {
        return this.conn;
    }

    /**
     * イベントの送信元オブジェクトを取得します
     * @return イベントの送信オブジェクトを取得
     */
    public Object getSource() {
        return this.source;
    }
}
