package org.spacebison.musicbrainz;

import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;

/**
 * Created by cmb on 16.03.16.
 *
 * This is a wrapper for {@link android.support.v7.widget.RecyclerView.Adapter} that allows
 * for notifying about changes in the data set from outside the main thread.
 */
public class RecyclerViewAdapterNotifier {
    private RecyclerView.Adapter mAdapter;
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    public RecyclerViewAdapterNotifier(RecyclerView.Adapter adapter) {
        mAdapter = adapter;
    }

    /**
     * @see RecyclerView.Adapter#notifyDataSetChanged()
     */
    public void notifyDataSetChanged() {
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * @see RecyclerView.Adapter#notifyItemChanged(int)
     */
    public void notifyItemChanged(final int position) {
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyItemChanged(position);
            }
        });
    }

    /**
     * @see RecyclerView.Adapter#notifyItemChanged(int, Object)
     */
    public void notifyItemChanged(final int position, final Object payload) {
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyItemChanged(position, payload);
            }
        });
    }

    /**
     * @see RecyclerView.Adapter#notifyItemRangeChanged(int, int)
     */
    public void notifyItemRangeChanged(final int positionStart, final int itemCount) {
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyItemRangeChanged(positionStart, itemCount);
            }
        });
    }

    /**
     * @see RecyclerView.Adapter#notifyItemRangeChanged(int, int, Object)
     */
    public void notifyItemRangeChanged(final int positionStart, final int itemCount, final Object payload) {
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyItemRangeChanged(positionStart, itemCount, payload);
            }
        });
    }

    /**
     * @see RecyclerView.Adapter#notifyItemInserted(int)
     */
    public void notifyItemInserted(final int position) {
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyItemInserted(position);
            }
        });
    }

    public void notifyItemInserted() {
        notifyItemInserted(mAdapter.getItemCount() - 1);
    }

    /**
     * @see RecyclerView.Adapter#notifyItemMoved(int, int)
     */
    public void notifyItemMoved(final int fromPosition, final int toPosition) {
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyItemMoved(fromPosition, toPosition);
            }
        });
    }

    /**
     * @see RecyclerView.Adapter#notifyItemRangeInserted(int, int)
     */
    public void notifyItemRangeInserted(final int positionStart, final int itemCount) {
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyItemRangeInserted(positionStart, itemCount);
            }
        });
    }

    /**
     * @see RecyclerView.Adapter#notifyItemRemoved(int)
     */
    public void notifyItemRemoved(final int position) {
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyItemRemoved(position);
            }
        });
    }

    public void notfiyItemRemoved() {
        notifyItemRemoved(mAdapter.getItemCount());
    }

    /**
     * @see RecyclerView.Adapter#notifyItemRangeRemoved(int, int)
     */
    public void notifyItemRangeRemoved(final int positionStart, final int itemCount) {
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyItemRangeRemoved(positionStart, itemCount);
            }
        });
    }

}
