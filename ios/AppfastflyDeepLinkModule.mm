#import "AppfastflyDeepLinkModule.h"
#import <React/RCTLog.h>
#import <AppfastflyDeepLinkSpec/AppfastflyDeepLinkSpec.h>
#import <React/RCTConversions.h>

#import "AppfastflyClipboard.h"
#import "AppfastflyFingerprint.h"
#import "AppfastflyApiClient.h"

static AppfastflyDeepLinkModule *_sharedInstance = nil;
static NSString *_pendingURL = nil;

@interface AppfastflyDeepLinkModule () <NativeAppfastflyDeepLinkSpec>
@end

@implementation AppfastflyDeepLinkModule {
  BOOL _hasListeners;
  NSDictionary *_cachedParams;
  NSString *_serviceUrl;
  NSString *_apiKey;
  AppfastflyApiClient *_apiClient;
}

RCT_EXPORT_MODULE(AppfastflyDeepLink)

- (instancetype)init {
  self = [super init];
  if (self) {
    _sharedInstance = self;

    NSDictionary *info = [[NSBundle mainBundle] infoDictionary];
    _serviceUrl = info[@"AppfastflyServiceUrl"] ?: @"https://api.appfastfly.io.vn";
    _apiKey = info[@"AppfastflyApiKey"] ?: @"";

    _apiClient = [[AppfastflyApiClient alloc] initWithServiceUrl:_serviceUrl apiKey:_apiKey];

    if (_apiKey.length == 0) {
      RCTLogWarn(@"[Appfastfly] Missing AppfastflyApiKey in Info.plist");
    }

    if (_pendingURL) {
      [self emitDeepLinkURL:_pendingURL];
      _pendingURL = nil;
    }
  }
  return self;
}

#pragma mark - AppDelegate integration

+ (void)continueUserActivity:(NSUserActivity *)userActivity {
  if ([userActivity.activityType isEqualToString:NSUserActivityTypeBrowsingWeb]) {
    NSString *url = userActivity.webpageURL.absoluteString;
    if (_sharedInstance) {
      [_sharedInstance emitDeepLinkURL:url];
    } else {
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
  if (_cachedParams) {
    [self sendEventWithName:@"onDeepLink" body:_cachedParams];
    _cachedParams = nil;
  }
}

- (void)stopObserving {
  _hasListeners = NO;
}

#pragma mark - Config & Fingerprint

RCT_EXPORT_METHOD(getConfig:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  @try {
    NSDictionary *info = [[NSBundle mainBundle] infoDictionary];
    resolve(@{
      @"serviceUrl": info[@"AppfastflyServiceUrl"] ?: @"https://api.appfastfly.io.vn",
      @"apiKey": info[@"AppfastflyApiKey"] ?: @"",
    });
  } @catch (NSException *exception) {
    resolve(@{@"serviceUrl": @"", @"apiKey": @""});
  }
}

RCT_EXPORT_METHOD(getDeviceFingerprint:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  @try {
    AppfastflyFingerprint *fp = [[AppfastflyFingerprint alloc] init];
    resolve([fp collect] ?: @{});
  } @catch (NSException *exception) {
    resolve(@{});
  }
}

RCT_EXPORT_METHOD(getClipboardToken:(NSString *)prefix
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  @try {
    AppfastflyClipboard *cb = [[AppfastflyClipboard alloc] init];
    resolve([cb getTokenWithPrefix:prefix]);
  } @catch (NSException *exception) {
    resolve(nil);
  }
}

RCT_EXPORT_METHOD(clearClipboard)
{
  @try {
    [[[AppfastflyClipboard alloc] init] clear];
  } @catch (NSException *exception) {}
}

RCT_EXPORT_METHOD(getInstallReferrer:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  resolve(nil);
}

#pragma mark - Launch state

RCT_EXPORT_METHOD(isFirstLaunch:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  @try {
    BOOL hasLaunched = [[NSUserDefaults standardUserDefaults] boolForKey:@"appfastfly_initialized"];
    resolve(@(!hasLaunched));
  } @catch (NSException *exception) {
    resolve(@(NO));
  }
}

RCT_EXPORT_METHOD(markInitialized)
{
  @try {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setBool:YES forKey:@"appfastfly_initialized"];
    [defaults synchronize];
  } @catch (NSException *exception) {}
}

#pragma mark - Cache

RCT_EXPORT_METHOD(getCachedParams:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  @try {
    NSData *data = [[NSUserDefaults standardUserDefaults] dataForKey:@"appfastfly_latest_params"];
    if (data) {
      NSError *error;
      NSDictionary *params = [NSJSONSerialization JSONObjectWithData:data options:0 error:&error];
      if (!error && params) { resolve(params); return; }
    }
    resolve(nil);
  } @catch (NSException *exception) {
    resolve(nil);
  }
}

RCT_EXPORT_METHOD(setCachedParams:(NSDictionary *)params)
{
  @try {
    if (!params || ![params isKindOfClass:[NSDictionary class]]) return;
    NSError *error;
    NSData *data = [NSJSONSerialization dataWithJSONObject:params options:0 error:&error];
    if (!error) {
      NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
      [defaults setObject:data forKey:@"appfastfly_latest_params"];
      [defaults synchronize];
    }
  } @catch (NSException *exception) {}
}

#pragma mark - Networking

RCT_EXPORT_METHOD(initSession:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  @try {
    AppfastflyFingerprint *fp = [[AppfastflyFingerprint alloc] init];
    NSDictionary *fingerprint = [fp collect];
    if (!fingerprint) { resolve(nil); return; }

    NSString *clipboardToken = nil;
    @try {
      AppfastflyClipboard *cb = [[AppfastflyClipboard alloc] init];
      clipboardToken = [cb getTokenWithPrefix:@"aff:"];
      if (clipboardToken) [cb clear];
    } @catch (NSException *exception) {}

    NSMutableDictionary *body = [fingerprint mutableCopy];
    body[@"platform"] = @"ios";
    if (clipboardToken) body[@"clipboardToken"] = clipboardToken;

    [_apiClient post:@"/api/v1/resolve" body:body completion:^(NSDictionary * _Nullable result) {
      resolve(result);
    }];
  } @catch (NSException *exception) {
    resolve(nil);
  }
}

RCT_EXPORT_METHOD(resolveLink:(NSString *)shortCode
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  @try {
    if (!shortCode || shortCode.length == 0) { resolve(nil); return; }

    NSDictionary *body = @{@"shortCode": shortCode, @"platform": @"ios"};
    [_apiClient post:@"/api/v1/resolve" body:body completion:^(NSDictionary * _Nullable result) {
      resolve(result);
    }];
  } @catch (NSException *exception) {
    resolve(nil);
  }
}

RCT_EXPORT_METHOD(setUserIdentity:(NSString *)userId
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  @try {
    if (!userId || userId.length == 0) { resolve(nil); return; }

    AppfastflyFingerprint *fp = [[AppfastflyFingerprint alloc] init];
    NSString *deviceId = [fp collect][@"deviceId"] ?: @"";

    NSDictionary *body = @{
      @"deviceId": deviceId,
      @"platform": @"ios",
      @"userId": userId,
    };

    [_apiClient post:@"/api/v1/identity" body:body completion:^(NSDictionary * _Nullable result) {
      resolve(nil);
    }];
  } @catch (NSException *exception) {
    resolve(nil);
  }
}

RCT_EXPORT_METHOD(clearUserIdentity:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  @try {
    AppfastflyFingerprint *fp = [[AppfastflyFingerprint alloc] init];
    NSString *deviceId = [fp collect][@"deviceId"] ?: @"";

    NSDictionary *body = @{
      @"deviceId": deviceId,
      @"platform": @"ios",
    };

    [_apiClient deleteRequest:@"/api/v1/identity" body:body completion:^(NSError * _Nullable error) {
      resolve(nil);
    }];
  } @catch (NSException *exception) {
    resolve(nil);
  }
}

#pragma mark - TurboModule

+ (BOOL)requiresMainQueueSetup {
  return NO;
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
  return std::make_shared<facebook::react::NativeAppfastflyDeepLinkSpecJSI>(params);
}

@end
