package pizzaaxx.bteconosur.Registry;

import java.util.Set;

public interface Registry<K, V> {

    boolean isLoaded(K id);

    void load(K id);

    void unload(K id);

    boolean exists(K id);

    V get(K id);

    Set<K> getIds();

    void scheduleDeletion(K id);

}
