package t_panda.jdbc.sqlite.internal.util;

import java.util.Optional;
import java.util.function.Supplier;

public class Switch {
    public static <R, V> Case<R, V> of(V obj) {
        return new Case<>(obj);
    }

    public interface End<R> {
        R end();
    }

    public static class Case<R, V> {
        private final V baseObj;
        private R rtnVal;
        private boolean doneVal;

        public Case(V baseObj) {
            this.baseObj = baseObj;
            this.rtnVal = null;
            doneVal = false;
        }

        public Case<R, V> then(V obj, Supplier<R> supplier) {
            if (!doneVal && baseObj.equals(obj)) {
                rtnVal = supplier.get();
                doneVal = true;
            }
            return this;
        }

        public End<R> defaultValue(Supplier<R> supplier) {
            rtnVal = supplier.get();
            doneVal = true;
            return () -> rtnVal;
        }

        public Optional<R> end() {
            return Optional.ofNullable(this.rtnVal);
        }
    }
}