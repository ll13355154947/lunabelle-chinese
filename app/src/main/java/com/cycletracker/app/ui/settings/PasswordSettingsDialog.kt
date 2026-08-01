package com.cycletracker.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.cycletracker.app.core.lock.PasswordManager

@Composable
fun PasswordSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var biometricEnabled by remember { mutableStateOf(PasswordManager.isBiometricEnabled(context)) }
    
    val hasPassword = PasswordManager.hasPassword(context)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("密码设置") },
        text = {
            Column {
                // 当前状态
                if (hasPassword) {
                    Text(
                        "已设置密码",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        "未设置密码",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // 如果已设置密码，需要输入当前密码
                if (hasPassword) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it; errorMessage = null },
                        label = { Text("当前密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
                
                // 新密码
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMessage = null },
                    label = { Text(if (hasPassword) "新密码" else "设置密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                
                // 确认密码
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text("确认密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            // 处理密码设置
                            handlePasswordSetup(
                                context, hasPassword, currentPassword, newPassword, confirmPassword,
                                onSuccess = {
                                    successMessage = if (hasPassword) "密码已修改" else "密码已设置"
                                    errorMessage = null
                                    currentPassword = ""
                                    newPassword = ""
                                    confirmPassword = ""
                                },
                                onError = { error ->
                                    errorMessage = error
                                    successMessage = null
                                }
                            )
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(16.dp))
                
                // 生物识别开关
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("启用指纹/人脸解锁")
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { 
                            biometricEnabled = it
                            PasswordManager.setBiometricEnabled(context, it)
                        }
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                // 错误信息
                errorMessage?.let { error ->
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                // 成功信息
                successMessage?.let { success ->
                    Text(
                        success,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 清除密码按钮
                if (hasPassword) {
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            PasswordManager.clearPassword(context)
                            successMessage = "密码已清除"
                            errorMessage = null
                        }
                    ) {
                        Text("清除密码", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    handlePasswordSetup(
                        context, hasPassword, currentPassword, newPassword, confirmPassword,
                        onSuccess = {
                            successMessage = if (hasPassword) "密码已修改" else "密码已设置"
                            errorMessage = null
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                        },
                        onError = { error ->
                            errorMessage = error
                            successMessage = null
                        }
                    )
                }
            ) {
                Text(if (hasPassword) "修改密码" else "设置密码")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

private fun handlePasswordSetup(
    context: android.content.Context,
    hasPassword: Boolean,
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    // 验证当前密码
    if (hasPassword) {
        if (currentPassword.isEmpty()) {
            onError("请输入当前密码")
            return
        }
        if (!PasswordManager.verifyPassword(context, currentPassword)) {
            onError("当前密码错误")
            return
        }
    }
    
    // 验证新密码
    if (newPassword.isEmpty()) {
        onError("请输入新密码")
        return
    }
    if (newPassword.length < 4) {
        onError("密码长度至少4位")
        return
    }
    if (newPassword != confirmPassword) {
        onError("两次输入的密码不一致")
        return
    }
    
    // 设置密码
    if (PasswordManager.setPassword(context, newPassword)) {
        onSuccess()
    } else {
        onError("密码设置失败")
    }
}
