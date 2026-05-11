package com.example.hourlychime

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

/**
 * Galaxy Routine の「アプリの操作を実行」機能でクイックアクション選択肢を定義するActivity。
 * ユーザーが「ルーチン」→「アプリの操作を実行」を選ぶ際に、この中から選択可能になる。
 */
class QuickActionActivity : Activity() {
    companion object {
        private const val TAG = "QuickActionActivity"

        const val ACTION_TOGGLE_TIME_SIGNAL = "com.example.hourlychime.ACTION_TOGGLE_TIME_SIGNAL"
        const val ACTION_TURN_ON = "com.example.hourlychime.ACTION_TURN_ON"
        const val ACTION_TURN_OFF = "com.example.hourlychime.ACTION_TURN_OFF"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        val action = intent?.action
        Log.d(TAG, "onCreate: action=$action")

        // CREATE_SHORTCUT（標準的なショートカット作成）に対応
        if (Intent.ACTION_CREATE_SHORTCUT == action) {
            handleCreateShortcut()
        }
        // Galaxy Routine 用 DEFINE_QUICKACTION アクション（Samsung固有）
        else if ("android.app.action.DEFINE_QUICKACTION" == action) {
            handleDefineQuickAction()
        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun handleCreateShortcut() {
        // ショートカット作成時に返すIntent
        val resultIntent = Intent()

        // 複数のアクション（ショートカット）を定義可能
        // ここではトグル操作を提供
        val shortcutNames =
            arrayOf(
                "時報ON/OFF 切り替え",
                "時報 ON",
                "時報 OFF",
            )
        val shortcutActions =
            arrayOf(
                ACTION_TOGGLE_TIME_SIGNAL,
                ACTION_TURN_ON,
                ACTION_TURN_OFF,
            )

        // 最初のアクションをデフォルトとして返す
        val shortcutIntent = Intent(this, QuickActionService::class.java).apply {
            action = shortcutActions[0]
        }

        resultIntent.apply {
            putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutNames[0])
            // Intentはシリアライズ可能なので Serializable として putExtra
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent as java.io.Serializable)
        }

        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun handleDefineQuickAction() {
        // Galaxy Routineの場合、クイックアクション一覧を定義して返す
        val actionName = "時報ON/OFF 切り替え"
        val shortcutIntent = Intent(this, QuickActionService::class.java).apply {
            action = ACTION_TOGGLE_TIME_SIGNAL
        }

        // 最初のアクションを返す（選択肢は Galaxy側で表示される）
        val resultIntent = Intent()
        resultIntent.putExtra("action", actionName)
        resultIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent as java.io.Serializable)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}

