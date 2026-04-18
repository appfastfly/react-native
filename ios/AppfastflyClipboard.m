#import "AppfastflyClipboard.h"

@implementation AppfastflyClipboard

- (NSString *)getTokenWithPrefix:(NSString *)prefix {
  NSString *content = [UIPasteboard generalPasteboard].string;
  if (!content) return nil;
  if (![content hasPrefix:prefix]) return nil;
  return [content substringFromIndex:prefix.length];
}

- (void)clear {
  [UIPasteboard generalPasteboard].string = @"";
}

@end
