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

    private Path addTailOnMarkerCenter(Bitmap marker, int tailSize, int shadowSize) {
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
        canvas.drawPath(addTailOnMarkerCenter(marker, priceMarkerTailSize, shadowSize), getMarkerPaint(Color.WHITE));

        float dx = getTextXOffset(width, rect);
        float dy = getTextYOffset(height, rect);
        canvas.drawText(text, dx, dy, priceMarkerTextStyle);
        return marker;
    }

    private Bitmap getPinBitmap(String text, int markerColor, int textColor, boolean hasTail) {

        // gets the text size based on the font
        Rect rect = new Rect();
        float textSize = size / 3.5f;
        Paint priceMarkerTextStyle = getTextPaint(textSize, textColor);
        priceMarkerTextStyle.getTextBounds(text, 0, text.length(), rect);

        // calculates if the minimum width will
        // be a specific size or is adjusted to the width of the string
        int smallestPinSize = size / 2;
        int minWidth = Math.max(rect.width(), smallestPinSize);

        // set the marker width as the minimum width with space for padding and shadow
        int padding = size / 2;
        int shadowSize = 6;
        int markerWidth = minWidth + padding + shadowSize;

        // set the marker height as the string height with space for padding and shadow
        int markerHeight = rect.height() + padding + shadowSize;

        // creates a bitmap with the marker width and height
        // if a tail will be used, gets an extra spacing in the marker height for the
        // tail
        int priceTailSize = (hasTail ? (int) (priceMarkerTailSize / 1.5f) : 0);
        Bitmap marker = Bitmap.createBitmap(markerWidth, markerHeight + priceTailSize, Bitmap.Config.ARGB_8888);

        // gets a bubble path, centering in a space for shadow on the left and top side
        int shapeWidth = markerWidth - shadowSize;
        int shapeHeight = markerHeight - shadowSize;
        RectF shape = new RectF(shadowSize, shadowSize, shapeWidth, shapeHeight);

        // add the path, and if a tail is used, add a tail path on the bottom center of
        // the marker
        int shapeBorderRadius = 50;
        Path bubblePath = new Path();
        bubblePath.addRoundRect(shape, shapeBorderRadius, shapeBorderRadius, Path.Direction.CW);
        if (hasTail) {
            bubblePath.addPath(addTailOnMarkerCenter(marker, priceTailSize, shadowSize));
        }

        // sets the shadow and marker paint
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(shadowSize);
        int shadowColor = Color.argb(128, 0, 0, 0);
        paint.setShadowLayer(shadowSize, 0, 0, shadowColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(markerColor);

        // draws the path
        Canvas canvas = new Canvas(marker);
        canvas.drawPath(bubblePath, paint);

        // gets the text offset from the marker and draws it
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