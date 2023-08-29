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

    private Bitmap getMarkerBitmap(String text, int markerColor, int textColor, boolean hasTail) {
        // gets the text size based on the font
        Rect rect = new Rect();
        float textSize = getDpFromPx(12f);
        Paint priceMarkerTextStyle = getTextPaint(textSize, textColor);
        priceMarkerTextStyle.getTextBounds(text, 0, text.length(), rect);

        // set the marker width
        int paddingVertical = Math.round(getDpFromPx(12f));
        int paddingHorizontal = Math.round(getDpFromPx(11f));
        int minMarkerWidth = Math.round(getDpFromPx(40f));
        int strokeSize = Math.round(getDpFromPx(1.5f));
        int markerWidth = rect.width() + (2 * paddingHorizontal) + strokeSize;
        if (markerWidth < minMarkerWidth) {
            markerWidth = minMarkerWidth;
        }

        // set the marker height as the string height with space for padding and stroke
        int markerHeight = rect.height() + (2 * paddingVertical) + strokeSize;

        // creates a bitmap with the marker width and height
        // if a tail will be used, gets an extra spacing in the marker height for the tail
        int priceTailSize = (hasTail ? (int) (priceMarkerTailSize / 1.5f) : 0);
        Bitmap marker = Bitmap.createBitmap(markerWidth, markerHeight + priceTailSize, Bitmap.Config.ARGB_8888);

        // gets a bubble path, centering in a space for stroke on the left and top side
        int shapeWidth = markerWidth - strokeSize;
        int shapeHeight = markerHeight - strokeSize;
        RectF shape = new RectF(strokeSize, strokeSize, shapeWidth, shapeHeight);

        // add the path, and if a tail is used, add a tail path on the bottom center of the marker
        int shapeBorderRadius = Math.round(getDpFromPx(50));
        Path bubblePath = new Path();
        bubblePath.addRoundRect(shape, shapeBorderRadius, shapeBorderRadius, Path.Direction.CW);
        if (hasTail) {
            Path tailPath = addTailOnMarkerCenter(marker, priceTailSize, strokeSize);
            bubblePath.op(bubblePath, tailPath, Path.Op.UNION);
        }

        Paint fillPaint = new Paint();
        fillPaint.setAntiAlias(true);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(markerColor);

        Paint strokePaint = new Paint();
        strokePaint.setAntiAlias(true);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(Color.parseColor("#D9DBD0"));
        strokePaint.setStrokeWidth(strokeSize);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);

        // draws the path
        Canvas canvas = new Canvas(marker);
        canvas.drawPath(bubblePath, fillPaint);
        canvas.drawPath(bubblePath, strokePaint);

        // gets the text offset from the marker and draws it
        float dx = getTextXOffset(markerWidth, rect);
        float dy = getTextYOffset(shapeHeight, rect);
        canvas.drawText(text, dx, dy, priceMarkerTextStyle);

        return marker;
    }

    private Bitmap getMarker(CozyMarkerData cozyMarkerData) {
        int defaultMarkerColor = Color.WHITE;
        int defaultTextColor = Color.BLACK;

        int selectedMarkerColor = Color.rgb(57, 87, 189);
        int selectedTextColor = Color.WHITE;

        int visitedMarkerColor = Color.rgb(248, 249, 245);
        int visitedTextColor = Color.rgb(110, 110, 100);

        int markerColor = defaultMarkerColor;
        int textColor = defaultTextColor;

        if(cozyMarkerData.isVisualized) {
            markerColor = visitedMarkerColor;
            textColor = visitedTextColor;
        }
        if(cozyMarkerData.isSelected) {
            markerColor = selectedMarkerColor;
            textColor = selectedTextColor;
        }

        return getMarkerBitmap(cozyMarkerData.label, markerColor, textColor, cozyMarkerData.hasPointer);
    }

    private static float getDpFromPx(float px){
        return px * Resources.getSystem().getDisplayMetrics().density;
    }

    private Bitmap copyOnlyBitmapProperties(Bitmap bitmap) {
        if (bitmap == null)
            return null;
        return bitmap.copy(bitmap.getConfig(), true);
    }

    private Bitmap bitmapWithCache(CozyMarkerData cozyMarkerData) {
        String key = cozyMarkerData.toString();
        final Bitmap bitmap = markerCache.getBitmapFromMemCache(key);
        if (bitmap != null) {
            return bitmap;
        }
        Bitmap marker = getMarker(cozyMarkerData);
        markerCache.addBitmapToMemoryCache(key, marker);
        return marker;
    }

    public void setCachingEnabled(boolean isCachingEnabled) {
        this.markerCache = isCachingEnabled ? new MarkerCache() : null;
    }

    public Bitmap buildMarker(CozyMarkerData cozyMarkerData) {
        if (markerCache != null) {
            final Bitmap marker = bitmapWithCache(cozyMarkerData);
            return copyOnlyBitmapProperties(marker);
        }
        final Bitmap marker = getMarker(cozyMarkerData);
        return copyOnlyBitmapProperties(marker);
    }
}