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

package dev.malangkey.app.settings.advanced

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import dev.malangkey.BuildConfig
import dev.malangkey.R
import dev.malangkey.app.FlorisPreferenceModel
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.LocalNavController
import dev.malangkey.cacheManager
import dev.malangkey.clipboardManager
import dev.malangkey.ime.clipboard.provider.ClipboardFileStorage
import dev.malangkey.ime.clipboard.provider.ClipboardItem
import dev.malangkey.ime.clipboard.provider.ItemType
import dev.malangkey.lib.cache.CacheManager
import dev.malangkey.app.apptheme.MalangButton
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSettingsSummary
import dev.malangkey.app.apptheme.MalangSettingsTitle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import dev.malangkey.lib.ext.ExtensionManager
import dev.malangkey.lib.io.ZipUtils
import dev.patrickgold.jetpref.datastore.runtime.AndroidAppDataStorage
import dev.patrickgold.jetpref.datastore.runtime.FileBasedStorage
import dev.patrickgold.jetpref.datastore.runtime.ImportStrategy
import dev.patrickgold.jetpref.datastore.ui.Preference
import java.io.FileNotFoundException
import java.text.DateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.florisboard.lib.android.readToFile
import org.florisboard.lib.android.showLongToast
import org.florisboard.lib.android.showLongToastSync
import org.florisboard.lib.compose.FlorisButtonBar
import org.florisboard.lib.compose.FlorisCardDefaults
import org.florisboard.lib.compose.FlorisOutlinedBox
import org.florisboard.lib.compose.FlorisOutlinedButton
import org.florisboard.lib.compose.defaultFlorisOutlinedBox
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.kotlin.io.deleteContentsRecursively
import org.florisboard.lib.kotlin.io.readJson
import org.florisboard.lib.kotlin.io.subDir
import org.florisboard.lib.kotlin.io.subFile

object Restore {
    const val MIN_VERSION_CODE = 64
    const val PACKAGE_NAME = "dev.malangkey"
    const val BACKUP_ARCHIVE_FILE_NAME = "backup.zip"
}

@Composable
fun RestoreScreen() {

    val navController = LocalNavController.current
    val context = LocalContext.current
    val cacheManager by context.cacheManager()

    val restoreFilesSelector = remember { Backup.FilesSelector() }
    var importStrategy by remember { mutableStateOf(ImportStrategy.Merge) }
    // TODO: rememberCoroutineScope() is unusable because it provides the scope in a cancelled state, which does
    //  not make sense at all. I suspect that this is a bug and once it is resolved we can use it here again.
    val restoreScope = remember { CoroutineScope(Dispatchers.Main) }
    var restoreWorkspace by remember {
        mutableStateOf<CacheManager.BackupAndRestoreWorkspace?>(null)
    }

    val restoreDataFromFileSystemLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            runCatching {
                restoreWorkspace?.close()
                restoreWorkspace = null
                val workspace = cacheManager.backupAndRestore.new()
                workspace.zipFile = workspace.inputDir.subFile(Restore.BACKUP_ARCHIVE_FILE_NAME)
                context.contentResolver.readToFile(uri, workspace.zipFile)
                ZipUtils.unzip(workspace.zipFile, workspace.outputDir)
                workspace.metadata = try {
                    workspace.outputDir.subFile(Backup.METADATA_JSON_NAME).readJson()
                } catch (e: FileNotFoundException) {
                    error("Invalid archive: either backup_metadata.json is missing or file is not a ZIP archive.")
                }
                workspace.restoreWarningId = when {
                    workspace.metadata.versionCode != BuildConfig.VERSION_CODE -> {
                        R.string.backup_and_restore__restore__metadata_warn_different_version
                    }
                    !workspace.metadata.packageName.startsWith(Restore.PACKAGE_NAME) -> {
                        R.string.backup_and_restore__restore__metadata_warn_different_vendor
                    }
                    else -> null
                }
                workspace.restoreErrorId = when {
                    workspace.metadata.packageName.isBlank() || workspace.metadata.versionCode < Restore.MIN_VERSION_CODE -> {
                        R.string.backup_and_restore__restore__metadata_error_invalid_metadata
                    }
                    else -> null
                }
                restoreWorkspace = workspace
            }.onFailure { error ->
                context.showLongToastSync(
                    R.string.backup_and_restore__restore__failure,
                    "error_message" to error.localizedMessage,
                )
            }
        },
    )

    suspend fun performRestore() {
        val workspace = restoreWorkspace!!
        val shouldReset = importStrategy == ImportStrategy.Erase
        if (restoreFilesSelector.jetprefDatastore) {
            val file = workspace.outputDir
                .subDir(AndroidAppDataStorage.JETPREF_DIR_NAME)
                .subFile("${FlorisPreferenceModel.NAME}.${AndroidAppDataStorage.JETPREF_FILE_EXT}")
            if (file.exists()) {
                val fileBasedStorage = FileBasedStorage(file.path)
                FlorisPreferenceStore.import(importStrategy, fileBasedStorage).getOrThrow()
            }
        }
        val workspaceFilesDir = workspace.outputDir.subDir("files")
        if (restoreFilesSelector.imeKeyboard) {
            val srcDir = workspaceFilesDir.subDir(ExtensionManager.IME_KEYBOARD_PATH)
            val dstDir = context.filesDir.subDir(ExtensionManager.IME_KEYBOARD_PATH)
            if (shouldReset) {
                dstDir.deleteContentsRecursively()
            }
            if (srcDir.exists()) {
                srcDir.copyRecursively(dstDir, overwrite = true)
            }
        }
        if (restoreFilesSelector.imeTheme) {
            val srcDir = workspaceFilesDir.subDir(ExtensionManager.IME_THEME_PATH)
            val dstDir = context.filesDir.subDir(ExtensionManager.IME_THEME_PATH)
            if (shouldReset) {
                dstDir.deleteContentsRecursively()
            }
            if (srcDir.exists()) {
                srcDir.copyRecursively(dstDir, overwrite = true)
            }
        }
        val clipboardManager = context.clipboardManager().value
        if (shouldReset) {
            clipboardManager.clearFullHistory()
            ClipboardFileStorage.resetClipboardFileStorage(context)
        }

        if (restoreFilesSelector.provideClipboardItems()) {
            val clipboardFilesDir = workspace.outputDir.subDir("clipboard")

            if (restoreFilesSelector.clipboardTextItems) {
                val clipboardItems = clipboardFilesDir.subFile(Backup.CLIPBOARD_TEXT_ITEMS_JSON_NAME)
                if (clipboardItems.exists()) {
                    val clipboardItemsList = clipboardItems.readJson<List<ClipboardItem>>()
                    clipboardManager.restoreHistory(items = clipboardItemsList.filter { it.type == ItemType.TEXT })
                }
            }
            if (restoreFilesSelector.clipboardImageItems) {
                val clipboardItems = clipboardFilesDir.subFile(Backup.CLIPBOARD_IMAGES_JSON_NAME)
                if (clipboardItems.exists()) {
                    val clipboardItemsList = clipboardItems.readJson<List<ClipboardItem>>()
                    for (item in clipboardItemsList.filter { it.type == ItemType.IMAGE }) {
                        ClipboardFileStorage.insertFileFromBackupIfNotExisting(
                            context,
                            clipboardFilesDir.subFile(
                                relPath = "${ClipboardFileStorage.CLIPBOARD_FILES_PATH}/${
                                    item.uri!!.path!!.split(
                                        '/'
                                    ).last()
                                }"
                            )
                        )
                    }
                    clipboardManager.restoreHistory(items = clipboardItemsList.filter { it.type == ItemType.IMAGE })
                }
            }
            if (restoreFilesSelector.clipboardVideoItems) {
                val clipboardItems = clipboardFilesDir.subFile(Backup.CLIPBOARD_VIDEO_JSON_NAME)
                if (clipboardItems.exists()) {
                    val clipboardItemsList = clipboardItems.readJson<List<ClipboardItem>>()
                    for (item in clipboardItemsList.filter { it.type == ItemType.VIDEO }) {
                        ClipboardFileStorage.insertFileFromBackupIfNotExisting(
                            context,
                            clipboardFilesDir.subFile(
                                relPath = "${ClipboardFileStorage.CLIPBOARD_FILES_PATH}/${
                                    item.uri!!.path!!.split(
                                        '/'
                                    ).last()
                                }"
                            )
                        )
                    }
                    clipboardManager.restoreHistory(items = clipboardItemsList.filter { it.type == ItemType.VIDEO })
                }
            }
        }
    }

    MalangSettingsScreen(title = "설정 복원", subtitle = "Restore", showTestInput = false) {
        MalangSettingsSection(
            title = "복원 방식",
            items = listOf(
                {
                    RadioListItem(
                        onClick = { importStrategy = ImportStrategy.Merge },
                        selected = importStrategy == ImportStrategy.Merge,
                        text = "지금 설정과 합치기",
                    )
                },
                {
                    RadioListItem(
                        onClick = { importStrategy = ImportStrategy.Erase },
                        selected = importStrategy == ImportStrategy.Erase,
                        text = "지금 설정을 지우고 덮어쓰기",
                    )
                },
            ),
        )
        MalangButton(
            "백업 파일 선택",
            modifier = Modifier.fillMaxWidth(),
            primary = false,
            onClick = {
                runCatching {
                    restoreDataFromFileSystemLauncher.launch("*/*")
                }.onFailure { error ->
                    context.showLongToastSync(
                        R.string.backup_and_restore__restore__failure,
                        "error_message" to error.localizedMessage,
                    )
                }
            },
        )
        val workspace = restoreWorkspace
        if (workspace == null) {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp),
                text = "선택한 파일이 없어요.",
                color = MalangSettingsSummary,
            )
        } else {
            MalangSettingsSection(title = "선택한 백업 파일", items = listOf({ Column {
                Preference(
                    icon = Icons.Default.Code,
                    title = workspace.metadata.packageName,
                )
                Preference(
                    icon = Icons.Outlined.Info,
                    title = "${workspace.metadata.versionName} (${workspace.metadata.versionCode})",
                )
                Preference(
                    icon = Icons.Default.Schedule,
                    title = remember(workspace.metadata.timestamp) {
                        val formatter = DateFormat.getDateTimeInstance()
                        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        calendar.timeInMillis = workspace.metadata.timestamp
                        formatter.format(calendar.time)
                    },
                )
                if (workspace.restoreErrorId != null) {
                    Column(modifier = Modifier.padding(FlorisCardDefaults.ContentPadding)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .padding(bottom = 8.dp)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.56f))
                        )
                        Text(
                            text = stringRes(workspace.restoreErrorId!!),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                } else if (workspace.restoreWarningId != null) {
                    Column(modifier = Modifier.padding(FlorisCardDefaults.ContentPadding)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .padding(bottom = 8.dp)
                                .background(LocalContentColor.current)
                        )
                        Text(
                            text = stringRes(workspace.restoreWarningId!!),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                }
            } }))
            if (workspace.restoreErrorId == null) {
                BackupFilesSelector(
                    filesSelector = restoreFilesSelector,
                    title = "복원할 항목",
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MalangButton("취소", modifier = Modifier.weight(1f), primary = false) {
                restoreWorkspace?.close()
                navController.navigateUp()
            }
            MalangButton(
                "복원하기",
                modifier = Modifier.weight(2f),
                enabled = restoreWorkspace != null && restoreWorkspace?.restoreErrorId == null,
                onClick = {
                    restoreScope.launch(Dispatchers.Main) {
                        try {
                            performRestore()
                            context.showLongToast(R.string.backup_and_restore__restore__success)
                            navController.navigateUp()
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            context.showLongToast(
                                R.string.backup_and_restore__restore__failure,
                                "error_message" to e.localizedMessage,
                            )
                        }
                    }
                },
            )
        }
    }
}
