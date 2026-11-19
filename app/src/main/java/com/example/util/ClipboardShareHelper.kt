package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

object ClipboardShareHelper {

    fun copyText(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun pasteNumericOrExpression(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val raw = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
            ?: return null
        // Extract valid mathematical characters or clean formatted numbers
        val cleaned = raw.replace(",", "").trim()
        val allowed = cleaned.filter {
            it.isDigit() || it in setOf('.', '+', '-', '*', '/', '×', '÷', '(', ')', '%', '^', 'E', 'e')
        }
        return allowed.ifBlank { null }
    }

    fun shareText(context: Context, subject: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, subject).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
