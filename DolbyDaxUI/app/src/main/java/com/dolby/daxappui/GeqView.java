package com.dolby.daxappui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public class GeqView extends View {
    private static final String PREFS_NAME = "dax_settings";
    private static final String PREF_EQ_GRAPH_LOCKED = "eq_graph_locked"; // Legacy global key.
    private static final String PREF_EQ_GRAPH_LOCKED_PROFILE = "eq_graph_locked_profile_";
    private int mInteractionProfile = 0;
    private DAXConfiguration mConfiguration;
    private Context mContext;
    private float mCurrentYPosition;
    private float mDragGain;
    private IDsFragObserver mFObserver;
    private float[] mGeqData;
    private Paint mGeqDataTitlePaint;
    private Paint mHLinePaint;
    private Paint mRecPaint;
    private Rect mRect;
    public int mSelectedBar;
    private Path mStokeLinePath;
    private Paint mStrokeLinePaint;
    private Paint mTitlePaint;
    private int[] mUserGain;
    private int[] mXLeftPosition;
    private int[] mXRightPosition;
    private boolean mGestureChanged;
    private final String[] yTitlesStrings;

    private void init() {
        setClickable(true);
        setFocusable(true);
        this.mTitlePaint = new Paint(1);
        this.mTitlePaint.setTextAlign(Paint.Align.RIGHT);
        this.mRecPaint = new Paint();
        this.mHLinePaint = new Paint();
        this.mStrokeLinePaint = new Paint(1);
        this.mStrokeLinePaint.setPathEffect(new DashPathEffect(new float[]{5.0f, 5.0f, 5.0f, 5.0f}, 1.0f));
        this.mStokeLinePath = new Path();
        this.mGeqDataTitlePaint = new Paint(1);
        this.mRect = new Rect();
        updateThisData(this.mGeqData);
    }

    private void updateUserGain() {
        if (this.mConfiguration == null) {
            return;
        }
        float gain = GeqScale.clamp(mGeqData[mSelectedBar],
                mConfiguration.getMinEditGain(), mConfiguration.getMaxEditGain());
        mGeqData[mSelectedBar] = GeqScale.fromUnits(GeqScale.toUnits(gain));
        if (this.mSelectedBar == 0) {
            this.mUserGain[this.mSelectedBar] = (int) (this.mGeqData[this.mSelectedBar] * 16.0f);
            this.mUserGain[this.mSelectedBar + 1] = (int) (((this.mGeqData[this.mSelectedBar] + this.mGeqData[this.mSelectedBar + 1]) / 2.0f) * 16.0f);
        } else if (this.mSelectedBar == 9) {
            this.mUserGain[(this.mSelectedBar * 2) - 1] = (int) (((this.mGeqData[this.mSelectedBar - 1] + this.mGeqData[this.mSelectedBar]) / 2.0f) * 16.0f);
            this.mUserGain[this.mSelectedBar * 2] = (int) (this.mGeqData[this.mSelectedBar] * 16.0f);
            this.mUserGain[(this.mSelectedBar * 2) + 1] = (int) (this.mGeqData[this.mSelectedBar] * 16.0f);
        } else {
            this.mUserGain[(this.mSelectedBar * 2) - 1] = (int) (((this.mGeqData[this.mSelectedBar - 1] + this.mGeqData[this.mSelectedBar]) / 2.0f) * 16.0f);
            this.mUserGain[this.mSelectedBar * 2] = (int) (this.mGeqData[this.mSelectedBar] * 16.0f);
            this.mUserGain[(this.mSelectedBar * 2) + 1] = (int) (((this.mGeqData[this.mSelectedBar] + this.mGeqData[this.mSelectedBar + 1]) / 2.0f) * 16.0f);
        }
        DsClientSettings.INSTANCE.setGraphicEqualizerBandGains(this.mFObserver, this.mUserGain);
    }
public GeqView(Context context) {
        super(context);
        this.yTitlesStrings = new String[]{"+12dB", "0dB", "-12dB"};
        this.mGeqData = new float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        this.mUserGain = new int[20];
        this.mXLeftPosition = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        this.mXRightPosition = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        this.mSelectedBar = -1;
        init();
        try {
            this.mFObserver = (IDsFragObserver) context;
            this.mContext = context;
            this.mConfiguration = DAXConfiguration.getInstance(this.mContext.getApplicationContext());
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement IDsFragObserver");
        }
    }
public GeqView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.yTitlesStrings = new String[]{"+12dB", "0dB", "-12dB"};
        this.mGeqData = new float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        this.mUserGain = new int[20];
        this.mXLeftPosition = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        this.mXRightPosition = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        this.mSelectedBar = -1;
        init();
        try {
            this.mFObserver = (IDsFragObserver) context;
            this.mContext = context;
            this.mConfiguration = DAXConfiguration.getInstance(this.mContext.getApplicationContext());
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement IDsFragObserver");
        }
    }

    public void updateThisData(float[] fArr) {
        this.mGeqData = fArr;
        postInvalidate();
    }

    private float getTrackTop() {
        return getResources().getDimensionPixelSize(R.dimen.geq_border_vertical_margin);
    }

    private float getTrackHeight() {
        return Math.max(0, getHeight() - 2 * getTrackTop());
    }

    float getFirstBarLeft() {
        return mXLeftPosition[0];
    }

    float getGainY(float gain) {
        return GeqScale.gainToY(gain, getTrackTop(), getTrackHeight(),
                mConfiguration.getMinEditGain(), mConfiguration.getMaxEditGain());
    }

    private int findBand(float x) {
        for (int band = 0; band < mGeqData.length; band++) {
            if (x >= mXLeftPosition[band] && x <= mXRightPosition[band]) return band;
        }
        return -1;
    }

    public void setInteractionProfile(int profile) {
        if (profile >= 0 && profile < 4) {
            mInteractionProfile = profile;
        }
        if (isInteractionLocked()) {
            mSelectedBar = -1;
        }
        invalidate();
    }

    private String getInteractionLockKey() {
        return PREF_EQ_GRAPH_LOCKED_PROFILE + mInteractionProfile;
    }

    public boolean isInteractionLocked() {
        return getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(getInteractionLockKey(), false);
    }

    public void setInteractionLocked(boolean locked) {
        getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(getInteractionLockKey(), locked)
                .apply();
        if (locked) {
            mSelectedBar = -1;
        }
        invalidate();
    }

    public static void clearAllInteractionLocks(Context context) {
        android.content.SharedPreferences.Editor editor = context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(PREF_EQ_GRAPH_LOCKED);
        for (int profile = 0; profile < 4; profile++) {
            editor.remove(PREF_EQ_GRAPH_LOCKED_PROFILE + profile);
        }
        editor.apply();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isInteractionLocked()) return false;
        if (!isEnabled() || mConfiguration == null || getTrackHeight() <= 0) return false;
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mSelectedBar = findBand(event.getX());
                if (mSelectedBar == -1) return false;
                mGestureChanged = false;
                mDragGain = mGeqData[mSelectedBar];
                mCurrentYPosition = y;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mSelectedBar == -1) return false;
                // Keep the unrounded drag value so slow sub-unit movements accumulate.
                mDragGain = GeqScale.dragGain(mDragGain, mCurrentYPosition - y,
                        getTrackHeight(), mConfiguration.getMinEditGain(),
                        mConfiguration.getMaxEditGain());
                mCurrentYPosition = y;
                mGeqData[mSelectedBar] = mDragGain;
                updateUserGain();
                mGestureChanged = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // MOVE already sent the displayed native gain; releasing must not snap it.
                mSelectedBar = -1;
                if (mGestureChanged && mFObserver != null) {
                    mFObserver.profileModificationChanged(mInteractionProfile);
                }
                mGestureChanged = false;
                invalidate();
                return true;
            default:
                return true;
        }
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        int width;
        float f;
        float f2;
        float f3;
        super.onDraw(canvas);
        boolean z2 = getResources().getBoolean(R.bool.tabletLayout);
        DsClientSettings dsClientSettings = DsClientSettings.INSTANCE;
        IDsFragObserver iDsFragObserver = this.mFObserver;
        boolean graphicEqualizerOn = dsClientSettings.getGraphicEqualizerOn(iDsFragObserver, iDsFragObserver.getActivePort());
        float f6 = getContext().getResources().getDisplayMetrics().density;
        int i2 = R.dimen.geq_border_vertical_margin;
        float height = getTrackHeight();
        if (z2) {
            width = getWidth() - getResources().getDimensionPixelSize(R.dimen.geq_bar_end_padding);
        } else {
            width = getWidth();
        }
        float f7 = height / 2.0f;
        int layoutDirectionFromLocale = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault());

        float trackTop = getTrackTop();
        float trackBottom = trackTop + height;

        // 10 EQ Sliders Geometry (100% Perfectly Centered with Equal Left and Right Margins)
        float startX = f6 * 6.0f; // 6dp from left
        float endX = width - (f6 * 6.0f); // 6dp from right
        float availableWidth = endX - startX;
        float colStep = availableWidth / this.mGeqData.length;

        // Paints for Capsule Channels and Sliders
        Paint trackBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackBgPaint.setColor(getResources().getColor(R.color.colorGeqBackgroundBar, this.mContext.getTheme()));

        Paint gainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint pillTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pillTextPaint.setTextSize(f6 * 9.5f);
        pillTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        pillTextPaint.setTextAlign(Paint.Align.CENTER);

        // 1. Draw Capsule Channel Tracks for all 10 bands
        for (int i7 = 0; i7 < this.mGeqData.length; i7++) {
            float colLeft = startX + (i7 * colStep) + (colStep * 0.16f);
            float colRight = startX + ((i7 + 1) * colStep) - (colStep * 0.16f);

            RectF trackRectF = new RectF(colLeft, trackTop, colRight, trackBottom);
            float radius = (colRight - colLeft) / 2.0f;
            canvas.drawRoundRect(trackRectF, radius, radius, trackBgPaint);
        }

        // 2. Draw Filled Capsule Sliders & Vertical Value Text inside the top curve
        for (int i10 = 0; i10 < this.mGeqData.length; i10++) {
            float f11 = this.mGeqData[i10];
            float colLeft = startX + (i10 * colStep) + (colStep * 0.16f);
            float colRight = startX + ((i10 + 1) * colStep) - (colStep * 0.16f);
            this.mXLeftPosition[i10] = (int) colLeft;
            this.mXRightPosition[i10] = (int) colRight;

            float gainTop = getGainY(f11);
            float radius = (colRight - colLeft) / 2.0f;

            if (graphicEqualizerOn) {
                RectF gainRectF = new RectF(colLeft, gainTop, colRight, trackBottom);
                gainPaint.setColor(getResources().getColor(R.color.colorGeqBar, this.mContext.getTheme()));
                canvas.drawRoundRect(gainRectF, radius, radius, gainPaint);

                // Draw Vertical Gain Text (rotated -90deg)
                String valStr = (f11 > 0 ? "+" : "") + new BigDecimal(f11).setScale(1, RoundingMode.HALF_UP).toString();
                float centerX = (colLeft + colRight) / 2.0f;
                float textY;
                int textColor;

                float halfLabel = pillTextPaint.measureText(valStr) / 2.0f;
                float labelInset = radius + halfLabel + f6 * 4.0f;
                if (trackBottom - gainTop < labelInset * 2.0f) {
                    // Keep the entire label on the unfilled track at low gains.
                    textY = gainTop - labelInset;
                    textColor = getResources().getColor(R.color.colorGeqTrackText, this.mContext.getTheme());
                } else {
                    textY = gainTop + labelInset;
                    textColor = getResources().getColor(R.color.colorGeqGainText, this.mContext.getTheme());
                }
                textY = Math.max(trackTop + labelInset, Math.min(trackBottom - labelInset, textY));

                pillTextPaint.setColor(textColor);
                canvas.save();
                canvas.rotate(-90.0f, centerX, textY);
                Paint.FontMetrics metrics = pillTextPaint.getFontMetrics();
                canvas.drawText(valStr, centerX, textY - (metrics.ascent + metrics.descent) / 2.0f, pillTextPaint);
                canvas.restore();
            }
        }
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
    }
    public void onUpdateGeqData() {
        mUserGain = DsClientSettings.INSTANCE.getGraphicEqualizerBandGains(mFObserver);
        if (mConfiguration == null || mUserGain == null) return;
        boolean gainWasClamped = false;
        for (int band = 0; band < mGeqData.length; band++) {
            int index = 2 * band;
            mGeqData[band] = mUserGain[index] / 16.0f;
            if (mGeqData[band] > mConfiguration.getMaxEditGain()) {
                mGeqData[band] = mConfiguration.getMaxEditGain();
                gainWasClamped = true;
            } else if (mGeqData[band] < mConfiguration.getMinEditGain()) {
                mGeqData[band] = mConfiguration.getMinEditGain();
                gainWasClamped = true;
            }
            // The flag deliberately persists across bands, matching the original Smali.
            if (gainWasClamped) {
                if (band == 0) {
                    mUserGain[band] = (int) (mGeqData[band] * 16.0f);
                    mUserGain[band + 1] = (int) (((mGeqData[band] + mGeqData[band + 1]) / 2.0f) * 16.0f);
                } else if (band == 9) {
                    mUserGain[index - 1] = (int) (((mGeqData[band - 1] + mGeqData[band]) / 2.0f) * 16.0f);
                    mUserGain[index] = (int) (mGeqData[band] * 16.0f);
                    mUserGain[index + 1] = (int) (mGeqData[band] * 16.0f);
                } else {
                    mUserGain[index - 1] = (int) (((mGeqData[band - 1] + mGeqData[band]) / 2.0f) * 16.0f);
                    mUserGain[index] = (int) (mGeqData[band] * 16.0f);
                    mUserGain[index + 1] = (int) (((mGeqData[band] + mGeqData[band + 1]) / 2.0f) * 16.0f);
                }
            }
        }
        if (gainWasClamped) DsClientSettings.INSTANCE.setGraphicEqualizerBandGains(mFObserver, mUserGain);
        updateThisData(mGeqData);
    }
}
