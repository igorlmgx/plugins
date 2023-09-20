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
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;

public class CozyMarkerBuilder {
    private final Typeface font;
    private final float strokeSize;
    private MarkerCache markerCache;

    CozyMarkerBuilder(Context context) {
        font = ResourcesCompat.getFont(context, R.font.oatmealpro2_semibold);
        strokeSize = getDpFromPx(1.5f);
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

    private Bitmap getIconBitmap(String svgIcon, int width, int height, int rgbColor) {
        if (svgIcon == null)
            return null;
        
        String key = String.format("%d%d%d%d", svgIcon.hashCode(), width, height, rgbColor);

        if (key == null)
            return null;
        
        final Bitmap bitmap = markerCache.getBitmapFromMemCache(key);   
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

            finalSvgIcon = recolorSvg(finalSvgIcon, rgbColor);

            SVG  svg = SVG.getFromString(finalSvgIcon);
            if(widthMatcher.find() && heightMatcher.find()){
                svg.setDocumentViewBox(0,0,Integer.parseInt(widthMatcher.group(2)),Integer.parseInt(heightMatcher.group(2)));
            }
            
            Bitmap iconBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(iconBitmap);
            svg.renderToCanvas(canvas, new RectF(0, 0, width, height));

            markerCache.addBitmapToMemoryCache(svgIcon, iconBitmap);
            return iconBitmap;
        }catch (Exception e) {}

        return null;
    }

    private String recolorSvg(String originalSvg, int rgbColor) {
        String hexColor = String.format("#%06X", (0xFFFFFF & rgbColor));
        return originalSvg.replaceAll("(fill=\")(.+?)(\")", "fill=\""+hexColor+"\"");
    }


    private CozyMarkerObjects cozyObjectsFromData(CozyMarkerData markerData){
        String text = markerData.label;
        boolean hasPointer = markerData.hasPointer;

        /* setting colors */
        final int defaultMarkerColor = Color.WHITE;
        final int defaultTextColor = Color.BLACK;
        final int defaultStrokeColor = Color.rgb(217, 219, 208);
        final int defaultIconCircleColor = Color.rgb(248, 249, 245);
        final int defaultIconColor = Color.BLACK;

        final int selectedMarkerColor = Color.rgb(57, 87, 189);
        final int selectedTextColor = Color.WHITE;
        final int selectedIconCircleColor = Color.WHITE;

        final int visualizedMarkerColor = Color.rgb(217, 219, 208);
        final int visualizedTextColor = Color.BLACK;
        final int visualizedStrokeColor = Color.rgb(197, 201, 186);
        final int visualizedIconCircleColor = Color.WHITE;

        final int specialIconCircleColor = Color.rgb(240, 243, 255);
        final int specialIconColor = Color.rgb(57, 87, 189);

        int markerColor = defaultMarkerColor;
        int textColor = defaultTextColor;
        int strokeColor = defaultStrokeColor;
        int iconCircleColor = defaultIconCircleColor;
        int iconColor = defaultIconColor;

        if (markerData.isVisualized) {
            markerColor = visualizedMarkerColor;
            textColor = visualizedTextColor;
            strokeColor = visualizedStrokeColor;
            iconCircleColor = visualizedIconCircleColor;
        }
        if (markerData.isSelected) {
            markerColor = selectedMarkerColor;
            textColor = selectedTextColor;
            strokeColor = defaultStrokeColor;
            iconCircleColor = selectedIconCircleColor;   
        }
        if (markerData.variant.equals("special") && !markerData.isVisualized){
            iconCircleColor = specialIconCircleColor;
            iconColor = specialIconColor;
        }

        /* setting constants */
        // setting global constants
        final float paddingVertical = getDpFromPx(12);
        final float paddingHorizontal = getDpFromPx(11.5f);
        final float minMarkerWidth = getDpFromPx(40);
        final float strokeSize = getDpFromPx(1.5f);

        // setting constants for pointer
        final float pointerWidth = getDpFromPx(7);
        final float pointerHeight = getDpFromPx(6);
        
        // setting constants for icon
        final float iconSize =  getDpFromPx(16);
        final float iconCircleSize =  getDpFromPx(24);
        final float iconLeftPadding =  getDpFromPx(5);
        final float iconRightPadding =  getDpFromPx(3);

        /* setting variables */
        // gets the text size based on the font
        Rect textBounds = new Rect();
        float textSize = getDpFromPx(12f);
        Paint priceMarkerTextStyle = getTextPaint(textSize, textColor, 1.0f);
        priceMarkerTextStyle.getTextBounds(text, 0, text.length(), textBounds);

        // getting icon bitmap
        Bitmap iconBitmap = getIconBitmap(markerData.icon, (int)Math.round(iconSize), (int)Math.round(iconSize), iconColor);

        // additionalIconWidth to be used on markerWidth
        float iconAdditionalWidth = iconBitmap != null ? iconCircleSize + iconRightPadding : 0;

        // pointerSize to be used on bitmap creation
        float pointerSize = (hasPointer ? pointerHeight : 0);
        
        // setting marker width with a minimum width in case the string size is below the minimum
        float markerWidth = textBounds.width() + (2 * paddingHorizontal) + (2 * strokeSize) + iconAdditionalWidth;
        if (markerWidth < minMarkerWidth) {
            markerWidth = minMarkerWidth;
        }

        // set the marker height as the string height with space for padding and stroke
        float markerHeight = textBounds.height() + (2 * paddingVertical) + 2 * strokeSize;

        // bubble coordinates
        float bubbleShapeX = strokeSize/2;
        float bubbleShapeY = strokeSize/2;
        float bubbleShapeWidth = markerWidth - strokeSize;
        float bubbleShapeHeight = markerHeight - strokeSize;

        // other important variables
        float middleOfMarkerY = (bubbleShapeHeight / 2) + strokeSize/2;
       
        // text coordinates
        float textDx = getTextXOffset(markerWidth, textBounds) + iconAdditionalWidth/2;
        float textDy = getTextYOffset(markerHeight, textBounds);
        float textHeight = textSize;
        float textWidth = textBounds.width();
        
        // Icon coordinates
        float iconX = iconLeftPadding + strokeSize + (iconCircleSize - iconSize)/2;
        float iconY = (bubbleShapeHeight / 2) - (iconSize / 2) + strokeSize/2;
        float iconWidth = iconSize;
        float iconHeight = iconSize;

         // Icon circle coordinates
        float circleX = iconLeftPadding + strokeSize + iconCircleSize/2 - iconCircleSize/2;
        float circleY = middleOfMarkerY - iconCircleSize/2;
        float circleWidth = iconCircleSize;
        float circleHeight = iconCircleSize;

        // Pointer coordinates
        float pointerX = markerWidth / 2f - pointerWidth;
        float pointerY = markerHeight - strokeSize;

        return new CozyMarkerObjects(
            // Canvas
            new CozyMarkerObject(
                new RectF(0, 0, markerWidth, markerHeight + pointerSize),
                0,
                0,
                1.0f
            ),
            // Bubble
            new CozyMarkerObject(
                new RectF(bubbleShapeX, bubbleShapeY, bubbleShapeX + bubbleShapeWidth, bubbleShapeY + bubbleShapeHeight),
                markerColor,
                strokeColor,
                1.0f
            ),
            // Label
            new CozyMarkerObject[]{
                new CozyMarkerObject(
                    new RectF(textDx, textDy, textDx + textWidth, textDy + textHeight),
                    textColor,
                    0,
                    1.0f,
                    text
                ),
            },
            // Icon
            new CozyMarkerObject(
                new RectF(iconX, iconY, iconX + svgWidth, iconY + svgHeight),
                iconColor,
                0,
                iconBitmap == null ? 0.0f : 1.0f,
                iconBitmap == null ? null : iconBitmap
            ),
            // IconCircle
            new CozyMarkerObject(
                new RectF(circleX, circleY, circleX + circleWidth, circleY + circleHeight),
                iconCircleColor,
                0,
                iconBitmap == null ? 0.0f : 1.0f
            ),
            // Pointer
            new CozyMarkerObject(
                new RectF(pointerX, pointerY, pointerX + 2 * pointerWidth, pointerY + pointerHeight),
                markerColor,
                strokeColor,
                1.0f
            )
        );
    }

    private Bitmap getMarkerBitmapFromObjects(CozyMarkerObjects cozyObjects){
        final CozyMarkerObject canvasObject = cozyObjects.canvas;
        final CozyMarkerObject bubble = cozyObjects.bubble;
        final CozyMarkerObject[] labels = cozyObjects.labels;
        final CozyMarkerObject icon = cozyObjects.icon;
        final CozyMarkerObject iconCircle = cozyObjects.iconCircle;
        final CozyMarkerObject pointer = cozyObjects.pointer;

        final Bitmap iconBitmap = (Bitmap) icon.data;
        
        /* start of drawing */
        // creates the marker bitmap
        Bitmap marker = Bitmap.createBitmap((int) Math.ceil(canvasObject.bounds.width()), (int) Math.round(canvasObject.bounds.height()), Bitmap.Config.ARGB_8888);
        
        // create the bubble shape
        RectF bubbleShape = bubble.bounds;

        Log.d("CozyMarkerBuilder", "bubbleShape: " + bubbleShape.width() + " " + bubbleShape.height());
        final int shapeBorderRadius = Math.round(getDpFromPx(50));

        Path bubblePath = new Path();
        bubblePath.addRoundRect(bubbleShape, shapeBorderRadius, shapeBorderRadius, Path.Direction.CW);

        // create the pointer shape
        Path pointerPath = new Path();
        pointerPath.setFillType(Path.FillType.EVEN_ODD);

        pointerPath.moveTo(pointer.bounds.left, pointer.bounds.top);
        pointerPath.lineTo(pointer.bounds.right, pointer.bounds.top);
        pointerPath.lineTo(pointer.bounds.centerX(), pointer.bounds.bottom);
        pointerPath.lineTo(pointer.bounds.left, pointer.bounds.top);
        pointerPath.close();
        bubblePath.op(bubblePath, pointerPath, Path.Op.UNION);

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

        Paint fillPaint2 = new Paint();
        fillPaint2.setAntiAlias(true);
        fillPaint2.setStyle(Paint.Style.FILL);
        fillPaint2.setColor(Color.RED);

        // draws the bubble
        Canvas canvas = new Canvas(marker);

        //canvas.drawRect(canvasObject.bounds, fillPaint2);
        canvas.drawPath(bubblePath, fillPaint);
        canvas.drawPath(bubblePath, strokePaint);

       
        // draws the text
        for (CozyMarkerObject label : labels){
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

    private CozyMarkerObject[] interpolateLabel(CozyMarkerObject interpolatedLabel, String startText, String endText, float step){
        if(startText.equals(endText)){
            return new CozyMarkerObject[]{
                new CozyMarkerObject(
                    interpolatedLabel.bounds,
                    interpolatedLabel.fillColor,
                    interpolatedLabel.strokeColor,
                    interpolatedLabel.alpha,
                    startText
                )
            };
        }

        // Calculate the size of string
        Paint textPaint = new Paint();
        textPaint.setTypeface(font);
        textPaint.setTextSize(interpolatedLabel.bounds.height());
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.LEFT);

        int startTextCharactersNumber = textPaint.breakText(startText, true, interpolatedLabel.bounds.width() * 1.2f, null);
        int endTextCharactersNumber = textPaint.breakText(endText, true, interpolatedLabel.bounds.width() * 1.2f, null);

        String clippedStartText = startText.substring(0, startTextCharactersNumber);
        String clippedEndText = endText.substring(0, endTextCharactersNumber);

        float startTextAlpha = 1.0f - step;
        float endTextAlpha = step;

        return new CozyMarkerObject[]{
            new CozyMarkerObject(
                interpolatedLabel.bounds,
                interpolatedLabel.fillColor,
                interpolatedLabel.strokeColor,
                startTextAlpha,
                clippedStartText
            ),
            new CozyMarkerObject(
                interpolatedLabel.bounds,
                interpolatedLabel.fillColor,
                interpolatedLabel.strokeColor,
                endTextAlpha,
                clippedEndText
            )
        };
    }
    
    private Bitmap interpolateIcons(Bitmap startIcon, Bitmap endIcon, float step){
        if(startIcon != null){
            return startIcon;
        }
        if(endIcon != null){
            return endIcon;
        }
        return null;
    }

    private Bitmap getMarkerBitmap(CozyMarkerData cozyMarkerData) {
        CozyMarkerObjects cozyMarkerObjects = cozyObjectsFromData(cozyMarkerData);
        return getMarkerBitmapFromObjects(cozyMarkerObjects);
    }

    private Bitmap getInterpolatedMarkerBitmap(CozyMarkerData startCozyMarkerData, CozyMarkerData endCozyMarkerData, float step) {
        CozyMarkerObjects startCozyMarkerObjects = cozyObjectsFromData(startCozyMarkerData);
        CozyMarkerObjects endCozyMarkerObjects = cozyObjectsFromData(endCozyMarkerData);

        // interpolate geometry
        CozyMarkerObject interpolatedCanvas = interpolateCozyMarkerObject(startCozyMarkerObjects.canvas, endCozyMarkerObjects.canvas, step);
        CozyMarkerObject interpolatedBubble = interpolateCozyMarkerObject(startCozyMarkerObjects.bubble, endCozyMarkerObjects.bubble, step);
        CozyMarkerObject interpolatedIcon = interpolateCozyMarkerObject(startCozyMarkerObjects.icon, endCozyMarkerObjects.icon, step);
        CozyMarkerObject interpolatedIconCircle = interpolateCozyMarkerObject(startCozyMarkerObjects.iconCircle, endCozyMarkerObjects.iconCircle, step);
        CozyMarkerObject interpolatedPointer = interpolateCozyMarkerObject(startCozyMarkerObjects.pointer, endCozyMarkerObjects.pointer, step);

        CozyMarkerObject interpolatedLabel = interpolateCozyMarkerObject(startCozyMarkerObjects.labels[0], endCozyMarkerObjects.labels[0], step);

        CozyMarkerObject[] interpolatedLabels = interpolateLabel(interpolatedLabel, (String) startCozyMarkerObjects.labels[0].data, (String) endCozyMarkerObjects.labels[0].data, step);
        interpolatedIcon.data = interpolateIcons((Bitmap) startCozyMarkerObjects.icon.data, (Bitmap) endCozyMarkerObjects.icon.data, step);

        CozyMarkerObjects interpolatedCozyMarkerObjects = new CozyMarkerObjects(interpolatedCanvas, interpolatedBubble, interpolatedLabels, interpolatedIcon, interpolatedIconCircle, interpolatedPointer);
        return getMarkerBitmapFromObjects(interpolatedCozyMarkerObjects);
    }

    private CozyMarkerObject interpolateCozyMarkerObject(CozyMarkerObject startCozyMarkerObject, CozyMarkerObject endCozyMarkerObject, float step) {
        RectF interpolatedBounds = new RectF();
        interpolatedBounds.left = interpolate(startCozyMarkerObject.bounds.left, endCozyMarkerObject.bounds.left, step);
        interpolatedBounds.top = interpolate(startCozyMarkerObject.bounds.top, endCozyMarkerObject.bounds.top, step);
        interpolatedBounds.right = interpolate(startCozyMarkerObject.bounds.right, endCozyMarkerObject.bounds.right, step);
        interpolatedBounds.bottom = interpolate(startCozyMarkerObject.bounds.bottom, endCozyMarkerObject.bounds.bottom, step);

        int interpolatedFillColor = interpolateColor(startCozyMarkerObject.fillColor, endCozyMarkerObject.fillColor, step);
        int interpolatedStrokeColor = interpolateColor(startCozyMarkerObject.strokeColor, endCozyMarkerObject.strokeColor, step);
        float interpolatedAlpha = interpolate(startCozyMarkerObject.alpha, endCozyMarkerObject.alpha, step);

        return new CozyMarkerObject(interpolatedBounds, interpolatedFillColor, interpolatedStrokeColor, interpolatedAlpha);
    }

    private float interpolate(float start, float end, float step) {
        return start + (end - start) * step;
    }

    private int interpolateColor(int start, int end, float step) {
        int startRed = (start >> 16) & 0xff;
        int startGreen = (start >> 8) & 0xff;
        int startBlue = start & 0xff;

        int endRed = (end >> 16) & 0xff;
        int endGreen = (end >> 8) & 0xff;
        int endBlue = end & 0xff;

        int interpolatedRed = (int) interpolate(startRed, endRed, step);
        int interpolatedGreen = (int) interpolate(startGreen, endGreen, step);
        int interpolatedBlue = (int) interpolate(startBlue, endBlue, step);

        return Color.rgb(interpolatedRed, interpolatedGreen, interpolatedBlue);
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

    public Bitmap buildAnimatedMarker(CozyMarkerData startMarkerData, CozyMarkerData endMarkerData, float step) {
        final Bitmap marker = getInterpolatedMarkerBitmap(startMarkerData, endMarkerData, step);
        return copyOnlyBitmapProperties(marker);
    }
}