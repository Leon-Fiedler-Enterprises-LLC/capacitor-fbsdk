import Foundation
import Capacitor
import FBSDKCoreKit
import UIKit

@objc(FacebookEventsPlugin)
public class FacebookEventsPlugin: CAPPlugin {
    private let facebookEvents = FacebookEvents()

    public override func load() {
        super.load()
        initializeFacebookSDK()
    }

    private func initializeFacebookSDK() {
        ApplicationDelegate.shared.initializeSDK()

        Settings.shared.isAutoLogAppEventsEnabled = false
        Settings.shared.isAdvertiserIDCollectionEnabled = true

        // Activate app to track installs
        if let application = UIApplication.value(forKeyPath: #keyPath(UIApplication.shared)) as? UIApplication {
            ApplicationDelegate.shared.application(
                application,
                didFinishLaunchingWithOptions: nil
            )
        }

        // Explicitly activate app events for install tracking
        AppEvents.shared.activateApp()
    }

    @objc func setAdvertiserTrackingEnabled(_ call: CAPPluginCall) {
        guard let enabled = call.getBool("enabled") else {
            call.reject("Missing enabled argument")
            return
        }

        facebookEvents.setAdvertiserTrackingEnabled(enabled: enabled)
        call.resolve()
    }

    @objc func logEvent(_ call: CAPPluginCall) {
        guard let event = call.getString("event") else {
            call.reject("Missing event argument")
            return
        }

        let params = call.getObject("params") ?? [:]
        facebookEvents.logEvent(event: event, params: params as NSDictionary)
        call.resolve()
    }

    @objc func getFBAnonymousID(_ call: CAPPluginCall) {
        let anonymousID = facebookEvents.getFBAnonymousID()
        call.resolve([
            "anonymousID": anonymousID
        ])
    }
}
