package com.cycletracker.app.core.lock

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

/**
 * 密码管理器 - 处理密码设置和验证
 */
object PasswordManager {
    private const val PREFS_NAME = "app_lock_prefs"
    private const val KEY_PASSWORD_HASH = "password_hash"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_LOCK_ENABLED = "lock_enabled"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 检查是否设置了密码
     */
    fun hasPassword(context: Context): Boolean {
        return getPrefs(context).contains(KEY_PASSWORD_HASH)
    }
    
    /**
     * 设置密码
     */
    fun setPassword(context: Context, password: String): Boolean {
        if (password.length < 4) return false
        
        val hash = hashPassword(password)
        getPrefs(context).edit()
            .putString(KEY_PASSWORD_HASH, hash)
            .putBoolean(KEY_LOCK_ENABLED, true)
            .apply()
        return true
    }
    
    /**
     * 验证密码
     */
    fun verifyPassword(context: Context, password: String): Boolean {
        val storedHash = getPrefs(context).getString(KEY_PASSWORD_HASH, null) ?: return false
        val inputHash = hashPassword(password)
        return storedHash == inputHash
    }
    
    /**
     * 清除密码
     */
    fun clearPassword(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_PASSWORD_HASH)
            .putBoolean(KEY_LOCK_ENABLED, false)
            .apply()
    }
    
    /**
     * 检查是否启用了锁屏
     */
    fun isLockEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_LOCK_ENABLED, false)
    }
    
    /**
     * 启用/禁用生物识别
     */
    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
    }
    
    /**
     * 检查是否启用了生物识别
     */
    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }
    
    /**
     * 密码哈希
     */
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
