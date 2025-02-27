// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import 'fake_maps_controllers.dart';

Widget _mapWithMarkers(Set<Marker> markers) {
  return Directionality(
    textDirection: TextDirection.ltr,
    child: GoogleMap(
      initialCameraPosition: const CameraPosition(target: LatLng(10.0, 15.0)),
      markers: markers,
    ),
  );
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  final FakePlatformViewsController fakePlatformViewsController =
      FakePlatformViewsController();

  setUpAll(() {
    SystemChannels.platform_views.setMockMethodCallHandler(
        fakePlatformViewsController.fakePlatformViewsMethodHandler);
  });

  setUp(() {
    fakePlatformViewsController.reset();
  });

  testWidgets('Initializing a marker', (WidgetTester tester) async {
    final Marker m1 = Marker(
        markerId: const MarkerId('marker_1'),
        markerType: MarkerType.count,
        label: '1');
    await tester.pumpWidget(_mapWithMarkers(<Marker>{m1}));

    final FakePlatformGoogleMap platformGoogleMap =
        fakePlatformViewsController.lastCreatedView!;
    expect(platformGoogleMap.markersToAdd.length, 1);

    final Marker initializedMarker = platformGoogleMap.markersToAdd.first;
    expect(initializedMarker, equals(m1));
    expect(platformGoogleMap.markerIdsToRemove.isEmpty, true);
    expect(platformGoogleMap.markersToChange.isEmpty, true);
  });

  testWidgets('Adding a marker', (WidgetTester tester) async {
    final Marker m1 = Marker(
        markerId: const MarkerId('marker_1'),
        markerType: MarkerType.count,
        label: '1');
    final Marker m2 = Marker(
        markerId: const MarkerId('marker_2'),
        markerType: MarkerType.count,
        label: '1');

    await tester.pumpWidget(_mapWithMarkers(<Marker>{m1}));
    await tester.pumpWidget(_mapWithMarkers(<Marker>{m1, m2}));

    final FakePlatformGoogleMap platformGoogleMap =
        fakePlatformViewsController.lastCreatedView!;
    expect(platformGoogleMap.markersToAdd.length, 1);

    final Marker addedMarker = platformGoogleMap.markersToAdd.first;
    expect(addedMarker, equals(m2));

    expect(platformGoogleMap.markerIdsToRemove.isEmpty, true);

    expect(platformGoogleMap.markersToChange.isEmpty, true);
  });

  testWidgets('Removing a marker', (WidgetTester tester) async {
    final Marker m1 = Marker(
        markerId: const MarkerId('marker_1'),
        markerType: MarkerType.count,
        label: '1');

    await tester.pumpWidget(_mapWithMarkers(<Marker>{m1}));
    await tester.pumpWidget(_mapWithMarkers(<Marker>{}));

    final FakePlatformGoogleMap platformGoogleMap =
        fakePlatformViewsController.lastCreatedView!;
    expect(platformGoogleMap.markerIdsToRemove.length, 1);
    expect(platformGoogleMap.markerIdsToRemove.first, equals(m1.markerId));

    expect(platformGoogleMap.markersToChange.isEmpty, true);
    expect(platformGoogleMap.markersToAdd.isEmpty, true);
  });

  testWidgets('Updating a marker', (WidgetTester tester) async {
    final Marker m1 = Marker(
        markerId: const MarkerId('marker_1'),
        markerType: MarkerType.count,
        label: '1');
    final Marker m2 = Marker(
        markerId: const MarkerId('marker_1'),
        alpha: 0.5,
        markerType: MarkerType.count,
        label: '1');

    await tester.pumpWidget(_mapWithMarkers(<Marker>{m1}));
    await tester.pumpWidget(_mapWithMarkers(<Marker>{m2}));

    final FakePlatformGoogleMap platformGoogleMap =
        fakePlatformViewsController.lastCreatedView!;
    expect(platformGoogleMap.markersToChange.length, 1);
    expect(platformGoogleMap.markersToChange.first, equals(m2));

    expect(platformGoogleMap.markerIdsToRemove.isEmpty, true);
    expect(platformGoogleMap.markersToAdd.isEmpty, true);
  });

  testWidgets('Updating a marker', (WidgetTester tester) async {
    final Marker m1 = Marker(
        markerId: const MarkerId('marker_1'),
        markerType: MarkerType.count,
        label: '1');
    final Marker m2 = Marker(
        markerId: const MarkerId('marker_1'),
        infoWindow: const InfoWindow(snippet: 'changed'),
        markerType: MarkerType.count,
        label: '1');

    await tester.pumpWidget(_mapWithMarkers(<Marker>{m1}));
    await tester.pumpWidget(_mapWithMarkers(<Marker>{m2}));

    final FakePlatformGoogleMap platformGoogleMap =
        fakePlatformViewsController.lastCreatedView!;
    expect(platformGoogleMap.markersToChange.length, 1);

    final Marker update = platformGoogleMap.markersToChange.first;
    expect(update, equals(m2));
    expect(update.infoWindow.snippet, 'changed');
  });

  testWidgets('Multi Update', (WidgetTester tester) async {
    Marker m1 = Marker(
        markerId: const MarkerId('marker_1'),
        markerType: MarkerType.count,
        label: '1');
    Marker m2 = Marker(
        markerId: const MarkerId('marker_2'),
        markerType: MarkerType.count,
        label: '1');
    final Set<Marker> prev = <Marker>{m1, m2};
    m1 = Marker(
        markerId: const MarkerId('marker_1'),
        visible: false,
        markerType: MarkerType.count,
        label: '1');
    m2 = Marker(
        markerId: const MarkerId('marker_2'),
        draggable: true,
        markerType: MarkerType.count,
        label: '1');
    final Set<Marker> cur = <Marker>{m1, m2};

    await tester.pumpWidget(_mapWithMarkers(prev));
    await tester.pumpWidget(_mapWithMarkers(cur));

    final FakePlatformGoogleMap platformGoogleMap =
        fakePlatformViewsController.lastCreatedView!;

    expect(platformGoogleMap.markersToChange, cur);
    expect(platformGoogleMap.markerIdsToRemove.isEmpty, true);
    expect(platformGoogleMap.markersToAdd.isEmpty, true);
  });

  testWidgets('Multi Update', (WidgetTester tester) async {
    Marker m2 = Marker(
        markerId: const MarkerId('marker_2'),
        markerType: MarkerType.count,
        label: '1');
    final Marker m3 = Marker(
        markerId: const MarkerId('marker_3'),
        markerType: MarkerType.count,
        label: '1');
    final Set<Marker> prev = <Marker>{m2, m3};

    // m1 is added, m2 is updated, m3 is removed.
    final Marker m1 = Marker(
        markerId: const MarkerId('marker_1'),
        markerType: MarkerType.count,
        label: '1');
    m2 = Marker(
        markerId: const MarkerId('marker_2'),
        draggable: true,
        markerType: MarkerType.count,
        label: '1');
    final Set<Marker> cur = <Marker>{m1, m2};

    await tester.pumpWidget(_mapWithMarkers(prev));
    await tester.pumpWidget(_mapWithMarkers(cur));

    final FakePlatformGoogleMap platformGoogleMap =
        fakePlatformViewsController.lastCreatedView!;

    expect(platformGoogleMap.markersToChange.length, 1);
    expect(platformGoogleMap.markersToAdd.length, 1);
    expect(platformGoogleMap.markerIdsToRemove.length, 1);

    expect(platformGoogleMap.markersToChange.first, equals(m2));
    expect(platformGoogleMap.markersToAdd.first, equals(m1));
    expect(platformGoogleMap.markerIdsToRemove.first, equals(m3.markerId));
  });

  testWidgets('Partial Update', (WidgetTester tester) async {
    final Marker m1 = Marker(
        markerId: const MarkerId('marker_1'),
        markerType: MarkerType.count,
        label: '1');
    final Marker m2 = Marker(
        markerId: const MarkerId('marker_2'),
        markerType: MarkerType.count,
        label: '1');
    Marker m3 = Marker(
        markerId: const MarkerId('marker_3'),
        markerType: MarkerType.count,
        label: '1');
    final Set<Marker> prev = <Marker>{m1, m2, m3};
    m3 = Marker(
        markerId: const MarkerId('marker_3'),
        draggable: true,
        markerType: MarkerType.count,
        label: '1');
    final Set<Marker> cur = <Marker>{m1, m2, m3};

    await tester.pumpWidget(_mapWithMarkers(prev));
    await tester.pumpWidget(_mapWithMarkers(cur));

    final FakePlatformGoogleMap platformGoogleMap =
        fakePlatformViewsController.lastCreatedView!;

    expect(platformGoogleMap.markersToChange, <Marker>{m3});
    expect(platformGoogleMap.markerIdsToRemove.isEmpty, true);
    expect(platformGoogleMap.markersToAdd.isEmpty, true);
  });

  testWidgets('Update non platform related attr', (WidgetTester tester) async {
    Marker m1 = Marker(
        markerId: const MarkerId('marker_1'),
        markerType: MarkerType.count,
        label: '1');
    final Set<Marker> prev = <Marker>{m1};
    m1 = Marker(
        markerId: const MarkerId('marker_1'),
        onTap: () => print('hello'),
        markerType: MarkerType.count,
        label: '1',
        onDragEnd: (LatLng latLng) => print(latLng));
    final Set<Marker> cur = <Marker>{m1};

    await tester.pumpWidget(_mapWithMarkers(prev));
    await tester.pumpWidget(_mapWithMarkers(cur));

    final FakePlatformGoogleMap platformGoogleMap =
        fakePlatformViewsController.lastCreatedView!;

    expect(platformGoogleMap.markersToChange.isEmpty, true);
    expect(platformGoogleMap.markerIdsToRemove.isEmpty, true);
    expect(platformGoogleMap.markersToAdd.isEmpty, true);
  });
}
