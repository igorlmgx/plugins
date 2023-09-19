package io.flutter.plugins.googlemaps;

import android.graphics.RectF;

public class CozyMarkerObject {
    public final RectF bounds;
    public final int fillColor;
    public final int strokeColor;
    public final float alpha;
    public Object data;

    public CozyMarkerObject(RectF bounds, int fillColor, int strokeColor, float alpha, Object data) {
        this.bounds = bounds;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.alpha = alpha;
        this.data = data;
    }

    public CozyMarkerObject(RectF bounds, int fillColor, int strokeColor, float alpha) {
        this(bounds, fillColor, strokeColor, alpha, null);
    }

    @Override
    public String toString() {
        return bounds.toString() + fillColor + strokeColor + alpha + data.toString();
    }
}