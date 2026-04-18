#import "AppfastflyDeepLinkModule.h"
#import <React/RCTLog.h>

#ifdef RCT_NEW_ARCH_ENABLED
#import <React/RCTConversions.h>
#endif

#import "Appfastfly-Swift.h"

// Static reference for forwarding events from AppDelegate
static AppfastflyDeepLinkModule *_sharedInstance = nil;
static NSString *_pendingURL = nil;

@implementation AppfastflyDeepLinkModule {
  BOOL _hasListeners;
  NSDictionary *_cachedParams;
  NSString *_serviceUrl;
  NSString *_apiKey;
}

RCT_EXPORT_MODULE(AppfastflyDeepLink)

- (instancetype)init {
  self = [super init];
  if (self) {
    _sharedInstance = self;

    // Read config from Info.plist
    NSDictionary *info = [[NSBundle mainBundle] infoDictionary];
    _serviceUrl = info[@"AppfastflyServiceUrl"];
    _apiKey = info[@"AppfastflyApiKey"];

    if (!_serviceUrl || !_apiKey) {
      RCTLogWarn(@"[Appfastfly] Missing AppfastflyServiceUrl or AppfastflyApiKey in Info.plist");
    }

    // If a Universal Link arrived before the module was ready, emit it now
    if (_pendingURL) {
      [self emitDeepLinkURL:_pendingURL];
      _pendingURL = nil;
    }
  }
  return self;
}

#pragma mark - Static methods for AppDelegate

+ (void)continueUserActivity:(NSUserActivity *)userActivity {
  if ([userActivity.activityType isEqualToString:NSUserActivityTypeBrowsingWeb]) {
    NSString *url = userActivity.webpageURL.absoluteString;
    if (_sharedInstance) {
      [_sharedInstance emitDeepLinkURL:url];
    } else {
      // Module not yet initialized — queue for later
      _pendingURL = url;
    }
  }
}

+ (void)openURL:(NSURL *)url {
  NSString *urlString = url.absoluteString;
  if (_sharedInstance) {
    [_sharedInstance emitDeepLinkURL:urlString];
  } else {
    _pendingURL = urlString;
  }
}

#pragma mark - Internal

- (void)emitDeepLinkURL:(NSString *)url {
  NSDictionary *event = @{@"url": url};
  if (_hasListeners) {
    [self sendEventWithName:@"onDeepLink" body:event];
  } else {
    _cachedParams = event;
  }
}

#pragma mark - Events

- (NSArray<NSString *> *)supportedEvents {
  return @[@"onDeepLink"];
}

- (void)startObserving {
  _hasListeners = YES;
  // If we have a cached event, emit immediately
  if (_cachedParams) {
    [self sendEventWithName:@"onDeepLink" body:_cachedParams];
    _cachedParams = nil;
  }
}

- (void)stopObserving {
  _hasListeners = NO;
}

#pragma mark - Exported methods

RCT_EXPORT_METHOD(getConfig:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSDictionary *info = [[NSBundle mainBundle] infoDictionary];
  NSDictionary *config = @{
    @"serviceUrl": info[@"AppfastflyServiceUrl"] ?: @"https://api.appfastfly.io.vn",
    @"apiKey": info[@"AppfastflyApiKey"] ?: @"",
  };
  resolve(config);
}

RCT_EXPORT_METHOD(getDeviceFingerprint:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  @try {
    AppfastflyFingerprint *fp = [[AppfastflyFingerprint alloc] init];
    NSDictionary *result = [fp collect];
    resolve(result);
  } @catch (NSException *exception) {
    reject(@"fingerprint_error", exception.reason, nil);
  }
}

RCT_EXPORT_METHOD(getClipboardToken:(NSString *)prefix
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  @try {
    AppfastflyClipboard *cb = [[AppfastflyClipboard alloc] init];
    NSString *token = [cb getTokenWithPrefix:prefix];
    resolve(token);
  } @catch (NSException *exception) {
    reject(@"clipboard_error", exception.reason, nil);
  }
}

RCT_EXPORT_METHOD(clearClipboard)
{
  AppfastflyClipboard *cb = [[AppfastflyClipboard alloc] init];
  [cb clear];
}

RCT_EXPORT_METHOD(getInstallReferrer:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  // iOS does not have install referrer
  resolve(nil);
}

RCT_EXPORT_METHOD(isFirstLaunch:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
  BOOL hasLaunched = [defaults boolForKey:@"appfastfly_initialized"];
  resolve(@(!hasLaunched));
}

RCT_EXPORT_METHOD(markInitialized)
{
  NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
  [defaults setBool:YES forKey:@"appfastfly_initialized"];
  [defaults synchronize];
}

RCT_EXPORT_METHOD(getCachedParams:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
  NSData *data = [defaults dataForKey:@"appfastfly_latest_params"];
  if (data) {
    NSError *error;
    NSDictionary *params = [NSJSONSerialization JSONObjectWithData:data options:0 error:&error];
    if (!error && params) {
      resolve(params);
      return;
    }
  }
  resolve(nil);
}

RCT_EXPORT_METHOD(setCachedParams:(NSDictionary *)params)
{
  NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
  NSError *error;
  NSData *data = [NSJSONSerialization dataWithJSONObject:params options:0 error:&error];
  if (!error) {
    [defaults setObject:data forKey:@"appfastfly_latest_params"];
    [defaults synchronize];
  }
}

+ (BOOL)requiresMainQueueSetup {
  return NO;
}

#ifdef RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
  return std::make_shared<facebook::react::NativeDeepLinkSpecJSI>(params);
}
#endif

@end
