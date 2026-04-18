#ifdef RCT_NEW_ARCH_ENABLED
#if __has_include(<AppfastflyDeepLinkSpec/AppfastflyDeepLinkSpec.h>)
#import <AppfastflyDeepLinkSpec/AppfastflyDeepLinkSpec.h>
#endif
@interface AppfastflyDeepLinkModule : NSObject <NativeAppfastflyDeepLinkSpec>
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
