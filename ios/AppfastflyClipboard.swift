import UIKit

@objc(AppfastflyClipboard)
public class AppfastflyClipboard: NSObject {

  @objc public func getToken(prefix: String) -> String? {
    guard let content = UIPasteboard.general.string else { return nil }
    guard content.hasPrefix(prefix) else { return nil }
    return String(content.dropFirst(prefix.count))
  }

  @objc public func clear() {
    UIPasteboard.general.string = ""
  }
}
