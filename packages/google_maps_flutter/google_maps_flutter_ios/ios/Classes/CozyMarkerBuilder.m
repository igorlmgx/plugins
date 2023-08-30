//
//  CozyMarkerBuilder.m
//  google_maps_flutter_ios
//
//  Created by Luiz Carvalho on 16/02/23.
//

#import <Foundation/Foundation.h>
#import <CoreText/CoreText.h>
#import "CozyMarkerBuilder.h"
#import "CozyMarkerData.h"


@implementation CozyMarkerBuilder


- (instancetype)initWithCache:(BOOL)useCache {
    self = [super init];
    if(self) {
        _fontPath = [self loadCozyFont];
        _markerSize = [self calculateMarkerSize];
        _shadowWidth = 3;
        _useCache = useCache;
        if(useCache == YES) {
            _cache = [[NSCache alloc] init];
        }
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

- (UIImage *)pinMarkerImageWithText:(NSString *)text withMarkerColor:(UIColor *)color withTextColor:(UIColor *)textColor withTail:(BOOL)withTail {
    
    // getting font and setting its size to 3 the size of the marker size
    CGFloat fontSize = 12;
    UIFont *textFont =  [UIFont fontWithName:self.fontPath size:fontSize];
    CGSize stringSize = [text sizeWithAttributes:@{NSFontAttributeName:textFont}];
    
    // setting padding and stroke size
    CGFloat paddingHorizontal = 11;
    CGFloat paddingVertical = 12;
    CGFloat strokeSize = 3;

    // setting stroke color
    UIColor *strokeColor = [UIColor colorWithRed:212.0f/255.0f green:(214.0f/255.0f) blue:(202.0f/255.0f) alpha:1];

    // setting marker width with a minimum width in case the string size is below the minimum
    CGFloat minMarkerWidth = 40;
    CGFloat markerWidth = stringSize.width + (2 * paddingVertical) + (2 * strokeSize);
    if(markerWidth < minMarkerWidth) {
        markerWidth = minMarkerWidth;
    }
     
    // in case a tail will be used, sets a tail size, else it becomes 0.
    CGFloat tailSize = withTail ? 6 : 0;
    
    // setting the marker height
    CGFloat markerHeight = stringSize.height + (2 * paddingHorizontal) + (2 * strokeSize) + tailSize;

    
    // creating canvas
    CGSize canvas = CGSizeMake(markerWidth, markerHeight);

    UIGraphicsBeginImageContext(canvas);
    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize: canvas];
    UIImage *image = [renderer imageWithActions:^(UIGraphicsImageRendererContext * _Nonnull rendererContext) {
        
        // setting colors and stroke
        CGContextSetAlpha(rendererContext.CGContext, 1.0);
        CGContextSetFillColorWithColor(rendererContext.CGContext, color.CGColor);
        CGContextSetStrokeColorWithColor(rendererContext.CGContext, strokeColor.CGColor);
        CGContextSetLineJoin(rendererContext.CGContext, 0);
        
        // drawing bubble from point x/y, and with width and height
        UIBezierPath *bezier = [UIBezierPath bezierPathWithRoundedRect:CGRectMake(strokeSize, strokeSize, markerWidth - (2 * strokeSize), markerHeight - (2 * strokeSize) - tailSize) cornerRadius:20];
        
        // if a tail will be used, sets a point in the bottom center of the bubble,
        // draws a triangle from this point and adds to the bubble path above
        if(withTail) {
            CGFloat x = canvas.width / 2;
            CGFloat y = markerHeight - tailSize - strokeSize;
            
            UIBezierPath *tailPath = [UIBezierPath bezierPath];
            [tailPath moveToPoint:CGPointMake(x - tailSize, y)];
            [tailPath addLineToPoint:CGPointMake(x, y + tailSize)];
            [tailPath addLineToPoint:CGPointMake(x + tailSize, y)];
            [tailPath closePath];
            [bezier appendPath:tailPath];
        }
        
        // draws the bubble with the tail, if used
        [bezier setLineWidth: strokeSize];
        [bezier stroke];
        [bezier fill];
     
        // draws the text
        CGFloat textY = ((canvas.height - tailSize) / 2) - (stringSize.height / 2);
        CGFloat textX = (canvas.width / 2) - (stringSize.width / 2);
        CGRect textRect = CGRectMake(textX, textY, stringSize.width, stringSize.height);
        [text drawInRect:CGRectIntegral(textRect) withAttributes:@{NSFontAttributeName:textFont, NSForegroundColorAttributeName: textColor}];
        
    }];
    UIGraphicsEndImageContext();
    return image;
}

- (UIImage *)getMarkerWithData:(CozyMarkerData *)cozyMarkerData {
    UIColor *defaultMarkerColor = UIColor.whiteColor;
    UIColor *defaultTextColor = UIColor.blackColor;
    
    UIColor *selectedMarkerColor = [UIColor colorWithRed:(57.0f/255.0f) green:(87.0f/255.0f) blue:(189.0f/255.0f) alpha:1];
    UIColor *selectedTextColor = UIColor.whiteColor;
    
    UIColor *visualizedMarkerColor = [UIColor colorWithRed:(248.0f/255.0f) green:(249.0f/255.0f) blue:(245.0f/255.0f) alpha:1];
    UIColor *visualizedTextColor = [UIColor colorWithRed:(110.0f/255.0f) green:(110.0f/255.0f) blue:(100.0f/255.0f) alpha:1];

    UIColor *markerColor = defaultMarkerColor;
    UIColor *textColor = defaultTextColor;
    
    if(cozyMarkerData.isVisualized){
        markerColor = visualizedMarkerColor;
        textColor = visualizedTextColor;
    }
    if(cozyMarkerData.isSelected){
        markerColor = selectedMarkerColor;
        textColor = selectedTextColor;
    }
    return [self pinMarkerImageWithText:cozyMarkerData.label withMarkerColor:markerColor 
                                                             withTextColor:textColor 
                                                             withTail:cozyMarkerData.hasPointer];
}

- (UIImage *)cacheMarkerWithData:(CozyMarkerData *)cozyMarkerData {
    NSString *key = [cozyMarkerData description];
    UIImage *cachedImage = [[self cache] objectForKey:key];
    if(cachedImage != nil) {
        return cachedImage;
    }
    UIImage *image = [self getMarkerWithData:cozyMarkerData];
    [[self cache] setObject:image forKey:key];
    return image;
}

- (UIImage *)buildMarkerWithData:(CozyMarkerData *)cozyMarkerData {
    if(_useCache == YES) {
        return [self cacheMarkerWithData:cozyMarkerData];
    }
    return [self getMarkerWithData:cozyMarkerData];
}

    
@end

