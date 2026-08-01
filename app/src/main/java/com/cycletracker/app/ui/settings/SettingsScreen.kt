package com.cycletracker.app.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycletracker.app.R
import com.cycletracker.app.core.lock.PasswordManager
import com.cycletracker.app.core.locale.LocaleManager
import com.cycletracker.app.data.backup.ImportResult
import com.cycletracker.app.domain.model.ReminderPhase
import com.cycletracker.app.domain.model.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val s = settings ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPasswordSettings by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val json = viewModel.exportJson(System.currentTimeMillis())
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            Toast.makeText(context, R.string.export_done, Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            val result = content?.let { viewModel.importJson(it) }
            val msg = when (result) {
                is ImportResult.Success -> R.string.import_done
                ImportResult.UnsupportedVersion -> R.string.import_unsupported
                else -> R.string.import_failed
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 外观设置
        SectionHeader(stringResource(R.string.settings_appearance))
        
        // 主题选择
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = s.themeMode == ThemeMode.SYSTEM,
                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                label = { Text(stringResource(R.string.theme_system)) }
            )
            FilterChip(
                selected = s.themeMode == ThemeMode.LIGHT,
                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                label = { Text(stringResource(R.string.theme_light)) }
            )
            FilterChip(
                selected = s.themeMode == ThemeMode.DARK,
                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                label = { Text(stringResource(R.string.theme_dark)) }
            )
        }

        // 动态颜色
        ToggleRow(stringResource(R.string.settings_dynamic_color), s.dynamicColor, viewModel::setDynamicColor)

        // 隐私设置
        SectionHeader(stringResource(R.string.settings_privacy))

        // 密码设置按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPasswordSettings = true }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "密码设置",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                if (PasswordManager.hasPassword(context)) "已设置" else "未设置",
                style = MaterialTheme.typography.bodyMedium,
                color = if (PasswordManager.hasPassword(context)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ToggleRow(stringResource(R.string.settings_app_lock), s.appLockEnabled, viewModel::setAppLock)

        // 数据设置
        SectionHeader(stringResource(R.string.settings_data))
        OutlinedButton(onClick = { exportLauncher.launch("cycle-backup.json") }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_export))
        }
        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_import))
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(
            stringResource(R.string.settings_about),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenAbout() }
                .padding(vertical = 8.dp),
        )
    }

    // 密码设置对话框
    if (showPasswordSettings) {
        PasswordSettingsDialog(
            onDismiss = { showPasswordSettings = false }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp)
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
