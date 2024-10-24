package t_panda.jdbc.sqlite.event;

import java.sql.SQLException;
import java.util.EventListener;

/**
 * SQLiteのイベントを受け取るリスナ
 */
public interface SQLiteEventListener extends EventListener {
    /**
     * コネクションがコミットされたときに発火します
     * @param e SQLiteEvent オブジェクト
     * @throws SQLException 例外発生時
     */
    void onCommit(SQLiteEvent e) throws SQLException;
    /**
     * 送信元のオブジェクトがクローズされたときに発火します
     * @param e SQLiteEvent オブジェクト
     * @throws SQLException 例外発生時
     */
    void onClose(SQLiteEvent e) throws SQLException;
}
