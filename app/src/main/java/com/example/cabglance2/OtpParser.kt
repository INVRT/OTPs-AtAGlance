package com.example.cabglance2

import java.util.regex.Pattern

enum class NotificationType {
    LOGIN, APPROACHING, LOGOUT, UNKNOWN, IDLE
}

data class RideInfo(
    val type: NotificationType,
    val signInOtp: String? = null,
    val signOutOtp: String? = null,
    val etp: String? = null,
    val cabNo: String? = null,
    val routeNo: String? = null,
    val isApproaching: Boolean = false,
    val rawText: String = ""
)

object OtpParser {
    // Regex for Login / Logout: SignIn/Out OTP:3949/1929
    private val otpPattern = Pattern.compile("OTP:(\\d{4})/(\\d{4})|OTP\\s*(\\d{4}),(\\d{4})")
    private val etpPattern = Pattern.compile("ETP:(\\d{2}:\\d{2})|drop for:(\\d{2}:\\d{2})")
    private val cabNoPattern = Pattern.compile("CabNo:([A-Z0-9-]+)|\\(([A-Z0-9-]+)\\)")
    private val routeNoPattern = Pattern.compile("RouteNo:([A-Za-z0-9\\s]+)[.,]")
    
    fun parseMessage(message: String): RideInfo {
        var type = NotificationType.UNKNOWN
        var signInOtp: String? = null
        var signOutOtp: String? = null
        var etp: String? = null
        var cabNo: String? = null
        var routeNo: String? = null
        var isApproaching = false

        // Determine type based on explicit MoveInSync/Cab keywords
        val lowerMsg = message.lowercase()
        if ((lowerMsg.contains("1.5 km") || lowerMsg.contains("approaching")) && lowerMsg.contains("cab")) {
            type = NotificationType.APPROACHING
            isApproaching = true
        } else if (lowerMsg.contains("shift:") || lowerMsg.contains("etp:")) {
            type = NotificationType.LOGIN
        } else if (lowerMsg.contains("drop for:") || lowerMsg.contains("routeno:")) {
            type = NotificationType.LOGOUT
        } else {
            // Not a cab notification, return UNKNOWN immediately so it's ignored
            return RideInfo(NotificationType.UNKNOWN, rawText = message)
        }

        // Extract OTPs
        val otpMatcher = otpPattern.matcher(message)
        if (otpMatcher.find()) {
            if (otpMatcher.group(1) != null) {
                signInOtp = otpMatcher.group(1)
                signOutOtp = otpMatcher.group(2)
            } else if (otpMatcher.group(3) != null) {
                signInOtp = otpMatcher.group(3)
                signOutOtp = otpMatcher.group(4)
            }
        }

        // Extract ETP
        val etpMatcher = etpPattern.matcher(message)
        if (etpMatcher.find()) {
            etp = etpMatcher.group(1) ?: etpMatcher.group(2)
        }

        // Extract Cab No
        val cabMatcher = cabNoPattern.matcher(message)
        if (cabMatcher.find()) {
            cabNo = cabMatcher.group(1) ?: cabMatcher.group(2)
        }

        // Extract Route No
        val routeMatcher = routeNoPattern.matcher(message)
        if (routeMatcher.find()) {
            routeNo = routeMatcher.group(1)
        }

        return RideInfo(
            type = type,
            signInOtp = signInOtp,
            signOutOtp = signOutOtp,
            etp = etp,
            cabNo = cabNo,
            routeNo = routeNo?.trim(),
            isApproaching = isApproaching,
            rawText = message
        )
    }
}

