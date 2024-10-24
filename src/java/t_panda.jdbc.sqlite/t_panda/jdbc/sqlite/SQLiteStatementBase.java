package t_panda.jdbc.sqlite;

import t_panda.jdbc.sqlite.event.SQLiteEvent;
import t_panda.jdbc.sqlite.event.SQLiteEventListener;
import t_panda.jdbc.sqlite.internal.bridge.NativePointer;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

abstract class SQLiteStatementBase implements Statement, SQLiteEventListener {
    private NativePointer<NativePointer.StatementPtr> stmtPtr = null;

    private final List<SQLiteEventListener> listeners = new ArrayList<>();

    /**
     * このステートメントに対応するネイティブポインタを取得します
     * @return このステートメントに対応するネイティブポインタ
     */
    NativePointer<NativePointer.StatementPtr> getStatementNativePtr() {
        return this.stmtPtr;
    }
    /**
     * このステートメントに対応するネイティブポインタを設定します
     * @param stmtPtr このステートメントに対応するネイティブポインタ
     */
    protected void setStatementNativePtr(NativePointer<NativePointer.StatementPtr> stmtPtr) {
        this.stmtPtr = stmtPtr;
    }
    /**
     * ネイティブで確保しているメモリを開放します
     * @throws SQLException メモリ開放に失敗した場合
     */
    protected void closeStatementNativePtr() throws SQLException {
        if (!Objects.isNull(this.stmtPtr))
            this.stmtPtr.close();
    }

    /**
     * このステートメントが属するデータベースコネクション、または、このオブジェクトがクローズされた時に、リスナに対してイベントを送る
     * @param eventSupplier 送信するイベントオブジェクトを生成するサプライヤ
     * @throws SQLException 例外発生
     */
    protected void dispatchCloseEvent(Supplier<SQLiteEvent> eventSupplier) throws SQLException {
        for(var listener : this.listeners) listener.onClose(eventSupplier.get());
    }

    /**
     * このステートメントが属するデータベースコネクションがコミットされた時に、リスナに対してイベントを送る
     * @param eventSupplier 送信するイベントオブジェクトを生成するサプライヤ
     * @throws SQLException 例外発生
     */
    protected void dispatchCommitEvent(Supplier<SQLiteEvent> eventSupplier) throws SQLException {
        for(var listener : this.listeners) listener.onCommit(eventSupplier.get());
    }

    /**
     * SQLiteイベント発生を受け取るリスナを登録します
     * @param listener SQLiteイベント発生を受け取るリスナ
     */
    public void addSQLiteEventListener(SQLiteEventListener listener) {
        this.listeners.add(listener);
    }
    /**
     * SQLiteイベント発生を受け取るリスナの登録を削除します
     * @param listener SQLiteイベント発生を受け取るリスナ
     */
    public void removeSQLiteEventListener(SQLiteEventListener listener) {
        this.listeners.remove(listener);
    }
    /**
     * SQLiteイベント発生を受け取るリスナの登録を全て削除します
     */
    public void removeAllSQLiteEventListener() { this.listeners.clear(); }
}