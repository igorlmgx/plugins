package io.flutter.plugins.googlemaps.cozy;

import android.content.Context;
import android.view.Display;
import android.annotation.TargetApi;
import android.view.WindowManager;
import android.graphics.Bitmap;
import android.animation.ValueAnimator;
import android.animation.ObjectAnimator;
import android.view.animation.Interpolator;
import androidx.core.view.animation.PathInterpolatorCompat;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.lang.SuppressWarnings;

import java.util.*;
import android.os.*;
import java.util.Map;

import io.flutter.plugins.googlemaps.MarkerBuilder;
import io.flutter.plugins.googlemaps.Convert;
import io.flutter.plugins.googlemaps.MarkerController;

public class CozyMarkerAnimator {
    // Needed simultaneous markers because when switching frames it could have a gap between the previous and the next frame, causing glitch
    private final int totalNumberOfSimultaneousMarkers = 3;
    private final int markersTransitionAnimationDuration = 400;

    private GoogleMap googleMap;
    private Map<String, String> googleMapsMarkerIdToDartMarkerId;

    private final int framesNumber;
    private final float deviceFPS;
    private final CozyMarkerBuilder cozyMarkerBuilder;
    
    public CozyMarkerAnimator(Context context, CozyMarkerBuilder cozyMarkerBuilder){
        this.cozyMarkerBuilder = cozyMarkerBuilder;
        deviceFPS = getDeviceFPS(context);
        framesNumber = (int) (deviceFPS * ((float) markersTransitionAnimationDuration / 1000)) - 1;
    }

    public void setGoogleMap(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }

    public void setGoogleMapsMarkerIdToDartMarkerId(Map<String, String> googleMapsMarkerIdToDartMarkerId){
        this.googleMapsMarkerIdToDartMarkerId = googleMapsMarkerIdToDartMarkerId;
    }

    private float getDeviceFPS(Context context){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return legacyGetDeviceFPS(context);
        }
        return context.getDisplay().getRefreshRate();
    }

    @SuppressWarnings("deprecation")
    private float legacyGetDeviceFPS(Context context){
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        return display.getRefreshRate();
    }

    private class AnimatorManager {
        final Deque<Marker> markersQueue;
        final MarkerController markerController;
        final MarkerOptions templateMarkerOptions;
        protected int lastAnimationIndex = -1;
        
        final String markerId;

        AnimatorManager(Deque<Marker> markersQueue, MarkerController markerController, MarkerOptions templateMarkerOptions, String markerId){
            this.markersQueue = markersQueue;
            this.markerController = markerController;
            this.templateMarkerOptions = templateMarkerOptions;
            this.markerId = markerId;
        }

        private Marker addFrame(BitmapDescriptor frameBitmapDescriptor, float zIndex){
            templateMarkerOptions.icon(frameBitmapDescriptor);
            templateMarkerOptions.zIndex(zIndex);
            Marker newMarker = googleMap.addMarker(templateMarkerOptions);
            markersQueue.add(newMarker);
            return newMarker;
        }

        protected void removeLatestFrame(){
            Marker markerToRemove = markersQueue.remove();
            markerToRemove.remove();
        }

        protected void addFrameBelow(BitmapDescriptor frameBitmapDescriptor, int index){
            final float zIndex = markerController.marker.getZIndex() - (float) index/framesNumber;
            addFrame(frameBitmapDescriptor, zIndex);
        }

        protected void addFrameAbove(BitmapDescriptor frameBitmapDescriptor){
            final float zIndex = markerController.marker.getZIndex();
            googleMapsMarkerIdToDartMarkerId.remove(markersQueue.peek().getId());
            final Marker marker = addFrame(frameBitmapDescriptor, zIndex);
            googleMapsMarkerIdToDartMarkerId.put(marker.getId(), markerId);
        }

        protected void removeLatestFrameBelow(){
            removeLatestFrame();
        }

        protected void removeLatestFrameAbove(){
            googleMapsMarkerIdToDartMarkerId.remove(markersQueue.peek().getId());
            removeLatestFrame();
            googleMapsMarkerIdToDartMarkerId.put(markersQueue.peek().getId(), markerId);
        }
        
    }

    public void animateMarkerTransition(MarkerController markerController, Object newMarker, CozyMarkerData startMarkerData, CozyMarkerData endMarkerData){
        final List<BitmapDescriptor> bitmapsForFrame = new ArrayList<BitmapDescriptor>();
        final Deque<Marker> markersQueue = new ArrayDeque<Marker>();

        // templateMarkerOptions to be used as template repeatedly on all frames
        MarkerBuilder templateMarkerBuilder = new MarkerBuilder();
        String markerId = Convert.interpretMarkerOptionsWithoutIcon(newMarker, templateMarkerBuilder);
        MarkerOptions templateMarkerOptions = templateMarkerBuilder.build();
        markersQueue.add(markerController.marker);

        final AnimatorManager animatorManager = new AnimatorManager(
            markersQueue,
            markerController, 
            templateMarkerOptions,
            markerId
        );

        // getting all frames
        float markerInitialBitmapArea = 0;
        float markerFinalBitmapArea = 0;

        for (int i = 1; i <= framesNumber; i++){
            float step = (float) i/framesNumber;
            final Bitmap frameBitmap = cozyMarkerBuilder.buildAnimatedMarker(startMarkerData, endMarkerData, step);
            if (i == 1){
                markerInitialBitmapArea = frameBitmap.getWidth() * frameBitmap.getHeight();
            }
            if (i == framesNumber){
                markerFinalBitmapArea = frameBitmap.getWidth() * frameBitmap.getHeight();
            }

            final BitmapDescriptor markerIconFrame = BitmapDescriptorFactory.fromBitmap(frameBitmap);
            bitmapsForFrame.add(markerIconFrame);
        }

        // check if marker grows or shrinks
        boolean isMarkerShrinking = markerFinalBitmapArea < markerInitialBitmapArea;

        // if isMarkerShrinking it adds the markers in reverse order, so it begins with markers stacked already
        if (isMarkerShrinking){
            for (int i = 1; i < totalNumberOfSimultaneousMarkers; i++){
                animatorManager.addFrameBelow(bitmapsForFrame.get(i - 1), i);
            }
        }

        // Animation itself    
        final Handler handler = new Handler(Looper.getMainLooper());
        ValueAnimator transitionAnimator = ValueAnimator.ofFloat(0f, 1f);
        transitionAnimator.setDuration(markersTransitionAnimationDuration);

        transitionAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animationIndex = (int) ((float) animation.getAnimatedValue() * framesNumber);
                if(animationIndex == animatorManager.lastAnimationIndex){
                    return;
                }
                animatorManager.lastAnimationIndex = animationIndex;

                if(isMarkerShrinking){
                    int nextAnimationIndex = animationIndex + totalNumberOfSimultaneousMarkers - 1;
                    if (animationIndex > 0 && nextAnimationIndex <= framesNumber) {
                        animatorManager.addFrameBelow(bitmapsForFrame.get(nextAnimationIndex - 1), nextAnimationIndex);
                    }

                    if (markersQueue.size() > totalNumberOfSimultaneousMarkers || 
                        nextAnimationIndex > framesNumber &&
                        markersQueue.size() > 1
                    ) {
                        animatorManager.removeLatestFrameAbove();
                    }

                    // After some time, remove all exceeding markers
                    if((float) animation.getAnimatedValue() == 1f){
                        animatorManager.addFrameAbove(bitmapsForFrame.get(bitmapsForFrame.size() - 1));
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                animatorManager.removeLatestFrameAbove();

                                markerController.replace(markersQueue.remove());
                                markerController.currentCozyMarkerData = endMarkerData;
                            }
                        }, 100);
                    }
                }else{
                    if(animationIndex > 0 && animationIndex <= framesNumber){
                        animatorManager.addFrameAbove(bitmapsForFrame.get(animationIndex - 1));
                    }

                    if(markersQueue.size() > totalNumberOfSimultaneousMarkers){
                        animatorManager.removeLatestFrameBelow();
                    }

                    // After some time, remove all exceeding markers
                    if((float) animation.getAnimatedValue() == 1f){
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                while(markersQueue.size() > 1){
                                    animatorManager.removeLatestFrameBelow();
                                }
                                markerController.replace(markersQueue.remove());
                                markerController.currentCozyMarkerData = endMarkerData;
                            }
                        }, 100);
                    }
                }
            }
        });

        // Ease-out interpolation: https://easings.net/#easeOutCubic
        Interpolator transitionInterpolator = PathInterpolatorCompat.create(0.33f, 1f, 0.68f, 1f);
        transitionAnimator.setInterpolator(transitionInterpolator);
        transitionAnimator.start();
    }

}