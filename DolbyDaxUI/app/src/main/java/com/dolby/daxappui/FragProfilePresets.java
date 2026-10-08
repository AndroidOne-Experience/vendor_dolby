package com.dolby.daxappui;

import com.dolby.dax.SpatialAudioProfile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
            ProfileTabLayout tabs = (ProfileTabLayout) inflate.findViewById(R.id.profiletable);
            this.mViewPager = (CustomViewPager) inflate.findViewById(R.id.profileViewpager);
            this.mViewPager.setPagingEnabled(false);
            this.mProfileAdapter = new FragProfilePanelPageAdapter(getChildFragmentManager());
            this.mViewPager.setAdapter(this.mProfileAdapter);
            this.mViewPager.setOffscreenPageLimit(profileNames.length - 1);
            int[] profileIcons = {
                R.drawable.ic_dynamic_profile_panel, R.drawable.ic_movie_profile_panel,
                R.drawable.ic_music_profile_panel, R.drawable.ic_custom_profile_panel
            };
            int[] icons = new int[profileNames.length];
            for (int position = 0; position < icons.length; position++) {
                int profile = SpatialAudioProfile.fromPosition(position);
                icons[position] = profile == SpatialAudioProfile.ID
                        ? R.drawable.ic_spatial_profile : profileIcons[profile];
            }
            tabs.setProfiles(profileNames, icons, position -> {
                int profile = SpatialAudioProfile.fromPosition(position);
                mFObserver.chooseProfile(profile);
                mFObserver.profileSettingsChanged(profile);
            });
        }
        return inflate;
    }

    public void updateProfileSettings(int i) {
        if (!SpatialAudioProfile.isVisible(i)) return;
        int position = SpatialAudioProfile.toPosition(i);
        IDsFragObserver iDsFragObserver = this.mFObserver;
        if ((iDsFragObserver == null ? null : iDsFragObserver.getDolbyAudioEffect()) != null) {
            if (this.mTabletLayout) {
                TabletProfilesAdapter tabletProfilesAdapter = this.mTabletProfilesAdapter;
                if (tabletProfilesAdapter != null) {
                    tabletProfilesAdapter.setProfileSelected(position);
                    return;
                }
                return;
            }
            if (this.mViewPager != null) {
                View view = getView();
                this.mViewPager.setCurrentItem(position, false);
                ProfileTabLayout tabs = view == null ? null
                        : (ProfileTabLayout) view.findViewById(R.id.profiletable);
                if (tabs != null) tabs.setSelectedProfilePosition(position);
                FragProfilePanelPageAdapter fragProfilePanelPageAdapter = this.mProfileAdapter;
                if (fragProfilePanelPageAdapter != null) {
                    ((FragProfilePanel) fragProfilePanelPageAdapter.instantiateItem(this.mViewPager, SpatialAudioProfile.toPosition(i))).updateProfilePanel(i);
                    return;
                }
                return;
            }
            return;
        }
        Log.e("FragProfilePresets", "updateProfileSettings(): Dolby audio effect is null!");
    }

    public void updateProfileModificationState(int profile) {
        if (!SpatialAudioProfile.isVisible(profile)) {
            return;
        }
        if (this.mTabletLayout) {
            return;
        }
        if (this.mProfileAdapter != null && this.mViewPager != null) {
            FragProfilePanel panel = (FragProfilePanel) this.mProfileAdapter.instantiateItem(this.mViewPager, SpatialAudioProfile.toPosition(profile));
            if (panel != null) {
                panel.updateResetButtonState(profile);
            }
        }
    }

    public void setGeqViewEnabled(int i) {
        FragProfilePanelPageAdapter fragProfilePanelPageAdapter = this.mProfileAdapter;
        if (fragProfilePanelPageAdapter != null) {
            ((FragProfilePanel) fragProfilePanelPageAdapter.instantiateItem(this.mViewPager, SpatialAudioProfile.toPosition(i))).setGeqViewEnabled();
        }
    }

    @Override // android.widget.AdapterView.OnItemClickListener
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        if (getView() == null || this.mFObserver == null || adapterView != getView().findViewById(R.id.presetsListView)) {
            return;
        }
        this.mFObserver.chooseProfile(SpatialAudioProfile.fromPosition(i));
        this.mFObserver.profileSettingsChanged(SpatialAudioProfile.fromPosition(i));
    }
}
