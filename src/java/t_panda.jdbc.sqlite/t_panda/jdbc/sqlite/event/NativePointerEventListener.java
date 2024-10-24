package t_panda.jdbc.sqlite.event;

import java.sql.SQLException;
import java.util.EventListener;

/**
 * ネイティブポインタに係るイベントを検知するリスナ
 */
public interface NativePointerEventListener extends EventListener {
    /**
     * ネイティブポインタのメモリが開放されたときに発生するイベントを受け取るメソッド
     * @param e ネイティブポインタイベントオブジェクト
     * @throws SQLException 例外発生時
     */
    void onCloseNativePointer(NativePointerEvent e) throws SQLException;
}
