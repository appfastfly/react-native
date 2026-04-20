export interface DeepLinkEvent {
  url?: string;
  payload: Record<string, any>;
  matchMethod?: string;
  matchConfidence?: number;
  isFirstSession: boolean;
}

export interface DeviceFingerprint {
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
}

export interface CreateLinkParams {
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

export interface ResolveResult {
  matched: boolean;
  payload?: Record<string, any>;
  matchMethod?: string;
  matchConfidence?: number;
  linkId?: string;
}

export type DeepLinkListener = (event: DeepLinkEvent) => void;
