package io.flutter.plugins.googlemaps.cozy;

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
import com.caverock.androidsvg.SVG;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;
import io.flutter.plugins.googlemaps.MarkerCache;
import io.flutter.plugins.googlemaps.R;

public class CozyMarkerBuilder {
    private final Typeface font;
    private final float strokeSize;
    private final MarkerCache markerCache;

    private final CozyMarkerElementsBuilder cozyMarkerElementsBuilder;
    private final CozyMarkerInterpolator cozyMarkerInterpolator;

    public CozyMarkerBuilder(Context context) {
        markerCache = new MarkerCache();
        font = ResourcesCompat.getFont(context, R.font.oatmealpro2_semibold);
        strokeSize = CozyMarkerElementsBuilder.getDpFromPx(1.5f);

        cozyMarkerElementsBuilder = new CozyMarkerElementsBuilder(font, markerCache, strokeSize);
        cozyMarkerInterpolator = new CozyMarkerInterpolator(font);
    }

    private Paint getTextPaint(float size, int color, float alpha) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setTypeface(font);
        paint.setTextSize(size);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setAlpha((int) (alpha * 255));
        return paint;
    }

    private Bitmap getMarkerBitmapFromElements(CozyMarkerElements cozyObjects){
        final CozyMarkerElement canvasObject = cozyObjects.canvas;
        final CozyMarkerElement bubble = cozyObjects.bubble;
        final CozyMarkerElement[] labels = cozyObjects.labels;
        final CozyMarkerElement icon = cozyObjects.icon;
        final CozyMarkerElement iconCircle = cozyObjects.iconCircle;
        final CozyMarkerElement pointer = cozyObjects.pointer;

        final Bitmap iconBitmap = (Bitmap) icon.data;
        
        /* start of drawing */
        // creates the marker bitmap
        Bitmap marker = Bitmap.createBitmap((int) Math.ceil(canvasObject.bounds.width()), (int) Math.round(canvasObject.bounds.height()), Bitmap.Config.ARGB_8888);
        
        // create the bubble shape
        RectF bubbleShape = bubble.bounds;
        final int shapeBorderRadius = Math.round(1000);

        Path bubblePath = new Path();
        bubblePath.addRoundRect(bubbleShape, shapeBorderRadius, shapeBorderRadius, Path.Direction.CW);

        // create the pointer shape
        if(pointer.bounds.width() > 0){
            Path pointerPath = new Path();
            pointerPath.setFillType(Path.FillType.EVEN_ODD);

            pointerPath.moveTo(pointer.bounds.left, pointer.bounds.top);
            pointerPath.lineTo(pointer.bounds.right, pointer.bounds.top);
            pointerPath.lineTo(pointer.bounds.centerX(), pointer.bounds.bottom);
            pointerPath.lineTo(pointer.bounds.left, pointer.bounds.top);
            pointerPath.close();
            bubblePath.op(bubblePath, pointerPath, Path.Op.UNION);
        }

        Paint fillPaint = new Paint();
        fillPaint.setAntiAlias(true);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(bubble.fillColor);

        Paint strokePaint = new Paint();
        strokePaint.setAntiAlias(true);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(bubble.strokeColor);
        strokePaint.setStrokeWidth(strokeSize);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);

        // draws the bubble with the pointer
        Canvas canvas = new Canvas(marker);
        canvas.drawPath(bubblePath, fillPaint);
        canvas.drawPath(bubblePath, strokePaint);

       
        // draws the text
        for (CozyMarkerElement label : labels){
            String text = (String) label.data;

            Paint priceMarkerTextStyle = getTextPaint(label.bounds.height(), label.fillColor, label.alpha);
            canvas.drawText(text, label.bounds.left, label.bounds.top, priceMarkerTextStyle);
        }
        
        // draws the icon if exists
        if (icon.alpha > 0) {
            // Draw the bigger circle
            Paint circlePaint = new Paint();
            circlePaint.setAntiAlias(true);
            circlePaint.setStyle(Paint.Style.FILL);
            circlePaint.setColor(iconCircle.fillColor);
            circlePaint.setAlpha((int) (iconCircle.alpha * 255));

            float circleX = iconCircle.bounds.centerX();
            float circleY = iconCircle.bounds.centerY();
            float circleRadius = iconCircle.bounds.width()/2;
            canvas.drawCircle(circleX, circleY, circleRadius, circlePaint);

            Paint iconPaint = new Paint();
            iconPaint.setAlpha((int) (icon.alpha * 255));
            
            // Draw the icon itself
            canvas.drawBitmap(iconBitmap, icon.bounds.left, icon.bounds.top, iconPaint);
        }

        return marker;
    }

    private Bitmap getMarkerBitmap(CozyMarkerData cozyMarkerData) {
        CozyMarkerElements cozyMarkerElements = cozyMarkerElementsBuilder.cozyElementsFromData(cozyMarkerData);
        return getMarkerBitmapFromElements(cozyMarkerElements);
    }
    
    private Bitmap getInterpolatedMarkerBitmap(CozyMarkerData startMarkerData, CozyMarkerData endMarkerData, float step){
        CozyMarkerElements startCozyMarkerElements = cozyMarkerElementsBuilder.cozyElementsFromData(startMarkerData);
        CozyMarkerElements endCozyMarkerElements = cozyMarkerElementsBuilder.cozyElementsFromData(endMarkerData);
        
        final CozyMarkerElements cozyMarkerInterpolatedObjects = cozyMarkerInterpolator.getInterpolatedMarkerObjects(startCozyMarkerElements, endCozyMarkerElements, step);
        return getMarkerBitmapFromElements(cozyMarkerInterpolatedObjects);
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
        Bitmap marker = getMarkerBitmap(cozyMarkerData);
        markerCache.addBitmapToMemoryCache(key, marker);
        return marker;
    }

    public Bitmap buildMarker(CozyMarkerData cozyMarkerData) {
        if (markerCache != null) {
            final Bitmap marker = bitmapWithCache(cozyMarkerData);
            return copyOnlyBitmapProperties(marker);
        }
        final Bitmap marker = getMarkerBitmap(cozyMarkerData);
        return copyOnlyBitmapProperties(marker);
    }

    public Bitmap buildAnimatedMarker(CozyMarkerData startMarkerData, CozyMarkerData endMarkerData, float step) {
        final Bitmap marker = getInterpolatedMarkerBitmap(startMarkerData, endMarkerData, step);
        return copyOnlyBitmapProperties(marker);
    }
}