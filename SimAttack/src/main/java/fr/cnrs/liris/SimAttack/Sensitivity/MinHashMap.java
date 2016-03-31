package fr.cnrs.liris.SimAttack.Sensitivity;

import java.util.HashMap;

/**
 * Created by apetit on 03/08/15.
 */
public class MinHashMap<K,V extends Number & Comparable<? super V>> extends HashMap<K, V> {

    @Override
    public V put(K key, V value) {
        V oldValue = super.get(key);
        if (oldValue != null) {
            if (oldValue.compareTo(value) > 0) {
                return super.put(key, value);
            }
            return oldValue;
        }
        return super.put(key, value);
    }

    public int getDistanceToRoot(K key) {
        V value = super.get(key);
        V max = super.get(null);
        return max.intValue() - value.intValue()+1;
    }
}
