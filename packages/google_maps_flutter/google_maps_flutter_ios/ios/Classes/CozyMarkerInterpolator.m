//
//  CozyMarkerInterpolator.m
//  Pods
//
//  Created by Pietro Domingues on 26/09/23.
//

#import <UIKit/UIKit.h>
#import "CozyMarkerInterpolator.h"
#import "CozyMarkerElements.h"

@implementation CozyMarkerInterpolator
// - (instancetype)initWithFont:(UIFont *)font {
//     self = [super init];
//     // if (self) {
//     //     _font = font;
//     // }
//     return self;
// }

- (CozyMarkerElements *)getInterpolatedMarkerElementsWithStart:(CozyMarkerElements *)start end:(CozyMarkerElements *)end step:(CGFloat)step {
    CozyMarkerElement *interpolatedCanvas = [self interpolateCozyMarkerElementWithStart:start.canvas end:end.canvas step:step];
    CozyMarkerElement *interpolatedBubble = [self interpolateCozyMarkerElementWithStart:start.bubble end:end.bubble step:step];
    CozyMarkerElement *interpolatedIcon = [self interpolateCozyMarkerElementWithStart:start.icon end:end.icon step:step];
    CozyMarkerElement *interpolatedIconCircle = [self interpolateCozyMarkerElementWithStart:start.iconCircle end:end.iconCircle step:step];
    CozyMarkerElement *interpolatedPointer = [self interpolateCozyMarkerElementWithStart:start.pointer end:end.pointer step:step];

    CozyMarkerElement *interpolatedLabel = [self interpolateCozyMarkerElementWithStart:start.labels[0] end:end.labels[0] step:step];
    NSArray<CozyMarkerElement *> *interpolatedLabels = [self interpolateLabelWithInterpolatedLabel:interpolatedLabel startText:start.labels[0].data endText:end.labels[0].data step:step];
    interpolatedIcon.data = [self interpolateIconsWithStart:start.icon.data end:end.icon.data step:step];

    return [[CozyMarkerElements alloc] initWithCanvas:interpolatedCanvas bubble:interpolatedBubble labels:interpolatedLabels icon:interpolatedIcon iconCircle:interpolatedIconCircle pointer:interpolatedPointer];
}

- (CozyMarkerElement *)interpolateCozyMarkerElementWithStart:(CozyMarkerElement *)start end:(CozyMarkerElement *)end step:(CGFloat)step {
    CGRect interpolatedBounds = CGRectMake(
        [self interpolate:start.bounds.origin.x end:end.bounds.origin.x step:step],
        [self interpolate:start.bounds.origin.y end:end.bounds.origin.y step:step],
        [self interpolate:start.bounds.size.width end:end.bounds.size.width step:step],
        [self interpolate:start.bounds.size.height end:end.bounds.size.height step:step]
    );

    UIColor *interpolatedFillColor = [self interpolateColorWithStart:start.fillColor end:end.fillColor step:step];
    UIColor *interpolatedStrokeColor = [self interpolateColorWithStart:start.strokeColor end:end.strokeColor step:step];
    CGFloat interpolatedAlpha = [self interpolate:start.alpha end:end.alpha step:step];

    return [[CozyMarkerElement alloc] initWithBounds:interpolatedBounds fillColor:interpolatedFillColor strokeColor:interpolatedStrokeColor alpha:interpolatedAlpha data:nil];
}

- (CGFloat)interpolate:(CGFloat)start end:(CGFloat)end step:(CGFloat)step {
    return start + (end - start) * step;
}

- (UIColor *)interpolateColorWithStart:(UIColor *)start end:(UIColor *)end step:(CGFloat)step {
    CGFloat startRed, startGreen, startBlue, startAlpha;
    CGFloat endRed, endGreen, endBlue, endAlpha;
    
    [start getRed:&startRed green:&startGreen blue:&startBlue alpha:&startAlpha];
    [end getRed:&endRed green:&endGreen blue:&endBlue alpha:&endAlpha];
    
    CGFloat interpolatedRed = [self interpolate:startRed end:endRed step:step];
    CGFloat interpolatedGreen = [self interpolate:startGreen end:endGreen step:step];
    CGFloat interpolatedBlue = [self interpolate:startBlue end:endBlue step:step];
    CGFloat interpolatedAlpha = [self interpolate:startAlpha end:endAlpha step:step];
    
    return [UIColor colorWithRed:interpolatedRed green:interpolatedGreen blue:interpolatedBlue alpha:interpolatedAlpha];
}

- (NSArray<CozyMarkerElement *> *)interpolateLabelWithInterpolatedLabel:(CozyMarkerElement *)interpolatedLabel startText:(NSString *)startText endText:(NSString *)endText step:(CGFloat)step {
    if ([startText isEqualToString:endText]) {
        return @[
            [[CozyMarkerElement alloc] initWithBounds:interpolatedLabel.bounds fillColor:interpolatedLabel.fillColor strokeColor:interpolatedLabel.strokeColor alpha:interpolatedLabel.alpha data:startText]
        ];
    }

    // UIFont *textFont = self.font;
    // CGFloat textFontSize = interpolatedLabel.bounds.size.height;
    // NSDictionary *attributes = @{
    //     NSFontAttributeName: textFont,
    //     NSForegroundColorAttributeName: interpolatedLabel.fillColor
    // };

    // CGSize labelSize = interpolatedLabel.bounds.size;
    // CGFloat labelWidth = labelSize.width * 1.2;
    
    // NSString *clippedStartText = [self clipText:startText withFont:textFont fontSize:textFontSize maxWidth:labelWidth];
    // NSString *clippedEndText = [self clipText:endText withFont:textFont fontSize:textFontSize maxWidth:labelWidth];
    
    CGFloat startTextAlpha = 1.0 - step;
    CGFloat endTextAlpha = step;
    
    CozyMarkerElement *startLabelElement = [[CozyMarkerElement alloc] initWithBounds:interpolatedLabel.bounds fillColor:interpolatedLabel.fillColor strokeColor:interpolatedLabel.strokeColor alpha:startTextAlpha data:startText];
    CozyMarkerElement *endLabelElement = [[CozyMarkerElement alloc] initWithBounds:interpolatedLabel.bounds fillColor:interpolatedLabel.fillColor strokeColor:interpolatedLabel.strokeColor alpha:endTextAlpha data:endText];
    
    return @[startLabelElement, endLabelElement];
}

// - (NSString *)clipText:(NSString *)text withFont:(UIFont *)font fontSize:(CGFloat)fontSize maxWidth:(CGFloat)maxWidth {
//     CGSize textSize = [text sizeWithAttributes:@{NSFontAttributeName: font}];
//     if (textSize.width <= maxWidth) {
//         return text;
//     }
    
//     NSMutableString *clippedText = [NSMutableString stringWithString:text];
    
//     while ([clippedText sizeWithAttributes:@{NSFontAttributeName: font}].width > maxWidth) {
//         [clippedText deleteCharactersInRange:NSMakeRange([clippedText length] - 1, 1)];
//     }
    
//     [clippedText appendString:@"..."];
    
//     return clippedText;
// }

- (NSString *)interpolateIconsWithStart:(NSString *)startIcon end:(NSString *)endIcon step:(CGFloat)step {
    if (startIcon) {
        return startIcon;
    }
    if (endIcon) {
        return endIcon;
    }
    return nil;
}
@end
