import Foundation
import MalangCore

/// 글자를 넣을 곳. 키보드 확장에서는 입력창(textDocumentProxy), 앱 미리보기에서는 문자열.
protocol TextTarget: AnyObject {
    var textBeforeCursor: String { get }
    func deleteBackward()
    func insertText(_ text: String)
}

/// 눌린 키를 글자로 바꾼다. 키보드 확장과 앱 미리보기가 똑같이 쓴다.
final class KeyboardInput {
    let model: KeyboardModel
    let composer = HangulComposer()
    weak var target: TextTarget?
    /// 지구본 키. 키보드 확장에서만 의미가 있다.
    var onNextKeyboard: (() -> Void)?
    private var lastShiftTap: TimeInterval = 0

    init(model: KeyboardModel) {
        self.model = model
        model.onAction = { [weak self] action in self?.perform(action) }
    }

    func perform(_ action: KeyAction) {
        guard let target else { return }
        switch action {
        case .text(let text):
            insert(text, into: target)
        case .backspace:
            backspace(target)
        case .space:
            if model.composesHangul && composer.spacePressed(layout: model.koreanLayout) { return }
            target.insertText(" ")
            if model.mode != .letters { model.mode = .letters }
        case .enter:
            composer.resetCycle()
            target.insertText("\n")
        case .shift:
            tapShift()
        case .nextKeyboard:
            onNextKeyboard?()
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

    private func insert(_ text: String, into target: TextTarget) {
        if model.composesHangul {
            let action = composer.actions(precedingText: target.textBeforeCursor, insert: text, layout: model.koreanLayout)
            for _ in 0..<action.delete { target.deleteBackward() }
            target.insertText(action.insert)
        } else {
            target.insertText(text)
        }
        if model.shift == .once { model.shift = .off }
    }

    private func backspace(_ target: TextTarget) {
        if model.composesHangul,
           let action = composer.actionsForBackspace(precedingText: target.textBeforeCursor) {
            for _ in 0..<action.delete { target.deleteBackward() }
            if !action.insert.isEmpty { target.insertText(action.insert) }
        } else {
            composer.resetCycle()
            target.deleteBackward()
        }
    }

    /// 한 번 누르면 한 글자만, 빨리 두 번 누르면 고정.
    private func tapShift() {
        let now = ProcessInfo.processInfo.systemUptime
        let quick = now - lastShiftTap < 0.3
        switch model.shift {
        case .off: model.shift = quick ? .locked : .once
        case .once: model.shift = quick ? .locked : .off
        case .locked: model.shift = .off
        }
        lastShiftTap = now
    }
}
