// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'dart:ui' show Offset;

import 'package:flutter/foundation.dart'
    show immutable, ValueChanged, VoidCallback;

import 'types.dart';

Object _offsetToJson(Offset offset) {
  return <Object>[offset.dx, offset.dy];
}

/// The type of marker icon which should be displayed.
enum MarkerType {
  /// A white round circle with text in the middle. If chosen, an label must
  /// be passed in the label argument.
  count,

  /// A white text bubble with text in the middle. The bubble adjusts itself
  /// to the text. If chosen, an label must be passed in the label argument.
  price,

  /// A non-standard icon. If chosen, an icon must be passed in the icon
  /// argument.
  icon
}

/// Text labels for a [Marker] info window.
@immutable
class InfoWindow {
  /// Creates an immutable representation of a label on for [Marker].
  const InfoWindow({
    this.title,
    this.snippet,
    this.anchor = const Offset(0.5, 0.0),
    this.onTap,
  });

  /// Text labels specifying that no text is to be displayed.
  static const InfoWindow noText = InfoWindow();

  /// Text displayed in an info window when the user taps the marker.
  ///
  /// A null value means no title.
  final String? title;

  /// Additional text displayed below the [title].
  ///
  /// A null value means no additional text.
  final String? snippet;

  /// The icon image point that will be the anchor of the info window when
  /// displayed.
  ///
  /// The image point is specified in normalized coordinates: An anchor of
  /// (0.0, 0.0) means the top left corner of the image. An anchor
  /// of (1.0, 1.0) means the bottom right corner of the image.
  final Offset anchor;

  /// onTap callback for this [InfoWindow].
  final VoidCallback? onTap;

  /// Creates a new [InfoWindow] object whose values are the same as this instance,
  /// unless overwritten by the specified parameters.
  InfoWindow copyWith({
    String? titleParam,
    String? snippetParam,
    Offset? anchorParam,
    VoidCallback? onTapParam,
  }) {
    return InfoWindow(
      title: titleParam ?? title,
      snippet: snippetParam ?? snippet,
      anchor: anchorParam ?? anchor,
      onTap: onTapParam ?? onTap,
    );
  }

  Object _toJson() {
    final Map<String, Object> json = <String, Object>{};

    void addIfPresent(String fieldName, Object? value) {
      if (value != null) {
        json[fieldName] = value;
      }
    }

    addIfPresent('title', title);
    addIfPresent('snippet', snippet);
    addIfPresent('anchor', _offsetToJson(anchor));

    return json;
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }
    if (other.runtimeType != runtimeType) {
      return false;
    }
    return other is InfoWindow &&
        title == other.title &&
        snippet == other.snippet &&
        anchor == other.anchor;
  }

  @override
  int get hashCode => Object.hash(title.hashCode, snippet, anchor);

  @override
  String toString() {
    return 'InfoWindow{title: $title, snippet: $snippet, anchor: $anchor}';
  }
}

/// Uniquely identifies a [Marker] among [GoogleMap] markers.
///
/// This does not have to be globally unique, only unique among the list.
@immutable
class MarkerId extends MapsObjectId<Marker> {
  /// Creates an immutable identifier for a [Marker].
  const MarkerId(String value) : super(value);
}

/// Marks a geographical location on the map.
///
/// A marker icon is drawn oriented against the device's screen rather than
/// the map's surface; that is, it will not necessarily change orientation
/// due to map rotations, tilting, or zooming.
@immutable
class Marker implements MapsObject<Marker> {
  /// Creates a set of marker configuration options.
  ///
  /// Default marker options.
  ///
  /// Specifies a marker that
  /// * is fully opaque; [alpha] is 1.0
  /// * uses icon bottom center to indicate map position; [anchor] is (0.5, 1.0)
  /// * has default tap handling; [consumeTapEvents] is false
  /// * is stationary; [draggable] is false
  /// * is drawn against the screen, not the map; [flat] is false
  /// * has a default icon; [icon] is `BitmapDescriptor.defaultMarker`
  /// * anchors the info window at top center; [infoWindowAnchor] is (0.5, 0.0)
  /// * has no info window text; [infoWindowText] is `InfoWindowText.noText`
  /// * is positioned at 0, 0; [position] is `LatLng(0.0, 0.0)`
  /// * has an axis-aligned icon; [rotation] is 0.0
  /// * is visible; [visible] is true
  /// * is placed at the base of the drawing order; [zIndex] is 0.0
  /// * reports [onTap] events
  /// * reports [onDragEnd] events
  const Marker({
    required this.markerId,
    required this.markerType,
    this.icon,
    this.alpha = 1.0,
    this.anchor = const Offset(0.5, 1.0),
    this.consumeTapEvents = false,
    this.draggable = false,
    this.flat = false,
    this.infoWindow = InfoWindow.noText,
    this.position = const LatLng(0.0, 0.0),
    this.rotation = 0.0,
    this.visible = true,
    this.zIndex = 0.0,
    this.onTap,
    this.label,
    this.onDrag,
    this.onDragStart,
    this.onDragEnd,
  }) : assert(0.0 <= alpha &&
            alpha <= 1.0 &&
            ((markerType != MarkerType.icon && label != null) ||
                (markerType == MarkerType.icon && icon != null)));

  /// Uniquely identifies a [Marker].
  final MarkerId markerId;

  /// The text which will be shown in the icon selected from
  /// the MarkerType enum. If a label is set, a markerType value must be
  /// informed.
  final String? label;

  /// The type of icon to be displayed as the marker. If a type is given,
  /// a label must be informed.
  final MarkerType markerType;

  /// A description of the bitmap used to draw the marker icon.
  final BitmapDescriptor? icon;

  @override
  MarkerId get mapsId => markerId;

  /// The opacity of the marker, between 0.0 and 1.0 inclusive.
  ///
  /// 0.0 means fully transparent, 1.0 means fully opaque.
  final double alpha;

  /// The icon image point that will be placed at the [position] of the marker.
  ///
  /// The image point is specified in normalized coordinates: An anchor of
  /// (0.0, 0.0) means the top left corner of the image. An anchor
  /// of (1.0, 1.0) means the bottom right corner of the image.
  final Offset anchor;

  /// True if the marker icon consumes tap events. If not, the map will perform
  /// default tap handling by centering the map on the marker and displaying its
  /// info window.
  final bool consumeTapEvents;

  /// True if the marker is draggable by user touch events.
  final bool draggable;

  /// True if the marker is rendered flatly against the surface of the Earth, so
  /// that it will rotate and tilt along with map camera movements.
  final bool flat;

  /// A Google Maps InfoWindow.
  ///
  /// The window is displayed when the marker is tapped.
  final InfoWindow infoWindow;

  /// Geographical location of the marker.
  final LatLng position;

  /// Rotation of the marker image in degrees clockwise from the [anchor] point.
  final double rotation;

  /// True if the marker is visible.
  final bool visible;

  /// The z-index of the marker, used to determine relative drawing order of
  /// map overlays.
  ///
  /// Overlays are drawn in order of z-index, so that lower values means drawn
  /// earlier, and thus appearing to be closer to the surface of the Earth.
  final double zIndex;

  /// Callbacks to receive tap events for markers placed on this map.
  final VoidCallback? onTap;

  /// Signature reporting the new [LatLng] at the start of a drag event.
  final ValueChanged<LatLng>? onDragStart;

  /// Signature reporting the new [LatLng] at the end of a drag event.
  final ValueChanged<LatLng>? onDragEnd;

  /// Signature reporting the new [LatLng] during the drag event.
  final ValueChanged<LatLng>? onDrag;

  /// Creates a new [Marker] object whose values are the same as this instance,
  /// unless overwritten by the specified parameters.
  Marker copyWith({
    double? alphaParam,
    Offset? anchorParam,
    bool? consumeTapEventsParam,
    bool? draggableParam,
    bool? flatParam,
    BitmapDescriptor? iconParam,
    InfoWindow? infoWindowParam,
    LatLng? positionParam,
    double? rotationParam,
    bool? visibleParam,
    double? zIndexParam,
    VoidCallback? onTapParam,
    String? labelParam,
    ValueChanged<LatLng>? onDragStartParam,
    ValueChanged<LatLng>? onDragParam,
    ValueChanged<LatLng>? onDragEndParam,
  }) {
    return Marker(
      markerType: markerType,
      markerId: markerId,
      alpha: alphaParam ?? alpha,
      anchor: anchorParam ?? anchor,
      consumeTapEvents: consumeTapEventsParam ?? consumeTapEvents,
      draggable: draggableParam ?? draggable,
      flat: flatParam ?? flat,
      infoWindow: infoWindowParam ?? infoWindow,
      position: positionParam ?? position,
      rotation: rotationParam ?? rotation,
      label: labelParam ?? label,
      icon: iconParam ?? icon,
      visible: visibleParam ?? visible,
      zIndex: zIndexParam ?? zIndex,
      onTap: onTapParam ?? onTap,
      onDragStart: onDragStartParam ?? onDragStart,
      onDrag: onDragParam ?? onDrag,
      onDragEnd: onDragEndParam ?? onDragEnd,
    );
  }

  /// Creates a new [Marker] object whose values are the same as this instance.
  @override
  Marker clone() => copyWith();

  /// Converts this object to something serializable in JSON.
  @override
  Object toJson() {
    final Map<String, Object> json = <String, Object>{};

    void addIfPresent(String fieldName, Object? value) {
      if (value != null) {
        json[fieldName] = value;
      }
    }

    addIfPresent('markerId', markerId.value);
    addIfPresent('alpha', alpha);
    addIfPresent('label', label);
    addIfPresent('anchor', _offsetToJson(anchor));
    addIfPresent('consumeTapEvents', consumeTapEvents);
    addIfPresent('draggable', draggable);
    if (icon != null) {
      addIfPresent('icon', icon!.toJson());
    }
    addIfPresent('markerType', markerType.name);
    addIfPresent('flat', flat);
    addIfPresent('infoWindow', infoWindow._toJson());
    addIfPresent('position', position.toJson());
    addIfPresent('rotation', rotation);
    addIfPresent('visible', visible);
    addIfPresent('zIndex', zIndex);
    return json;
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }
    if (other.runtimeType != runtimeType) {
      return false;
    }
    return other is Marker &&
        markerId == other.markerId &&
        alpha == other.alpha &&
        anchor == other.anchor &&
        icon == other.icon &&
        consumeTapEvents == other.consumeTapEvents &&
        draggable == other.draggable &&
        flat == other.flat &&
        infoWindow == other.infoWindow &&
        position == other.position &&
        rotation == other.rotation &&
        visible == other.visible &&
        zIndex == other.zIndex &&
        label == other.label;
  }

  @override
  int get hashCode => markerId.hashCode;

  @override
  String toString() {
    return 'Marker{markerId: $markerId, alpha: $alpha, anchor: $anchor, '
        'consumeTapEvents: $consumeTapEvents, label: $label, draggable: $draggable, flat: $flat, '
        'infoWindow: $infoWindow, position: $position, rotation: $rotation, '
        'visible: $visible, zIndex: $zIndex, onTap: $onTap, onDragStart: $onDragStart, '
        'onDrag: $onDrag, onDragEnd: $onDragEnd, icon: $icon}';
  }
}
