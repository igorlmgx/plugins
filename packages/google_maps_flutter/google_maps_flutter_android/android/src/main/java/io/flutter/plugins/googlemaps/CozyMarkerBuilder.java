package io.flutter.plugins.googlemaps;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import androidx.core.content.res.ResourcesCompat;

public class CozyMarkerBuilder {
    private final int shadowSize = 4;
    private final int priceMarkerTailSize;
    private final int padding;
    private final int size;
    private final Typeface font;
    private MarkerCache markerCache;

    CozyMarkerBuilder(Context context) {
        size = getMarkerSize();
        padding = size / 3;
        priceMarkerTailSize = size / 6;
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

    private Paint getShadowPaint(int alpha) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(alpha);
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

    private float getTextYOffset(float markerHeight, Rect rect) {
        return (markerHeight / 2f) + (rect.height() / 2f) - rect.bottom;
    }

    private float getTextXOffset(float markerWidth, Rect rect) {
        return (markerWidth / 2f) - (rect.width() / 2f) - rect.left;
    }

    private Bitmap getClusterBitmap(String text) {
        Bitmap marker = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(marker);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, getShadowPaint(15));
        canvas.drawCircle(size / 2f, size / 2f, (size / 2.2f) - shadowSize, getMarkerPaint(Color.WHITE));
        Rect clusterRect = new Rect();
        Paint clusterTextStyle = getTextPaint(size / 3f, Color.BLACK);
        clusterTextStyle.getTextBounds(text, 0, text.length(), clusterRect);
        float dx = getTextXOffset(marker.getWidth(), clusterRect);
        float dy = getTextYOffset(marker.getHeight(), clusterRect);
        canvas.drawText(text, dx, dy, clusterTextStyle);
        return marker;
    }

    private Path addTailOnMarkerCenter(Bitmap marker, int tailSize) {
        Path pointer = new Path();
        pointer.setFillType(Path.FillType.EVEN_ODD);
        float width = marker.getWidth();
        float height = marker.getHeight() - tailSize - shadowSize;
        pointer.moveTo(width / 2f - tailSize, height);
        pointer.lineTo(width / 2f + tailSize, height);
        pointer.lineTo(width / 2f, height + tailSize);
        pointer.lineTo(width / 2f - tailSize, height);
        pointer.close();
        return pointer;
    }

    private Bitmap getPriceBitmap(String text) {
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

        int borderRadius = 20;
        canvas.drawRoundRect(shadow, borderRadius, borderRadius, getShadowPaint(15));
        canvas.drawRoundRect(bubble, borderRadius, borderRadius, getMarkerPaint(Color.WHITE));
        canvas.drawPath(addTailOnMarkerCenter(marker, priceMarkerTailSize), getMarkerPaint(Color.WHITE));

        float dx = getTextXOffset(width, rect);
        float dy = getTextYOffset(height, rect);
        canvas.drawText(text, dx, dy, priceMarkerTextStyle);
        return marker;
    }

    private Bitmap getPinBitmap(String text, int markerColor, int textColor, boolean hasTail) {

        float textSize = size / 3.5f;
        Rect rect = new Rect();
        Paint priceMarkerTextStyle = getTextPaint(textSize, textColor);
        priceMarkerTextStyle.getTextBounds(text, 0, text.length(), rect);

        int smallestPinSize = size / 2;
        int minWidth = Math.max(rect.width(), smallestPinSize);
        int markerWidth = minWidth + padding + shadowSize;

        int priceTailSize = (hasTail ? (int) (priceMarkerTailSize / 1.5f) : 0);
        int markerHeight = rect.height() + padding + shadowSize;

        Bitmap marker = Bitmap.createBitmap(markerWidth, markerHeight + priceTailSize, Bitmap.Config.ARGB_8888);

        int shapeWidth = markerWidth - shadowSize;
        int shapeHeight = markerHeight - shadowSize;

        RectF shape = new RectF(shadowSize, shadowSize, shapeWidth, shapeHeight);
        Path bubblePath = new Path();
        int shapeBorderRadius = 50;
        bubblePath.addRoundRect(shape, shapeBorderRadius, shapeBorderRadius, Path.Direction.CW);

        Canvas canvas = new Canvas(marker);

        if (hasTail) {
            bubblePath.addPath(addTailOnMarkerCenter(marker, priceTailSize));
        }

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(shadowSize);
        paint.setAlpha(50);
        paint.setShadowLayer(shadowSize, 0, 0, Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(markerColor);
        canvas.drawPath(bubblePath, paint);

        float dx = getTextXOffset(markerWidth, rect);
        float dy = getTextYOffset(shapeHeight, rect);

        canvas.drawText(text, dx, dy, priceMarkerTextStyle);
        return marker;
    }

    private Bitmap getMarker(String type, String text) {
        switch (type) {
            case "cluster":
                return getClusterBitmap(text);
            case "price":
                return getPriceBitmap(text);
            case "pin_cluster":
                return getPinBitmap(text, Color.WHITE, Color.BLACK, false);
            case "pin_cluster_selected":
                return getPinBitmap(text, Color.rgb(57, 87, 189), Color.WHITE, false);
            case "pin_cluster_visited":
                return getPinBitmap(text, Color.WHITE, Color.rgb(110, 110, 100), false);
            case "pin_price":
                return getPinBitmap(text, Color.WHITE, Color.BLACK, true);
            case "pin_price_selected":
                return getPinBitmap(text, Color.rgb(57, 87, 189), Color.WHITE, true);
            case "pin_price_visited":
                return getPinBitmap(text, Color.WHITE, Color.rgb(110, 110, 100), true);
            default:
                return null;
        }
    }

    private Bitmap copyOnlyBitmapProperties(Bitmap bitmap) {
        if (bitmap == null)
            return null;
        return bitmap.copy(bitmap.getConfig(), true);
    }

    private Bitmap bitmapWithCache(String type, String text) {
        String key = String.format("%s:%s", type, text);
        final Bitmap bitmap = markerCache.getBitmapFromMemCache(key);
        if (bitmap != null) {
            return bitmap;
        }
        Bitmap marker = getMarker(type, text);
        markerCache.addBitmapToMemoryCache(key, marker);
        return marker;
    }

    public void setCachingEnabled(boolean isCachingEnabled) {
        this.markerCache = isCachingEnabled ? new MarkerCache() : null;
    }

    public Bitmap buildMarker(String type, String text) {
        if (markerCache != null) {
            final Bitmap marker = bitmapWithCache(type, text);
            return copyOnlyBitmapProperties(marker);
        }
        final Bitmap marker = getMarker(type, text);
        return copyOnlyBitmapProperties(marker);
    }
}