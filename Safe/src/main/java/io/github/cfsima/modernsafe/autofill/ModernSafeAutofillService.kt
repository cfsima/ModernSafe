package io.github.cfsima.modernsafe.autofill

import android.app.PendingIntent
import android.content.Intent
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.util.Log
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import io.github.cfsima.modernsafe.AuthManager
import io.github.cfsima.modernsafe.FrontDoor
import io.github.cfsima.modernsafe.R
import io.github.cfsima.modernsafe.model.PassEntry
import io.github.cfsima.modernsafe.model.Passwords

/**
 * ModernSafe Autofill Service providing credential autofill support for apps and browsers.
 */
class ModernSafeAutofillService : AutofillService() {

    companion object {
        private const val TAG = "ModernSafeAutofill"
        private const val MAX_SUGGESTIONS = 5
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }

        val target = AutofillParser.parseStructure(structure)
        if (target.usernameId == null && target.passwordId == null) {
            callback.onSuccess(null)
            return
        }

        val responseBuilder = FillResponse.Builder()

        // If user is not authenticated, show an unlock prompt dataset
        if (!AuthManager.isSignedIn()) {
            val intent = Intent(this, FrontDoor::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                1001,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val unlockViews = RemoteViews(packageName, R.layout.autofill_item).apply {
                setTextViewText(R.id.autofill_title, getString(R.string.autofill_unlock_title))
                setTextViewText(R.id.autofill_subtitle, getString(R.string.autofill_unlock_subtitle))
            }

            val authDataset = Dataset.Builder(unlockViews)
                .setAuthentication(pendingIntent.intentSender)

            if (target.usernameId != null) {
                authDataset.setValue(target.usernameId, null, unlockViews)
            }
            if (target.passwordId != null) {
                authDataset.setValue(target.passwordId, null, unlockViews)
            }

            responseBuilder.addDataset(authDataset.build())
            callback.onSuccess(responseBuilder.build())
            return
        }

        // Vault is unlocked: fetch credentials
        try {
            Passwords.Initialize(applicationContext)
            val allEntries = Passwords.getPassEntries(0L, decrypt = true, descriptionOnly = false)

            val domain = target.webDomain?.lowercase()
                ?.removePrefix("https://")
                ?.removePrefix("http://")
                ?.removePrefix("www.")
            val targetPackage = target.packageName?.lowercase()

            val matchingEntries = allEntries.filter { entry ->
                val entryDomain = entry.plainWebsite?.lowercase()
                    ?.removePrefix("https://")
                    ?.removePrefix("http://")
                    ?.removePrefix("www.")
                val domainMatch = domain != null && entryDomain != null &&
                        (entryDomain.contains(domain) || domain.contains(entryDomain))

                val packageMatch = targetPackage != null && (
                        entry.packageAccess?.any { it.equals(targetPackage, ignoreCase = true) } == true ||
                                entry.plainDescription.lowercase().contains(targetPackage)
                        )

                val descMatch = (domain != null && entry.plainDescription.lowercase().contains(domain)) ||
                        (targetPackage != null && entry.plainDescription.lowercase().contains(targetPackage.substringAfterLast('.')))

                domainMatch || packageMatch || descMatch
            }.ifEmpty {
                allEntries.take(MAX_SUGGESTIONS)
            }

            if (matchingEntries.isEmpty()) {
                callback.onSuccess(null)
                return
            }

            for (entry in matchingEntries.take(MAX_SUGGESTIONS)) {
                val title = entry.plainDescription.ifBlank {
                    entry.plainUsername ?: getString(R.string.app_name)
                }
                val subtitle = entry.plainUsername ?: ""

                val itemViews = RemoteViews(packageName, R.layout.autofill_item).apply {
                    setTextViewText(R.id.autofill_title, title)
                    setTextViewText(R.id.autofill_subtitle, subtitle)
                }

                val datasetBuilder = Dataset.Builder(itemViews)
                var hasField = false

                if (target.usernameId != null && !entry.plainUsername.isNullOrEmpty()) {
                    datasetBuilder.setValue(
                        target.usernameId,
                        AutofillValue.forText(entry.plainUsername),
                        itemViews
                    )
                    hasField = true
                }

                if (target.passwordId != null && !entry.plainPassword.isNullOrEmpty()) {
                    datasetBuilder.setValue(
                        target.passwordId,
                        AutofillValue.forText(entry.plainPassword),
                        itemViews
                    )
                    hasField = true
                }

                if (hasField) {
                    responseBuilder.addDataset(datasetBuilder.build())
                }
            }

            callback.onSuccess(responseBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Error servicing autofill request", e)
            callback.onSuccess(null)
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }
}
