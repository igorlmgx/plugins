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
import java.lang.Math;

import java.util.*;
import android.os.*;
import java.util.Map;

import io.flutter.plugins.googlemaps.MarkerBuilder;
import io.flutter.plugins.googlemaps.Convert;
import io.flutter.plugins.googlemaps.MarkerController;

public class CozyMarkerAnimator {
    // Needed simultaneous markers because when switching frames it could have a gap
    // between the previous and the next frame, causing glitch
    private final int totalNumberOfSimultaneousMarkers = 3;
    private final int markersTransitionAnimationDuration = 400;
    private final float deltaZIndex = 0.9f;
    private final HashMap<String, ValueAnimator> markerAnimationMap = new HashMap<>();

    private GoogleMap googleMap;
    private Map<String, String> googleMapsMarkerIdToDartMarkerId;

    private final int framesNumber;
    private final float deviceFPS;
    private final CozyMarkerBuilder cozyMarkerBuilder;

    public CozyMarkerAnimator(Context context, CozyMarkerBuilder cozyMarkerBuilder) {
        this.cozyMarkerBuilder = cozyMarkerBuilder;
        deviceFPS = getDeviceFPS(context);
        framesNumber = (int) (deviceFPS * ((float) markersTransitionAnimationDuration / 1000)) - 1;
    }

    public void setGoogleMap(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }

    public void setGoogleMapsMarkerIdToDartMarkerId(Map<String, String> googleMapsMarkerIdToDartMarkerId) {
        this.googleMapsMarkerIdToDartMarkerId = googleMapsMarkerIdToDartMarkerId;
    }

    private float getDeviceFPS(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return legacyGetDeviceFPS(context);
        }
        return context.getDisplay().getRefreshRate();
    }

    @SuppressWarnings("deprecation")
    private float legacyGetDeviceFPS(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        return display.getRefreshRate();
    }

    private class AnimatorManager {
        final Deque<Marker> markersQueue;
        final MarkerController markerController;
        final MarkerOptions templateMarkerOptions;
        final float initialZIndex;
        final float finalZIndex;

        final String markerId;

        protected int lastAnimationIndex = -1;

        AnimatorManager(Deque<Marker> markersQueue, MarkerController markerController,
                MarkerOptions templateMarkerOptions, String markerId, float initialZIndex, float finalZIndex) {
            this.markersQueue = markersQueue;
            this.markerController = markerController;
            this.templateMarkerOptions = templateMarkerOptions;
            this.initialZIndex = initialZIndex;
            this.finalZIndex = finalZIndex;
            this.markerId = markerId;
        }

        private Marker addFrame(BitmapDescriptor frameBitmapDescriptor, float zIndex) {
            templateMarkerOptions.zIndex(zIndex);
            templateMarkerOptions.icon(frameBitmapDescriptor);
            Marker newMarker = googleMap.addMarker(templateMarkerOptions);
            markersQueue.add(newMarker);
            return newMarker;
        }

        protected void removeLatestFrame() {
            Marker markerToRemove = markersQueue.remove();
            markerToRemove.remove();
        }

        protected void addFrameBelow(BitmapDescriptor frameBitmapDescriptor, int index) {
            final float zIndex = this.initialZIndex - deltaZIndex * (float) index / framesNumber;
            addFrame(frameBitmapDescriptor, zIndex);
        }

        protected void addFrameAbove(BitmapDescriptor frameBitmapDescriptor, int index) {
            final float zIndex = this.initialZIndex + deltaZIndex * (float) index / framesNumber;

            final Marker topMarker = markersQueue.peek();

            if (topMarker != null) {
                googleMapsMarkerIdToDartMarkerId.remove(topMarker.getId());
            }

            final Marker marker = addFrame(frameBitmapDescriptor, zIndex);
            googleMapsMarkerIdToDartMarkerId.put(marker.getId(), markerId);
        }

        protected void removeLatestFrameBelow() {
            removeLatestFrame();
        }

        protected void removeLatestFrameAbove() {
            googleMapsMarkerIdToDartMarkerId.remove(markersQueue.peek().getId());
            removeLatestFrame();
            googleMapsMarkerIdToDartMarkerId.put(markersQueue.peek().getId(), markerId);
        }

        protected void addLastFrameAbove(BitmapDescriptor frameBitmapDescriptor) {
            googleMapsMarkerIdToDartMarkerId.remove(markersQueue.peek().getId());
            final Marker marker = addFrame(frameBitmapDescriptor, finalZIndex);
            googleMapsMarkerIdToDartMarkerId.put(marker.getId(), markerId);
        }
    }

    public void animateMarkerTransition(MarkerController markerController, Object newMarker,
            CozyMarkerData endMarkerData) {
        final List<BitmapDescriptor> bitmapsForFrame = new ArrayList<BitmapDescriptor>();
        final Deque<Marker> markersQueue = new ArrayDeque<Marker>();

        // templateMarkerOptions to be used as template repeatedly on all frames
        MarkerBuilder templateMarkerBuilder = new MarkerBuilder();
        String markerId = Convert.interpretMarkerOptionsWithoutIcon(newMarker, templateMarkerBuilder);
        MarkerOptions templateMarkerOptions = templateMarkerBuilder.build();
        markersQueue.add(markerController.marker);

        endAnimationIfExists(markerId);

        final CozyMarkerData startMarkerData = markerController.currentCozyMarkerData;

        // getting all frames
        float markerInitialBitmapArea = 0;
        float markerFinalBitmapArea = 0;

        for (int i = 1; i <= framesNumber; i++) {
            float step = (float) i / framesNumber;
            // Ease-out interpolation: https://easings.net/#easeOutCubic
            float interpolatedStep = 1.0f - (float) Math.pow(1.0f - step, 3.0f);

            final Bitmap frameBitmap = cozyMarkerBuilder.buildAnimatedMarker(startMarkerData, endMarkerData,
                    interpolatedStep);
            if (i == 1) {
                markerInitialBitmapArea = frameBitmap.getWidth() * frameBitmap.getHeight();
            }
            if (i == framesNumber) {
                markerFinalBitmapArea = frameBitmap.getWidth() * frameBitmap.getHeight();
            }

            final BitmapDescriptor markerIconFrame = BitmapDescriptorFactory.fromBitmap(frameBitmap);
            bitmapsForFrame.add(markerIconFrame);
        }

        // check if marker grows or shrinks
        boolean isMarkerShrinking = markerFinalBitmapArea < markerInitialBitmapArea;

        float initialMarkerZIndex = markerController.marker.getZIndex();
        float finalMarkerZIndex = templateMarkerOptions.getZIndex();

        final AnimatorManager animatorManager = new AnimatorManager(
                markersQueue,
                markerController,
                templateMarkerOptions,
                markerId,
                isMarkerShrinking ? Math.min(initialMarkerZIndex, finalMarkerZIndex)
                        : Math.max(initialMarkerZIndex, finalMarkerZIndex),
                finalMarkerZIndex);

        // if isMarkerShrinking it adds the markers in reverse order, so it begins with
        // markers stacked already
        if (isMarkerShrinking) {
            for (int i = 1; i < totalNumberOfSimultaneousMarkers; i++) {
                animatorManager.addFrameBelow(bitmapsForFrame.get(i - 1), i);
            }
        }

        // Animation itself
        final Handler handler = new Handler(Looper.getMainLooper());
        ValueAnimator transitionAnimator = ValueAnimator.ofFloat(0f, 1f);
        transitionAnimator.setDuration(markersTransitionAnimationDuration);
        final String markerLabel = endMarkerData.label;

        transitionAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float animatedValue = (float) animation.getAnimatedValue();
                int animationIndex = (int) (animatedValue * framesNumber);

                if (animationIndex == animatorManager.lastAnimationIndex) {
                    return;
                }

                if (animatedValue == 0f) {
                    markerController.currentCozyMarkerData = endMarkerData;
                }

                animatorManager.lastAnimationIndex = animationIndex;

                if (isMarkerShrinking) {
                    int nextAnimationIndex = animationIndex + totalNumberOfSimultaneousMarkers - 1;
                    if (animationIndex > 0 && nextAnimationIndex <= framesNumber) {
                        animatorManager.addFrameBelow(bitmapsForFrame.get(nextAnimationIndex - 1), nextAnimationIndex);
                    }

                    if (markersQueue.size() > totalNumberOfSimultaneousMarkers ||
                            nextAnimationIndex > framesNumber &&
                                    markersQueue.size() > 1) {
                        animatorManager.removeLatestFrameAbove();
                    }

                    // After some time, remove all exceeding markers
                    if (animatedValue == 1f) {
                        animatorManager.addLastFrameAbove(bitmapsForFrame.get(bitmapsForFrame.size() - 1));
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                animatorManager.removeLatestFrameAbove();

                                markerController.replace(markersQueue.remove());

                                endAnimationIfExists(markerId);
                            }
                        }, 100);
                    }
                } else {
                    if (animationIndex > 0 && animationIndex <= framesNumber) {
                        animatorManager.addFrameAbove(bitmapsForFrame.get(animationIndex - 1), animationIndex);
                    }

                    if (markersQueue.size() > totalNumberOfSimultaneousMarkers) {
                        animatorManager.removeLatestFrameBelow();
                    }

                    // After some time, remove all exceeding markers
                    if (animatedValue == 1f) {
                        animatorManager.addLastFrameAbove(bitmapsForFrame.get(bitmapsForFrame.size() - 1));
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                while (markersQueue.size() > 1) {
                                    animatorManager.removeLatestFrameBelow();
                                }

                                markerController.replace(markersQueue.remove());

                                endAnimationIfExists(markerId);
                            }
                        }, 100);
                    }
                }
            }
        });

        startAnimation(markerId, endMarkerData.label, transitionAnimator);
    }

    private void startAnimation(String markerId, String markerLabel, ValueAnimator animation) {
        markerAnimationMap.put(markerId, animation);

        final Handler handler = new Handler(Looper.getMainLooper());

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                animation.start();
            }
        }, 150);
    }

    private void endAnimationIfExists(String markerId) {
        if (markerAnimationMap.containsKey(markerId)) {
            final ValueAnimator animation = markerAnimationMap.get(markerId);
            animation.end();

            markerAnimationMap.remove(markerId);
        }
    }

}