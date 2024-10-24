package t_panda.jdbc.sqlite.event;

/**
 * ネイティブポインタに係るイベント発生時に生成されるオブジェクト
 */
public class NativePointerEvent {
    private final Object source;

    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public NativePointerEvent(Object source) {
        this.source = source;
    }

    /**
     * イベントの送信元オブジェクトを取得します
     * @return イベントの送信オブジェクトを取得
     */
    public Object getSource() {
        return this.source;
    }
}
