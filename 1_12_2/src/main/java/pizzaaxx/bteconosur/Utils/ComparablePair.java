package pizzaaxx.bteconosur.Utils;

import org.jetbrains.annotations.NotNull;

public class ComparablePair<K extends Comparable<K>, V extends Comparable<V>> extends Pair<K, V> implements Comparable<Pair<K, V>> {

    public ComparablePair(K k, V v) {
        super(k, v);
    }

    @Override
    public int compareTo(@NotNull Pair<K, V> o) {
        return (getKey().compareTo(o.getKey()) == 0 ? getValue().compareTo(o.getValue()) : getKey().compareTo(o.getKey()));
    }
}
