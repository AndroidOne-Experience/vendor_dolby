package com.dolby.daxappui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.StateListDrawable;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.StateSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Keeps inline profile icons close to their labels and spaces the groups evenly. */
public class ProfileTabLayout extends TabLayout {
    public ProfileTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getTabMode() != MODE_FIXED || !(getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout strip = (LinearLayout) getChildAt(0);
        int count = strip.getChildCount();
        if (count == 0) return;
        float density = getResources().getDisplayMetrics().density;
        int gap = Math.round(4 * density);
        int iconHeight = Math.round(18 * density);
        int[] contentWidths = new int[count];
        int totalContentWidth = 0;
        for (int i = 0; i < count; i++) {
            ViewGroup tab = (ViewGroup) strip.getChildAt(i);
            ImageView icon = null;
            TextView label = null;
            for (int j = 0; j < tab.getChildCount(); j++) {
                View child = tab.getChildAt(j);
                if (child instanceof ImageView) icon = (ImageView) child;
                if (child instanceof TextView) label = (TextView) child;
            }
            if (icon == null || label == null || icon.getDrawable() == null) return;
            Drawable drawable = icon.getDrawable();
            int iconWidth = Math.round(iconHeight * (float) drawable.getIntrinsicWidth()
                    / Math.max(1, drawable.getIntrinsicHeight()));
            LinearLayout.LayoutParams iconParams = (LinearLayout.LayoutParams) icon.getLayoutParams();
            iconParams.width = iconWidth;
            iconParams.height = iconHeight;
            iconParams.setMarginEnd(gap);
            icon.setLayoutParams(iconParams);
            contentWidths[i] = iconWidth + gap
                    + (int) Math.ceil(label.getPaint().measureText(label.getText().toString()));
            totalContentWidth += contentWidths[i];
        }

        int availableWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        int freeWidth = Math.max(0, availableWidth - totalContentWidth);
        int allocatedWidth = 0;
        for (int i = 0; i < count; i++) {
            View tab = strip.getChildAt(i);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tab.getLayoutParams();
            // Half of the same free space sits on either side of each centered group.
            int width = totalContentWidth <= availableWidth
                    ? contentWidths[i] + freeWidth / count + (i < freeWidth % count ? 1 : 0)
                    : availableWidth / count;
            if (i == count - 1) width = availableWidth - allocatedWidth;
            params.width = width;
            params.weight = 0;
            tab.setLayoutParams(params);
            applySelectedPill(tab, contentWidths[i], width);
            allocatedWidth += width;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Adds a compact selected-profile pill in both light and dark themes.
     * The drawable is inset to hug the icon + label instead of filling the whole TabLayout cell.
     */
    private void applySelectedPill(View tab, int contentWidth, int tabWidth) {
        float density = getResources().getDisplayMetrics().density;
        int horizontalPadding = Math.round(10 * density);
        int verticalInset = Math.round(6 * density);
        int desiredPillWidth = Math.min(tabWidth, contentWidth + (horizontalPadding * 2));
        int horizontalInset = Math.max(0, (tabWidth - desiredPillWidth) / 2);

        GradientDrawable pill = new GradientDrawable();
        pill.setShape(GradientDrawable.RECTANGLE);
        pill.setColor(ContextCompat.getColor(getContext(), R.color.colorProfileSelectedPill));
        pill.setCornerRadius(Math.round(18 * density));

        InsetDrawable selected = new InsetDrawable(
                pill, horizontalInset, verticalInset, horizontalInset, verticalInset);

        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_selected}, selected);
        background.addState(StateSet.WILD_CARD, new ColorDrawable(Color.TRANSPARENT));
        tab.setBackground(background);
    }
}
