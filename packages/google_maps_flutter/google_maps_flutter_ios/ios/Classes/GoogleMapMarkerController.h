// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#import <Flutter/Flutter.h>
#import <GoogleMaps/GoogleMaps.h>
#import "GoogleMapController.h"
#import "CozyMarkerBuilder.h"

NS_ASSUME_NONNULL_BEGIN

// Defines marker controllable by Flutter.
@interface FLTGoogleMapMarkerController : NSObject
@property(assign, nonatomic, readonly) BOOL consumeTapEvents;
@property(assign, nonatomic, readonly) BOOL markersAnimationEnabled;
@property(assign, nonatomic, readonly) int markersAnimationDuration;
- (instancetype)initMarkerWithPosition:(CLLocationCoordinate2D)position
                            identifier:(NSString *)identifier
                               mapView:(GMSMapView *)mapView
                     cozyMarkerBuilder:(CozyMarkerBuilder *)cozy
                     markersAnimationEnabled:(BOOL)markersAnimationEnabled;
- (void)showInfoWindow;
- (void)hideInfoWindow;
- (BOOL)isInfoWindowShown;
- (void)removeMarker;
@end

@interface FLTMarkersController : NSObject
- (instancetype)initWithMethodChannel:(FlutterMethodChannel *)methodChannel
                              mapView:(GMSMapView *)mapView
                            registrar:(NSObject<FlutterPluginRegistrar> *)registrar
                            cozyMarkerBuilder:(CozyMarkerBuilder *)cozy
                            markersAnimationEnabled:(BOOL)markersAnimationEnabled;
- (void)addMarkers:(NSArray *)markersToAdd;
- (void)changeMarkers:(NSArray *)markersToChange;
- (void)removeMarkersWithIdentifiers:(NSArray *)identifiers;
- (BOOL)didTapMarkerWithIdentifier:(NSString *)identifier;
- (void)didStartDraggingMarkerWithIdentifier:(NSString *)identifier
                                    location:(CLLocationCoordinate2D)coordinate;
- (void)didEndDraggingMarkerWithIdentifier:(NSString *)identifier
                                  location:(CLLocationCoordinate2D)coordinate;
- (void)didDragMarkerWithIdentifier:(NSString *)identifier
                           location:(CLLocationCoordinate2D)coordinate;
- (void)didTapInfoWindowOfMarkerWithIdentifier:(NSString *)identifier;
- (void)showMarkerInfoWindowWithIdentifier:(NSString *)identifier result:(FlutterResult)result;
- (void)hideMarkerInfoWindowWithIdentifier:(NSString *)identifier result:(FlutterResult)result;
- (void)isInfoWindowShownForMarkerWithIdentifier:(NSString *)identifier
                                          result:(FlutterResult)result;
- (void)setMarkersAnimationEnabled:(BOOL)enabled;
@end

NS_ASSUME_NONNULL_END
