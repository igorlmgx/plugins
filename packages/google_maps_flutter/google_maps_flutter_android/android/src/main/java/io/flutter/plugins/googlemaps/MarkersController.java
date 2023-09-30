// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.googlemaps;

import android.graphics.Bitmap;
import android.animation.ValueAnimator;
import android.animation.ObjectAnimator;
import android.view.animation.Interpolator;
import androidx.core.view.animation.PathInterpolatorCompat;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.model.BitmapDescriptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.*;
import android.os.*;

import io.flutter.plugins.googlemaps.cozy.*;
import io.flutter.plugin.common.MethodChannel;

class MarkersController {

  private final Map<String, MarkerController> markerIdToController;
  private final Map<String, String> googleMapsMarkerIdToDartMarkerId;
  private final MethodChannel methodChannel;
  private GoogleMap googleMap;
  private final CozyMarkerBuilder cozyMarkerBuilder;
  private final CozyMarkerAnimator cozyMarkerAnimator;
  private boolean markersAnimationEnabled;
  private final int markersAnimationDuration = 100;
  private final int markersTransitionAnimationDuration = 400;

  MarkersController(MethodChannel methodChannel, CozyMarkerBuilder cozyMarkerBuilder, CozyMarkerAnimator cozyMarkerAnimator) {
    this.markerIdToController = new HashMap<>();
    this.googleMapsMarkerIdToDartMarkerId = new HashMap<>();
    this.methodChannel = methodChannel;
    this.cozyMarkerBuilder = cozyMarkerBuilder;
    this.cozyMarkerAnimator = cozyMarkerAnimator;

    cozyMarkerAnimator.setGoogleMapsMarkerIdToDartMarkerId(googleMapsMarkerIdToDartMarkerId);
  }

  public void setMarkersAnimationEnabled(boolean markersAnimationEnabled){
    this.markersAnimationEnabled = markersAnimationEnabled;
  }

  public void setGoogleMap(GoogleMap googleMap) {
    this.googleMap = googleMap;
    cozyMarkerAnimator.setGoogleMap(googleMap);
  }

  public void addMarkers(List<Object> markersToAdd) {
    if (markersToAdd != null) {
      for (Object markerToAdd : markersToAdd) {
        addMarker(markerToAdd);
      }
    }
  }

  void changeMarkers(List<Object> markersToChange) {
    if (markersToChange != null) {
      for (Object markerToChange : markersToChange) {
        changeMarker(markerToChange);
      }
    }
  }

  void removeMarkers(List<Object> markerIdsToRemove) {
    if (markerIdsToRemove == null) {
      return;
    }
    for (Object rawMarkerId : markerIdsToRemove) {
      if (rawMarkerId == null) {
        continue;
      }
      String markerId = (String) rawMarkerId;
      final MarkerController markerController = markerIdToController.remove(markerId);
      if (markerController != null) {
        if (this.markersAnimationEnabled) {
          ValueAnimator fadeOut = ValueAnimator.ofFloat(1f, 0f);
          fadeOut.setDuration(this.markersAnimationDuration);
          fadeOut.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
              @Override
              public void onAnimationUpdate(ValueAnimator animation) {
                  markerController.setAlpha((float) animation.getAnimatedValue());
                  if((float) animation.getAnimatedValue() == 0f){
                    markerController.remove();
                  }
              }
          });
          Interpolator fadeOutInterpolator = PathInterpolatorCompat.create(0.11f, 0f, 0.5f, 0f);
          fadeOut.setInterpolator(fadeOutInterpolator);

          fadeOut.start();
        }else{
          markerController.remove();
        }
        googleMapsMarkerIdToDartMarkerId.remove(markerController.getGoogleMapsMarkerId());
      }
    }
  }

  void showMarkerInfoWindow(String markerId, MethodChannel.Result result) {
    MarkerController markerController = markerIdToController.get(markerId);
    if (markerController != null) {
      markerController.showInfoWindow();
      result.success(null);
    } else {
      result.error("Invalid markerId", "showInfoWindow called with invalid markerId", null);
    }
  }

  void hideMarkerInfoWindow(String markerId, MethodChannel.Result result) {
    MarkerController markerController = markerIdToController.get(markerId);
    if (markerController != null) {
      markerController.hideInfoWindow();
      result.success(null);
    } else {
      result.error("Invalid markerId", "hideInfoWindow called with invalid markerId", null);
    }
  }

  void isInfoWindowShown(String markerId, MethodChannel.Result result) {
    MarkerController markerController = markerIdToController.get(markerId);
    if (markerController != null) {
      result.success(markerController.isInfoWindowShown());
    } else {
      result.error("Invalid markerId", "isInfoWindowShown called with invalid markerId", null);
    }
  }

  boolean onMarkerTap(String googleMarkerId) {
    String markerId = googleMapsMarkerIdToDartMarkerId.get(googleMarkerId);
    if (markerId == null) {
      return false;
    }
    methodChannel.invokeMethod("marker#onTap", Convert.markerIdToJson(markerId));
    MarkerController markerController = markerIdToController.get(markerId);
    if (markerController != null) {
      return markerController.consumeTapEvents();
    }
    return false;
  }

  void onMarkerDragStart(String googleMarkerId, LatLng latLng) {
    String markerId = googleMapsMarkerIdToDartMarkerId.get(googleMarkerId);
    if (markerId == null) {
      return;
    }
    final Map<String, Object> data = new HashMap<>();
    data.put("markerId", markerId);
    data.put("position", Convert.latLngToJson(latLng));
    methodChannel.invokeMethod("marker#onDragStart", data);
  }

  void onMarkerDrag(String googleMarkerId, LatLng latLng) {
    String markerId = googleMapsMarkerIdToDartMarkerId.get(googleMarkerId);
    if (markerId == null) {
      return;
    }
    final Map<String, Object> data = new HashMap<>();
    data.put("markerId", markerId);
    data.put("position", Convert.latLngToJson(latLng));
    methodChannel.invokeMethod("marker#onDrag", data);
  }

  void onMarkerDragEnd(String googleMarkerId, LatLng latLng) {
    String markerId = googleMapsMarkerIdToDartMarkerId.get(googleMarkerId);
    if (markerId == null) {
      return;
    }
    final Map<String, Object> data = new HashMap<>();
    data.put("markerId", markerId);
    data.put("position", Convert.latLngToJson(latLng));
    methodChannel.invokeMethod("marker#onDragEnd", data);
  }

  void onInfoWindowTap(String googleMarkerId) {
    String markerId = googleMapsMarkerIdToDartMarkerId.get(googleMarkerId);
    if (markerId == null) {
      return;
    }
    methodChannel.invokeMethod("infoWindow#onTap", Convert.markerIdToJson(markerId));
  }


  private void addMarker(Object marker) {
    if (marker == null) {
      return;
    }
    MarkerBuilder markerBuilder = new MarkerBuilder();
    String markerId = Convert.interpretMarkerOptions(marker, markerBuilder, cozyMarkerBuilder);
    CozyMarkerData cozyMarkerData = Convert.toCozyMarkerData(marker);
    MarkerOptions options = markerBuilder.build();
    addMarker(markerId, options, markerBuilder.consumeTapEvents(), cozyMarkerData);
  }

  private void addMarker(String markerId, MarkerOptions markerOptions, boolean consumeTapEvents, CozyMarkerData cozyMarkerData) {
    final Marker marker = googleMap
            .addMarker(markerOptions);
    if (this.markersAnimationEnabled) {
      ValueAnimator fadeIn = ValueAnimator.ofFloat(0f, 1f);
      fadeIn.setDuration(this.markersAnimationDuration);
      fadeIn.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator animation) {
              marker.setAlpha((float) animation.getAnimatedValue());
          }
      });

      Interpolator fadeInInterpolator = PathInterpolatorCompat.create(0.5f, 1f, 0.89f, 1f);
      fadeIn.setInterpolator(fadeInInterpolator);
      fadeIn.start();
    }
    MarkerController controller = new MarkerController(marker, consumeTapEvents, cozyMarkerData);
    markerIdToController.put(markerId, controller);
    googleMapsMarkerIdToDartMarkerId.put(marker.getId(), markerId);
  }

  private void changeMarker(Object newMarker) {
    if (newMarker == null) {
      return;
    }
    String markerId = getMarkerId(newMarker);
    MarkerController markerController = markerIdToController.get(markerId);
    if (markerController != null) {
      final CozyMarkerData startCozyMarkerData = markerController.currentCozyMarkerData;
      final CozyMarkerData endCozyMarkerData = Convert.toCozyMarkerData(newMarker);
      final boolean isTheSameMarker = Objects.equals(startCozyMarkerData, endCozyMarkerData);

      if(startCozyMarkerData != null && 
         endCozyMarkerData != null &&
         endCozyMarkerData.isAnimated && 
         !isTheSameMarker){
        cozyMarkerAnimator.animateMarkerTransition(markerController, newMarker, startCozyMarkerData, endCozyMarkerData);
      }else{
        Convert.interpretMarkerOptions(newMarker, markerController, cozyMarkerBuilder);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static String getMarkerId(Object marker) {
    Map<String, Object> markerMap = (Map<String, Object>) marker;
    return (String) markerMap.get("markerId");
  }
}
