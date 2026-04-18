package com.appfastfly.deeplink

import com.facebook.react.TurboReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

class AppfastflyDeepLinkPackage : TurboReactPackage() {

  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
    return if (name == AppfastflyDeepLinkModule.NAME) {
      AppfastflyDeepLinkModule(reactContext)
    } else {
      null
    }
  }

  override fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
    return ReactModuleInfoProvider {
      val moduleInfos = mutableMapOf<String, ReactModuleInfo>()
      moduleInfos[AppfastflyDeepLinkModule.NAME] = ReactModuleInfo(
        AppfastflyDeepLinkModule.NAME,
        AppfastflyDeepLinkModule.NAME,
        false,  // canOverrideExistingModule
        false,  // needsEagerInit
        false,  // isCxxModule
        BuildConfig.IS_NEW_ARCHITECTURE_ENABLED  // isTurboModule
      )
      moduleInfos
    }
  }
}
