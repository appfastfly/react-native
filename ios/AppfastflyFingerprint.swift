import UIKit
import WebKit

@objc(AppfastflyFingerprint)
public class AppfastflyFingerprint: NSObject {

  @objc public func collect() -> [String: Any] {
    let device = UIDevice.current
    let screen = UIScreen.main
    let locale = Locale.current

    var result: [String: Any] = [
      "deviceId": identifierForVendor(),
      "brand": "Apple",
      "model": deviceModel(),
      "os": "iOS",
      "osVersion": device.systemVersion,
      "screenWidth": Int(screen.bounds.width * screen.scale),
      "screenHeight": Int(screen.bounds.height * screen.scale),
      "screenScale": screen.scale,
      "locale": locale.identifier,
      "language": locale.languageCode ?? "en",
      "timezone": TimeZone.current.identifier,
      "userAgent": defaultUserAgent(),
      "isEmulator": isSimulator(),
    ]

    if let country = locale.regionCode {
      result["country"] = country
    }

    if let osBuild = osBuildVersion() {
      result["osBuild"] = osBuild
    }

    return result
  }

  private func identifierForVendor() -> String {
    return UIDevice.current.identifierForVendor?.uuidString ?? UUID().uuidString
  }

  private func deviceModel() -> String {
    var systemInfo = utsname()
    uname(&systemInfo)
    let machineMirror = Mirror(reflecting: systemInfo.machine)
    let identifier = machineMirror.children.reduce("") { identifier, element in
      guard let value = element.value as? Int8, value != 0 else { return identifier }
      return identifier + String(UnicodeScalar(UInt8(value)))
    }
    return identifier
  }

  private func osBuildVersion() -> String? {
    var size = 0
    sysctlbyname("kern.osversion", nil, &size, nil, 0)
    var build = [CChar](repeating: 0, count: size)
    sysctlbyname("kern.osversion", &build, &size, nil, 0)
    return String(cString: build)
  }

  private func defaultUserAgent() -> String {
    return "Mozilla/5.0 (iPhone; CPU iPhone OS \(UIDevice.current.systemVersion.replacingOccurrences(of: ".", with: "_")) like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148"
  }

  private func isSimulator() -> Bool {
    #if targetEnvironment(simulator)
    return true
    #else
    return false
    #endif
  }
}
