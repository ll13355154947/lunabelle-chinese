package com.cycletracker.app.ui.lock

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.Icons.Default.Lock
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.Icons.Default.Lock
import androidx.compose.material.Icons.Default.LockOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.cycletracker.app.R
import com.cycletracker.app.core.lock.PasswordManager

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var biometricAvailable by remember { mutableStateOf(false) }
    var useBiometric by remember { mutableStateOf(true) }
    
    // 检查生物识别是否可用
    LaunchedEffect(Unit) {
        activity?.let { act ->
            val biometricManager = BiometricManager.from(act)
            val canAuth = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            biometricAvailable = canAuth == BiometricManager.BIOMETRIC_SUCCESS
            
            // 如果启用了生物识别且可用，自动弹出生物识别
            if (biometricAvailable && PasswordManager.isBiometricEnabled(context)) {
                promptBiometricUnlock(act, onUnlocked) { error ->
                    errorMessage = error
                    useBiometric = false
                }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.Lock, 
            contentDescription = null, 
            modifier = Modifier.height(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.lock_title), 
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.lock_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        
        // 密码输入框
        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it
                errorMessage = null
            },
            label = { Text("输入密码") },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (password.isNotEmpty()) {
                        if (PasswordManager.verifyPassword(context, password)) {
                            onUnlocked()
                        } else {
                            errorMessage = "密码错误"
                        }
                    }
                }
            ),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.LockOff else Icons.Default.Lock,
                        contentDescription = if (showPassword) "隐藏密码" else "显示密码"
                    )
                }
            },
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth()
        )
        
        // 错误信息
        errorMessage?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        // 解锁按钮
        Button(
            onClick = {
                if (password.isNotEmpty()) {
                    if (PasswordManager.verifyPassword(context, password)) {
                        onUnlocked()
                    } else {
                        errorMessage = "密码错误"
                    }
                } else {
                    errorMessage = "请输入密码"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("密码解锁")
        }
        
        // 生物识别按钮（如果可用）
        if (biometricAvailable && PasswordManager.isBiometricEnabled(context)) {
            Spacer(Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = {
                    activity?.let { act ->
                        promptBiometricUnlock(act, onUnlocked) { error ->
                            errorMessage = error
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("指纹/人脸解锁")
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // 跳过按钮（仅在未设置密码时显示）
        if (!PasswordManager.hasPassword(context)) {
            TextButton(onClick = onUnlocked) {
                Text("跳过解锁")
            }
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

private fun promptBiometricUnlock(
    activity: FragmentActivity, 
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val biometricManager = BiometricManager.from(activity)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            when (canAuth) {
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    onError("此设备没有生物识别硬件")
                    return
                }
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    onError("生物识别硬件不可用")
                    return
                }
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    onError("未设置指纹或面部识别")
                    return
                }
                else -> {
                    onError("生物识别不可用")
                    return
                }
            }
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && 
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onError(errString.toString())
                    }
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // 认证失败时不调用 onError，让用户重试
                }
            },
        )
        
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("解锁应用")
            .setSubtitle("使用指纹或面部识别解锁")
            .setNegativeButtonText("使用密码")
            .build()
        
        prompt.authenticate(info)
    } catch (e: Exception) {
        onError("解锁失败: ${e.message}")
    }
}

