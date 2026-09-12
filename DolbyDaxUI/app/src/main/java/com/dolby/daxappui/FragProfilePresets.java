package com.dolby.daxappui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

public class FragProfilePresets extends Fragment implements AdapterView.OnItemClickListener {
    private FragProfilePanelPageAdapter mProfileAdapter;
    private boolean mTabletLayout;
    private TabletProfilesAdapter mTabletProfilesAdapter;
    private CustomViewPager mViewPager;
    private IDsFragObserver mFObserver = null;
    private ViewPager.OnPageChangeListener pagerListener = new ViewPager.OnPageChangeListener() { // from class: com.dolby.daxappui.FragProfilePresets.1
        @Override // android.support.v4.view.ViewPager.OnPageChangeListener
        public void onPageScrollStateChanged(int i) {
        }

        @Override // android.support.v4.view.ViewPager.OnPageChangeListener
        public void onPageScrolled(int i, float f, int i2) {
        }

        @Override // android.support.v4.view.ViewPager.OnPageChangeListener
        public void onPageSelected(int i) {
            FragProfilePresets.this.mFObserver.chooseProfile(i);
            // The ViewPager keeps all profile fragments cached. Re-read the newly active
            // engine profile immediately so a previously cached page never shows stale data.
            FragProfilePresets.this.mFObserver.profileSettingsChanged(i);
        }
    };
@Override // android.support.v4.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mFObserver = (IDsFragObserver) context;
        } catch (ClassCastException unused) {
            throw new ClassCastException(context.toString() + " must implement IDsFragObserver");
        }
    }

    @Override // android.support.v4.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override // android.support.v4.app.Fragment
    @SuppressLint({"ClickableViewAccessibility"})
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mTabletLayout = getResources().getBoolean(R.bool.tabletLayout);
        View inflate = layoutInflater.inflate(R.layout.fragprofilepresets, viewGroup, false);
        String[] profileNames = DAXApplication.getInstance().getProfileNames();
        if (this.mTabletLayout) {
            AdapterView adapterView = (AdapterView) inflate.findViewById(R.id.presetsListView);
            this.mTabletProfilesAdapter = new TabletProfilesAdapter(getActivity());
            if (adapterView != null) {
                adapterView.setAdapter(this.mTabletProfilesAdapter);
                adapterView.setOnItemClickListener(this);
            }
        } else {
            TabLayout tabLayout = (TabLayout) inflate.findViewById(R.id.profiletable);
            for (String str : profileNames) {
                TabLayout.Tab newTab = tabLayout.newTab();
                newTab.setText(str);
                tabLayout.addTab(newTab);
            }
            if (tabLayout != null) {
                this.mViewPager = (CustomViewPager) inflate.findViewById(R.id.profileViewpager);
                this.mViewPager.setPagingEnabled(false);
                this.mProfileAdapter = new FragProfilePanelPageAdapter(getChildFragmentManager());
                this.mViewPager.setAdapter(this.mProfileAdapter);
                this.mViewPager.addOnPageChangeListener(this.pagerListener);
                this.mViewPager.setOffscreenPageLimit(3);
                tabLayout.setupWithViewPager(this.mViewPager);
                tabLayout.setTabMode(TabLayout.MODE_FIXED);
                tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
                tabLayout.setSelectedTabIndicatorHeight(0);
                int[] tabIcons = {
                    R.drawable.ic_dynamic_profile_panel,
                    R.drawable.ic_movie_profile_panel,
                    R.drawable.ic_music_profile_panel,
                    R.drawable.ic_custom_profile_panel
                };
                for (int i = 0; i < tabLayout.getTabCount(); i++) {
                    TabLayout.Tab tab = tabLayout.getTabAt(i);
                    if (tab != null && i < tabIcons.length) {
                        tab.setIcon(tabIcons[i]);
                    }
                }
            }
        }
        return inflate;
    }

    public void updateProfileSettings(int i) {
        TabLayout.Tab tabAt;
        IDsFragObserver iDsFragObserver = this.mFObserver;
        if ((iDsFragObserver == null ? null : iDsFragObserver.getDolbyAudioEffect()) != null) {
            if (this.mTabletLayout) {
                TabletProfilesAdapter tabletProfilesAdapter = this.mTabletProfilesAdapter;
                if (tabletProfilesAdapter != null) {
                    tabletProfilesAdapter.setProfileSelected(i);
                    return;
                }
                return;
            }
            if (this.mViewPager != null) {
                View view = getView();
                if (view != null && (tabAt = ((TabLayout) view.findViewById(R.id.profiletable)).getTabAt(i)) != null) {
                    tabAt.select();
                }
                this.mViewPager.setCurrentItem(i);
                FragProfilePanelPageAdapter fragProfilePanelPageAdapter = this.mProfileAdapter;
                if (fragProfilePanelPageAdapter != null) {
                    ((FragProfilePanel) fragProfilePanelPageAdapter.instantiateItem(this.mViewPager, i)).updateProfilePanel(i);
                    return;
                }
                return;
            }
            return;
        }
        Log.e("FragProfilePresets", "updateProfileSettings(): Dolby audio effect is null!");
    }

    public void updateProfileModificationState(int profile) {
        if (profile < 0 || profile >= 4) {
            return;
        }
        if (this.mTabletLayout) {
            return;
        }
        if (this.mProfileAdapter != null && this.mViewPager != null) {
            FragProfilePanel panel = (FragProfilePanel) this.mProfileAdapter.instantiateItem(this.mViewPager, profile);
            if (panel != null) {
                panel.updateResetButtonState(profile);
            }
        }
    }

    public void setGeqViewEnabled(int i) {
        FragProfilePanelPageAdapter fragProfilePanelPageAdapter = this.mProfileAdapter;
        if (fragProfilePanelPageAdapter != null) {
            ((FragProfilePanel) fragProfilePanelPageAdapter.instantiateItem(this.mViewPager, i)).setGeqViewEnabled();
        }
    }

    @Override // android.widget.AdapterView.OnItemClickListener
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        if (getView() == null || this.mFObserver == null || adapterView != getView().findViewById(R.id.presetsListView)) {
            return;
        }
        this.mFObserver.chooseProfile(i);
        this.mFObserver.profileSettingsChanged(i);
    }
}
