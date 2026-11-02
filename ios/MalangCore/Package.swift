// swift-tools-version:5.9
// 키보드 확장과 앱이 함께 쓰는 핵심 코드(한글 조합, 자판 배치, 공유 설정).
// UIKit 에 기대지 않아서 Mac 에서 `swift test` 로 바로 시험할 수 있다.

import PackageDescription

let package = Package(
    name: "MalangCore",
    platforms: [.iOS(.v16), .macOS(.v13)],
    products: [
        .library(name: "MalangCore", targets: ["MalangCore"]),
    ],
    targets: [
        .target(name: "MalangCore"),
        .testTarget(name: "MalangCoreTests", dependencies: ["MalangCore"]),
    ]
)
