/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package dev.malangkey.app

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import dev.malangkey.app.ext.ExtensionImportScreen
import dev.malangkey.app.ext.ExtensionImportScreenType
import dev.malangkey.app.settings.HomeScreen
import dev.malangkey.app.settings.about.AboutScreen
import dev.malangkey.app.settings.about.ProjectLicenseScreen
import dev.malangkey.app.settings.about.ThirdPartyLicensesScreen
import dev.malangkey.app.settings.advanced.BackupScreen
import dev.malangkey.app.settings.advanced.RestoreScreen
import dev.malangkey.app.settings.clipboard.ClipboardScreen
import dev.malangkey.app.settings.dictionary.DictionaryScreen
import dev.malangkey.app.settings.dictionary.UserDictionaryScreen
import dev.malangkey.app.settings.dictionary.UserDictionaryType
import dev.malangkey.app.settings.gestures.GesturesScreen
import dev.malangkey.app.settings.keyboard.InputFeedbackScreen
import dev.malangkey.app.settings.keyboard.KeyboardScreen
import dev.malangkey.app.settings.keyboard.KeyboardOrderScreen
import dev.malangkey.app.settings.keyboard.KeyboardLayoutScreen
import dev.malangkey.app.settings.keyboard.KeyboardSelectionScreen
import dev.malangkey.app.settings.keyboard.KeyboardTestScreen
import dev.malangkey.app.settings.media.MediaScreen
import dev.malangkey.app.settings.smartbar.SmartbarScreen
import dev.malangkey.app.settings.theme.ThemeScreen
import dev.malangkey.app.settings.typing.TypingScreen
import dev.malangkey.app.game.AnthemGameScreen
import dev.malangkey.app.setup.QuickSetupScreen
import dev.malangkey.app.setup.SetupScreen
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Deeplink(val path: String)

inline fun <reified T : Any> NavGraphBuilder.composableWithDeepLink(
    kClass: KClass<T>,
    noinline content: @Composable (AnimatedContentScope.(NavBackStackEntry) -> Unit),
) {
    val deeplink = requireNotNull(kClass.annotations.firstOrNull { it is Deeplink } as? Deeplink) {
        "faulty class: $kClass with annotations ${kClass.annotations}"
    }
    composable<T>(
        deepLinks = listOf(navDeepLink<T>(basePath = "ui://malangkey/${deeplink.path}")),
        content = content,
    )
}

object Routes {
    object Game {
        /** 애국가 빨리치기. [full]이면 1~4절 전체, 아니면 1절. */
        @Serializable
        data class Anthem(val full: Boolean = false)
    }

    object Setup {
        @Serializable
        object Screen

        @Serializable
        object Quick
    }

    object Settings {
        @Serializable
        @Deeplink("settings/home")
        object Home

        @Serializable
        @Deeplink("settings/theme")
        object Theme

        @Serializable
        @Deeplink("settings/keyboard")
        object Keyboard

        @Serializable
        @Deeplink("settings/keyboard/selection")
        object KeyboardSelection

        @Serializable
        @Deeplink("settings/keyboard/order")
        object KeyboardOrder

        @Serializable
        @Deeplink("settings/keyboard/layout")
        object KeyboardLayout

        @Serializable
        @Deeplink("settings/keyboard/test")
        object KeyboardTest

        @Serializable
        @Deeplink("settings/keyboard/input-feedback")
        object InputFeedback

        @Serializable
        @Deeplink("settings/smartbar")
        object Smartbar

        @Serializable
        @Deeplink("settings/typing")
        object Typing

        @Serializable
        @Deeplink("settings/dictionary")
        object Dictionary

        @Serializable
        @Deeplink("settings/dictionary/user-dictionary")
        data class UserDictionary(val type: UserDictionaryType)

        @Serializable
        @Deeplink("settings/gestures")
        object Gestures

        @Serializable
        @Deeplink("settings/clipboard")
        object Clipboard

        @Serializable
        @Deeplink("settings/media")
        object Media

        @Serializable
        @Deeplink("settings/other/backup")
        object Backup

        @Serializable
        @Deeplink("settings/other/restore")
        object Restore

        @Serializable
        @Deeplink("settings/about")
        object About

        @Serializable
        @Deeplink("settings/about/project-license")
        object ProjectLicense

        @Serializable
        @Deeplink("settings/about/third-party-licenses")
        object ThirdPartyLicenses
    }

    object Ext {
        @Serializable
        @Deeplink("ext/import")
        data class Import(val type: ExtensionImportScreenType, val uuid: String? = null)
    }

    @Composable
    fun AppNavHost(
        modifier: Modifier,
        navController: NavHostController,
        startDestination: KClass<*>,
    ) {
        NavHost(
            modifier = modifier,
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                slideIn { IntOffset(it.width, 0) } + fadeIn()
            },
            exitTransition = {
                slideOut { IntOffset(-it.width, 0) } + fadeOut()
            },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = {
                scaleOut(
                    targetScale = 0.85F,
                    transformOrigin = TransformOrigin(pivotFractionX = 0.8f, pivotFractionY = 0.5f)
                ) + fadeOut(spring(stiffness = Spring.StiffnessMedium))
            },
        ) {
            composable<Setup.Screen> { SetupScreen() }
            composable<Setup.Quick> { QuickSetupScreen() }
            composable<Game.Anthem> { entry ->
                AnthemGameScreen(full = entry.toRoute<Game.Anthem>().full)
            }

            composableWithDeepLink(Settings.Home::class) { HomeScreen() }

            composableWithDeepLink(Settings.Theme::class) { ThemeScreen() }

            composableWithDeepLink(Settings.Keyboard::class) { KeyboardScreen() }
            composableWithDeepLink(Settings.KeyboardSelection::class) { KeyboardSelectionScreen() }
            composableWithDeepLink(Settings.KeyboardOrder::class) { KeyboardOrderScreen() }
            composableWithDeepLink(Settings.KeyboardLayout::class) { KeyboardLayoutScreen() }
            composableWithDeepLink(Settings.KeyboardTest::class) { KeyboardTestScreen() }
            composableWithDeepLink(Settings.InputFeedback::class) { InputFeedbackScreen() }

            composableWithDeepLink(Settings.Smartbar::class) { SmartbarScreen() }

            composableWithDeepLink(Settings.Typing::class) { TypingScreen() }

            composableWithDeepLink(Settings.Dictionary::class) { DictionaryScreen() }
            composableWithDeepLink(Settings.UserDictionary::class) { navBackStack ->
                val payload = navBackStack.toRoute<Settings.UserDictionary>()
                UserDictionaryScreen(payload.type)
            }

            composableWithDeepLink(Settings.Gestures::class) { GesturesScreen() }

            composableWithDeepLink(Settings.Clipboard::class) { ClipboardScreen() }

            composableWithDeepLink(Settings.Media::class) { MediaScreen() }

            composableWithDeepLink(Settings.Backup::class) { BackupScreen() }
            composableWithDeepLink(Settings.Restore::class) { RestoreScreen() }

            composableWithDeepLink(Settings.About::class) { AboutScreen() }
            composableWithDeepLink(Settings.ProjectLicense::class) { ProjectLicenseScreen() }
            composableWithDeepLink(Settings.ThirdPartyLicenses::class) { ThirdPartyLicensesScreen() }

            composableWithDeepLink(Ext.Import::class) { navBackStack ->
                val payload = navBackStack.toRoute<Ext.Import>()
                val uuid = payload.uuid
                ExtensionImportScreen(payload.type, uuid)
            }
        }
    }
}
