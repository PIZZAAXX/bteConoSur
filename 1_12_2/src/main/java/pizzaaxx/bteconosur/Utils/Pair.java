package pizzaaxx.bteconosur.Utils;

public class Pair<K, V> {

    private final K key;
    private final V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass()) {
            return false;
        }

        Pair<?,?> pair = (Pair<?, ?>) obj;

        return key.equals(pair.key) && value.equals(pair.value);
    }
}
