package t_panda.jdbc.sqlite.internal.bridge;

import java.util.Objects;
import java.util.function.Supplier;

public class Out<T> {
    private T value;

    public Out(T value) {
        this.value = value;
    }
    public Out() { this(null); }

    void set(T value) { this.value = value; }
    public T getAcceptNull() { return this.value; }
    public <X extends Throwable> T getOrElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (Objects.isNull(this.value))
            throw exceptionSupplier.get();
        return this.value;
    }
    public boolean isNull() {
        return Objects.isNull(this.value);
    }
}
