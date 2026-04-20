import { Linking, NativeEventEmitter, Platform } from 'react-native';
import { getNativeModule } from './NativeAppfastflyDeepLink';
import type {
  DeepLinkEvent,
  CreateLinkParams,
  ResolveResult,
  DeepLinkListener,
} from './types';

let initialized = false;
let serviceUrl = '';
let apiKey = '';
let latestParams: Record<string, any> | null = null;
let firstParams: Record<string, any> | null = null;
const listeners: Set<DeepLinkListener> = new Set();
let emitter: NativeEventEmitter | null = null;

function emit(event: DeepLinkEvent) {
  latestParams = event.payload;
  getNativeModule().setCachedParams(event.payload);
  listeners.forEach((fn) => fn(event));
}

async function loadConfig() {
  const config = await getNativeModule().getConfig();
  serviceUrl = config.serviceUrl;
  apiKey = config.apiKey;
}

async function handleDirectLink(url: string): Promise<void> {
  try {
    const shortCode = extractShortCode(url);
    if (!shortCode) return;

    // Resolve payload from server — never extract from URL
    const result: ResolveResult = await apiPost('/api/v1/resolve', {
      shortCode,
      platform: Platform.OS,
    });

    if (result.payload) {
      emit({
        url,
        payload: result.payload,
        isFirstSession: false,
        matchMethod: 'direct',
        matchConfidence: 1.0,
      });
    }
  } catch {
    // Silently ignore - link may be invalid
  }
}

function extractShortCode(url: string): string | null {
  try {
    // URI scheme: myapp://link?shortCode=abc123
    if (url.includes('://link')) {
      const match = url.match(/[?&]shortCode=([^&]+)/);
      if (match) return match[1] ?? null;
    }

    // Universal Link / App Link: https://domain.com/l/abc123 or https://domain.com/slug/abc123
    const parsed = new URL(url);
    const segments = parsed.pathname.split('/').filter(Boolean);
    if (segments.length === 1) {
      // /abc123 (single segment, unlikely but handle)
      return segments[0] ?? null;
    }
    if (segments.length === 2) {
      // /l/abc123 or /slug/abc123
      return segments[1] ?? null;
    }
    return null;
  } catch {
    return null;
  }
}

async function resolveDeferred(): Promise<void> {
  try {
    const fingerprint = await getNativeModule().getDeviceFingerprint();

    let clipboardToken: string | null = null;
    try {
      clipboardToken = await getNativeModule().getClipboardToken('aff:');
      if (clipboardToken) getNativeModule().clearClipboard();
    } catch {}

    let installReferrer: string | null = null;
    if (Platform.OS === 'android') {
      try {
        installReferrer = await getNativeModule().getInstallReferrer();
      } catch {}
    }

    const result: ResolveResult = await apiPost('/api/v1/resolve', {
      deviceId: fingerprint.deviceId,
      platform: Platform.OS,
      os: fingerprint.os,
      osVersion: fingerprint.osVersion,
      osBuild: fingerprint.osBuild,
      brand: fingerprint.brand,
      model: fingerprint.model,
      screenWidth: fingerprint.screenWidth,
      screenHeight: fingerprint.screenHeight,
      screenScale: fingerprint.screenScale,
      screenDpi: fingerprint.screenDpi,
      locale: fingerprint.locale,
      language: fingerprint.language,
      country: fingerprint.country,
      timezone: fingerprint.timezone,
      userAgent: fingerprint.userAgent,
      clipboardToken,
      installReferrer,
    });

    if (result.matched && result.payload) {
      firstParams = result.payload;
      emit({
        payload: result.payload,
        matchMethod: result.matchMethod,
        matchConfidence: result.matchConfidence,
        isFirstSession: true,
      });
    }
  } catch {
    // Silently ignore resolve failures
  }
}

// --- HTTP helpers ---

async function apiPost(path: string, body: any): Promise<any> {
  const res = await fetch(`${serviceUrl}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': apiKey,
    },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

async function apiDelete(path: string, body: any): Promise<void> {
  await fetch(`${serviceUrl}${path}`, {
    method: 'DELETE',
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': apiKey,
    },
    body: JSON.stringify(body),
  });
}

// =============================================================
// Public API
// =============================================================

/**
 * Initialize the SDK. Call once at app startup (e.g. in your root component).
 * Config is read from native (Info.plist / AndroidManifest).
 */
async function init(): Promise<void> {
  if (initialized) return;
  initialized = true;

  await loadConfig();

  // Setup native event listener — receives {url: "..."} from Universal Link / App Link
  emitter = new NativeEventEmitter(getNativeModule() as any);
  emitter.addListener('onDeepLink', (event: { url?: string }) => {
    if (event.url) {
      handleDirectLink(event.url);
    }
  });

  // Listen for incoming URLs via Linking (URI scheme, warm start)
  Linking.addEventListener('url', ({ url }) => {
    handleDirectLink(url);
  });

  // Check cold-start URL
  const initialUrl = await Linking.getInitialURL();
  if (initialUrl) {
    await handleDirectLink(initialUrl);
    return;
  }

  // First launch → deferred deep link
  const isFirst = await getNativeModule().isFirstLaunch();
  if (isFirst) {
    await resolveDeferred();
    getNativeModule().markInitialized();
  } else {
    // Load cached
    latestParams = await getNativeModule().getCachedParams();
  }
}

/**
 * Subscribe to deep link events.
 * Returns unsubscribe function.
 * If params are already available, callback fires immediately.
 */
function subscribe(listener: DeepLinkListener): () => void {
  listeners.add(listener);

  // If we already have params, fire immediately
  if (latestParams) {
    listener({
      payload: latestParams,
      isFirstSession: !!firstParams,
    });
  }

  return () => {
    listeners.delete(listener);
  };
}

/**
 * Get the latest deep link params (cached).
 */
function getLatestParams(): Record<string, any> | null {
  return latestParams;
}

/**
 * Get the first deep link params from deferred matching (first session only).
 */
function getFirstParams(): Record<string, any> | null {
  return firstParams;
}

/**
 * Create a new deep link. Returns the full URL.
 */
async function createLink(
  params: CreateLinkParams
): Promise<{ shortCode: string; url: string }> {
  const link = await apiPost('/api/v1/links', params);
  return { shortCode: link.shortCode, url: link.url };
}

/**
 * Associate the current device with a user ID.
 */
async function setIdentity(userId: string): Promise<void> {
  const fingerprint = await getNativeModule().getDeviceFingerprint();
  await apiPost('/api/v1/identity', {
    deviceId: fingerprint.deviceId,
    platform: Platform.OS,
    userId,
  });
}

/**
 * Remove the current device-user association.
 */
async function logout(): Promise<void> {
  const fingerprint = await getNativeModule().getDeviceFingerprint();
  await apiDelete('/api/v1/identity', {
    deviceId: fingerprint.deviceId,
    platform: Platform.OS,
  });
}

export const Appfastfly = {
  init,
  subscribe,
  getLatestParams,
  getFirstParams,
  createLink,
  setIdentity,
  logout,
};
