package org.spacebison.musicbrainz;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Created by cmb on 09.03.16.
 */
public class MappedHashSet <T> implements Set<T> {
    private final LinkedHashMap<Integer, T> mMap = new LinkedHashMap<>();

    @Override
    public boolean add(T object) {
        return mMap.put(object.hashCode(), object) != object;
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> collection) {
        boolean changed = false;

        for (T t : collection) {
            changed |= add(t);
        }

        return changed;
    }

    @Override
    public void clear() {
        mMap.clear();
    }

    @Override
    public boolean contains(Object object) {
        return mMap.containsValue(object);
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> collection) {
        return mMap.values().containsAll(collection);
    }

    @Override
    public boolean isEmpty() {
        return mMap.isEmpty();
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return mMap.values().iterator();
    }

    @Override
    public boolean remove(Object object) {
        return mMap.remove(object.hashCode()) == object;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> collection) {
        boolean changed = false;
        for (Object t : collection) {
            changed |= remove(t);
        }
        return changed;
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> collection) {
        // TODO: 09.03.16
        return false;
    }

    @Override
    public int size() {
        return mMap.size();
    }

    @NonNull
    @Override
    public Object[] toArray() {
        return mMap.values().toArray();
    }

    @NonNull
    @Override
    public <T1> T1[] toArray(@NonNull T1[] array) {
        return mMap.values().toArray(array);
    }

    public T getByHash(int hash) {
        return mMap.get(hash);
    }

    public T get(int position) {
        return Utils.get(mMap.values(), position);
    }
}
