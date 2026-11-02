import MalangCore
import SwiftUI

@main
struct MalangKeyApp: App {
    var body: some Scene {
        WindowGroup {
            if let options = LaunchPreview.fromArguments() {
                if options.screen == "home" {
                    ContentView()
                } else {
                    NavigationStack { KeyboardPreviewScreen(options: options) }
                }
            } else {
                ContentView()
            }
        }
    }
}

/// `-preview` 로 켜면 바로 미리보기 화면을 연다. CI 가 시뮬레이터 화면을 찍을 때 쓴다.
/// 예: `-preview -layout cheonjiin -language english -mode symbols -panel emoji -theme blue -text 안녕`
struct LaunchPreview {
    var layout: KoreanLayout = MalangSettings.koreanLayout
    var language: KeyboardLanguage = .korean
    var mode: KeyboardMode = .letters
    var panel: KeyboardPanel = .none
    var themeID: String?
    var text = ""
    var screen = "keyboard"

    static func fromArguments(_ args: [String] = ProcessInfo.processInfo.arguments) -> LaunchPreview? {
        guard args.contains("-preview") else { return nil }
        func value(_ name: String) -> String? {
            guard let i = args.firstIndex(of: name), i + 1 < args.count else { return nil }
            return args[i + 1]
        }
        var options = LaunchPreview()
        if let layout = value("-layout").flatMap(KoreanLayout.init(rawValue:)) { options.layout = layout }
        if let language = value("-language").flatMap(KeyboardLanguage.init(rawValue:)) { options.language = language }
        switch value("-mode") {
        case "symbols": options.mode = .symbols
        case "moreSymbols": options.mode = .moreSymbols
        default: break
        }
        switch value("-panel") {
        case "emoji": options.panel = .emoji
        case "phrases": options.panel = .phrases
        case "themes": options.panel = .themes
        case "settings": options.panel = .settings
        default: break
        }
        options.themeID = value("-theme")
        options.text = value("-text") ?? ""
        options.screen = value("-screen") ?? "keyboard"
        return options
    }
}
