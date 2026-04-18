# @appfastfly/react-native

React Native SDK for [Appfastfly](https://appfastfly.com) — mobile deep linking & attribution.

## Requirements

| Platform     | Minimum version |
| ------------ | --------------- |
| iOS          | 15.0+           |
| Android      | API 24+ (7.0)   |
| React Native | 0.78+           |

## Installation

```bash
npm install @appfastfly/react-native
# or
yarn add @appfastfly/react-native
```

**iOS:**

```bash
cd ios && pod install
```

**Android:** No additional steps — Gradle auto-links the module.

---

## 1. Native Configuration

### iOS — Info.plist

```xml
<key>AppfastflyApiKey</key>
<string>YOUR_API_KEY</string>
```

### Android — AndroidManifest.xml

Add inside the `<application>` tag:

```xml
<meta-data
    android:name="com.appfastfly.API_KEY"
    android:value="YOUR_API_KEY" />
```

> Get your API key from the [Appfastfly Dashboard](https://appfastfly.com/dashboard/integration).

---

## 2. Deep Links Setup

### iOS

#### Associated Domains

In Xcode > target > **Signing & Capabilities** > **Associated Domains**, add:

```
applinks:link.yourdomain.com
```

#### AppDelegate — Objective-C

```objc
// AppDelegate.mm
#import <AppfastflyDeepLinkModule.h>

- (BOOL)application:(UIApplication *)application
    continueUserActivity:(NSUserActivity *)userActivity
    restorationHandler:(void (^)(NSArray<id<UIUserActivityRestoring>> *))handler
{
  [AppfastflyDeepLinkModule continueUserActivity:userActivity];
  return YES;
}

- (BOOL)application:(UIApplication *)application
    openURL:(NSURL *)url
    options:(NSDictionary<UIApplicationOpenURLOptionsKey, id> *)options
{
  [AppfastflyDeepLinkModule openURL:url];
  return YES;
}
```

#### AppDelegate — Swift

```swift
// AppDelegate.swift
import AppfastflyDeepLinkModule

func application(
  _ application: UIApplication,
  continue userActivity: NSUserActivity,
  restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void
) -> Bool {
  AppfastflyDeepLinkModule.continue(userActivity)
  return true
}

func application(
  _ app: UIApplication,
  open url: URL,
  options: [UIApplication.OpenURLOptionsKey: Any] = [:]
) -> Bool {
  AppfastflyDeepLinkModule.open(url)
  return true
}
```

#### URL Scheme (recommended)

In Xcode > target > **Info** > **URL Types**, add your custom scheme (e.g. `myapp`).

---

### Android

#### AndroidManifest.xml — Intent Filters

Add inside your `MainActivity` tag:

```xml
<activity
    android:name=".MainActivity"
    android:launchMode="singleTask"
    android:exported="true">

    <!-- App Links -->
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="https"
              android:host="link.yourdomain.com" />
    </intent-filter>

    <!-- URI Scheme -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="myapp" />
    </intent-filter>
</activity>
```

#### MainActivity — Kotlin

```kotlin
import android.content.Intent
import android.os.Bundle
import com.appfastfly.deeplink.AppfastflyDeepLinkModule

class MainActivity : ReactActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AppfastflyDeepLinkModule.handleIntent(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    AppfastflyDeepLinkModule.handleIntent(intent)
  }
}
```

#### MainActivity — Java

```java
import android.content.Intent;
import android.os.Bundle;
import com.appfastfly.deeplink.AppfastflyDeepLinkModule;

public class MainActivity extends ReactActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    AppfastflyDeepLinkModule.handleIntent(getIntent());
  }

  @Override
  public void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    AppfastflyDeepLinkModule.handleIntent(intent);
  }
}
```

---

## 3. Usage

### Initialize

Call once at app startup, before any navigation:

```typescript
import { Appfastfly } from "@appfastfly/react-native";

await Appfastfly.init();
```

### Subscribe to Deep Links

```typescript
const unsubscribe = Appfastfly.subscribe((event) => {
  console.log("Payload:", event.payload);
  console.log("First session?", event.isFirstSession);

  if (event.payload.screen) {
    navigation.navigate(event.payload.screen, event.payload);
  }
});

// Clean up
unsubscribe();
```

### Full Example

```tsx
import React, { useEffect } from "react";
import { Appfastfly } from "@appfastfly/react-native";

function App() {
  useEffect(() => {
    Appfastfly.init();

    const unsubscribe = Appfastfly.subscribe((event) => {
      if (event.payload.productId) {
        navigation.navigate("Product", { id: event.payload.productId });
      }
    });

    return unsubscribe;
  }, []);

  return <MainNavigator />;
}
```

### Read Cached Params

```typescript
const latest = Appfastfly.getLatestParams();
const first = Appfastfly.getFirstParams();
```

### Create Links

```typescript
const link = await Appfastfly.createLink({
  payload: { screen: "Product", productId: "123" },
  channel: "social",
  campaign: "summer-sale",
  ogTitle: "Check out this product!",
  ogDescription: "Get 20% off with this link",
  ogImageUrl: "https://example.com/product.jpg",
});

console.log(link.url);
// → https://link.myapp.com/l/abc123
```

### User Identity

```typescript
await Appfastfly.setIdentity("user-456");
await Appfastfly.logout();
```

---

## 4. API Reference

### `Appfastfly.init()`

Initialize the SDK. Must be called once before any other method.

Returns: `Promise<void>`

### `Appfastfly.subscribe(listener)`

Listen for deep link events. Fires immediately if params are already available.

```typescript
type DeepLinkEvent = {
  url?: string;
  payload: Record<string, any>;
  isFirstSession: boolean;
};
```

Returns: `() => void` (unsubscribe function)

### `Appfastfly.getLatestParams()`

Returns the most recent deep link payload, or `null`.

### `Appfastfly.getFirstParams()`

Returns the payload from the install-attributed deep link (first session only), or `null`.

### `Appfastfly.createLink(params)`

```typescript
type CreateLinkParams = {
  payload: Record<string, any>;
  ogTitle?: string;
  ogDescription?: string;
  ogImageUrl?: string;
  channel?: string;
  campaign?: string;
  tags?: string[];
  iosRedirectUrl?: string;
  androidRedirectUrl?: string;
  webRedirectUrl?: string;
  expiresAt?: string;
};
```

Returns: `Promise<{ shortCode: string; url: string }>`

### `Appfastfly.setIdentity(userId: string)`

Associate the device with a user ID.

### `Appfastfly.logout()`

Remove the device-user association.

---

## 5. Troubleshooting

### Universal Links not opening the app (iOS)

- Check that `applinks:` in Associated Domains matches your link domain exactly
- AASA must be served over HTTPS with a valid certificate
- Long-press a link to verify "Open in App" appears

### App Links not opening the app (Android)

- Verify `assetlinks.json` is accessible at `https://your-domain/.well-known/assetlinks.json`
- SHA-256 fingerprint must match your signing certificate
- `android:autoVerify="true"` must be set on the intent-filter

### No events in subscribe()

- Ensure `init()` is called before `subscribe()`
- Verify `continueUserActivity:` (iOS) or `handleIntent()` (Android) is wired up
- Check native logs for `[Appfastfly]` warnings

---

## License

MIT
