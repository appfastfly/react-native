#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface AppfastflyDeepLinkModule : RCTEventEmitter <RCTBridgeModule>

/// Call from AppDelegate application:continueUserActivity:restorationHandler:
/// to forward Universal Link URLs to the SDK.
+ (void)continueUserActivity:(NSUserActivity *)userActivity;

/// Call from AppDelegate application:openURL:options:
/// to forward URI scheme deep links to the SDK.
+ (void)openURL:(NSURL *)url;

@end
