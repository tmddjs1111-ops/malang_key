/*
 * Copyright (C) 2026 The MalangKey Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.malangkey.ime.input

/** 키 소리 종류. [label]은 설정 화면에 보이는 이름이다. 순서가 곧 화면의 순서다. */
enum class KeySoundStyle(val label: String) {
    CLICK("기본 딸깍"),
    MALANG("말랑 뽁"),
    TYPEWRITER("타자기"),
    MECHANICAL("기계식 키보드"),
    SOFT("저소음"),
    DROP("물방울"),
    WOOD("나무 블록"),
    MARIMBA("마림바"),
    RETRO("8비트 게임"),
    TICK("가볍게 톡"),
}
