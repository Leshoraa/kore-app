package com.leshoraa.kore.data.parser

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.leshoraa.kore.domain.model.NavEvent
import java.util.regex.Pattern

/**
 * Data-driven turn-by-turn navigation extractor for supported navigation packages.
 *
 * Utilizes precompiled regular expression registries and declarative maneuver rules
 * to parse direction icons, step distances, ETA, duration, and road targets.
 */
class MapsNavigationParser {

    companion object {
        private val SUPPORTED_PACKAGES = setOf(
            "com.google.android.apps.maps",
            "com.google.android.apps.mapslite",
            "com.waze",
            "com.here.app.maps"
        )

        private val DISTANCE_REGEX = Regex(
            """\b(\d+(?:[.,]\d+)?)\s*(km|meter|kilometer|mi|miles?|ft|feet|yd|m)(?!\s*nt|\s*enit|\s*in)\b""",
            RegexOption.IGNORE_CASE
        )

        private val ETA_REGEX = Regex(
            """(?:waktu tiba|tiba|eta)?\s*[:\s]?\s*\b(\d{1,2}[:.]\d{2}(?:\s*(?:AM|PM|am|pm))?)\b""",
            RegexOption.IGNORE_CASE
        )

        private val DURATION_REGEX = Regex(
            """\b(\d+\s*(?:jam|hr|hrs|hours?)\s*\d+\s*(?:mnt|min|mins|menit)|\d+\s*(?:mnt|min|mins|menit|jam|hr|hrs))\b""",
            RegexOption.IGNORE_CASE
        )

        private val TERMINATION_PATTERNS = listOf(
            Regex("""\b(?:telah\s*tiba\s*di\s*tujuan|anda\s*telah\s*tiba|telah\s*sampai\s*di\s*tujuan|you\s*have\s*arrived|rute\s*selesai|perjalanan\s*selesai|navigation\s*finished)\b""", RegexOption.IGNORE_CASE)
        )

        private val STREET_PREFIX_PATTERNS = listOf(
            Regex("""\b(?:Jl\.|Jalan|Gg\.|Gang)\s*([^·|•\n\r]+)""", RegexOption.IGNORE_CASE),
            Regex("""\b(?:ke\s*arah|onto|toward|ke|di|on)\s*([^·|•\n\r]+)""", RegexOption.IGNORE_CASE)
        )

        private val DIRECTIONAL_TERMS = setOf("utara", "selatan", "barat", "timur", "north", "south", "east", "west")
    }

    /**
     * Declarative maneuver taxonomy mapping regex patterns to HUD icon identifiers.
     */
    private enum class ManeuverRule(val icon: String, val pattern: Regex) {
        U_TURN(
            "uturn",
            Regex("""\b(?:putar\s*balik|u-?turn|make\s*a\s*u-?turn)\b""", RegexOption.IGNORE_CASE)
        ),
        SHARP_RIGHT(
            "sharp_right",
            Regex("""\b(?:belok\s*tajam\s*(?:ke\s*)?kanan|sharp\s*right)\b""", RegexOption.IGNORE_CASE)
        ),
        SHARP_LEFT(
            "sharp_left",
            Regex("""\b(?:belok\s*tajam\s*(?:ke\s*)?kiri|sharp\s*left)\b""", RegexOption.IGNORE_CASE)
        ),
        SLIGHT_RIGHT(
            "slight_right",
            Regex("""\b(?:serong\s*kanan|agak\s*(?:ke\s*)?kanan|sedikit\s*(?:ke\s*)?kanan|slight\s*right|bear\s*right|keep\s*right|tetap\s*di\s*kanan)\b""", RegexOption.IGNORE_CASE)
        ),
        SLIGHT_LEFT(
            "slight_left",
            Regex("""\b(?:serong\s*kiri|agak\s*(?:ke\s*)?kiri|sedikit\s*(?:ke\s*)?kiri|slight\s*left|bear\s*left|keep\s*left|tetap\s*di\s*kiri)\b""", RegexOption.IGNORE_CASE)
        ),
        ROUNDABOUT(
            "roundabout",
            Regex("""\b(?:bundaran|roundabout|keluar\s*bundaran|ambil\s*jalan\s*keluar)\b""", RegexOption.IGNORE_CASE)
        ),
        TURN_RIGHT(
            "turn_right",
            Regex("""\b(?:belok\s*kanan|(?:kemudian\s*)?ke\s*kanan|arah\s*kanan|turn\s*right|right\s*onto)\b""", RegexOption.IGNORE_CASE)
        ),
        TURN_LEFT(
            "turn_left",
            Regex("""\b(?:belok\s*kiri|(?:kemudian\s*)?ke\s*kiri|arah\s*kiri|turn\s*left|left\s*onto)\b""", RegexOption.IGNORE_CASE)
        ),
        ARRIVE(
            "arrive",
            Regex("""\b(?:telah\s*tiba|telah\s*sampai|you\s*have\s*arrived|arrived\s*at\s*destination)\b""", RegexOption.IGNORE_CASE)
        );

        fun matches(input: String): Boolean = pattern.containsMatchIn(input)
    }

    /**
     * Checks if the source notification package is supported.
     */
    fun isNavigationNotification(sbn: StatusBarNotification): Boolean {
        return sbn.packageName in SUPPORTED_PACKAGES
    }

    /**
     * Parses an active navigation StatusBarNotification into a domain NavEvent.
     */
    fun parse(sbn: StatusBarNotification): NavEvent? {
        if (!isNavigationNotification(sbn)) return null

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
        val ticker = sbn.notification.tickerText?.toString()?.trim().orEmpty()

        val combined = "$title $text $bigText $subText $ticker"

        if (isNavigationEnded(combined)) {
            return NavEvent(
                icon = "arrive",
                distance = "0 m",
                instruction = sanitizeText(title),
                street = extractStreet(title, text),
                isActive = false
            )
        }

        val (turnDistance, totalDistance) = extractDistances(title, text, subText, bigText)
        val eta = extractEta(subText, text, bigText, title)
        val duration = extractDuration(subText, text, bigText, title)
        val icon = classifyIcon(combined)
        val instruction = extractInstruction(title, text)
        val street = extractStreet(title, text)

        return NavEvent(
            icon = icon,
            distance = turnDistance,
            instruction = instruction,
            street = street,
            eta = eta,
            duration = duration,
            totalDistance = totalDistance,
            isActive = true
        )
    }

    /**
     * Parses raw strings directly for decoupled testing.
     */
    fun parseRaw(title: String, text: String, subText: String = ""): NavEvent {
        val combined = "$title $text $subText"

        if (isNavigationEnded(combined)) {
            return NavEvent(
                icon = "arrive",
                distance = "0 m",
                instruction = sanitizeText(title),
                street = extractStreet(title, text),
                isActive = false
            )
        }

        val (turnDistance, totalDistance) = extractDistances(title, text, subText)
        val eta = extractEta(subText, text, title)
        val duration = extractDuration(subText, text, title)
        val icon = classifyIcon(combined)
        val instruction = extractInstruction(title, text)
        val street = extractStreet(title, text)

        return NavEvent(
            icon = icon,
            distance = turnDistance,
            instruction = instruction,
            street = street,
            eta = eta,
            duration = duration,
            totalDistance = totalDistance,
            isActive = true
        )
    }

    private fun isNavigationEnded(text: String): Boolean {
        return TERMINATION_PATTERNS.any { it.containsMatchIn(text) }
    }

    private fun classifyIcon(text: String): String {
        return ManeuverRule.entries.firstOrNull { it.matches(text) }?.icon ?: "straight"
    }

    private fun extractInstruction(title: String, text: String): String {
        val candidate = when {
            title.isNotBlank() && !DISTANCE_REGEX.matches(title) -> title
            text.isNotBlank() && !DISTANCE_REGEX.matches(text) -> text
            else -> ""
        }
        return sanitizeText(candidate)
    }

    private fun extractDistances(vararg sources: String): Pair<String, String> {
        val matches = mutableListOf<String>()
        for (src in sources) {
            if (src.isBlank()) continue
            DISTANCE_REGEX.findAll(src).forEach { match ->
                val num = match.groupValues.getOrNull(1)?.trim().orEmpty()
                val unit = match.groupValues.getOrNull(2)?.trim()?.lowercase().orEmpty()
                val standardizedUnit = when {
                    unit.startsWith("km") || unit.startsWith("kilo") -> "km"
                    unit.startsWith("mi") -> "mi"
                    unit.startsWith("ft") || unit.startsWith("feet") -> "ft"
                    unit.startsWith("yd") -> "yd"
                    else -> "m"
                }
                val formatted = "$num $standardizedUnit"
                if (formatted !in matches) {
                    matches.add(formatted)
                }
            }
        }

        return when {
            matches.size >= 2 -> matches[0] to matches[1]
            matches.size == 1 -> matches[0] to ""
            else -> "" to ""
        }
    }

    private fun extractEta(vararg sources: String): String {
        for (src in sources) {
            if (src.isBlank()) continue
            val match = ETA_REGEX.find(src)
            if (match != null) {
                val groupVal = match.groupValues.getOrNull(1)?.trim().orEmpty()
                if (groupVal.isNotEmpty()) return groupVal
            }
        }
        return ""
    }

    private fun extractDuration(vararg sources: String): String {
        for (src in sources) {
            if (src.isBlank()) continue
            val match = DURATION_REGEX.find(src)
            if (match != null) {
                val groupVal = match.groupValues.getOrNull(1)?.trim().orEmpty()
                if (groupVal.isNotEmpty()) return groupVal
            }
        }
        return ""
    }

    private fun extractStreet(title: String, text: String): String {
        for (candidate in listOf(title, text)) {
            if (candidate.isBlank()) continue
            for (pattern in STREET_PREFIX_PATTERNS) {
                val match = pattern.find(candidate)
                if (match != null) {
                    val rawTarget = match.groupValues.getOrNull(1)?.trim().orEmpty()
                    val firstWord = rawTarget.split(" ").firstOrNull()?.lowercase().orEmpty()
                    if (rawTarget.isNotBlank() && firstWord !in DIRECTIONAL_TERMS) {
                        return sanitizeText(rawTarget)
                    }
                }
            }
        }

        val cleaned = DISTANCE_REGEX.replace(title, "").replace("·", "").replace("-", "").trim()
        if (cleaned.length > 2 && !cleaned.startsWith("Belok", ignoreCase = true) && !cleaned.startsWith("Turn", ignoreCase = true)) {
            return sanitizeText(cleaned)
        }

        return ""
    }

    private fun sanitizeText(raw: String): String {
        var res = raw.trim()
        val sepIdx = res.indexOfAny(charArrayOf('·', '|', '•', '-'))
        if (sepIdx > 0) {
            res = res.substring(0, sepIdx).trim()
        }
        return if (res.length > 24) res.substring(0, 24).trim() else res
    }
}
