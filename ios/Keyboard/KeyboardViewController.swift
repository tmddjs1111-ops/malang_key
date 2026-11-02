import MalangCore
import SwiftUI
import UIKit

/// 말랑키 키보드 확장의 시작점. 키 화면은 SwiftUI(`KeyboardView`)로 그리고,
/// 여기서는 눌린 키를 입력창(textDocumentProxy)에 반영한다.
final class KeyboardViewController: UIInputViewController {
    private let model = KeyboardModel()
    private let composer = HangulComposer()
    private var lastShiftTap: TimeInterval = 0

    override func viewDidLoad() {
        super.viewDidLoad()
        model.onAction = { [weak self] action in self?.perform(action) }

        let host = UIHostingController(rootView: KeyboardView(model: model))
        host.view.backgroundColor = .clear
        host.view.translatesAutoresizingMaskIntoConstraints = false
        addChild(host)
        view.addSubview(host.view)
        NSLayoutConstraint.activate([
            host.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            host.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            host.view.topAnchor.constraint(equalTo: view.topAnchor),
            host.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        host.didMove(toParent: self)

        let height = view.heightAnchor.constraint(equalToConstant: KeyboardView.height)
        height.priority = UILayoutPriority(999)
        height.isActive = true
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        model.koreanLayout = MalangSettings.koreanLayout
        model.showsGlobe = needsInputModeSwitchKey
        composer.resetCycle()
        updateReturnKey()
    }

    override func textDidChange(_ textInput: UITextInput?) {
        super.textDidChange(textInput)
        updateReturnKey()
    }

    override func selectionDidChange(_ textInput: UITextInput?) {
        super.selectionDidChange(textInput)
        // 커서를 옮기면 연타 중이던 글자와 이어 붙이지 않는다.
        composer.resetCycle()
    }

    private var proxy: UITextDocumentProxy { textDocumentProxy }

    private func perform(_ action: KeyAction) {
        switch action {
        case .text(let text):
            insert(text)
        case .backspace:
            backspace()
        case .space:
            if model.composesHangul && composer.spacePressed(layout: model.koreanLayout) { return }
            proxy.insertText(" ")
            if model.mode != .letters { model.mode = .letters }
        case .enter:
            composer.resetCycle()
            proxy.insertText("\n")
        case .shift:
            tapShift()
        case .nextKeyboard:
            advanceToNextInputMode()
        case .toggleLanguage:
            composer.resetCycle()
            model.language = model.language == .korean ? .english : .korean
            model.shift = .off
        case .symbols:
            model.mode = .symbols
        case .moreSymbols:
            model.mode = .moreSymbols
        case .letters:
            model.mode = .letters
        case .noop:
            break
        }
    }

    private func insert(_ text: String) {
        if model.composesHangul {
            let before = proxy.documentContextBeforeInput ?? ""
            let action = composer.actions(precedingText: before, insert: text, layout: model.koreanLayout)
            for _ in 0..<action.delete { proxy.deleteBackward() }
            proxy.insertText(action.insert)
        } else {
            proxy.insertText(text)
        }
        if model.shift == .once { model.shift = .off }
    }

    private func backspace() {
        if model.composesHangul,
           let action = composer.actionsForBackspace(precedingText: proxy.documentContextBeforeInput ?? "") {
            for _ in 0..<action.delete { proxy.deleteBackward() }
            if !action.insert.isEmpty { proxy.insertText(action.insert) }
        } else {
            composer.resetCycle()
            proxy.deleteBackward()
        }
    }

    /// 한 번 누르면 한 글자만, 빨리 두 번 누르면 고정.
    private func tapShift() {
        let now = ProcessInfo.processInfo.systemUptime
        switch model.shift {
        case .off: model.shift = now - lastShiftTap < 0.3 ? .locked : .once
        case .once: model.shift = now - lastShiftTap < 0.3 ? .locked : .off
        case .locked: model.shift = .off
        }
        lastShiftTap = now
    }

    private func updateReturnKey() {
        switch proxy.returnKeyType ?? .default {
        case .go: model.returnLabel = "이동"
        case .search, .google, .yahoo: model.returnLabel = "검색"
        case .send: model.returnLabel = "보내기"
        case .done: model.returnLabel = "완료"
        case .next: model.returnLabel = "다음"
        case .join: model.returnLabel = "가입"
        default: model.returnLabel = nil
        }
    }
}
