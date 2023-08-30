import 'dart:ui' show Offset;
import 'package:flutter/foundation.dart'
    show immutable, ValueChanged, VoidCallback;
import '../../../../google_maps_flutter_platform_interface.dart';

/// CozyMarker is a subtype of marker that can be used on Google Maps.
/// It follows and is compliant with the properties described on cozy MapPin component.
@immutable
class CozyMarker extends Marker {
  /// Create CozyMarker with
  /// required markerId
  /// required cozyMarkerData
  /// all other data follows the same requirements as [Marker] object
  const CozyMarker({
    required MarkerId markerId,
    required this.cozyMarkerData,
    double alpha = 1.0,
    InfoWindow infoWindow = InfoWindow.noText,
    LatLng position = const LatLng(0.0, 0.0),
    Offset anchor = const Offset(0.5, 1.0),
    bool visible = true,
    double zIndex = 0.0,
    VoidCallback? onTap,
  }) : super(
          markerId: markerId,
          alpha: alpha,
          anchor: anchor,
          position: position,
          visible: visible,
          infoWindow: infoWindow,
          zIndex: zIndex,
          onTap: onTap,
          consumeTapEvents: true,
        );

  /// Payload describing all properties needed to render the marker according to cozy MapPin component.
  final CozyMarkerData cozyMarkerData;

  @override
  CozyMarker copyWith({
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
    ValueChanged<LatLng>? onDragStartParam,
    ValueChanged<LatLng>? onDragParam,
    ValueChanged<LatLng>? onDragEndParam,
    CozyMarkerData? cozyMarkerDataParam,
  }) {
    return CozyMarker(
      markerId: markerId,
      alpha: alphaParam ?? alpha,
      anchor: anchorParam ?? anchor,
      infoWindow: infoWindowParam ?? infoWindow,
      position: positionParam ?? position,
      visible: visibleParam ?? visible,
      zIndex: zIndexParam ?? zIndex,
      onTap: onTapParam ?? onTap,
      cozyMarkerData: cozyMarkerDataParam ?? cozyMarkerData,
    );
  }

  /// Creates a new [Marker] object whose values are the same as this instance.
  @override
  CozyMarker clone() => copyWith();

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
    addIfPresent('anchor', offsetToJson(anchor));
    addIfPresent('consumeTapEvents', consumeTapEvents);
    addIfPresent('draggable', draggable);
    if (icon != null) {
      addIfPresent('icon', icon!.toJson());
    }
    addIfPresent('flat', flat);
    addIfPresent('infoWindow', infoWindow.toJson());
    addIfPresent('position', position.toJson());
    addIfPresent('rotation', rotation);
    addIfPresent('visible', visible);
    addIfPresent('zIndex', zIndex);

    //Specific for cozy
    addIfPresent('cozyMarkerData', cozyMarkerData.toJson());

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
    return other is CozyMarker &&
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
        cozyMarkerData == other.cozyMarkerData;
  }

  @override
  int get hashCode => markerId.hashCode;

  @override
  String toString() {
    return 'Marker{markerId: $markerId, cozyMarkerData: $cozyMarkerData alpha: $alpha, anchor: $anchor, '
        'consumeTapEvents: $consumeTapEvents, draggable: $draggable, flat: $flat, '
        'infoWindow: $infoWindow, position: $position, rotation: $rotation, '
        'visible: $visible, zIndex: $zIndex, onTap: $onTap, onDragStart: $onDragStart, '
        'onDrag: $onDrag, onDragEnd: $onDragEnd, icon: $icon }';
  }
}
