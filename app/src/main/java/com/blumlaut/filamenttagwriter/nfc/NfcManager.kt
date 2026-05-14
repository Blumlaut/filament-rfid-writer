package com.blumlaut.filamenttagwriter.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.nfc.tech.Ndef
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Manages NFC foreground dispatch for the app.
 */
class NfcManager(private val context: Context) {

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    val lastTag: MutableState<Tag?> = mutableStateOf(null)
    val nfcAvailable: Boolean get() = adapter != null
    val nfcEnabled: Boolean get() = adapter?.isEnabled == true

    private var foregroundDispatchEnabled = false
    private var pendingIntent: PendingIntent? = null

    fun enableForegroundDispatch(activity: Activity, activityClass: Class<*> = activity.javaClass) {
        val nfcAdapter = adapter ?: return
        if (!nfcAdapter.isEnabled || foregroundDispatchEnabled) return

        val intent = Intent(context, activityClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val techFilters = arrayOf(
            arrayOf(Ndef::class.java.name, MifareUltralight::class.java.name),
            arrayOf(NfcA::class.java.name),
            arrayOf(NfcB::class.java.name),
            arrayOf(NfcF::class.java.name),
            arrayOf(NfcV::class.java.name),
            arrayOf(Tag::class.java.name)
        )

        val intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("NEW_API")
            nfcAdapter.enableForegroundDispatch(activity, pendingIntent, intentFilters, techFilters)
        } else {
            @Suppress("DEPRECATION")
            nfcAdapter.enableForegroundDispatch(activity, pendingIntent, intentFilters, techFilters)
        }
        foregroundDispatchEnabled = true
    }

    fun disableForegroundDispatch(activity: Activity) {
        val nfcAdapter = adapter ?: return
        if (!foregroundDispatchEnabled) return
        try { pendingIntent?.cancel() } catch (_: Exception) {}
        try { nfcAdapter.disableForegroundDispatch(activity) } catch (_: Exception) {}
        foregroundDispatchEnabled = false
        pendingIntent = null
    }

    fun processNfcIntent(intent: Intent) {
        if (intent.action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED
        ) {
            @Suppress("DEPRECATION")
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            lastTag.value = tag
        }
    }

    fun clearLastTag() {
        lastTag.value = null
    }
}

