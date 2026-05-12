package com.example.hourlychime

import android.app.Activity
import android.os.Bundle
import android.util.Log

class ShortcutActionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val handled = TimeSignalActionHandler.handle(this, intent?.action)
        if (!handled) {
            Log.w(TAG, "未対応アクション: ${intent?.action}")
        }
        finish()
    }

    companion object {
        private const val TAG = "ShortcutActionActivity"
    }
}