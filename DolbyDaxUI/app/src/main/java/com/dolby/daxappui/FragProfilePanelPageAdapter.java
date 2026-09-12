package com.dolby.daxappui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import java.util.ArrayList;
import java.util.List;

class FragProfilePanelPageAdapter extends FragmentStatePagerAdapter {
    private List<FragProfilePanel> mFragments;
    private final String[] mProfileNames;

    @Override // android.support.v4.view.PagerAdapter
    public int getItemPosition(Object obj) {
        return -2;
    }

    FragProfilePanelPageAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
        this.mFragments = new ArrayList();
        this.mProfileNames = DAXApplication.getInstance().getProfileNames();
        for (int i = 0; i < this.mProfileNames.length; i++) {
            this.mFragments.add(FragProfilePanel.newInstance(i));
        }
    }

    @Override // android.support.v4.view.PagerAdapter
    public int getCount() {
        return this.mProfileNames.length;
    }

    @Override // android.support.v4.view.PagerAdapter
    public CharSequence getPageTitle(int i) {
        return this.mProfileNames[i];
    }

    @Override // android.support.v4.app.FragmentStatePagerAdapter
    public Fragment getItem(int i) {
        if (i == 0 || i == 1 || i == 2 || i == 3) {
            return this.mFragments.get(i);
        }
        return null;
    }
}
