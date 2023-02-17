//
//  CozyMarkerBuilder.m
//  google_maps_flutter_ios
//
//  Created by Luiz Carvalho on 16/02/23.
//

#import <Foundation/Foundation.h>
#import <CoreText/CoreText.h>
#import "CozyMarkerBuilder.h"


@implementation CozyMarkerBuilder


- (instancetype)initCozy {
    self = [super init];
    if(self) {
    _cache = [[NSCache alloc] init];
    _fontPath = [self loadCozyFont];
    _markerSize = [self calculateMarkerSize];
    _shadowWidth = 5;
    }
    return self;
}

- (CGFloat)calculateMarkerSize {
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

- (NSString *)loadCozyFont {
    CGFontRef font = [self fontRefFromBundle];
    [self registerFont:font];
    NSString *fontName = (__bridge NSString *)CGFontCopyPostScriptName(font);
    CFSafeRelease(font);
    return fontName;
}

- (void)registerFont:(CGFontRef)font {
    CFErrorRef error;
    if (!CTFontManagerRegisterGraphicsFont(font, &error)) {
        CFStringRef errorDescription = CFErrorCopyDescription(error);
        NSLog(@"Failed to load font: %@", errorDescription);
        CFRelease(errorDescription);
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

void CFSafeRelease(CFTypeRef cf) {
    if (cf != NULL) {
        CFRelease(cf);
    }
}

- (UIImage *)baseClusterMarker {
    CGFloat size = ([self markerSize] / [UIScreen mainScreen].scale) + [self shadowWidth];
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
        CGContextAddEllipseInRect(rendererContext.CGContext, CGRectMake([self shadowWidth] / 2, [self shadowWidth] / 2, size - [self shadowWidth], size - [self shadowWidth]));
        CGContextDrawPath(rendererContext.CGContext, kCGPathFillStroke);
    }];
    UIGraphicsEndImageContext();
    return image;
}

- (UIImage *)clusterMarkerImageWithText:(NSString *)string {
    UIImage *circle = [self baseClusterMarker];
    CGSize size = [circle size];
    UIFont *textFont = [UIFont fontWithName:self.fontPath size:size.width / 2.5];
    CGSize stringSize = [string sizeWithAttributes:@{NSFontAttributeName:textFont}];
    CGFloat textY = (size.height / 2) - (stringSize.height / 2);
    CGFloat textX = (size.width / 2) - (stringSize.width / 2);
    UIGraphicsBeginImageContext(size);
    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize:size];
    UIImage *image = [renderer imageWithActions:^(UIGraphicsImageRendererContext * _Nonnull rendererContext) {
        CGRect rect = CGRectMake(0, 0, size.width, size.height);
        [circle drawInRect:CGRectIntegral(rect)];
        CGRect textRect = CGRectMake(textX, textY, stringSize.width, stringSize.height);
        [string drawInRect:CGRectIntegral(textRect) withAttributes:@{NSFontAttributeName:textFont}];
    }];
    UIGraphicsEndImageContext();
    return image;
}

- (UIImage *)priceMarkerImageWithText:(NSString *)text {
    UIFont *textFont = [UIFont fontWithName:self.fontPath size:([self markerSize] / [UIScreen mainScreen].scale) / 3];
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
        
        CGFloat heightWithoutShadow = markerHeight - [self shadowWidth];
        CGMutablePathRef path = CGPathCreateMutable();
        CGPathMoveToPoint(path, nil, canvas.width / 2 - 10, heightWithoutShadow);
        CGPathAddLineToPoint(path, nil, canvas.width / 2, heightWithoutShadow + 10);
        CGPathAddLineToPoint(path, nil, canvas.width / 2 + 10, heightWithoutShadow);
        CGPathAddLineToPoint(path, nil, canvas.width / 2 - 10, heightWithoutShadow);
        
        CGContextSetAlpha(rendererContext.CGContext, 0.02);
        CGContextSetFillColorWithColor(rendererContext.CGContext, UIColor.grayColor.CGColor);
        UIBezierPath *shadow = [UIBezierPath bezierPathWithRoundedRect:CGRectMake(0, 0, markerWidth, markerHeight) cornerRadius:7];
        [shadow fill];
        [shadow stroke];
        CGContextSetAlpha(rendererContext.CGContext, 1.0);
        CGContextSetFillColorWithColor(rendererContext.CGContext, UIColor.whiteColor.CGColor);
        CGContextSetStrokeColorWithColor(rendererContext.CGContext, UIColor.clearColor.CGColor);
        CGContextSetLineWidth(rendererContext.CGContext, 5);
        CGContextSetLineJoin(rendererContext.CGContext, 0);
        CGContextAddPath(rendererContext.CGContext, path);
        CGContextDrawPath(rendererContext.CGContext, 3);
        UIBezierPath *bezier = [UIBezierPath bezierPathWithRoundedRect:CGRectMake([self shadowWidth] / 2, [self shadowWidth] / 2, markerWidth - [self shadowWidth], markerHeight - [self shadowWidth]) cornerRadius:7];
        [bezier fill];
        [bezier stroke];
        [text drawInRect:CGRectIntegral(textRect) withAttributes:@{NSFontAttributeName:textFont}];
        CGPathRelease(path);
    }];
    UIGraphicsEndImageContext();
    return image;
}

- (UIImage *)roundedMarkerImageWithText:(NSString *)text withMarkerColor:(UIColor *)color withTextColor:(UIColor *)textColor {
    UIFont *textFont =  [UIFont fontWithName:self.fontPath size:[self.baseClusterMarker size].width / 3.5];
    CGSize stringSize = [text sizeWithAttributes:@{NSFontAttributeName:textFont}];
    
    CGFloat padding = 15;

    CGFloat minMarkerWidth = ([self markerSize] / [UIScreen mainScreen].scale) / 2;
    CGFloat markerWidth = ((stringSize.width > minMarkerWidth) ? stringSize.width : minMarkerWidth) + padding + [self shadowWidth];
    CGFloat markerHeight = stringSize.height + padding + [self shadowWidth];

    CGSize canvas = CGSizeMake(markerWidth, markerHeight);
    CGFloat y = (canvas.height / 2) - (stringSize.height / 2);
    CGFloat x = (canvas.width / 2) - (stringSize.width / 2);
    CGRect textRect = CGRectMake(x, y, stringSize.width, stringSize.height);

    UIGraphicsBeginImageContext(canvas);
    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize: canvas];
    UIImage *image = [renderer imageWithActions:^(UIGraphicsImageRendererContext * _Nonnull rendererContext) {
        
        CGContextSetAlpha(rendererContext.CGContext, 0.02);
        CGContextSetFillColorWithColor(rendererContext.CGContext, UIColor.grayColor.CGColor);
        UIBezierPath *shadow = [UIBezierPath bezierPathWithRoundedRect:CGRectMake(0, 0, markerWidth, markerHeight) cornerRadius:20];
        [shadow fill];
        [shadow stroke];
        CGContextSetAlpha(rendererContext.CGContext, 1.0);
        CGContextSetFillColorWithColor(rendererContext.CGContext, color.CGColor);
        CGContextSetStrokeColorWithColor(rendererContext.CGContext, UIColor.clearColor.CGColor);
        CGContextSetLineWidth(rendererContext.CGContext, 5);
        CGContextSetLineJoin(rendererContext.CGContext, 0);
        CGContextDrawPath(rendererContext.CGContext, 3);
        UIBezierPath *bezier = [UIBezierPath bezierPathWithRoundedRect:CGRectMake([self shadowWidth] / 2, [self shadowWidth] / 2, markerWidth - [self shadowWidth], markerHeight - [self shadowWidth]) cornerRadius:20];
        [bezier fill];
        [bezier stroke];
        [text drawInRect:CGRectIntegral(textRect) withAttributes:@{NSFontAttributeName:textFont, NSForegroundColorAttributeName: textColor}];
    }];
    UIGraphicsEndImageContext();
    return image;
}

- (UIImage *)getMarker:(NSString *)label withMarkerType:(NSString *)markerType {
    if([markerType isEqualToString:@"count"]) {
        return [self clusterMarkerImageWithText:label];
    } else if([markerType isEqualToString:@"rounded"]) {
        return [self roundedMarkerImageWithText:label withMarkerColor:UIColor.whiteColor withTextColor:UIColor.blackColor];
    } else if([markerType isEqualToString:@"rounded_visited"]) {
        return [self roundedMarkerImageWithText:label withMarkerColor:UIColor.whiteColor withTextColor:[UIColor colorWithRed:(110.0f/255.0f) green:(110.0f/255.0f) blue:(100.0f/255.0f) alpha:1]];
    }
    else if([markerType isEqualToString:@"rounded_selected"]) {
        return [self roundedMarkerImageWithText:label withMarkerColor:[UIColor colorWithRed:(57.0f/255.0f) green:(87.0f/255.0f) blue:(189.0f/255.0f) alpha:1] withTextColor:UIColor.whiteColor];
    }
    else if([markerType isEqualToString:@"price"]) {
        return [self priceMarkerImageWithText:label];
    }
    @throw [NSException exceptionWithName:@"InvalidMarker"
                                       reason:@"markerType not found for icon."
                                     userInfo:nil];
    
}

- (UIImage *)buildMarker:(NSString *)label withMarkerType:(NSString *)markerType {
    if(label == (id)[NSNull null] || [label isEqualToString:@""]) {
        @throw [NSException exceptionWithName:@"InvalidMarker"
                                       reason:@"no label was provided when expected."
                                     userInfo:nil];
    }
    NSString *key = [NSString stringWithFormat:@"%@:%@", markerType, label];
    UIImage *cachedImage = [[self cache] objectForKey:key];
    if(cachedImage != nil) {
        return cachedImage;
    }
    UIImage *image = [self getMarker:label withMarkerType:markerType];
   
    [[self cache] setObject:image forKey:key];
    return image;
}

    
@end
