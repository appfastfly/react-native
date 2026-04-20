# @appfastfly/react-native

React Native SDK for [Appfastfly](https://appfastfly.com) — mobile deep linking & attribution.

[![npm version](https://img.shields.io/npm/v/@appfastfly/react-native)](https://www.npmjs.com/package/@appfastfly/react-native)
[![CI](https://github.com/appfastfly/react-native/actions/workflows/ci.yml/badge.svg)](https://github.com/appfastfly/react-native/actions/workflows/ci.yml)
[![license](https://img.shields.io/npm/l/@appfastfly/react-native)](LICENSE)

## Features

- **Deep Linking** — Universal Links (iOS) + App Links (Android) + URI schemes
- **Deferred Deep Links** — attribute installs to the original link via device fingerprint, clipboard token, and install referrer
- **Link Creation** — generate trackable short links with custom payloads and Open Graph metadata
- **User Identity** — associate devices with user IDs for cross-device attribution
- **Event-driven** — subscribe to deep link events with automatic caching

## Requirements

| Platform     | Minimum version                          |
| ------------ | ---------------------------------------- |
| iOS          | 15.0+                                    |
| Android      | API 24+ (7.0)                            |
| React Native | 0.76+ (New Architecture / TurboModules)  |

> **Note:** This SDK requires the New Architecture (TurboModules) which is enabled by default starting from React Native 0.76. It does not support the old architecture (Bridge).

## Installation

```bash
yarn add @appfastfly/react-native
# or
npm install @appfastfly/react-native
```

**iOS** — install CocoaPods:

```bash
cd ios && bundle exec pod install
```

**Android** — no additional steps, Gradle auto-links the module.

---

## 1. Native Configuration

### iOS — Info.plist

```xml
<key>AppfastflyApiKey</key>
<string>YOUR_API_KEY</string>
```

Optionally override the service URL (defaults to `https://api.appfastfly.io.vn`):

```xml
<key>AppfastflyServiceUrl</key>
<string>https://your-custom-api.com</string>
```

### Android — AndroidManifest.xml

Add inside the `<application>` tag:

```xml
<meta-data
    android:name="com.appfastfly.API_KEY"
    android:value="YOUR_API_KEY" />
```

Optionally override the service URL:

```xml
<meta-data
    android:name="com.appfastfly.SERVICE_URL"
    android:value="https://your-custom-api.com" />
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

#### AppDelegate (Objective-C)

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

#### AppDelegate (Swift)

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

#### MainActivity (Kotlin)

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

---

## 3. Usage

### Initialize

Call once at app startup, before any navigation:

```typescript
import { Appfastfly } from '@appfastfly/react-native';

await Appfastfly.init();
```

### Subscribe to Deep Links

```typescript
const unsubscribe = Appfastfly.subscribe((event) => {
  console.log('Payload:', event.payload);
  console.log('First session?', event.isFirstSession);

  if (event.payload.screen) {
    navigation.navigate(event.payload.screen, event.payload);
  }
});

// Clean up when done
unsubscribe();
```

### Full Example

```tsx
import React, { useEffect } from 'react';
import { Appfastfly } from '@appfastfly/react-native';

function App() {
  useEffect(() => {
    Appfastfly.init();

    const unsubscribe = Appfastfly.subscribe((event) => {
      if (event.payload.productId) {
        navigation.navigate('Product', { id: event.payload.productId });
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
  payload: { screen: 'Product', productId: '123' },
  channel: 'social',
  campaign: 'summer-sale',
  ogTitle: 'Check out this product!',
  ogDescription: 'Get 20% off with this link',
  ogImageUrl: 'https://example.com/product.jpg',
});

console.log(link.url);
// → https://link.myapp.com/l/abc123
```

### User Identity

```typescript
// Associate device with a user
await Appfastfly.setIdentity('user-456');

// Remove association
await Appfastfly.logout();
```

---

## 4. API Reference

### `Appfastfly.init()`

Initialize the SDK. Must be called once before any other method. Reads configuration from native (Info.plist / AndroidManifest).

Returns: `Promise<void>`

### `Appfastfly.subscribe(listener)`

Listen for deep link events. If params are already available, fires the callback immediately.

| Parameter  | Type                            | Description         |
| ---------- | ------------------------------- | ------------------- |
| `listener` | `(event: DeepLinkEvent) => void` | Callback on event   |

Returns: `() => void` — unsubscribe function

```typescript
interface DeepLinkEvent {
  url?: string;
  payload: Record<string, any>;
  matchMethod?: string;
  matchConfidence?: number;
  isFirstSession: boolean;
}
```

### `Appfastfly.getLatestParams()`

Returns the most recent deep link payload, or `null`.

### `Appfastfly.getFirstParams()`

Returns the payload from the install-attributed deep link (first session only), or `null`.

### `Appfastfly.createLink(params)`

Create a trackable short link.

```typescript
interface CreateLinkParams {
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
}
```

Returns: `Promise<{ shortCode: string; url: string }>`

### `Appfastfly.setIdentity(userId)`

Associate the current device with a user ID for cross-device attribution.

### `Appfastfly.logout()`

Remove the current device-user association.

---

## 5. Troubleshooting

### Universal Links not opening the app (iOS)

- Verify `applinks:` in Associated Domains matches your link domain exactly
- Ensure the Apple App Site Association (AASA) file is served over HTTPS with a valid certificate
- Long-press a link in Safari to verify "Open in App" appears

### App Links not opening the app (Android)

- Verify `assetlinks.json` is accessible at `https://your-domain/.well-known/assetlinks.json`
- The SHA-256 fingerprint must match your signing certificate
- `android:autoVerify="true"` must be set on the intent-filter

### No events in `subscribe()`

- Ensure `init()` is called before `subscribe()`
- Verify `continueUserActivity:` (iOS) or `handleIntent()` (Android) is wired in your AppDelegate / MainActivity
- Check native logs for `[Appfastfly]` warnings

---

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
