import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  // Config from native (Info.plist / AndroidManifest)
  getConfig(): Promise<{ serviceUrl: string; apiKey: string }>;

  // Device fingerprinting
  getDeviceFingerprint(): Promise<{
    deviceId: string;
    advertisingId: string | null;
    brand: string;
    model: string;
    os: string;
    osVersion: string;
    osBuild: string | null;
    screenWidth: number;
    screenHeight: number;
    screenScale: number | null;
    screenDpi: number | null;
    locale: string;
    language: string;
    country: string | null;
    timezone: string;
    userAgent: string;
    isEmulator: boolean;
  }>;

  // Clipboard & referrer
  getClipboardToken(prefix: string): Promise<string | null>;
  clearClipboard(): void;
  getInstallReferrer(): Promise<string | null>;

  // Launch state
  isFirstLaunch(): Promise<boolean>;
  markInitialized(): void;

  // Native cache (UserDefaults / SharedPreferences)
  getCachedParams(): Promise<Object | null>;
  setCachedParams(params: Object): void;

  // Event emitter
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

const NativeDeepLink = TurboModuleRegistry.get<Spec>('AppfastflyDeepLink');

export function getNativeModule(): Spec {
  if (!NativeDeepLink) {
    throw new Error(
      '[Appfastfly] Native module not found. ' +
        'Make sure you linked the library and rebuilt the app. ' +
        'On Android ensure the package is registered in MainApplication.'
    );
  }
  return NativeDeepLink;
}

export default NativeDeepLink;
