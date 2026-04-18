#import <UIKit/UIKit.h>

@interface AppfastflyClipboard : NSObject

- (nullable NSString *)getTokenWithPrefix:(nonnull NSString *)prefix;
- (void)clear;

@end
