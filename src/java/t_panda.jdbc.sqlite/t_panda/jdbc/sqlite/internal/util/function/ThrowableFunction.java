package t_panda.jdbc.sqlite.internal.util.function;

@FunctionalInterface
public interface ThrowableFunction<T, R, X extends Throwable> {
    R apply(T t) throws X;
}
