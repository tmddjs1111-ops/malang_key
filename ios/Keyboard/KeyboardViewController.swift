import Combine
import MalangCore
import SwiftUI
import UIKit

/// 키 누르는 소리(`playInputClick`)를 내려면 키보드 뷰가 이 프로토콜을 따라야 한다.
final class KeyboardRootView: UIInputView, UIInputViewAudioFeedback {
    var enableInputClicksWhenVisible: Bool { true }
}

/// 말랑키 키보드 확장의 시작점. 키 화면은 SwiftUI(`KeyboardView`)로 그리고,
/// 눌린 키는 `KeyboardInput` 이 입력창(textDocumentProxy)에 반영한다.
final class KeyboardViewController: UIInputViewController {
    private let model = KeyboardModel()
    private lazy var input = KeyboardInput(model: model)
    private var heightConstraint: NSLayoutConstraint?
    private var heightObserver: AnyCancellable?

    override func loadView() {
        let root = KeyboardRootView(frame: .zero, inputViewStyle: .keyboard)
        root.allowsSelfSizing = true
        view = root
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        input.target = self
        input.onNextKeyboard = { [weak self] in self?.advanceToNextInputMode() }
        input.onDismiss = { [weak self] in self?.dismissKeyboard() }
        model.onKeyDown = { [weak self] in
            if self?.model.sound == true { UIDevice.current.playInputClick() }
        }

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

        let height = view.heightAnchor.constraint(equalToConstant: model.totalHeight)
        height.priority = UILayoutPriority(999)
        height.isActive = true
        heightConstraint = height
        // 키보드 안에서 높이를 바꾸면 바로 반영한다.
        heightObserver = model.$height.sink { [weak self] newHeight in
            guard let self else { return }
            self.heightConstraint?.constant = KeyboardModel.toolbarHeight
                + CGFloat(newHeight.rowHeight) * 4 + KeyboardModel.verticalPadding * 2
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        model.reloadSettings()
        model.panel = .none
        model.showsGlobe = needsInputModeSwitchKey
        input.emojiSuggestions = MalangSettings.emojiSuggestions
        input.composer.resetCycle()
        updateReturnKey()
    }

    override func textDidChange(_ textInput: UITextInput?) {
        super.textDidChange(textInput)
        updateReturnKey()
    }

    override func selectionDidChange(_ textInput: UITextInput?) {
        super.selectionDidChange(textInput)
        // 커서를 옮기면 연타 중이던 글자와 이어 붙이지 않는다.
        input.composer.resetCycle()
    }

    private func updateReturnKey() {
        switch textDocumentProxy.returnKeyType ?? .default {
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

extension KeyboardViewController: TextTarget {
    var textBeforeCursor: String { textDocumentProxy.documentContextBeforeInput ?? "" }
    func deleteBackward() { textDocumentProxy.deleteBackward() }
    func insertText(_ text: String) { textDocumentProxy.insertText(text) }
}
