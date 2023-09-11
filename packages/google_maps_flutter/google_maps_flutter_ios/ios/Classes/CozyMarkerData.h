//
//  CozyMarkerData.h
//  Pods
//
//  Created by Pietro Domingues on 24/08/23.
//

@interface CozyMarkerData : NSObject
- (instancetype)initWithLabel:(NSString *)label
                         icon:(NSString *)icon
                  hasTail:(BOOL)hasTail
                   isSelected:(BOOL)isSelected
                   isVisualized:(BOOL)isVisualized
                        state:(NSString *)state
                      variant:(NSString *)variant
                         size:(NSString *)size;

@property (nonatomic, strong) NSString *label;
@property (nonatomic, strong) NSString *icon;
@property (nonatomic, strong) NSString *state;
@property (nonatomic, strong) NSString *variant;
@property (nonatomic, strong) NSString *size;
@property (nonatomic, assign) BOOL hasTail;
@property (nonatomic, assign) BOOL isSelected;
@property (nonatomic, assign) BOOL isVisualized;
@end
