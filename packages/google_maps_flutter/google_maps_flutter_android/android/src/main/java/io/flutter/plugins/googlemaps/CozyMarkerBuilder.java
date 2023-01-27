package io.flutter.plugins.googlemaps;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import androidx.core.content.res.ResourcesCompat;

public class CozyMarkerBuilder {
    private final int shadowSize = 3;
    private final int priceMarkerTailSize;
    private final int padding;
    private final int size;
    private final Bitmap blankClusterMarker;
    private final Typeface font;

    CozyMarkerBuilder(Context context) {
        size = getMarkerSize();
        padding = size / 3;
        priceMarkerTailSize = size / 6;
        blankClusterMarker = getEmptyClusterBitmap(size);
        font = ResourcesCompat.getFont(context, R.font.oatmealpro2_semibold);
    }

    private Paint getTextPaint(float size, int color) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setTypeface(font);
        paint.setTextSize(size);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.LEFT);
        return paint;
    }

    private Paint getMarkerPaint(int color) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAntiAlias(true);
        return paint;
    }

    private Paint getShadowPaint() {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(15);
        paint.setStrokeWidth(shadowSize);
        paint.setAntiAlias(true);
        return paint;
    }

    private int getMarkerSize() {
        int baseScreenHeight = 2467;
        int baseMarkerSize = 167;
        int maxMarkerSize = 172;
        int minMarkerSize = 67;

        int physicalPixelHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        double heightRatio = ((double) (physicalPixelHeight)) / ((double) (baseScreenHeight));
        int proportionalMarkerSize = (int) (baseMarkerSize * heightRatio);

        if (proportionalMarkerSize > maxMarkerSize) {
            return maxMarkerSize;
        } else
            return Math.max(proportionalMarkerSize, minMarkerSize);
    }

    private int getBorderRadius(int multiply) {
        double density = Resources.getSystem().getDisplayMetrics().density;
        return (int) (density * multiply);
    }

    private Bitmap getEmptyClusterBitmap(int size) {
        Bitmap marker = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(marker);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, getShadowPaint());
        canvas.drawCircle(size / 2f, size / 2f, (size / 2.2f) - shadowSize, getMarkerPaint(Color.WHITE));
        return marker;
    }

    private Bitmap getClusterMarker(String text) {
        Bitmap marker = Bitmap.createBitmap(this.blankClusterMarker);
        Rect clusterRect = new Rect();
        Paint clusterTextStyle = getTextPaint(size / 3f, Color.BLACK);
        clusterTextStyle.getTextBounds(text, 0, text.length(), clusterRect);
        float dx = getTextXOffset(marker.getWidth(), clusterRect);
        float dy = getTextYOffset(marker.getHeight(), clusterRect);
        Canvas canvas = new Canvas(marker);
        canvas.drawText(text, dx, dy, clusterTextStyle);
        return marker;
    }

    private Path getPriceMarkerTail(Bitmap marker) {
        Path pointer = new Path();
        pointer.setFillType(Path.FillType.EVEN_ODD);
        float width = marker.getWidth();
        float height = marker.getHeight() - priceMarkerTailSize - shadowSize;
        pointer.moveTo(width / 2f - priceMarkerTailSize, height);
        pointer.lineTo(width / 2f + priceMarkerTailSize, height);
        pointer.lineTo(width / 2f, height + priceMarkerTailSize);
        pointer.lineTo(width / 2f - priceMarkerTailSize, height);
        pointer.close();
        return pointer;
    }

    private Bitmap getPriceMarker(String text) {
        Rect rect = new Rect();
        Paint priceMarkerTextStyle = getTextPaint(size / 3.5f, Color.BLACK);
        priceMarkerTextStyle.getTextBounds(text, 0, text.length(), rect);

        int width = rect.width() + padding;
        int height = rect.height() + padding;
        int shadowWidth = width + shadowSize;
        int shadowHeight = height + shadowSize;
        Bitmap marker = Bitmap.createBitmap(shadowWidth, shadowHeight + priceMarkerTailSize, Bitmap.Config.ARGB_8888);

        RectF shadow = new RectF(0, 0, shadowWidth, shadowHeight);
        RectF bubble = new RectF(shadowSize, shadowSize, width, height);

        Canvas canvas = new Canvas(marker);

        int borderRadius = getBorderRadius(5);
        int shadowBorderRadius = getBorderRadius(10);
        canvas.drawRoundRect(shadow, shadowBorderRadius, shadowBorderRadius, getShadowPaint());
        canvas.drawRoundRect(bubble, borderRadius, borderRadius, getMarkerPaint(Color.WHITE));
        canvas.drawPath(getPriceMarkerTail(marker), getMarkerPaint(Color.WHITE));

        float dx = getTextXOffset(width, rect);
        float dy = getTextYOffset(height, rect);
        canvas.drawText(text, dx, dy, priceMarkerTextStyle);
        return marker;
    }

    private float getTextYOffset(float height, Rect rect) {
        return (height / 2f) + (rect.height() / 2f) - rect.bottom;
    }

    private float getTextXOffset(float width, Rect rect) {
        return (width / 2f) - (rect.width() / 2f) - rect.left;
    }

    private Bitmap getRoundedMarker(String text, int markerColor, int textColor) {
        Rect rect = new Rect();
        Paint priceMarkerTextStyle = getTextPaint(size / 3.5f, textColor);
        priceMarkerTextStyle.getTextBounds(text, 0, text.length(), rect);
        int minWidth = Math.max(rect.width(), size / 2);

        int markerWidth = minWidth + padding + shadowSize;
        int markerHeight = rect.height() + padding + shadowSize;
        Bitmap marker = Bitmap.createBitmap(markerWidth, markerHeight, Bitmap.Config.ARGB_8888);

        RectF shadow = new RectF(0, 0, markerWidth, markerHeight);
        RectF shape = new RectF(shadowSize, shadowSize, markerWidth - shadowSize, markerHeight - shadowSize);

        int borderRadius = getBorderRadius(15);
        int shadowRadius = getBorderRadius(20);
        Canvas canvas = new Canvas(marker);
        canvas.drawRoundRect(shadow, shadowRadius, shadowRadius, getShadowPaint());
        canvas.drawRoundRect(shape, borderRadius, borderRadius, getMarkerPaint(markerColor));

        float dx = getTextXOffset(markerWidth, rect);
        float dy = getTextYOffset(markerHeight, rect);

        canvas.drawText(text, dx, dy, priceMarkerTextStyle);
        return marker;
    }

    public Bitmap buildMarker(String type, String text) {
        switch (type) {
            case "count":
                return getClusterMarker(text);
            case "price":
                return getPriceMarker(text);
            case "rounded":
                return getRoundedMarker(text, Color.WHITE, Color.BLACK);
            case "rounded_selected":
                return getRoundedMarker(text, Color.rgb(57, 87, 189), Color.WHITE);
            case "rounded_visited":
                return getRoundedMarker(text, Color.WHITE, Color.rgb(110, 110, 100));
            default:
                return null;
        }
    }

}