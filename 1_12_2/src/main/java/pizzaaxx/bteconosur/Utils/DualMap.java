package pizzaaxx.bteconosur.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DualMap<K, V> {

    private final Map<K, V> KtoV = new HashMap<>();
    private final Map<V, K> VtoK = new HashMap<>();

    public void put(K k, V v) {
        KtoV.put(k, v);
        VtoK.put(v, k);
    }

    public void removeK(K k) {
        KtoV.remove(k);
        VtoK.values().remove(k);
    }

    public void removeV(V v) {
        VtoK.remove(v);
        KtoV.values().remove(v);
    }

    public K getK(V v) {
        return VtoK.get(v);
    }

    public V getV(K k) {
        return KtoV.get(k);
    }

    public Set<K> getKs() {
        return KtoV.keySet();
    }

    public Set<V> getVs() {
        return VtoK.keySet();
    }

    public boolean containsK(K k) {
        return KtoV.containsKey(k);
    }

    public boolean containsV(V v) {
        return VtoK.containsKey(v);
    }
}
