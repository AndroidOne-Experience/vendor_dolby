package com.dolby.daxappui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class TabletProfilesAdapter extends BaseAdapter {
    private final Context mContext;
    private int mSelectedProfile = 3;
    private final String[] mProfileNames = DAXApplication.getInstance().getProfileNames();
    private final Profile[] mProfiles = new Profile[4];

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return i;
    }

    private class Holder {
        ImageView imageView;
        int position;
        TextView title;

        private Holder() {
        }
    }

    TabletProfilesAdapter(Context context) {
        this.mContext = context;
        this.mProfiles[0] = new Profile(R.drawable.ic_dynamic_profile_panel, R.drawable.ic_dynamic_unselected_profile_panel);
        this.mProfiles[1] = new Profile(R.drawable.ic_movie_profile_panel, R.drawable.ic_movie_unselected_profile_panel);
        this.mProfiles[2] = new Profile(R.drawable.ic_music_profile_panel, R.drawable.ic_music_unselected_profile_panel);
        this.mProfiles[3] = new Profile(R.drawable.ic_custom_profile_panel, R.drawable.ic_custom_unselected_profile_panel);
    }

    @Override // android.widget.Adapter
    public int getCount() {
        return this.mProfiles.length;
    }

    @Override // android.widget.Adapter
    public Profile getItem(int i) {
        return this.mProfiles[i];
    }
@Override // android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        Holder holder;
        if (view == null) {
            view = LayoutInflater.from(this.mContext).inflate(R.layout.profile_list_item, (ViewGroup) null);
            holder = new Holder();
            holder.imageView = (ImageView) view.findViewById(R.id.profileIcon);
            holder.title = (TextView) view.findViewById(R.id.profileName);
            holder.position = i;
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }
        holder.title.setText(this.mProfileNames[i]);
        holder.imageView.setImageResource(this.mProfiles[i].getIcon(i == this.mSelectedProfile));
        holder.title.setTextColor(this.mContext.getResources().getColor(R.color.colorSelectedTabText, this.mContext.getTheme()));
        return view;
    }

    void setProfileSelected(int i) {
        this.mSelectedProfile = i;
        notifyDataSetChanged();
    }
}
