require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "Appfastfly"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["repository"]
  s.license      = package["license"]
  s.authors      = "Appfastfly"
  s.platforms    = { :ios => "13.4" }
  s.source       = { :git => "#{package["repository"]}.git", :tag => "#{s.version}" }
  s.source_files = "ios/**/*.{h,m,mm}"

  install_modules_dependencies(s)

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES'
  }
end
