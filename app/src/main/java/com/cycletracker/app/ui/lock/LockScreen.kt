package com.cycletracker.app.ui.lock

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.cycletracker.app.R

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    var biometricAvailable by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 检查生物识别是否可用
    LaunchedEffect(Unit) {
        activity?.let { act ->
            val biometricManager = BiometricManager.from(act)
            val canAuth = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            biometricAvailable = canAuth == BiometricManager.BIOMETRIC_SUCCESS
            
            if (biometricAvailable) {
                promptUnlock(act, onUnlocked) { error ->
                    errorMessage = error
                }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.height(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.lock_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        
        if (biometricAvailable) {
            Button(onClick = { 
                activity?.let { act ->
                    promptUnlock(act, onUnlocked) { error ->
                        errorMessage = error
                    }
                }
            }) {
                Text(stringResource(R.string.lock_unlock))
            }
        } else {
            // 如果生物识别不可用，显示跳过按钮
            Text(
                "此设备不支持生物识别或未设置锁屏密码",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onUnlocked) {
                Text("跳过解锁")
            }
        }
        
        // 显示错误信息
        errorMessage?.let { error ->
            Spacer(Modifier.height(16.dp))
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
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

private fun promptUnlock(
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
                    onError("生物识别不可用，错误代码: $canAuth")
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
            .setTitle(activity.getString(R.string.lock_title))
            .setSubtitle(activity.getString(R.string.lock_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()
        
        prompt.authenticate(info)
    } catch (e: Exception) {
        onError("解锁失败: ${e.message}")
    }
}
