package com.dolby.daxappui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/** Draws the gain scale in the existing margin without changing the graph bounds. */
public class GeqAxisView extends View {
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect graphBounds = new Rect();
    private final float[] gains = {12f, 0f, -12f};
    private final String[] labels = {"+12dB", "0dB", "-12dB"};

    public GeqAxisView(Context context, AttributeSet attrs) {
        super(context, attrs);
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        ViewGroup parent = (ViewGroup) getParent();
        GeqView graph = parent.findViewById(R.id.geqView);
        if (graph == null || graph.getHeight() == 0) return;

        graphBounds.set(0, 0, graph.getWidth(), graph.getHeight());
        parent.offsetDescendantRectToMyCoords(graph, graphBounds);
        float density = getResources().getDisplayMetrics().density;
        float right = graphBounds.left - getLeft() + graph.getFirstBarLeft() - 6 * density;
        labelPaint.setColor(getResources().getColor(R.color.colorProfileDesText, getContext().getTheme()));
        labelPaint.setTextSize(getResources().getDimension(R.dimen.geq_axis_text_size));
        // Fit the existing margin even with increased system font scaling.
        float availableWidth = Math.max(0, right - 2 * density);
        float widest = Math.max(labelPaint.measureText(labels[0]), labelPaint.measureText(labels[2]));
        if (widest > availableWidth) labelPaint.setTextSize(labelPaint.getTextSize() * availableWidth / widest);
        Paint.FontMetrics metrics = labelPaint.getFontMetrics();
        for (int i = 0; i < labels.length; i++) {
            float y = graphBounds.top - getTop() + graph.getGainY(gains[i]);
            float baseline = y - (metrics.ascent + metrics.descent) / 2;
            baseline = Math.max(-metrics.ascent, Math.min(getHeight() - metrics.descent, baseline));
            canvas.drawText(labels[i], right, baseline, labelPaint);
        }
    }
}
