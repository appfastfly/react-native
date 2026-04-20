package com.appfastfly.deeplink

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

class AppfastflyDeepLinkPackage : BaseReactPackage() {

  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
    return if (name == NativeAppfastflyDeepLinkSpec.NAME) {
      AppfastflyDeepLinkModule(reactContext)
    } else {
      null
    }
  }

  override fun getReactModuleInfoProvider() = ReactModuleInfoProvider {
    mapOf(
      NativeAppfastflyDeepLinkSpec.NAME to ReactModuleInfo(
        name = NativeAppfastflyDeepLinkSpec.NAME,
        className = NativeAppfastflyDeepLinkSpec.NAME,
        canOverrideExistingModule = false,
        needsEagerInit = false,
        isCxxModule = false,
        isTurboModule = true
      )
    )
  }
}
