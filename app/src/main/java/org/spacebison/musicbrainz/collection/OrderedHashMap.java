package org.spacebison.musicbrainz.collection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by cmb on 16.03.16.
 */
public class OrderedHashMap<K, V> extends HashMap<K, V> implements Serializable {
    private final List<K> mKeyList;

    public OrderedHashMap() {
        super();
        mKeyList = new LinkedList<>();
    }

    public OrderedHashMap(int capacity) {
        super(capacity);
        mKeyList = new ArrayList<>(capacity);
    }

    public OrderedHashMap(int capacity, float loadFactor) {
        super(capacity, loadFactor);
        mKeyList = new ArrayList<>(capacity);
    }

    public OrderedHashMap(Map<? extends K, ? extends V> map) {
        super(map);
        mKeyList = new ArrayList<>(map.keySet());
    }

    @Override
    public V put(K key, V value) {
        mKeyList.remove(key);
        mKeyList.add(key);
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        final Set<? extends K> keySet = map.keySet();
        mKeyList.removeAll(keySet);
        mKeyList.addAll(keySet);
        super.putAll(map);
    }

    @Override
    public V remove(Object key) {
        mKeyList.remove(key);
        return super.remove(key);
    }

    @Override
    public void clear() {
        super.clear();
        mKeyList.clear();
    }

    public V getValueAt(int location) {
        return get(mKeyList.get(location));
    }

    public K getKeyAt(int location) {
        return mKeyList.get(location);
    }

    public Map.Entry<K, V> getEntryAt(int location) {
        K key = mKeyList.get(location);
        return new SimpleEntry<>(key, get(key));
    }

    public int indexOf(Object object) {
        return mKeyList.indexOf(object);
    }

    public int lastIndexOf(Object object) {
        return mKeyList.lastIndexOf(object);
    }
}
