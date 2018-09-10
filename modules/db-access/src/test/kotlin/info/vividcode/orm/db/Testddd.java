package info.vividcode.orm.db;

import kotlin.reflect.KProperty1;

import java.util.Objects;

public class Testddd {

    static <T, R> boolean test(T o, KProperty1<T, R> p, R v) {
        return Objects.equals(p.get(o), v);
    }

}
