import MalangCore
import SwiftUI

enum ShiftState { case off, once, locked }

/// 키보드 화면 상태. 컨트롤러가 바꾸고 SwiftUI 가 그린다.
final class KeyboardModel: ObservableObject {
    @Published var language: KeyboardLanguage = .korean
    @Published var koreanLayout: KoreanLayout = .dubeolsik
    @Published var mode: KeyboardMode = .letters
    @Published var shift: ShiftState = .off
    @Published var showsGlobe = false
    /// nil 이면 줄바꿈 기호를 그린다.
    @Published var returnLabel: String?

    var onAction: ((KeyAction) -> Void)?

    var composesHangul: Bool { language == .korean && mode == .letters }

    var rows: [[Key]] {
        KeyboardLayouts.rows(language: language, korean: koreanLayout, mode: mode, showsGlobe: showsGlobe)
    }
}

/// 말랑키 색. 밝은 화면은 크림·분홍, 어두운 화면은 따뜻한 회색.
enum MalangPalette {
    static let background = dynamic(light: 0xF6EEEA, dark: 0x1F1B1D)
    static let key = dynamic(light: 0xFFFFFF, dark: 0x3A3437)
    static let keyPressed = dynamic(light: 0xFFE3E8, dark: 0x5A4A50)
    static let functionKey = dynamic(light: 0xF3D6DC, dark: 0x2C2629)
    static let accent = dynamic(light: 0xF28CA0, dark: 0xE77F95)
    static let text = dynamic(light: 0x3B2F33, dark: 0xF4EAEC)
    static let shadow = dynamic(light: 0xD9C6C2, dark: 0x000000)

    private static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(UIColor { traits in
            let hex = traits.userInterfaceStyle == .dark ? dark : light
            return UIColor(red: CGFloat((hex >> 16) & 0xFF) / 255,
                           green: CGFloat((hex >> 8) & 0xFF) / 255,
                           blue: CGFloat(hex & 0xFF) / 255,
                           alpha: 1)
        })
    }
}

struct KeyboardView: View {
    static let rowHeight: CGFloat = 52
    static let verticalPadding: CGFloat = 6
    static let height: CGFloat = rowHeight * 4 + verticalPadding * 2

    @ObservedObject var model: KeyboardModel

    var body: some View {
        GeometryReader { geo in
            VStack(spacing: 0) {
                ForEach(Array(model.rows.enumerated()), id: \.offset) { _, row in
                    let total = row.reduce(0) { $0 + $1.width }
                    HStack(spacing: 0) {
                        ForEach(Array(row.enumerated()), id: \.offset) { _, key in
                            KeyButton(key: key, model: model)
                                .frame(width: (geo.size.width - 6) * key.width / total, height: Self.rowHeight)
                        }
                    }
                }
            }
            .padding(.horizontal, 3)
            .padding(.vertical, Self.verticalPadding)
        }
        .frame(height: Self.height)
        .background(MalangPalette.background)
    }
}

struct KeyButton: View {
    let key: Key
    @ObservedObject var model: KeyboardModel
    @State private var pressed = false
    @State private var repeatTimer: Timer?

    private var isShifted: Bool { model.shift != .off }

    var body: some View {
        if key.style == .spacer {
            Color.clear
        } else {
            ZStack {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(fill)
                    .shadow(color: MalangPalette.shadow, radius: 0, x: 0, y: pressed ? 0 : 1.5)
                    .padding(.horizontal, 3)
                    .padding(.vertical, 4)
                content
            }
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in
                        guard !pressed else { return }
                        pressed = true
                        touchDown()
                    }
                    .onEnded { _ in
                        pressed = false
                        touchUp()
                    }
            )
        }
    }

    private var fill: Color {
        if pressed { return MalangPalette.keyPressed }
        if key.action == .shift && model.shift != .off { return MalangPalette.accent.opacity(0.35) }
        return key.style == .function ? MalangPalette.functionKey : MalangPalette.key
    }

    @ViewBuilder private var content: some View {
        if let symbol = symbolName {
            Image(systemName: symbol)
                .font(.system(size: 18, weight: .medium))
                .foregroundColor(MalangPalette.text)
        } else {
            Text(labelText)
                .font(.system(size: fontSize, weight: key.style == .function ? .medium : .regular, design: .rounded))
                .foregroundColor(MalangPalette.text)
                .minimumScaleFactor(0.6)
                .lineLimit(1)
        }
    }

    private var symbolName: String? {
        switch key.action {
        case .shift:
            switch model.shift {
            case .off: return "shift"
            case .once: return "shift.fill"
            case .locked: return "capslock.fill"
            }
        case .enter:
            return model.returnLabel == nil ? key.symbol : nil
        default:
            return key.symbol
        }
    }

    private var labelText: String {
        if key.action == .enter, let label = model.returnLabel { return label }
        return key.label(shifted: isShifted)
    }

    private var fontSize: CGFloat {
        if key.style == .function || key.action == .space { return 15 }
        return key.label.count > 1 ? 19 : 23
    }

    /// 지우기는 누르는 순간 지우고, 계속 누르고 있으면 반복한다. 나머지 키는 뗄 때 입력한다.
    private func touchDown() {
        guard key.action == .backspace else { return }
        model.onAction?(.backspace)
        repeatTimer = Timer.scheduledTimer(withTimeInterval: 0.45, repeats: false) { _ in
            repeatTimer = Timer.scheduledTimer(withTimeInterval: 0.07, repeats: true) { _ in
                model.onAction?(.backspace)
            }
        }
    }

    private func touchUp() {
        repeatTimer?.invalidate()
        repeatTimer = nil
        guard key.action != .backspace else { return }
        model.onAction?(key.action(shifted: isShifted))
    }
}
