package io.github.cfsima.modernsafe.autofill

import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.view.View
import android.view.autofill.AutofillId

data class AutofillTarget(
    val usernameId: AutofillId? = null,
    val passwordId: AutofillId? = null,
    val webDomain: String? = null,
    val packageName: String? = null
)

object AutofillParser {

    fun parseStructure(structure: AssistStructure): AutofillTarget {
        val packageName = structure.activityComponent?.packageName
        var usernameId: AutofillId? = null
        var passwordId: AutofillId? = null
        var webDomain: String? = null

        fun traverse(node: ViewNode) {
            if (node.webDomain != null && webDomain == null) {
                webDomain = node.webDomain
            }

            val hints = node.autofillHints?.map { it.lowercase() } ?: emptyList()
            val idEntry = node.idEntry?.lowercase() ?: ""
            val hint = node.hint?.toString()?.lowercase() ?: ""

            val isPassword = hints.contains(View.AUTOFILL_HINT_PASSWORD) ||
                    idEntry.contains("password") || idEntry.contains("pwd") || idEntry.contains("pass") ||
                    hint.contains("password")

            val isUsername = hints.contains(View.AUTOFILL_HINT_USERNAME) ||
                    hints.contains(View.AUTOFILL_HINT_EMAIL_ADDRESS) ||
                    idEntry.contains("user") || idEntry.contains("login") || idEntry.contains("email") || idEntry.contains("account") ||
                    hint.contains("username") || hint.contains("email") || hint.contains("user")

            if (isPassword && passwordId == null && node.autofillId != null) {
                passwordId = node.autofillId
            } else if (isUsername && usernameId == null && node.autofillId != null) {
                usernameId = node.autofillId
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChildAt(i))
            }
        }

        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            traverse(windowNode.rootViewNode)
        }

        return AutofillTarget(
            usernameId = usernameId,
            passwordId = passwordId,
            webDomain = webDomain,
            packageName = packageName
        )
    }
}
