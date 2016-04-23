package org.spacebison.musicbrainz.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import org.spacebison.musicbrainz.collection.OrderedHashMap;

/**
 * Created by cmb on 16.03.16.
 */
public class GenericFragmentPagerAdapter extends FragmentStatePagerAdapter {
    private final OrderedHashMap<Fragment, String> mFragments = new OrderedHashMap<>();

    public GenericFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public void addFragment(Fragment fragment) {
        addFragment(fragment, "");
    }

    public void addFragment(Fragment fragment, String title) {
        mFragments.put(fragment, title);
    }

    public void removeFragment(Fragment object) {
        mFragments.remove(object);
    }

    @Override
    public Fragment getItem(int position) {
        return mFragments.getKeyAt(position);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mFragments.getValueAt(position);
    }
}
