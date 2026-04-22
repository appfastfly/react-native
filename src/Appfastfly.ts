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

    // Resolve payload via native networking — never visible in JS debugger
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
    // Silently ignore - link may be invalid or network error
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
 * All API calls run on native layer — invisible to JS debugger.
 */
async function init(): Promise<void> {
  if (initialized) return;
  initialized = true;

  try {
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

    // First launch → deferred deep link via native networking
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
        // Deferred resolve failed — silently ignore, app continues normally
      }
      getNativeModule().markInitialized();
    } else {
      // Load cached params from previous session
      try {
        latestParams = (await getNativeModule().getCachedParams()) as Record<
          string,
          any
        > | null;
      } catch {}
    }
  } catch {
    // SDK initialization failed — silently ignore, app must not crash
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
 * Runs via native networking.
 */
async function setIdentity(userId: string): Promise<void> {
  try {
    await getNativeModule().setUserIdentity(userId);
  } catch {
    // Identity call failed — silently ignore
  }
}

/**
 * Remove the current device-user association.
 * Runs via native networking.
 */
async function logout(): Promise<void> {
  try {
    await getNativeModule().clearUserIdentity();
  } catch {
    // Logout call failed — silently ignore
  }
}

export const Appfastfly = {
  init,
  subscribe,
  getLatestParams,
  getFirstParams,
  setIdentity,
  logout,
};
