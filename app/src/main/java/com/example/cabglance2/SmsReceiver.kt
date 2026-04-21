package com.example.cabglance2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            // Check if we are in SMS sourcing mode
            if (SettingsManager.getSourcingMode(context) != SourcingMode.SMS) {
                return
            }

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (smsMessage in messages) {
                val sender = smsMessage.displayOriginatingAddress ?: ""
                val body = smsMessage.displayMessageBody ?: ""

                Log.d("CabGlanceSMS", "Received SMS from: $sender - $body")

                // SMS Sender Validation (e.g. "XX-MISETS-S")
                if (!sender.contains("MISETS", ignoreCase = true) && !sender.contains("MoveInSync", ignoreCase = true)) {
                    continue
                }

                if (body.contains("MovelnSync", ignoreCase = true) || 
                    body.contains("MoveInSync", ignoreCase = true) || 
                    body.contains("OTP", ignoreCase = true)) {
                    
                    val rideInfo = OtpParser.parseMessage(body)
                    if (rideInfo.type != NotificationType.UNKNOWN) {
                        Log.d("CabGlanceSMS", "Parsed SMS Info: $rideInfo")
                        WidgetUpdateHelper.updateWidgetAndNotification(context, rideInfo)
                    }
                }
            }
        }
    }
}

