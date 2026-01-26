import Foundation
import Capacitor
import FBSDKCoreKit

@objc public class FacebookEvents: NSObject {

    /// Configure l'état de suivi publicitaire
    @objc public func setAdvertiserTrackingEnabled(enabled: Bool) {
        Settings.shared.isAdvertiserTrackingEnabled = enabled
    }


    private func convertNSDictionaryToFBParameters(_ dict: NSDictionary?) -> [AppEvents.ParameterName: Any] {
        var parameters: [AppEvents.ParameterName: Any] = [:]
        if let dict = dict {
            for (key, value) in dict {
                if let keyString = key as? String, let value = value as? String {
                    parameters[AppEvents.ParameterName(keyString)] = value
                }
            }
        }
        return parameters
    }


    @objc public func logEvent(event: String, params: NSDictionary?) {
        let eventName = AppEvents.Name(event)

        if let params = params, params.count > 0 {
            let fbParameters = convertNSDictionaryToFBParameters(params)
            AppEvents.shared.logEvent(eventName, parameters: fbParameters)
        } else {
            AppEvents.shared.logEvent(eventName)
        }
    }


    @objc public func logPurchase(amount: Double, currency: String, transactionId: String, productId: String, params: NSDictionary?) {
        var fbParameters = convertNSDictionaryToFBParameters(params)
        fbParameters[AppEvents.ParameterName.orderID] = transactionId
        fbParameters[AppEvents.ParameterName.content] = productId

        AppEvents.shared.logPurchase(
            amount: amount,
            currency: currency,
            parameters: fbParameters
        )
    }


    @objc public func getFBAnonymousID() -> String {
        return AppEvents.shared.anonymousID
    }
}
