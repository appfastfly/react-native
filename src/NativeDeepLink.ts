import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  // Config from native (Info.plist / AndroidManifest)
  getConfig(): Promise<{ serviceUrl: string; apiKey: string }>;

  // Device fingerprinting
  getDeviceFingerprint(): Promise<{
    deviceId: string;
    advertisingId?: string;
    brand: string;
    model: string;
    os: string;
    osVersion: string;
    osBuild?: string;
    screenWidth: number;
    screenHeight: number;
    screenScale?: number;
    screenDpi?: number;
    locale: string;
    language: string;
    country?: string;
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
  getCachedParams(): Promise<Record<string, any> | null>;
  setCachedParams(params: Record<string, any>): void;

  // Event emitter
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'AppfastflyDeepLink',
);
