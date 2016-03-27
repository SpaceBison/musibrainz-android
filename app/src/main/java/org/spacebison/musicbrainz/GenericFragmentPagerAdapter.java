package org.spacebison.musicbrainz;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.LinkedList;

/**
 * Created by cmb on 16.03.16.
 */
public class GenericFragmentPagerAdapter extends FragmentPagerAdapter {
    private final LinkedList<Fragment> mFragments = new LinkedList<>();

    public GenericFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public boolean addFragment(Fragment fragment) {
        return mFragments.add(fragment);
    }

    public void addFragment(int location, Fragment fragment) {
        mFragments.add(location, fragment);
    }

    public boolean removeFragment(Fragment object) {
        return mFragments.remove(object);
    }

    public Fragment removeFragment(int position) {
        return mFragments.remove(position);
    }

    @Override
    public Fragment getItem(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }
}
