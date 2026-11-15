import MalangCore
import SwiftUI

enum ShiftState { case off, once, locked }

/// 글자판 대신 보이는 화면
enum KeyboardPanel { case none, emoji, phrases, themes, settings }

/// 키보드 화면 상태. 컨트롤러가 바꾸고 SwiftUI 가 그린다.
final class KeyboardModel: ObservableObject {
    @Published var language: KeyboardLanguage = .korean
    @Published var koreanLayout: KoreanLayout = .dubeolsik
    @Published var mode: KeyboardMode = .letters
    @Published var shift: ShiftState = .off
    @Published var showsGlobe = false
    /// nil 이면 줄바꿈 기호를 그린다.
    @Published var returnLabel: String?
    @Published var themeID = KeyboardTheme.automaticID
    @Published var height: KeyboardHeight = .normal
    @Published var numberHints = true
    @Published var panel: KeyboardPanel = .none
    /// 마지막 단어에 어울리는 이모지. 비어 있으면 툴바에 기능 버튼이 보인다.
    @Published var suggestions: [String] = []
    @Published var phrases: [String] = []
    @Published var recentEmoji: [String] = []

    var sound = true
    var onAction: ((KeyAction) -> Void)?
    /// 키를 누르는 순간 (소리)
    var onKeyDown: (() -> Void)?

    var composesHangul: Bool { language == .korean && mode == .letters }

    var rows: [[Key]] {
        KeyboardLayouts.rows(language: language, korean: koreanLayout, mode: mode, showsGlobe: showsGlobe)
    }

    /// 저장된 설정을 다시 읽는다. 키보드가 열릴 때마다 부른다.
    func reloadSettings() {
        koreanLayout = MalangSettings.koreanLayout
        themeID = MalangSettings.themeID
        height = MalangSettings.height
        numberHints = MalangSettings.numberHints
        sound = MalangSettings.sound
        phrases = MalangSettings.phrases
        recentEmoji = MalangSettings.recentEmoji
    }

    static let toolbarHeight: CGFloat = 40
    static let verticalPadding: CGFloat = 6

    var rowHeight: CGFloat { CGFloat(height.rowHeight) }

    /// 툴바 + 글자판 전체 높이
    var totalHeight: CGFloat { Self.toolbarHeight + rowHeight * 4 + Self.verticalPadding * 2 }

    func theme(dark: Bool) -> KeyboardTheme { KeyboardTheme.resolve(themeID, systemIsDark: dark) }
}

extension Color {
    init(hex: UInt32) {
        self.init(red: Double((hex >> 16) & 0xFF) / 255, green: Double((hex >> 8) & 0xFF) / 255, blue: Double(hex & 0xFF) / 255)
    }
}

/// SwiftUI 에서 쓰기 좋게 바꾼 테마 색
struct ThemeColors {
    let background, key, keyText, keyPressed, function, action, actionText, actionPressed, shadow: Color

    init(_ theme: KeyboardTheme) {
        background = Color(hex: theme.background)
        key = Color(hex: theme.key)
        keyText = Color(hex: theme.keyText)
        keyPressed = Color(hex: theme.keyPressed)
        function = Color(hex: theme.keyPressed)
        action = Color(hex: theme.action)
        actionText = Color(hex: theme.actionText)
        actionPressed = Color(hex: theme.actionPressed)
        shadow = theme.isDark ? Color.black.opacity(0.5) : Color(hex: theme.keyText).opacity(0.18)
    }
}

/// 앱 화면(설정·미리보기)에서 쓰는 말랑키 강조색
enum MalangPalette {
    static let accent = Color(hex: 0x8D6E63)
    static let brown = Color(hex: 0x311D18)
    static let cream = Color(hex: 0xFCF5D6)
    /// 밝은 화면에서는 갈색, 어두운 화면에서는 크림색 (앱 제목)
    static let title = Color(UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 0xFC / 255, green: 0xF5 / 255, blue: 0xD6 / 255, alpha: 1)
            : UIColor(red: 0x31 / 255, green: 0x1D / 255, blue: 0x18 / 255, alpha: 1)
    })
}

struct KeyboardView: View {
    @ObservedObject var model: KeyboardModel
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let colors = ThemeColors(model.theme(dark: colorScheme == .dark))
        VStack(spacing: 0) {
            ToolbarView(model: model, colors: colors)
                .frame(height: KeyboardModel.toolbarHeight)
            Group {
                switch model.panel {
                case .none: KeyGrid(model: model, colors: colors)
                case .emoji: EmojiPanel(model: model, colors: colors)
                case .phrases: PhrasesPanel(model: model, colors: colors)
                case .themes: ThemePanel(model: model, colors: colors)
                case .settings: SettingsPanel(model: model, colors: colors)
                }
            }
            .frame(height: model.totalHeight - KeyboardModel.toolbarHeight)
        }
        .frame(height: model.totalHeight)
        .background(colors.background)
    }
}

/// 글자판
struct KeyGrid: View {
    @ObservedObject var model: KeyboardModel
    let colors: ThemeColors

    var body: some View {
        GeometryReader { geo in
            VStack(spacing: 0) {
                ForEach(Array(model.rows.enumerated()), id: \.offset) { _, row in
                    let total = row.reduce(0) { $0 + $1.width }
                    HStack(spacing: 0) {
                        ForEach(Array(row.enumerated()), id: \.offset) { _, key in
                            KeyButton(key: key, model: model, colors: colors)
                                .frame(width: (geo.size.width - 6) * key.width / total, height: model.rowHeight)
                        }
                    }
                }
            }
            .padding(.horizontal, 3)
            .padding(.vertical, KeyboardModel.verticalPadding)
        }
    }
}

/// 위쪽 줄. 이모지 추천이 있으면 추천을, 없으면 기능 버튼을 보여 준다.
struct ToolbarView: View {
    @ObservedObject var model: KeyboardModel
    let colors: ThemeColors

    var body: some View {
        HStack(spacing: 0) {
            if model.panel == .none && !model.suggestions.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 2) {
                        ForEach(model.suggestions, id: \.self) { emoji in
                            Button { model.onAction?(.commit(emoji)) } label: {
                                Text(emoji).font(.system(size: 24)).frame(width: 40, height: 36)
                            }
                        }
                    }
                    .padding(.horizontal, 6)
                }
                toolButton("xmark", active: false) { model.suggestions = [] }
            } else {
                toolButton("face.smiling", active: model.panel == .emoji) { toggle(.emoji) }
                toolButton("text.bubble", active: model.panel == .phrases) { toggle(.phrases) }
                toolButton("paintpalette", active: model.panel == .themes) { toggle(.themes) }
                toolButton("slider.horizontal.3", active: model.panel == .settings) { toggle(.settings) }
                Spacer(minLength: 0)
                Text(model.panel == .none ? model.koreanLayout.title : "")
                    .font(.system(size: 12, weight: .medium, design: .rounded))
                    .foregroundColor(colors.keyText.opacity(0.5))
                Spacer(minLength: 0)
                toolButton("keyboard.chevron.compact.down", active: false) { model.onAction?(.dismiss) }
            }
        }
        .padding(.horizontal, 4)
    }

    private func toggle(_ panel: KeyboardPanel) {
        model.panel = model.panel == panel ? .none : panel
    }

    private func toolButton(_ symbol: String, active: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 17, weight: .medium))
                .foregroundColor(active ? colors.actionText : colors.keyText.opacity(0.8))
                .frame(width: 42, height: 32)
                .background(RoundedRectangle(cornerRadius: 9, style: .continuous).fill(active ? colors.action : Color.clear))
        }
        .frame(width: 46)
    }
}

struct KeyButton: View {
    let key: Key
    @ObservedObject var model: KeyboardModel
    let colors: ThemeColors
    @State private var pressed = false
    @State private var repeatTimer: Timer?
    @State private var longPressTimer: Timer?
    @State private var longPressed = false

    private var isShifted: Bool { model.shift != .off }
    private var isAction: Bool { key.action == .enter }

    var body: some View {
        if key.style == .spacer {
            Color.clear
        } else {
            ZStack {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(fill)
                    .shadow(color: colors.shadow, radius: 0, x: 0, y: pressed ? 0 : 1.2)
                    .padding(.horizontal, 3)
                    .padding(.vertical, 4)
                content
                if let hint = key.longPress, model.numberHints {
                    Text(hint)
                        .font(.system(size: 10, weight: .medium, design: .rounded))
                        .foregroundColor(longPressed ? colors.action : colors.keyText.opacity(0.45))
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                        .padding(.top, 6)
                        .padding(.trailing, 7)
                }
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
        if isAction { return pressed ? colors.actionPressed : colors.action }
        if key.action == .shift && model.shift != .off { return colors.action.opacity(0.35) }
        if key.style == .function { return pressed ? colors.key : colors.function }
        return pressed ? colors.keyPressed : colors.key
    }

    private var textColor: Color { isAction ? colors.actionText : colors.keyText }

    @ViewBuilder private var content: some View {
        if let symbol = symbolName {
            Image(systemName: symbol)
                .font(.system(size: 18, weight: .medium))
                .foregroundColor(textColor)
        } else {
            Text(labelText)
                .font(.system(size: fontSize, weight: key.style == .function ? .medium : .regular, design: .rounded))
                .foregroundColor(textColor)
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
        // 천지인 아래아 점은 글꼴에서 너무 작게 나와서 굵은 점으로 그린다. (넣는 글자는 그대로 '·')
        if key.label == "·" { return "•" }
        return key.label(shifted: isShifted)
    }

    private var fontSize: CGFloat {
        if key.style == .function || key.action == .space { return 15 }
        if key.label == "·" { return 30 }
        return key.label.count > 1 ? 19 : 23
    }

    /// 지우기는 누르는 순간 지우고, 계속 누르고 있으면 반복한다. 나머지 키는 뗄 때 입력하고,
    /// 길게 누르면 오른쪽 위에 적힌 글자를 넣는다.
    private func touchDown() {
        model.onKeyDown?()
        longPressed = false
        if let alternate = key.longPress {
            longPressTimer = Timer.scheduledTimer(withTimeInterval: 0.4, repeats: false) { _ in
                longPressed = true
                model.onAction?(.commit(alternate))
            }
        }
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
        longPressTimer?.invalidate()
        longPressTimer = nil
        if longPressed {
            longPressed = false
            return
        }
        guard key.action != .backspace else { return }
        model.onAction?(key.action(shifted: isShifted))
    }
}
