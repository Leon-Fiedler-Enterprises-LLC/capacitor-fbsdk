export interface FacebookEventsPlugin {
  setAdvertiserTrackingEnabled(options: { enabled: boolean }): Promise<void>;
  logEvent(options: { event: string; params?: any }): Promise<void>;
  getFBAnonymousID(): Promise<{ anonymousID: string }>;
  logPurchase(options: { amount: number; currency: string; transactionId: string; productId: string; }): Promise<void>;
}
