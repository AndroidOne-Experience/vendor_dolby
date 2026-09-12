package com.dolby.daxappui;

import android.content.Context;

import android.support.v4.content.ContextCompat;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

class EqualizerAdapter extends BaseAdapter implements GestureDetector.OnGestureListener {
    private Context mContext;
    private GestureDetector mGestureDetector;
    private final IPresetListener mListener;
    private Integer mPosition = -1;
    private int mSelectedPreset = -1;
    private boolean mInteractionLocked = false;
    private int[] imgIdForIeqOn = {R.drawable.ic_focus_on_ieq, R.drawable.ic_open_on_ieq, R.drawable.ic_rich_on_ieq};
    private int[] imgIdForIeqOff = {R.drawable.ic_focus_off_ieq, R.drawable.ic_open_off_ieq, R.drawable.ic_rich_off_ieq};

    interface IPresetListener {
        void onPresetChanged(int i, boolean z);
    }

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return i;
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return false;
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public void onLongPress(MotionEvent motionEvent) {
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return false;
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public void onShowPress(MotionEvent motionEvent) {
    }

    private class Holder {
        ImageView imageView;
        LinearLayout linearLayout;
        int position;

        private Holder() {
        }
    }

    EqualizerAdapter(Context context, IPresetListener iPresetListener) {
        this.mContext = context;
        this.mListener = iPresetListener;
    }

    @Override // android.widget.Adapter
    public int getCount() {
        return this.imgIdForIeqOn.length;
    }

    @Override // android.widget.Adapter
    public Object getItem(int i) {
        return Integer.valueOf(i);
    }

    @Override // android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        Holder holder;
        if (view == null) {
            view = LayoutInflater.from(this.mContext).inflate(R.layout.equalizer_list_item, (ViewGroup) null);
            holder = new Holder();
            holder.imageView = (ImageView) view.findViewById(R.id.icon);
            holder.linearLayout = (LinearLayout) view.findViewById(R.id.equalizerListViewLayout);
            holder.position = i;
            this.mGestureDetector = new GestureDetector(this.mContext, this);
            view.setOnTouchListener(new View.OnTouchListener() { // from class: com.dolby.daxappui.EqualizerAdapter.1
                @Override // android.view.View.OnTouchListener
                public boolean onTouch(View view2, MotionEvent motionEvent) {
                    EqualizerAdapter.this.mPosition = Integer.valueOf(((Holder) view2.getTag()).position);
                    EqualizerAdapter.this.mGestureDetector.onTouchEvent(motionEvent);
                    return true;
                }
            });
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }
        if (i == this.mSelectedPreset) {
            holder.linearLayout.setBackground(ContextCompat.getDrawable(this.mContext, R.drawable.background_with_blue_shadow));
            holder.imageView.setImageResource(this.imgIdForIeqOn[i]);
        } else {
            holder.linearLayout.setBackground(ContextCompat.getDrawable(this.mContext, R.drawable.background_with_shadow));
            holder.imageView.setImageResource(this.imgIdForIeqOff[i]);
        }
        return view;
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        if (this.mInteractionLocked) {
            return true;
        }
        Integer num = this.mPosition;
        if (num == null) {
            return false;
        }
        this.mListener.onPresetChanged(num.intValue(), true);
        this.mSelectedPreset = this.mPosition.intValue();
        notifyDataSetChanged();
        return false;
    }

    void setInteractionLocked(boolean locked) {
        this.mInteractionLocked = locked;
    }

    void setIeqSelection(int i, boolean z) {
        this.mListener.onPresetChanged(i, z);
        this.mSelectedPreset = i;
        notifyDataSetChanged();
    }
}
