#import "AppfastflyFingerprint.h"
#include <sys/utsname.h>
#include <sys/sysctl.h>

@implementation AppfastflyFingerprint

- (NSDictionary<NSString *, id> *)collect {
  UIDevice *device = [UIDevice currentDevice];
  UIScreen *screen = [UIScreen mainScreen];
  NSLocale *locale = [NSLocale currentLocale];

  NSMutableDictionary *result = [NSMutableDictionary dictionaryWithDictionary:@{
    @"deviceId": [self identifierForVendor],
    @"brand": @"Apple",
    @"model": [self deviceModel],
    @"os": @"iOS",
    @"osVersion": device.systemVersion,
    @"screenWidth": @((NSInteger)(screen.bounds.size.width)),
    @"screenHeight": @((NSInteger)(screen.bounds.size.height)),
    @"screenScale": @(screen.scale),
    @"locale": locale.localeIdentifier,
    @"language": locale.languageCode ?: @"en",
    @"timezone": [NSTimeZone localTimeZone].name,
    @"userAgent": [self defaultUserAgent],
    @"isEmulator": @([self isSimulator]),
  }];

  NSString *country = locale.countryCode;
  if (country) {
    result[@"country"] = country;
  }

  NSString *osBuild = [self osBuildVersion];
  if (osBuild) {
    result[@"osBuild"] = osBuild;
  }

  return [result copy];
}

- (NSString *)identifierForVendor {
  NSUUID *uuid = [UIDevice currentDevice].identifierForVendor;
  return uuid ? uuid.UUIDString : [[NSUUID UUID] UUIDString];
}

- (NSString *)deviceModel {
  struct utsname systemInfo;
  uname(&systemInfo);
  return [NSString stringWithCString:systemInfo.machine encoding:NSUTF8StringEncoding];
}

- (NSString *)osBuildVersion {
  size_t size = 0;
  sysctlbyname("kern.osversion", NULL, &size, NULL, 0);
  if (size == 0) return nil;
  char *build = malloc(size);
  sysctlbyname("kern.osversion", build, &size, NULL, 0);
  NSString *result = [NSString stringWithCString:build encoding:NSUTF8StringEncoding];
  free(build);
  return result;
}

- (NSString *)defaultUserAgent {
  NSString *osVersion = [[UIDevice currentDevice].systemVersion stringByReplacingOccurrencesOfString:@"." withString:@"_"];
  return [NSString stringWithFormat:
    @"Mozilla/5.0 (iPhone; CPU iPhone OS %@ like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148",
    osVersion];
}

- (BOOL)isSimulator {
#if TARGET_OS_SIMULATOR
  return YES;
#else
  return NO;
#endif
}

@end
