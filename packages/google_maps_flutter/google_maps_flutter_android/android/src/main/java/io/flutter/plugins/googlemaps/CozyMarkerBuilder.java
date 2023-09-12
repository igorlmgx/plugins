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
import com.caverock.androidsvg.SVG;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import androidx.core.content.res.ResourcesCompat;

public class CozyMarkerBuilder {
    private final Typeface font;
    private MarkerCache markerCache;

    CozyMarkerBuilder(Context context) {
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

    private float getTextYOffset(float markerHeight, Rect rect) {
        return (markerHeight / 2f) + (rect.height() / 2f) - rect.bottom;
    }

    private float getTextXOffset(float markerWidth, Rect rect) {
        return (markerWidth / 2f) - (rect.width() / 2f) - rect.left;
    }

    private Path addPointerOnMarkerCenter(Bitmap marker, int pointerWidth, int pointerHeight, int shadowSize) {
        Path pointer = new Path();
        pointer.setFillType(Path.FillType.EVEN_ODD);
        float width = marker.getWidth();
        float height = marker.getHeight() - pointerHeight - shadowSize;
        pointer.moveTo(width / 2f - pointerWidth, height);
        pointer.lineTo(width / 2f + pointerWidth, height);
        pointer.lineTo(width / 2f, height + pointerHeight);
        pointer.lineTo(width / 2f - pointerWidth, height);
        pointer.close();
        return pointer;
    }

    private Bitmap getIconBitmap(String svgIcon, int width, int height) {
        if(svgIcon == null)
            return null;
        
        final Bitmap bitmap = markerCache.getBitmapFromMemCache(svgIcon);   
        if (bitmap != null) {
            return bitmap;
        }

        try{
            //Needed this because svg doesn't scale if there is fixed width and height
            String widthPattern = "(width=\")(.+?)(\")";
            String heightPattern = "(height=\")(.+?)(\")";

            Matcher widthMatcher = Pattern.compile(widthPattern).matcher(svgIcon);
            Matcher heightMatcher = Pattern.compile(heightPattern).matcher(svgIcon);

            String finalSvgIcon = svgIcon.replaceAll(widthPattern, "");
            finalSvgIcon = finalSvgIcon.replaceAll(heightPattern, "");

            SVG  svg = SVG.getFromString(finalSvgIcon);
            if(widthMatcher.find() && heightMatcher.find()){
                svg.setDocumentViewBox(0,0,Integer.parseInt(widthMatcher.group(2)),Integer.parseInt(heightMatcher.group(2)));
            }
            
            Bitmap iconBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(iconBitmap);
            svg.renderToCanvas(canvas, new RectF(0, 0, width, height));

            markerCache.addBitmapToMemoryCache(svgIcon, iconBitmap);
            return iconBitmap;
        }catch(Exception e){}

        return null;
    }

    private Bitmap getMarkerBitmap(CozyMarkerData markerData) {
        String text = markerData.label;
        boolean hasPointer = markerData.hasPointer;

        /* setting colors */
        final int defaultMarkerColor = Color.WHITE;
        final int defaultTextColor = Color.BLACK;
        final int defaultIconCircleColor = Color.rgb(248, 249, 245);

        final int selectedMarkerColor = Color.rgb(57, 87, 189);
        final int selectedTextColor = Color.WHITE;
        final int selectedIconCircleColor = Color.WHITE;

        final int visualizedMarkerColor = Color.rgb(248, 249, 245);
        final int visualizedTextColor = Color.rgb(110, 110, 100);
        final int visualizedIconCircleColor = Color.WHITE;

        int markerColor = defaultMarkerColor;
        int textColor = defaultTextColor;
        int iconCircleColor = defaultIconCircleColor;

        if(markerData.isVisualized) {
            markerColor = visualizedMarkerColor;
            textColor = visualizedTextColor;
            iconCircleColor = visualizedIconCircleColor;
        }
        if(markerData.isSelected) {
            markerColor = selectedMarkerColor;
            textColor = selectedTextColor;
            iconCircleColor = selectedIconCircleColor;
        }

        /* setting constants */
        // setting global constants
        final int paddingVertical = Math.round(getDpFromPx(12));
        final int paddingHorizontal = Math.round(getDpFromPx(11.5f));
        final int minMarkerWidth = Math.round(getDpFromPx(40));
        final int strokeSize = Math.round(getDpFromPx(1.5f));

        // setting constants for pointer
        final int pointerWidth = Math.round(getDpFromPx(7));
        final int pointerHeight = Math.round(getDpFromPx(6));
        
        // setting constants for icon
        final int iconSize =  Math.round(getDpFromPx(16));
        final int iconCircleSize =  Math.round(getDpFromPx(24));
        final int iconLeftPadding =  Math.round(getDpFromPx(5));
        final int iconRightPadding =  Math.round(getDpFromPx(3));

        /* setting variables */
        // gets the text size based on the font
        Rect textBounds = new Rect();
        float textSize = getDpFromPx(12f);
        Paint priceMarkerTextStyle = getTextPaint(textSize, textColor);
        priceMarkerTextStyle.getTextBounds(text, 0, text.length(), textBounds);

        // getting icon bitmap
        Bitmap iconBitmap = getIconBitmap(markerData.icon,iconSize,iconSize);

        // additionalIconWidth to be used on markerWidth
        final int iconAdditionalWidth = iconBitmap != null ? iconCircleSize + iconRightPadding : 0;

        // pointerSize to be used on bitmap creation
        final int pointerSize = (hasPointer ? pointerHeight : 0);
        
        // setting marker width with a minimum width in case the string size is below the minimum
        int markerWidth = textBounds.width() + (2 * paddingHorizontal) + (2 * strokeSize) + iconAdditionalWidth;
        if (markerWidth < minMarkerWidth) {
            markerWidth = minMarkerWidth;
        }

        // set the marker height as the string height with space for padding and stroke
        final int markerHeight = textBounds.height() + (2 * paddingVertical) + 2 * strokeSize;

        // gets a bubble path, centering in a space for stroke on the left and top side
        final int bubbleShapeWidth = markerWidth - strokeSize/2;
        final int bubbleShapeHeight = markerHeight - strokeSize/2;

        // other important variables
        float middleOfMarkerY = (bubbleShapeHeight / 2) + strokeSize/2;


        /* start of drawing */
        // creates the marker bitmap
        Bitmap marker = Bitmap.createBitmap(markerWidth, markerHeight + pointerSize, Bitmap.Config.ARGB_8888);
        
        // create the bubble shape
        RectF bubbleShape = new RectF(strokeSize/2, strokeSize/2, bubbleShapeWidth, bubbleShapeHeight);
        final int shapeBorderRadius = Math.round(getDpFromPx(50));
        Path bubblePath = new Path();
        bubblePath.addRoundRect(bubbleShape, shapeBorderRadius, shapeBorderRadius, Path.Direction.CW);

        // add pointer to shape if needed
        if (hasPointer) {
            Path pointerPath = addPointerOnMarkerCenter(marker, pointerWidth, pointerHeight, strokeSize);
            bubblePath.op(bubblePath, pointerPath, Path.Op.UNION);
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

        Paint fillPaint2 = new Paint();
        fillPaint2.setAntiAlias(true);
        fillPaint2.setStyle(Paint.Style.FILL);
        fillPaint2.setColor(Color.RED);

        // draws the bubble
        Canvas canvas = new Canvas(marker);
        canvas.drawRect(0, 0, markerWidth, markerHeight + pointerSize, fillPaint2);
        canvas.drawPath(bubblePath, fillPaint);
        canvas.drawPath(bubblePath, strokePaint);
       
        // draws the text
        float dx = getTextXOffset(markerWidth, textBounds) + iconAdditionalWidth/2;
        float dy = getTextYOffset(markerHeight, textBounds);

        canvas.drawText(text, dx, dy, priceMarkerTextStyle);
        
        // draws the icon if exists
        if (iconBitmap != null) {
            // Draw the bigger circle
            Paint circlePaint = new Paint();
            circlePaint.setAntiAlias(true);
            circlePaint.setStyle(Paint.Style.FILL);
            circlePaint.setColor(iconCircleColor);
            
            float circleX = iconLeftPadding + strokeSize + iconCircleSize/2;
            float circleY = middleOfMarkerY;
            float circleRadius = iconCircleSize/2;
            canvas.drawCircle(circleX, circleY, circleRadius, circlePaint);
            
            // Draw the icon itself
            float svgX = iconLeftPadding + strokeSize + (iconCircleSize - iconSize)/2;
            float svgY = (bubbleShapeHeight / 2) - (iconSize / 2) + strokeSize/2;

            canvas.drawBitmap(iconBitmap, svgX, svgY, null);
        }

        return marker;
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
        Bitmap marker = getMarkerBitmap(cozyMarkerData);
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
        final Bitmap marker = getMarkerBitmap(cozyMarkerData);
        return copyOnlyBitmapProperties(marker);
    }
}