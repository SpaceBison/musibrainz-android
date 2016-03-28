package org.spacebison.musicbrainz;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by cmb on 16.03.16.
 */
public class OrderedHashSet<E> extends HashSet<E> {
    private final List<E> mList;

    public OrderedHashSet() {
        super();
        mList = new LinkedList<>();
    }

    public OrderedHashSet(int capacity) {
        super(capacity);
        mList = new ArrayList<>(capacity);
    }

    public OrderedHashSet(int capacity, float loadFactor) {
        super(capacity, loadFactor);
        mList = new ArrayList<>(capacity);
    }

    public OrderedHashSet(Collection<? extends E> collection) {
        super(collection);
        mList = new ArrayList<>(collection);
    }

    public void add(int location, E object) {
        if (super.add(object)) {
            mList.add(location, object);
        }
    }

    @Override
    public boolean add(E object) {
        mList.add(object);
        return super.add(object);
    }

    public boolean addAll(int location, Collection<? extends E> collection) {
        return false;
    }

    @Override
    public void clear() {
        mList.clear();
        super.clear();
    }

    @Override
    public boolean remove(Object object) {
        mList.remove(object);
        return super.remove(object);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        mList.removeAll(collection);
        return super.removeAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        ArrayList<E> prior = new ArrayList<>(this);
        if (prior.retainAll(collection)) {
            ArrayList<E> added = new ArrayList<>(collection);
            added.removeAll(prior);
            mList.addAll(added);
            super.addAll(added);
            return true;
        }
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        mList.retainAll(collection);
        return super.retainAll(collection);
    }

    public E set(int location, E object) {
        final E previous = mList.get(location);
        if (super.contains(object) && previous != object) {
            mList.remove(location);
        } else {
            mList.set(location, object);
            super.add(object);
        }

        return previous;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return super.containsAll(collection);
    }

    public E get(int location) {
        return mList.get(location);
    }

    public int indexOf(Object object) {
        return mList.indexOf(object);
    }

    public int lastIndexOf(Object object) {
        return mList.lastIndexOf(object);
    }

    public ListIterator<E> listIterator() {
        return mList.listIterator();
    }

    @NonNull
    public ListIterator<E> listIterator(int location) {
        return mList.listIterator(location);
    }

    public E remove(int location) {
        E removed = mList.remove(location);
        super.remove(removed);
        return removed;
    }

    public List<E> toList() {
        return new ArrayList<>(mList);
    }
}
