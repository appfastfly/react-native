import { Linking, NativeEventEmitter } from 'react-native';
import { getNativeModule } from './NativeAppfastflyDeepLink';
import type { DeepLinkEvent, DeepLinkListener } from './types';

let initialized = false;
let latestParams: Record<string, any> | null = null;
let firstParams: Record<string, any> | null = null;
const listeners: Set<DeepLinkListener> = new Set();
let emitter: NativeEventEmitter | null = null;

function emit(event: DeepLinkEvent) {
  latestParams = event.payload;
  try {
    getNativeModule().setCachedParams(event.payload);
  } catch {}
  listeners.forEach((fn) => {
    try {
      fn(event);
    } catch {}
  });
}

async function handleDirectLink(url: string): Promise<void> {
  try {
    const shortCode = extractShortCode(url);
    if (!shortCode) return;

    const result: any = await getNativeModule().resolveLink(shortCode);

    if (result?.payload) {
      emit({
        url,
        payload: result.payload,
        isFirstSession: false,
        matchMethod: 'direct',
        matchConfidence: 1.0,
      });
    }
  } catch {
    // Silently ignore
  }
}

function extractShortCode(url: string): string | null {
  try {
    if (url.includes('://link')) {
      const match = url.match(/[?&]shortCode=([^&]+)/);
      if (match) return match[1] ?? null;
    }

    const parsed = new URL(url);
    const segments = parsed.pathname.split('/').filter(Boolean);
    if (segments.length === 1) {
      return segments[0] ?? null;
    }
    if (segments.length === 2) {
      return segments[1] ?? null;
    }
    return null;
  } catch {
    return null;
  }
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

  try {
    emitter = new NativeEventEmitter(getNativeModule() as any);
    emitter.addListener('onDeepLink', (event: { url?: string }) => {
      if (event.url) {
        handleDirectLink(event.url);
      }
    });

    Linking.addEventListener('url', ({ url }) => {
      handleDirectLink(url);
    });

    // Check cold-start URL
    const initialUrl = await Linking.getInitialURL();
    if (initialUrl) {
      await handleDirectLink(initialUrl);
      return;
    }

    // Deferred deep link on first launch
    const isFirst = await getNativeModule().isFirstLaunch();
    if (isFirst) {
      try {
        const result: any = await getNativeModule().initSession();
        if (result?.matched && result?.payload) {
          firstParams = result.payload;
          emit({
            payload: result.payload,
            matchMethod: result.matchMethod,
            matchConfidence: result.matchConfidence,
            isFirstSession: true,
          });
        }
      } catch {
        // Deferred resolve failed — app continues normally
      }
      getNativeModule().markInitialized();
    } else {
      try {
        latestParams = (await getNativeModule().getCachedParams()) as Record<
          string,
          any
        > | null;
      } catch {}
    }
  } catch {
    // SDK init failed — app continues normally
  }
}

/**
 * Subscribe to deep link events.
 * Returns unsubscribe function.
 * If params are already available, callback fires immediately.
 */
function subscribe(listener: DeepLinkListener): () => void {
  listeners.add(listener);

  if (latestParams) {
    try {
      listener({
        payload: latestParams,
        isFirstSession: !!firstParams,
      });
    } catch {}
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
 * Associate the current device with a user ID.
 */
async function setIdentity(userId: string): Promise<void> {
  try {
    await getNativeModule().setUserIdentity(userId);
  } catch {}
}

/**
 * Remove the current device-user association.
 */
async function logout(): Promise<void> {
  try {
    await getNativeModule().clearUserIdentity();
  } catch {}
}

export const Appfastfly = {
  init,
  subscribe,
  getLatestParams,
  getFirstParams,
  setIdentity,
  logout,
};
