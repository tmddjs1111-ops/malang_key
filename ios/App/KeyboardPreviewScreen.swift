import MalangCore
import SwiftUI

/// 앱 안에서 바로 쳐 보는 말랑키. 키보드를 켜지 않아도 자판을 시험할 수 있다.
final class PreviewText: ObservableObject, TextTarget {
    @Published var text: String

    init(text: String = "") { self.text = text }

    var textBeforeCursor: String { text }
    func deleteBackward() { if !text.isEmpty { text.removeLast() } }
    func insertText(_ text: String) { self.text += text }
}

struct KeyboardPreviewScreen: View {
    @StateObject private var model: KeyboardModel
    @StateObject private var preview: PreviewText
    @State private var input: KeyboardInput?

    init(layout: KoreanLayout, language: KeyboardLanguage = .korean, mode: KeyboardMode = .letters, text: String = "") {
        let model = KeyboardModel()
        model.koreanLayout = layout
        model.language = language
        model.mode = mode
        _model = StateObject(wrappedValue: model)
        _preview = StateObject(wrappedValue: PreviewText(text: text))
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                HStack(alignment: .top, spacing: 0) {
                    Text(preview.text)
                        .font(.title3)
                    Rectangle()
                        .fill(MalangPalette.accent)
                        .frame(width: 2, height: 24)
                    Spacer(minLength: 0)
                }
                .padding()
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(uiColor: .systemBackground))

            KeyboardView(model: model)
        }
        .navigationTitle("\(model.koreanLayout.title) 미리보기")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            guard input == nil else { return }
            let input = KeyboardInput(model: model)
            input.target = preview
            self.input = input
        }
    }
}
