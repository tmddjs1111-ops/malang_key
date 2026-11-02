import MalangCore
import SwiftUI
import UIKit

/// 말랑키 키보드 확장의 시작점. 키 화면은 SwiftUI(`KeyboardView`)로 그리고,
/// 눌린 키는 `KeyboardInput` 이 입력창(textDocumentProxy)에 반영한다.
final class KeyboardViewController: UIInputViewController {
    private let model = KeyboardModel()
    private lazy var input = KeyboardInput(model: model)

    override func viewDidLoad() {
        super.viewDidLoad()
        input.target = self
        input.onNextKeyboard = { [weak self] in self?.advanceToNextInputMode() }

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
