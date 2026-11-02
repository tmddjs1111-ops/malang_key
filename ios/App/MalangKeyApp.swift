import MalangCore
import SwiftUI

@main
struct MalangKeyApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
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
