package com.dolby.daxappui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

/** A recycled, circular profile strip. Scrolling never changes the audio profile. */
public class ProfileTabLayout extends RecyclerView {
    private static final int COPIES = 1001;
    public interface OnProfileClickListener { void onProfileClick(int position); }

    private final LinearLayoutManager layout;
    private final ProfileAdapter profiles = new ProfileAdapter();
    private String[] names = new String[0];
    private int[] icons = new int[0];
    private int[] contentWidths = new int[0];
    private int[] widths = new int[0];
    private int selectedPosition;
    private boolean dolbyEnabled = true;
    private OnProfileClickListener listener;
    private final Runnable recenterTask = this::recenter;

    public ProfileTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        layout = new LinearLayoutManager(context, HORIZONTAL, false) {
            @Override public void onInitializeAccessibilityNodeInfo(Recycler recycler, State state,
                    AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(recycler, state, info);
                info.setCollectionInfo(AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(
                        1, names.length, false,
                        AccessibilityNodeInfoCompat.CollectionInfoCompat.SELECTION_MODE_SINGLE));
            }

            @Override public void onInitializeAccessibilityNodeInfoForItem(Recycler recycler,
                    State state, View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfoForItem(recycler, state, host, info);
                int position = getPosition(host);
                if (position < 0 || names.length == 0) return;
                info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(
                        0, 1, position % names.length, 1, false, host.isSelected()));
            }

            @Override public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
                super.onInitializeAccessibilityEvent(event);
                event.setItemCount(names.length);
                if (names.length > 0) {
                    int first = findFirstVisibleItemPosition();
                    int last = findLastVisibleItemPosition();
                    if (first >= 0) event.setFromIndex(first % names.length);
                    if (last >= 0) event.setToIndex(last % names.length);
                }
            }
        };
        setLayoutManager(layout);
        setAdapter(profiles);
        setHasFixedSize(true);
        setItemAnimator(null);
        setNestedScrollingEnabled(false);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        addOnScrollListener(new OnScrollListener() {
            @Override public void onScrollStateChanged(RecyclerView view, int state) {
                removeCallbacks(recenterTask);
                if (state == SCROLL_STATE_IDLE) post(recenterTask);
            }
        });
    }

    public void setProfiles(String[] labels, int[] resources, OnProfileClickListener callback) {
        if (labels.length != resources.length) throw new IllegalArgumentException("Profile icons/labels differ");
        stopScroll();
        removeCallbacks(recenterTask);
        // Drop holders from a previous configuration before replacing their icon cache.
        setAdapter(null);
        names = labels.clone();
        icons = resources.clone();
        listener = callback;
        contentWidths = new int[names.length];
        widths = new int[names.length];
        selectedPosition = 0;
        // Measure once per configuration, not on each scroll frame or layout pass.
        TextView probe = new TextView(getContext());
        probe.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        probe.setTextAppearance(getContext(), R.style.TabLayoutText);
        probe.setSingleLine(true);
        for (int i = 0; i < names.length; i++) {
            probe.setText(names[i]);
            probe.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            Drawable icon = ContextCompat.getDrawable(getContext(), icons[i]);
            contentWidths[i] = probe.getMeasuredWidth() + iconWidth(icon) + dp(4);
        }
        updateWidths(getWidth());
        setAdapter(profiles);
        if (names.length > 0) layout.scrollToPositionWithOffset(middle(), 0);
    }

    /** UI-only selection: never dispatch a profile command in response to a system update. */
    public void setSelectedProfilePosition(int position) {
        if (position < 0 || position >= names.length) return;
        boolean changed = selectedPosition != position;
        selectedPosition = position;
        refreshVisibleSelection();
        if (!changed) return;
        stopScroll();
        removeCallbacks(recenterTask);
        int first = layout.findFirstVisibleItemPosition();
        int last = layout.findLastVisibleItemPosition();
        if (first == NO_POSITION) {
            layout.scrollToPositionWithOffset(middle() + position, 0);
            return;
        }
        int target = nearestPosition(first, position, names.length);
        // Prefer an already-visible copy, including one across the cycle boundary.
        if (target < first) target += names.length;
        if (target <= last) {
            View tab = layout.findViewByPosition(target);
            if (tab != null && layout.getDecoratedLeft(tab) >= getPaddingLeft()
                    && layout.getDecoratedRight(tab) <= getWidth() - getPaddingRight()) return;
        }
        target = nearestPosition(first, position, names.length);
        layout.scrollToPositionWithOffset(target, 0);
    }

    static int nearestPosition(int anchor, int position, int count) {
        int target = anchor - anchor % count + position;
        if (target - anchor > count / 2) target -= count;
        else if (anchor - target > count / 2) target += count;
        // At the finite backing list's edges, use the next equivalent copy rather
        // than clamping to a different logical profile.
        if (target < 0) target += count;
        else if (target >= count * COPIES) target -= count;
        return target;
    }

    public void setDolbyEnabled(boolean enabled) {
        dolbyEnabled = enabled;
        refreshVisibleSelection();
    }

    private void refreshVisibleSelection() {
        for (int i = 0; i < getChildCount(); i++) {
            ViewHolder holder = getChildViewHolder(getChildAt(i));
            if (holder instanceof ProfileHolder) ((ProfileHolder) holder).updateSelection();
        }
    }

    private int middle() { return (COPIES / 2) * names.length; }

    private void recenter() {
        if (names.length == 0 || getScrollState() != SCROLL_STATE_IDLE) return;
        int first = layout.findFirstVisibleItemPosition();
        if (first == NO_POSITION || Math.abs(first - middle()) < names.length * 2) return;
        View anchor = layout.findViewByPosition(first);
        if (anchor == null || hasFocus()) return;
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i).isAccessibilityFocused()) return;
        }
        int offset = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
                ? getWidth() - getPaddingRight() - layout.getDecoratedRight(anchor)
                : layout.getDecoratedLeft(anchor) - getPaddingLeft();
        // The equivalent copy has identical widths and drawable states. Preserve its
        // pixel offset and only rebase at rest, leaving Android's fling physics alone.
        layout.scrollToPositionWithOffset(middle() + first % names.length, offset);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw && updateWidths(w)) profiles.notifyDataSetChanged();
    }

    @Override protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        // MainActivity handles density/font-size changes without being recreated.
        // Rebuild the small visible cache so text, icons, and widths stay in sync.
        int selected = selectedPosition;
        setProfiles(names, icons, listener);
        setSelectedProfilePosition(selected);
    }

    private boolean updateWidths(int viewport) {
        if (names.length == 0) return false;
        int total = 0;
        for (int width : contentWidths) total += width;
        int free = Math.max(0, viewport - getPaddingLeft() - getPaddingRight() - total);
        int spacing = Math.max(dp(28), free / names.length);
        boolean changed = false;
        for (int i = 0; i < names.length; i++) {
            int width = contentWidths[i] + spacing;
            if (free >= dp(28) * names.length && i < free % names.length) width++;
            changed |= widths[i] != width;
            widths[i] = width;
        }
        return changed;
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(recenterTask);
        stopScroll();
        super.onDetachedFromWindow();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private int iconWidth(Drawable icon) {
        return Math.round(dp(18) * (float) icon.getIntrinsicWidth() / Math.max(1, icon.getIntrinsicHeight()));
    }

    private final class ProfileHolder extends ViewHolder {
        final TextView label;
        final GradientDrawable pill;
        final Drawable[] cachedIcons;
        int position = -1;

        ProfileHolder(FrameLayout cell) {
            super(cell);
            cell.setLayoutParams(new RecyclerView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
            cell.setFocusable(true);
            label = new TextView(getContext());
            label.setTextAppearance(getContext(), R.style.TabLayoutText);
            label.setSingleLine(true);
            label.setGravity(Gravity.CENTER);
            label.setPadding(dp(10), 0, dp(10), 0);
            label.setCompoundDrawablePadding(dp(4));
            label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            pill = new GradientDrawable();
            pill.setCornerRadius(dp(18));
            label.setBackground(pill);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp(36), Gravity.CENTER);
            cell.addView(label, params);
            cachedIcons = new Drawable[names.length];
            cell.setOnClickListener(view -> {
                if (dolbyEnabled && position >= 0 && listener != null) listener.onProfileClick(position);
            });
        }

        void bind(int logicalPosition) {
            position = logicalPosition;
            itemView.getLayoutParams().width = widths[position];
            label.setText(names[position]);
            itemView.setContentDescription(names[position]);
            Drawable icon = cachedIcons[position];
            if (icon == null) {
                icon = ContextCompat.getDrawable(getContext(), icons[position]).mutate();
                icon.setBounds(0, 0, iconWidth(icon), dp(18));
                cachedIcons[position] = icon;
            }
            label.setCompoundDrawablesRelative(icon, null, null, null);
            updateSelection();
        }

        void updateSelection() {
            if (position < 0) return;
            boolean selected = dolbyEnabled && position == selectedPosition;
            int color = ContextCompat.getColor(getContext(), selected
                    ? R.color.colorSelectedTabText : R.color.colorProfileTextOff);
            itemView.setSelected(selected);
            itemView.setEnabled(dolbyEnabled);
            itemView.setClickable(dolbyEnabled);
            label.setTextColor(color);
            cachedIcons[position].setTint(color);
            pill.setColor(selected ? ContextCompat.getColor(getContext(), R.color.colorProfileSelectedPill)
                    : android.graphics.Color.TRANSPARENT);
        }
    }

    private final class ProfileAdapter extends Adapter<ProfileHolder> {
        @Override public int getItemCount() { return names.length * COPIES; }
        @Override public ProfileHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ProfileHolder(new FrameLayout(getContext()));
        }
        @Override public void onBindViewHolder(ProfileHolder holder, int position) {
            holder.bind(position % names.length);
        }
        @Override public void onViewAttachedToWindow(ProfileHolder holder) {
            super.onViewAttachedToWindow(holder);
            // RecyclerView can reattach a cached tab without binding it again.
            // Selection/power may have changed while it was off screen, so never
            // reuse its old pill, icon tint, or clickability when it becomes visible.
            holder.updateSelection();
        }
    }
}
