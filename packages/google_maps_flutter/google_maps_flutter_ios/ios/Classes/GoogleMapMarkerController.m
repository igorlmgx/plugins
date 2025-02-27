// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#import "GoogleMapMarkerController.h"
#import "FLTGoogleMapJSONConversions.h"
#import "UIKit/UIGraphics.h"
#import <CoreGraphics/CoreGraphics.h>
#import <CoreText/CoreText.h>

@interface FLTGoogleMapMarkerController ()

@property(strong, nonatomic) GMSMarker *marker;
@property(weak, nonatomic) GMSMapView *mapView;
@property(assign, nonatomic, readwrite) BOOL consumeTapEvents;
@property(strong, nonatomic) UIImage *iconImage;
@property(strong, nonatomic) NSString *fontPath;

@end

@implementation FLTGoogleMapMarkerController

- (instancetype)initMarkerWithPosition:(CLLocationCoordinate2D)position
                            identifier:(NSString *)identifier
                               mapView:(GMSMapView *)mapView
                             iconImage:(UIImage *)iconImage
                              fontPath:(nonnull NSString *)fontPath {
    self = [super init];
    if (self) {
        _marker = [GMSMarker markerWithPosition:position];
        _mapView = mapView;
        _marker.userData = @[ identifier ];
        _iconImage = iconImage;
        _fontPath = fontPath;
    }
    return self;
}

- (void)showInfoWindow {
    self.mapView.selectedMarker = self.marker;
}

- (void)hideInfoWindow {
    if (self.mapView.selectedMarker == self.marker) {
        self.mapView.selectedMarker = nil;
    }
}

- (BOOL)isInfoWindowShown {
    return self.mapView.selectedMarker == self.marker;
}

- (void)removeMarker {
    self.marker.map = nil;
}

- (void)setAlpha:(float)alpha {
    self.marker.opacity = alpha;
}

- (void)setAnchor:(CGPoint)anchor {
    self.marker.groundAnchor = anchor;
}

- (void)setDraggable:(BOOL)draggable {
    self.marker.draggable = draggable;
}

- (void)setFlat:(BOOL)flat {
    self.marker.flat = flat;
}

- (void)setIcon:(UIImage *)icon {
    self.marker.icon = icon;
}

- (void)setInfoWindowAnchor:(CGPoint)anchor {
    self.marker.infoWindowAnchor = anchor;
}

- (void)setInfoWindowTitle:(NSString *)title snippet:(NSString *)snippet {
    self.marker.title = title;
    self.marker.snippet = snippet;
}

- (void)setPosition:(CLLocationCoordinate2D)position {
    self.marker.position = position;
}

- (void)setRotation:(CLLocationDegrees)rotation {
    self.marker.rotation = rotation;
}

- (void)setVisible:(BOOL)visible {
    self.marker.map = visible ? self.mapView : nil;
}

- (void)setZIndex:(int)zIndex {
    self.marker.zIndex = zIndex;
}

- (UIImage *)clusterMarkerImageWithText:(NSString *)string {
    CGSize size = [_iconImage size];
    UIFont *textFont = [UIFont fontWithName:self.fontPath size:size.width / 2.5];
    CGSize stringSize = [string sizeWithAttributes:@{NSFontAttributeName:textFont}];
    CGFloat textY = (size.height / 2) - (stringSize.height / 2);
    CGFloat textX = (size.width / 2) - (stringSize.width / 2);
    UIGraphicsBeginImageContext(size);
    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize:size];
    UIImage *image = [renderer imageWithActions:^(UIGraphicsImageRendererContext * _Nonnull rendererContext) {
        CGRect rect = CGRectMake(0, 0, size.width, size.height);
        [self.iconImage drawInRect:CGRectIntegral(rect)];
        CGRect textRect = CGRectMake(textX, textY, stringSize.width, stringSize.height);
        [string drawInRect:CGRectIntegral(textRect) withAttributes:@{NSFontAttributeName:textFont}];
    }];
    UIGraphicsEndImageContext();
    return image;
}

- (UIImage *)priceMarkerImageWithText:(NSString *)text {
    UIFont *textFont =  [UIFont fontWithName:self.fontPath size:[_iconImage size].width / 3.5];
    CGSize stringSize = [text sizeWithAttributes:@{NSFontAttributeName:textFont}];
    CGFloat markerWidth = stringSize.width * 1.25;
    CGFloat markerHeight = stringSize.height * 1.50;
    CGSize canvas = CGSizeMake(markerWidth + 2, markerHeight + 10);
    CGFloat y = ((canvas.height - 10) / 2) - (stringSize.height / 2);
    CGFloat x = (canvas.width / 2) - (stringSize.width / 2);
    CGRect textRect = CGRectMake(x, y, stringSize.width, stringSize.height);
    UIGraphicsBeginImageContext(canvas);
    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize: canvas];
    UIImage *image = [renderer imageWithActions:^(UIGraphicsImageRendererContext * _Nonnull rendererContext) {
        
        CGMutablePathRef path = CGPathCreateMutable();
        CGPathMoveToPoint(path, nil, canvas.width / 2 - 10, markerHeight);
        CGPathAddLineToPoint(path, nil, canvas.width / 2, markerHeight + 10);
        CGPathAddLineToPoint(path, nil, canvas.width / 2 + 10, markerHeight);
        CGPathAddLineToPoint(path, nil, canvas.width / 2 - 10, markerHeight);
        
        CGContextSetAlpha(rendererContext.CGContext, 0.02);
        CGContextSetFillColorWithColor(rendererContext.CGContext, UIColor.grayColor.CGColor);
        UIBezierPath *shadow = [UIBezierPath bezierPathWithRoundedRect:CGRectMake(0, 0, markerWidth + 2, markerHeight + 2) cornerRadius:5];
        [shadow fill];
        [shadow stroke];
        CGContextSetAlpha(rendererContext.CGContext, 1.0);
        CGContextSetFillColorWithColor(rendererContext.CGContext, UIColor.whiteColor.CGColor);
        CGContextSetStrokeColorWithColor(rendererContext.CGContext, UIColor.clearColor.CGColor);
        CGContextSetLineWidth(rendererContext.CGContext, 5);
        CGContextSetLineJoin(rendererContext.CGContext, 0);
        CGContextAddPath(rendererContext.CGContext, path);
        CGContextDrawPath(rendererContext.CGContext, 3);
        UIBezierPath *bezier = [UIBezierPath bezierPathWithRoundedRect:CGRectMake(1, 1, markerWidth, markerHeight) cornerRadius:5];
        [bezier fill];
        [bezier stroke];
        [text drawInRect:CGRectIntegral(textRect) withAttributes:@{NSFontAttributeName:textFont}];
        CGPathRelease(path);
    }];
    UIGraphicsEndImageContext();
    return image;
}


- (void)interpretMarkerOptions:(NSDictionary *)data
                     registrar:(NSObject<FlutterPluginRegistrar> *)registrar {
    NSNumber *alpha = data[@"alpha"];
    if (alpha && alpha != (id)[NSNull null]) {
        [self setAlpha:[alpha floatValue]];
    }
    NSArray *anchor = data[@"anchor"];
    if (anchor && anchor != (id)[NSNull null]) {
        [self setAnchor:[FLTGoogleMapJSONConversions pointFromArray:anchor]];
    }
    NSNumber *draggable = data[@"draggable"];
    if (draggable && draggable != (id)[NSNull null]) {
        [self setDraggable:[draggable boolValue]];
    }
    NSString *markerType = data[@"markerType"];
    if(markerType && markerType != (id)[NSNull null]) {
        if([markerType isEqualToString:@"icon"]) {
            NSArray *icon = data[@"icon"];
            if (icon && icon != (id)[NSNull null]) {
                UIImage *image = [self extractIconFromData:icon registrar:registrar];
                [self setIcon:image];
            }
        } else if([markerType isEqualToString:@"count"]) {
            NSString *label = data[@"label"];
            if(label && label != (id)[NSNull null]) {
                UIImage *img = [self clusterMarkerImageWithText:label];
                [self setIcon:img];
            } else {
                NSString *error =
                [NSString stringWithFormat:@"label was not provided."];
                NSException *exception = [NSException exceptionWithName:@"InvalidBitmapDescriptor"
                                                                 reason:error
                                                               userInfo:nil];
                @throw exception;
            }
        }
        else if([markerType isEqualToString:@"price"]) {
            NSString *label = data[@"label"];
            if(label && label != (id)[NSNull null]) {
                UIImage *img = [self priceMarkerImageWithText:label];
                [self setIcon:img];
            } else {
                NSString *error =
                [NSString stringWithFormat:@"label was not provided."];
                NSException *exception = [NSException exceptionWithName:@"InvalidBitmapDescriptor"
                                                                 reason:error
                                                               userInfo:nil];
                @throw exception;
            }            }
        else {
            NSString *error =
            [NSString stringWithFormat:@"MarkerType was not provided."];
            NSException *exception = [NSException exceptionWithName:@"InvalidBitmapDescriptor"
                                                             reason:error
                                                           userInfo:nil];
            @throw exception;
        }
        
    }
    NSNumber *flat = data[@"flat"];
    if (flat && flat != (id)[NSNull null]) {
        [self setFlat:[flat boolValue]];
    }
    NSNumber *consumeTapEvents = data[@"consumeTapEvents"];
    if (consumeTapEvents && consumeTapEvents != (id)[NSNull null]) {
        [self setConsumeTapEvents:[consumeTapEvents boolValue]];
    }
    [self interpretInfoWindow:data];
    NSArray *position = data[@"position"];
    if (position && position != (id)[NSNull null]) {
        [self setPosition:[FLTGoogleMapJSONConversions locationFromLatLong:position]];
    }
    NSNumber *rotation = data[@"rotation"];
    if (rotation && rotation != (id)[NSNull null]) {
        [self setRotation:[rotation doubleValue]];
    }
    NSNumber *visible = data[@"visible"];
    if (visible && visible != (id)[NSNull null]) {
        [self setVisible:[visible boolValue]];
    }
    NSNumber *zIndex = data[@"zIndex"];
    if (zIndex && zIndex != (id)[NSNull null]) {
        [self setZIndex:[zIndex intValue]];
    }
}

- (void)interpretInfoWindow:(NSDictionary *)data {
    NSDictionary *infoWindow = data[@"infoWindow"];
    if (infoWindow && infoWindow != (id)[NSNull null]) {
        NSString *title = infoWindow[@"title"];
        NSString *snippet = infoWindow[@"snippet"];
        if (title && title != (id)[NSNull null]) {
            [self setInfoWindowTitle:title snippet:snippet];
        }
        NSArray *infoWindowAnchor = infoWindow[@"infoWindowAnchor"];
        if (infoWindowAnchor && infoWindowAnchor != (id)[NSNull null]) {
            [self setInfoWindowAnchor:[FLTGoogleMapJSONConversions pointFromArray:infoWindowAnchor]];
        }
    }
}

- (UIImage *)extractIconFromData:(NSArray *)iconData
                       registrar:(NSObject<FlutterPluginRegistrar> *)registrar {
    UIImage *image;
    if ([iconData.firstObject isEqualToString:@"defaultMarker"]) {
        CGFloat hue = (iconData.count == 1) ? 0.0f : [iconData[1] doubleValue];
        image = [GMSMarker markerImageWithColor:[UIColor colorWithHue:hue / 360.0
                                                           saturation:1.0
                                                           brightness:0.7
                                                                alpha:1.0]];
    } else if ([iconData.firstObject isEqualToString:@"fromAsset"]) {
        if (iconData.count == 2) {
            image = [UIImage imageNamed:[registrar lookupKeyForAsset:iconData[1]]];
        } else {
            image = [UIImage imageNamed:[registrar lookupKeyForAsset:iconData[1]
                                                         fromPackage:iconData[2]]];
        }
    } else if ([iconData.firstObject isEqualToString:@"fromAssetImage"]) {
        if (iconData.count == 3) {
            image = [UIImage imageNamed:[registrar lookupKeyForAsset:iconData[1]]];
            id scaleParam = iconData[2];
            image = [self scaleImage:image by:scaleParam];
        } else {
            NSString *error =
            [NSString stringWithFormat:@"'fromAssetImage' should have exactly 3 arguments. Got: %lu",
             (unsigned long)iconData.count];
            NSException *exception = [NSException exceptionWithName:@"InvalidBitmapDescriptor"
                                                             reason:error
                                                           userInfo:nil];
            @throw exception;
        }
    } else if ([iconData[0] isEqualToString:@"fromBytes"]) {
        if (iconData.count == 2) {
            @try {
                FlutterStandardTypedData *byteData = iconData[1];
                CGFloat screenScale = [[UIScreen mainScreen] scale];
                image = [UIImage imageWithData:[byteData data] scale:screenScale];
            } @catch (NSException *exception) {
                @throw [NSException exceptionWithName:@"InvalidByteDescriptor"
                                               reason:@"Unable to interpret bytes as a valid image."
                                             userInfo:nil];
            }
        } else {
            NSString *error = [NSString
                               stringWithFormat:@"fromBytes should have exactly one argument, the bytes. Got: %lu",
                               (unsigned long)iconData.count];
            NSException *exception = [NSException exceptionWithName:@"InvalidByteDescriptor"
                                                             reason:error
                                                           userInfo:nil];
            @throw exception;
        }
    }
    
    return image;
}

- (UIImage *)scaleImage:(UIImage *)image by:(id)scaleParam {
    double scale = 1.0;
    if ([scaleParam isKindOfClass:[NSNumber class]]) {
        scale = [scaleParam doubleValue];
    }
    if (fabs(scale - 1) > 1e-3) {
        return [UIImage imageWithCGImage:[image CGImage]
                                   scale:(image.scale * scale)
                             orientation:(image.imageOrientation)];
    }
    return image;
}

@end

@interface FLTMarkersController ()

@property(strong, nonatomic) NSMutableDictionary *markerIdentifierToController;
@property(strong, nonatomic) FlutterMethodChannel *methodChannel;
@property(strong, nonatomic) UIImage *emptyClusterMarker;
@property(strong, nonatomic) NSString *fontPath;
@property(weak, nonatomic) NSObject<FlutterPluginRegistrar> *registrar;
@property(weak, nonatomic) GMSMapView *mapView;


@end

@implementation FLTMarkersController

- (instancetype)initWithMethodChannel:(FlutterMethodChannel *)methodChannel
                              mapView:(GMSMapView *)mapView
                            registrar:(NSObject<FlutterPluginRegistrar> *)registrar {
    self = [super init];
    if (self) {
        _methodChannel = methodChannel;
        _mapView = mapView;
        _markerIdentifierToController = [[NSMutableDictionary alloc] init];
        _registrar = registrar;
        _emptyClusterMarker = [self baseClusterMarker];
        _fontPath = [self loadFont];
    }
    return self;
}


void CFSafeRelease(CFTypeRef cf) {
    if (cf != NULL) {
        CFRelease(cf);
    }
}

- (CGFontRef)fontRefFromBundle {
    NSBundle *frameworkBundle = [NSBundle bundleForClass:self.classForCoder];
    NSURL *bundleURL = [[frameworkBundle resourceURL] URLByAppendingPathComponent:@"CozyFonts.bundle"];
    NSBundle *bundle = [NSBundle  bundleWithURL:bundleURL];
    NSURL *fontURL = [bundle URLForResource:@"oatmealpro2_semibold" withExtension:@"otf"];
    NSData *inData = [NSData dataWithContentsOfURL:fontURL];
    CGDataProviderRef provider = CGDataProviderCreateWithCFData((CFDataRef)inData);
    CGFontRef font = CGFontCreateWithDataProvider(provider);
    CFSafeRelease(provider);
    return font;
}

- (void)registerFont:(CGFontRef)font {
    CFErrorRef error;
    if (!CTFontManagerRegisterGraphicsFont(font, &error)) {
        CFStringRef errorDescription = CFErrorCopyDescription(error);
        NSLog(@"Failed to load font: %@", errorDescription);
        CFRelease(errorDescription);
    }
}

- (NSString *)loadFont {
    CGFontRef font = [self fontRefFromBundle];
    [self registerFont:font];
    NSString *fontName = (__bridge NSString *)CGFontCopyPostScriptName(font);
    CFSafeRelease(font);
    return fontName;
}

- (CGFloat)markerRadius {
    CGFloat baseScreenHeight = 2220;
    CGFloat maxMarkerRadius = 155;
    CGFloat minMarkerRadius = 60;
    CGFloat devicePixelRatio = [UIScreen mainScreen].bounds.size.height * [UIScreen mainScreen].scale;
    CGFloat proportionalMarkerRadius = 150 * (devicePixelRatio / baseScreenHeight);
    if(proportionalMarkerRadius > maxMarkerRadius) {
        return maxMarkerRadius;
    } else if (proportionalMarkerRadius < minMarkerRadius) {
        return minMarkerRadius;
    }
    return proportionalMarkerRadius;
}


- (UIImage *)baseClusterMarker {
    CGFloat shadowWidth = 2;
    CGFloat size = ([self markerRadius] / [UIScreen mainScreen].scale) + shadowWidth;
    CGSize canvas = CGSizeMake(size, size);
    UIGraphicsBeginImageContext(canvas);
    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize: canvas];
    UIImage *image = [renderer imageWithActions:^(UIGraphicsImageRendererContext * _Nonnull rendererContext) {
        CGContextSetAlpha(rendererContext.CGContext, 0.02);
        CGContextSetFillColorWithColor(rendererContext.CGContext, UIColor.grayColor.CGColor);
        CGContextAddEllipseInRect(rendererContext.CGContext, CGRectMake(0, 0, size, size));
        CGContextDrawPath(rendererContext.CGContext, kCGPathFillStroke);
        CGContextSetAlpha(rendererContext.CGContext, 1.0);
        CGContextSetFillColorWithColor(rendererContext.CGContext, UIColor.whiteColor.CGColor);
        CGContextSetStrokeColorWithColor(rendererContext.CGContext, UIColor.clearColor.CGColor);
        CGContextSetLineWidth(rendererContext.CGContext, 10);
        CGContextAddEllipseInRect(rendererContext.CGContext, CGRectMake(1, 1, size - 2, size - 2));
        CGContextDrawPath(rendererContext.CGContext, kCGPathFillStroke);
    }];
    UIGraphicsEndImageContext();
    return image;
}

- (void)addMarkers:(NSArray *)markersToAdd {
    for (NSDictionary *marker in markersToAdd) {
        CLLocationCoordinate2D position = [FLTMarkersController getPosition:marker];
        NSString *identifier = marker[@"markerId"];
        FLTGoogleMapMarkerController *controller =
        [[FLTGoogleMapMarkerController alloc] initMarkerWithPosition:position
                                                          identifier:identifier
                                                             mapView:self.mapView
                                                           iconImage:self.emptyClusterMarker
                                                            fontPath:self.fontPath];
        [controller interpretMarkerOptions:marker registrar:self.registrar];
        self.markerIdentifierToController[identifier] = controller;
    }
}

- (void)changeMarkers:(NSArray *)markersToChange {
    for (NSDictionary *marker in markersToChange) {
        NSString *identifier = marker[@"markerId"];
        FLTGoogleMapMarkerController *controller = self.markerIdentifierToController[identifier];
        if (!controller) {
            continue;
        }
        [controller interpretMarkerOptions:marker registrar:self.registrar];
    }
}

- (void)removeMarkersWithIdentifiers:(NSArray *)identifiers {
    for (NSString *identifier in identifiers) {
        FLTGoogleMapMarkerController *controller = self.markerIdentifierToController[identifier];
        if (!controller) {
            continue;
        }
        [controller removeMarker];
        [self.markerIdentifierToController removeObjectForKey:identifier];
    }
}

- (BOOL)didTapMarkerWithIdentifier:(NSString *)identifier {
    if (!identifier) {
        return NO;
    }
    FLTGoogleMapMarkerController *controller = self.markerIdentifierToController[identifier];
    if (!controller) {
        return NO;
    }
    [self.methodChannel invokeMethod:@"marker#onTap" arguments:@{@"markerId" : identifier}];
    return controller.consumeTapEvents;
}

- (void)didStartDraggingMarkerWithIdentifier:(NSString *)identifier
                                    location:(CLLocationCoordinate2D)location {
    if (!identifier) {
        return;
    }
    FLTGoogleMapMarkerController *controller = self.markerIdentifierToController[identifier];
    if (!controller) {
        return;
    }
    [self.methodChannel invokeMethod:@"marker#onDragStart"
                           arguments:@{
        @"markerId" : identifier,
        @"position" : [FLTGoogleMapJSONConversions arrayFromLocation:location]
    }];
}

- (void)didDragMarkerWithIdentifier:(NSString *)identifier
                           location:(CLLocationCoordinate2D)location {
    if (!identifier) {
        return;
    }
    FLTGoogleMapMarkerController *controller = self.markerIdentifierToController[identifier];
    if (!controller) {
        return;
    }
    [self.methodChannel invokeMethod:@"marker#onDrag"
                           arguments:@{
        @"markerId" : identifier,
        @"position" : [FLTGoogleMapJSONConversions arrayFromLocation:location]
    }];
}

- (void)didEndDraggingMarkerWithIdentifier:(NSString *)identifier
                                  location:(CLLocationCoordinate2D)location {
    FLTGoogleMapMarkerController *controller = self.markerIdentifierToController[identifier];
    if (!controller) {
        return;
    }
    [self.methodChannel invokeMethod:@"marker#onDragEnd"
                           arguments:@{
        @"markerId" : identifier,
        @"position" : [FLTGoogleMapJSONConversions arrayFromLocation:location]
    }];
}

- (void)didTapInfoWindowOfMarkerWithIdentifier:(NSString *)identifier {
    if (identifier && self.markerIdentifierToController[identifier]) {
        [self.methodChannel invokeMethod:@"infoWindow#onTap" arguments:@{@"markerId" : identifier}];
    }
}

- (void)showMarkerInfoWindowWithIdentifier:(NSString *)identifier result:(FlutterResult)result {
    FLTGoogleMapMarkerController *controller = self.markerIdentifierToController[identifier];
    if (controller) {
        [controller showInfoWindow];
        result(nil);
    } else {
        result([FlutterError errorWithCode:@"Invalid markerId"
                                   message:@"showInfoWindow called with invalid markerId"
                                   details:nil]);
    }
}

- (void)hideMarkerInfoWindowWithIdentifier:(NSString *)identifier result:(FlutterResult)result {
    FLTGoogleMapMarkerController *controller = self.markerIdentifierToController[identifier];
    if (controller) {
        [controller hideInfoWindow];
        result(nil);
    } else {
        result([FlutterError errorWithCode:@"Invalid markerId"
                                   message:@"hideInfoWindow called with invalid markerId"
                                   details:nil]);
    }
}

- (void)isInfoWindowShownForMarkerWithIdentifier:(NSString *)identifier
                                          result:(FlutterResult)result {
    FLTGoogleMapMarkerController *controller = self.markerIdentifierToController[identifier];
    if (controller) {
        result(@([controller isInfoWindowShown]));
    } else {
        result([FlutterError errorWithCode:@"Invalid markerId"
                                   message:@"isInfoWindowShown called with invalid markerId"
                                   details:nil]);
    }
}

+ (CLLocationCoordinate2D)getPosition:(NSDictionary *)marker {
    NSArray *position = marker[@"position"];
    return [FLTGoogleMapJSONConversions locationFromLatLong:position];
}

@end
