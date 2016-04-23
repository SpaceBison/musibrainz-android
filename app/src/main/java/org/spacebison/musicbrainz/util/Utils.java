package org.spacebison.musicbrainz.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.BundleCompat;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Created by cmb on 09.03.16.
 */
public class Utils {
    private static final String EXTRA_CUSTOM_TABS_SESSION       = "android.support.customtabs.extra.SESSION";
    private static final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR";

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

    public static Set<File> getFilesRecursively(final File file) {
        if (file.isFile()) {
            return Collections.singleton(file);
        }

        final Set<File> files = new HashSet<>();
        final Queue<File> dirQueue = new LinkedList<>();

        dirQueue.add(file);

        while (!dirQueue.isEmpty()) {
            final File currentDir = dirQueue.poll();
            final File[] dirFiles = currentDir.listFiles();

            for (File f : dirFiles) {
                if (f.isDirectory()) {
                    dirQueue.add(f);
                } else {
                    if (!files.add(f)) { // recursive loop check;
                        dirQueue.removeAll(Arrays.asList(dirFiles));
                        break;
                    }
                }
            }
        }

        return files;
    }

    @NonNull
    public static Intent getChromeCustomTabIntent(String url) {
        return getChromeCustomTabIntent(Uri.parse(url), 0);
    }

    @NonNull
    public static Intent getChromeCustomTabIntent(String url, int color) {
        return getChromeCustomTabIntent(Uri.parse(url), color);

    }

    @NonNull
    public static Intent getChromeCustomTabIntent(Uri uri) {
        return getChromeCustomTabIntent(uri, 0);
    }

    @NonNull
    public static Intent getChromeCustomTabIntent(Uri uri, int color) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        if (color != 0) {
            intent.putExtra(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR, color);
        }

        Bundle extras = new Bundle();
        BundleCompat.putBinder(extras, EXTRA_CUSTOM_TABS_SESSION, null);
        intent.putExtras(extras);

        return intent;
    }

    @NonNull
    public static String getUrlForIntent(Intent intent) {
        StringBuilder builder = new StringBuilder("intent:");

        Uri data = intent.getData();
        if (data != null) {
            builder.append(data.getHost());
        }

        builder.append("#Intent;");

        String package_ = intent.getPackage();
        if (package_ != null) {
            builder.append("package=").append(package_).append(';');
        }

        Set<String> categories = intent.getCategories();
        if (categories != null) {
            for (String category : categories) {
                builder.append("category=").append(category).append(';');
            }
        }

        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            builder.append("component=").append(componentName.getClassName()).append(';');
        }

        String scheme = intent.getScheme();
        if (scheme != null) {
            builder.append("scheme=").append(scheme).append(';');
        }

        builder.append("end;");

        return builder.toString();
    }

    public static void showToast(final Context context, final CharSequence text, final int duration) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, duration).show();
            }
        });
    }

    public static void showToast(final Context context, final int resId, final int duration) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, resId, duration).show();
            }
        });
    }
}
