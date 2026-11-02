import MalangCore
import SwiftUI

@main
struct MalangKeyApp: App {
    var body: some Scene {
        WindowGroup {
            if let shot = LaunchPreview.fromArguments() {
                NavigationStack {
                    KeyboardPreviewScreen(layout: shot.layout, language: shot.language, mode: shot.mode, text: shot.text)
                }
            } else {
                ContentView()
            }
        }
    }
}

/// `-preview` 로 켜면 바로 미리보기 화면을 연다. CI 가 시뮬레이터 화면을 찍을 때 쓴다.
/// 예: `-preview -layout cheonjiin -language english -mode symbols -text 안녕`
struct LaunchPreview {
    var layout: KoreanLayout = .dubeolsik
    var language: KeyboardLanguage = .korean
    var mode: KeyboardMode = .letters
    var text = ""

    static func fromArguments(_ args: [String] = ProcessInfo.processInfo.arguments) -> LaunchPreview? {
        guard args.contains("-preview") else { return nil }
        func value(_ name: String) -> String? {
            guard let i = args.firstIndex(of: name), i + 1 < args.count else { return nil }
            return args[i + 1]
        }
        var shot = LaunchPreview()
        if let layout = value("-layout").flatMap(KoreanLayout.init(rawValue:)) { shot.layout = layout }
        if let language = value("-language").flatMap(KeyboardLanguage.init(rawValue:)) { shot.language = language }
        switch value("-mode") {
        case "symbols": shot.mode = .symbols
        case "moreSymbols": shot.mode = .moreSymbols
        default: break
        }
        shot.text = value("-text") ?? ""
        return shot
    }
}

/// 키보드를 켜는 방법, 한글 자판 고르기, 시험 입력창.
struct ContentView: View {
    @AppStorage(MalangSettings.Keys.koreanLayout, store: MalangSettings.defaults)
    private var koreanLayout: KoreanLayout = .dubeolsik
    @State private var testText = ""
    @Environment(\.openURL) private var openURL

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("말랑키")
                            .font(.system(size: 34, weight: .bold, design: .rounded))
                        Text("귀엽고 말랑한 오프라인 키보드. 입력한 글자는 어디로도 보내지 않아요.")
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 8)
                }

                Section("키보드 켜기") {
                    step(1, "설정 → 일반 → 키보드 → 키보드")
                    step(2, "새로운 키보드 추가… → 말랑키")
                    step(3, "입력할 때 🌐 지구본 키를 눌러 말랑키로 바꾸기")
                    Button("설정 열기") {
                        if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                    }
                }

                Section {
                    Picker("한글 자판", selection: $koreanLayout) {
                        ForEach(KoreanLayout.available, id: \.self) { layout in
                            Text(layout.title).tag(layout)
                        }
                    }
                    .pickerStyle(.segmented)
                } header: {
                    Text("한글 자판")
                } footer: {
                    Text("키보드를 다시 열면 바뀐 자판이 나와요.")
                }

                Section {
                    NavigationLink("\(koreanLayout.title) 자판 쳐 보기") {
                        KeyboardPreviewScreen(layout: koreanLayout)
                    }
                } header: {
                    Text("미리보기")
                } footer: {
                    Text("키보드를 켜지 않아도 앱 안에서 말랑키를 써 볼 수 있어요.")
                }

                Section("시험해 보기") {
                    TextField("여기에 입력해 보세요", text: $testText, axis: .vertical)
                        .lineLimit(3...6)
                }
            }
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
        }
        .tint(Color(red: 0.95, green: 0.55, blue: 0.63))
    }

    private func step(_ number: Int, _ text: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 10) {
            Text("\(number)")
                .font(.subheadline.bold())
                .frame(width: 22, height: 22)
                .background(Circle().fill(Color.pink.opacity(0.15)))
            Text(text)
        }
    }
}
