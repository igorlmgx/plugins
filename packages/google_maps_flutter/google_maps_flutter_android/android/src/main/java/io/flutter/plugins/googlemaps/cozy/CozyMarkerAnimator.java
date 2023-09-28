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

    int lastAnimationIndex = -1;

    final float deviceFPS;
    final int markersTransitionAnimationDuration = 2000;
    final CozyMarkerBuilder cozyMarkerBuilder;

    public CozyMarkerAnimator(Context context, CozyMarkerBuilder cozyMarkerBuilder){
        this.cozyMarkerBuilder = cozyMarkerBuilder;
        deviceFPS = getDeviceFPS(context);
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

    public void animateMarkerTransition(MarkerController markerController, Object newMarker, CozyMarkerData startMarkerData, CozyMarkerData endMarkerData, GoogleMap googleMap, Map<String, String> googleMapsMarkerIdToDartMarkerId){
        final List<BitmapDescriptor> bitmapsForFrame = new ArrayList<BitmapDescriptor>();
        final Deque<Marker> markersQueue = new ArrayDeque<Marker>();

        // Needed simultaneous markers because when switching frames it could have a gap between the previous and the next frame, causing glitch
        final int totalNumberOfSimultaneousMarkers = 3;
        final int framesNumber = (int) (2 * ((float) markersTransitionAnimationDuration / 1000)) - 1;

        float markerInitialBitmapArea = 0;
        float markerFinalBitmapArea = 0;

        for(int i = 1; i <= framesNumber; i++){
            float step = (float) i/framesNumber;
            final Bitmap frameBitmap = cozyMarkerBuilder.buildAnimatedMarker(startMarkerData, endMarkerData, step);
            if(i == 1){
                markerInitialBitmapArea = frameBitmap.getWidth() * frameBitmap.getHeight();
            }
            if(i == framesNumber){
                markerFinalBitmapArea = frameBitmap.getWidth() * frameBitmap.getHeight();
            }

            final BitmapDescriptor markerIconFrame = BitmapDescriptorFactory.fromBitmap(frameBitmap);
            bitmapsForFrame.add(markerIconFrame);
        }

        // check if marker grows or shrinks
        boolean isMarkerShrinking = markerFinalBitmapArea < markerInitialBitmapArea;

        // templateMarkerOptions to be used as template repeatedly on all frames
        MarkerBuilder templateMarkerBuilder = new MarkerBuilder();
        String markerId = Convert.interpretMarkerOptionsWithoutIcon(newMarker, templateMarkerBuilder);
        MarkerOptions templateMarkerOptions = templateMarkerBuilder.build();
        markersQueue.add(markerController.marker);

        if(isMarkerShrinking){
            for (int i=1; i<totalNumberOfSimultaneousMarkers; i++){
                templateMarkerOptions.icon(bitmapsForFrame.get(i - 1));
                templateMarkerOptions.zIndex(markerController.marker.getZIndex() - (float) i/framesNumber);
                Marker newMapMarker = googleMap.addMarker(templateMarkerOptions);
                markersQueue.add(newMapMarker);
            }
        }

        // Animation itself    
        final Handler handler = new Handler(Looper.getMainLooper());
        ValueAnimator transitionAnimator = ValueAnimator.ofFloat(0f, 1f);
        transitionAnimator.setDuration(markersTransitionAnimationDuration);
        lastAnimationIndex = -1;
        transitionAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animationIndex = (int) ((float) animation.getAnimatedValue() * framesNumber);
                if(animationIndex == lastAnimationIndex){
                    return;
                }
                lastAnimationIndex = animationIndex;
                Log.d("CozyMarkerAnimator", "animationIndex: " + animationIndex);

                if(isMarkerShrinking){
                    int nextAnimationIndex = animationIndex + totalNumberOfSimultaneousMarkers - 1;
                    Log.d("CozyMarkerAnimator", "nextAnimationIndex: " + nextAnimationIndex);
                    if(animationIndex > 0 && nextAnimationIndex <= framesNumber){
                        Log.d("CozyMarkerAnimator", "adding marker");
                        templateMarkerOptions.icon(bitmapsForFrame.get(nextAnimationIndex - 1));
                        templateMarkerOptions.zIndex(markerController.marker.getZIndex() - (float) nextAnimationIndex/framesNumber);
                        Marker newMarker = googleMap.addMarker(templateMarkerOptions);
                        markersQueue.add(newMarker);
                    }

                    if(markersQueue.
                        size() > totalNumberOfSimultaneousMarkers || nextAnimationIndex > framesNumber){
                            Log.d("CozyMarkerAnimator", "removing marker");
                        Marker markerToRemove = markersQueue.remove();
                        googleMapsMarkerIdToDartMarkerId.remove(markerToRemove.getId());
                        markerToRemove.remove();

                        googleMapsMarkerIdToDartMarkerId.remove(markerToRemove.getId());
                        googleMapsMarkerIdToDartMarkerId.put(markersQueue.peek().getId(), markerId);
                    }

                    // // After some time, remove all exceeding markers
                    if((float) animation.getAnimatedValue() == 1f){
                        markerController.replace(markersQueue.peek());
                        markerController.currentCozyMarkerData = endMarkerData;

                        handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            while(markersQueue.size() > 1){
                                Marker markerToRemove = markersQueue.remove();
                                markerToRemove.remove();
                            }
                        }});
                    }
                }else{
                    if(animationIndex > 0 && animationIndex <= framesNumber){
                        templateMarkerOptions.icon(bitmapsForFrame.get(animationIndex - 1));
                        Marker newMarker = googleMap.addMarker(templateMarkerOptions);
                        markersQueue.add(newMarker);
                        googleMapsMarkerIdToDartMarkerId.put(newMarker.getId(), markerId);
                    }

                    if(markersQueue.
                        size() > totalNumberOfSimultaneousMarkers){
                        Marker markerToRemove = markersQueue.remove();
                        googleMapsMarkerIdToDartMarkerId.remove(markerToRemove.getId());
                        markerToRemove.remove();
                    }

                    // After some time, remove all exceeding markers
                    if((float) animation.getAnimatedValue() == 1f){
                        handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            while(markersQueue.size() > 1){
                                    Marker markerToRemove = markersQueue.remove();
                                    markerToRemove.remove();
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
        // Interpolator transitionInterpolator = PathInterpolatorCompat.create(0.33f, 1f, 0.68f, 1f);
        // transitionAnimator.setInterpolator(transitionInterpolator);
        transitionAnimator.start();
    }
}