package org.spacebison.musicbrainz;

import android.support.annotation.NonNull;

import java.util.Iterator;

/**
 * Created by cmb on 09.03.16.
 */
public class Utils {
    public static <T> T get(@NonNull Iterable<T> iterable, int position) {
        if (position < 0) {
            return null;
        }

        Iterator<T> iterator = iterable.iterator();

        while (position-- > 0) {
            if (iterator.hasNext()) {
                iterator.next();
            } else {
                return null;
            }
        }

        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            return null;
        }
    }
}
