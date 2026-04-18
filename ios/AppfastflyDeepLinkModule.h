#ifdef RCT_NEW_ARCH_ENABLED
#import <AppfastflyDeepLinkSpec/AppfastflyDeepLinkSpec.h>
@interface AppfastflyDeepLinkModule : RCTEventEmitter <NativeDeepLinkSpec>
#else
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
@interface AppfastflyDeepLinkModule : RCTEventEmitter <RCTBridgeModule>
#endif

/// Call from AppDelegate application:continueUserActivity:restorationHandler:
/// to forward Universal Link URLs to the SDK.
+ (void)continueUserActivity:(NSUserActivity *)userActivity;

/// Call from AppDelegate application:openURL:options:
/// to forward URI scheme deep links to the SDK.
+ (void)openURL:(NSURL *)url;

@end
