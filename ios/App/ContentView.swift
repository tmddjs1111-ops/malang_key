import MalangCore
import SwiftUI

/// 말랑키 앱 첫 화면: 키보드 켜기 안내, 미리보기, 테마, 자판, 키보드 설정, 상용구, 정보.
struct ContentView: View {
    @AppStorage(MalangSettings.Keys.koreanLayout, store: MalangSettings.defaults)
    private var koreanLayout: KoreanLayout = .dubeolsik
    @AppStorage(MalangSettings.Keys.theme, store: MalangSettings.defaults)
    private var themeID = KeyboardTheme.automaticID
    @AppStorage(MalangSettings.Keys.height, store: MalangSettings.defaults)
    private var height: KeyboardHeight = .normal
    @AppStorage(MalangSettings.Keys.sound, store: MalangSettings.defaults)
    private var sound = true
    @AppStorage(MalangSettings.Keys.numberHints, store: MalangSettings.defaults)
    private var numberHints = true
    @AppStorage(MalangSettings.Keys.emojiSuggestions, store: MalangSettings.defaults)
    private var emojiSuggestions = true
    @State private var testText = ""
    @Environment(\.openURL) private var openURL

    var body: some View {
        NavigationStack {
            Form {
                header

                Section("키보드 켜기") {
                    step(1, "설정 → 일반 → 키보드 → 키보드")
                    step(2, "새로운 키보드 추가… → 말랑키")
                    step(3, "입력할 때 🌐 지구본 키를 길게 눌러 말랑키 고르기")
                    Button("설정 열기") {
                        if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                    }
                }

                Section {
                    NavigationLink {
                        KeyboardPreviewScreen(options: LaunchPreview(layout: koreanLayout))
                    } label: {
                        Label("말랑키 쳐 보기", systemImage: "keyboard")
                    }
                    TextField("다른 키보드와 바꿔 가며 입력해 보세요", text: $testText, axis: .vertical)
                        .lineLimit(2...5)
                } header: {
                    Text("시험해 보기")
                } footer: {
                    Text("‘쳐 보기’는 키보드를 켜지 않아도 앱 안에서 말랑키를 써 볼 수 있어요.")
                }

                Section("테마") {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 4), spacing: 12) {
                        themeButton(id: KeyboardTheme.automaticID, title: "자동", theme: nil)
                        ForEach(KeyboardTheme.all) { theme in themeButton(id: theme.id, title: theme.title, theme: theme) }
                    }
                    .padding(.vertical, 6)
                }

                Section("한글 자판") {
                    ForEach(KoreanLayout.available, id: \.self) { layout in
                        Button { koreanLayout = layout } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(layout.title).foregroundStyle(.primary)
                                    Text(layout.subtitle).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                if layout == koreanLayout {
                                    Image(systemName: "checkmark").foregroundStyle(MalangPalette.accent)
                                }
                            }
                        }
                    }
                }

                Section {
                    Picker("키보드 높이", selection: $height) {
                        ForEach(KeyboardHeight.allCases, id: \.self) { Text($0.title).tag($0) }
                    }
                    Toggle("키 누르는 소리", isOn: $sound)
                    Toggle("길게 눌러 넣는 숫자·기호 표시", isOn: $numberHints)
                    Toggle("이모지 추천", isOn: $emojiSuggestions)
                    NavigationLink("상용구") { PhrasesEditor() }
                } header: {
                    Text("키보드")
                } footer: {
                    Text("키 소리는 아이폰 설정 → 사운드 및 햅틱 → 키보드 피드백의 ‘소리’가 켜져 있어야 나요. 이모지 추천은 ‘사랑’, ‘행복’처럼 친 단어에 맞는 이모지를 위에 띄워요.")
                }

                Section("정보") {
                    LabeledContent("버전", value: Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "-")
                    Link("개인정보처리방침", destination: URL(string: "https://github.com/tmddjs1111-ops/malang_key/blob/main/docs/privacy.html")!)
                    Text("말랑키는 입력한 글자를 저장하거나 어디로도 보내지 않아요. 전체 접근 허용도 필요 없어요.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    Text("FlorisBoard(Apache 2.0)를 바탕으로 만든 오픈소스 키보드예요.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
        }
        .tint(MalangPalette.accent)
    }

    private var header: some View {
        Section {
            HStack(spacing: 14) {
                Image("Logo")
                    .resizable()
                    .frame(width: 64, height: 64)
                    .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
                VStack(alignment: .leading, spacing: 4) {
                    Text("말랑키")
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundStyle(MalangPalette.title)
                    Text("귀엽고 말랑한 오프라인 키보드")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.vertical, 6)
        }
    }

    private func step(_ number: Int, _ text: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 10) {
            Text("\(number)")
                .font(.subheadline.bold())
                .foregroundStyle(MalangPalette.cream)
                .frame(width: 22, height: 22)
                .background(Circle().fill(MalangPalette.accent))
            Text(text)
        }
    }

    private func themeButton(id: String, title: String, theme: KeyboardTheme?) -> some View {
        Button { themeID = id } label: {
            VStack(spacing: 5) {
                ThemeSwatch(theme: theme)
                    .frame(height: 46)
                    .overlay(
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .stroke(themeID == id ? MalangPalette.accent : Color.secondary.opacity(0.2),
                                    lineWidth: themeID == id ? 3 : 1)
                    )
                Text(title)
                    .font(.caption2)
                    .foregroundStyle(.primary)
                    .lineLimit(1)
            }
        }
        .buttonStyle(.plain)
    }
}

/// 상용구 고치기
struct PhrasesEditor: View {
    @State private var phrases = MalangSettings.phrases
    @State private var newPhrase = ""

    var body: some View {
        List {
            Section {
                HStack {
                    TextField("새 상용구", text: $newPhrase)
                    Button("추가") {
                        let trimmed = newPhrase.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !trimmed.isEmpty else { return }
                        phrases.append(trimmed)
                        newPhrase = ""
                    }
                    .disabled(newPhrase.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            } footer: {
                Text("키보드 위쪽 💬 버튼을 누르면 상용구를 바로 넣을 수 있어요.")
            }
            Section {
                ForEach(phrases, id: \.self) { Text($0) }
                    .onDelete { phrases.remove(atOffsets: $0) }
                    .onMove { phrases.move(fromOffsets: $0, toOffset: $1) }
            }
        }
        .navigationTitle("상용구")
        .toolbar { EditButton() }
        .onChange(of: phrases) { MalangSettings.phrases = $0 }
    }
}
