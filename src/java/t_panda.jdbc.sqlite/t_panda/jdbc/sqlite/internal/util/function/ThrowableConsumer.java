package t_panda.jdbc.sqlite.internal.util.function;

@FunctionalInterface
public interface ThrowableConsumer<T, X extends Throwable> {
    void accept(T t) throws X;
}
