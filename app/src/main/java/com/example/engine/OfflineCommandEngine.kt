package com.example.engine

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.example.data.model.ActionType
import com.example.data.model.CommandExecutionResult
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class OfflineCommandEngine(private val context: Context) {

    private val cameraManager by lazy { context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }
    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    private var isTorchOn = false

    fun processCommand(rawInput: String): CommandExecutionResult {
        val clean = rawInput.trim().lowercase(Locale.ROOT)
            .replace("hey siri", "")
            .replace("siri", "")
            .replace("હે સિરી", "")
            .replace("સિરી", "")
            .trim()

        val input = if (clean.isEmpty()) rawInput.trim().lowercase(Locale.ROOT) else clean

        // 1. Torch / Flashlight
        if (containsAny(input, "torch on", "flashlight on", "turn on flashlight", "turn on torch", "લાઈટ ચાલુ", "ટોર્ચ ચાલુ", "ફ્લેશલાઇટ ચાલુ", "light on")) {
            val success = toggleTorch(true)
            return if (success) {
                CommandExecutionResult(
                    spokenText = if (isGujaratiQuery(input)) "ટોર્ચ ચાલુ કરવામાં આવી છે." else "Flashlight turned on.",
                    actionType = ActionType.FLASHLIGHT,
                    actionBadge = "🔦 Torch ON",
                    isSuccess = true
                )
            } else {
                CommandExecutionResult(
                    spokenText = "Unable to access device flashlight.",
                    actionType = ActionType.FLASHLIGHT,
                    isSuccess = false
                )
            }
        }

        if (containsAny(input, "torch off", "flashlight off", "turn off flashlight", "turn off torch", "લાઈટ બંધ", "ટોર્ચ બંધ", "ફ્લેશલાઇટ બંધ", "light off")) {
            toggleTorch(false)
            return CommandExecutionResult(
                spokenText = if (isGujaratiQuery(input)) "ટોર્ચ બંધ કરી દીધી છે." else "Flashlight turned off.",
                actionType = ActionType.FLASHLIGHT,
                actionBadge = "🔦 Torch OFF",
                isSuccess = true
            )
        }

        // 2. Battery Status
        if (containsAny(input, "battery", "બેટરી", "charge", "ચાર્જિંગ", "ketli che battery", "how much battery")) {
            val batteryLevel = getBatteryLevel()
            val isCharging = isBatteryCharging()
            val chargingStatus = if (isCharging) {
                if (isGujaratiQuery(input)) "અને ફોન ચાર્જ થઈ રહ્યો છે" else "and charging"
            } else {
                if (isGujaratiQuery(input)) "અને ચાર્જિંગમાં નથી" else "and not charging"
            }

            val spoken = if (isGujaratiQuery(input)) {
                "તમારા ફોનમાં બેટરી $batteryLevel% છે, $chargingStatus."
            } else {
                "Your battery level is $batteryLevel%, $chargingStatus."
            }

            return CommandExecutionResult(
                spokenText = spoken,
                actionType = ActionType.BATTERY,
                actionBadge = "🔋 Battery $batteryLevel%",
                payload = "$batteryLevel%"
            )
        }

        // 3. Time
        if (containsAny(input, "time", "સમય", "ketla vagya", "કેટલા વાગ્યા", "વોટ ઇઝ ટાઇમ", "clock")) {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val currentTime = sdf.format(Date())
            val spoken = if (isGujaratiQuery(input)) {
                "અત્યારે સમય $currentTime થયો છે."
            } else {
                "The time is $currentTime."
            }
            return CommandExecutionResult(
                spokenText = spoken,
                actionType = ActionType.TIME_DATE,
                actionBadge = "🕒 $currentTime"
            )
        }

        // 4. Date
        if (containsAny(input, "date", "તારીખ", "tarikh", "કઈ તારીખ", "today's date", "day")) {
            val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
            val currentDate = sdf.format(Date())
            val spoken = if (isGujaratiQuery(input)) {
                "આજની તારીખ $currentDate છે."
            } else {
                "Today is $currentDate."
            }
            return CommandExecutionResult(
                spokenText = spoken,
                actionType = ActionType.TIME_DATE,
                actionBadge = "📅 $currentDate"
            )
        }

        // 5. Volume Control
        if (containsAny(input, "volume up", "increase volume", "અવાજ વધારો", "વોલ્યુમ વધારો", "loud")) {
            adjustVolume(AudioManager.ADJUST_RAISE)
            return CommandExecutionResult(
                spokenText = if (isGujaratiQuery(input)) "વોલ્યુમ વધારવામાં આવ્યું છે." else "Volume increased.",
                actionType = ActionType.VOLUME,
                actionBadge = "🔊 Volume +"
            )
        }

        if (containsAny(input, "volume down", "decrease volume", "અવાજ ઓછો", "વોલ્યુમ ઘટાડો", "quieter")) {
            adjustVolume(AudioManager.ADJUST_LOWER)
            return CommandExecutionResult(
                spokenText = if (isGujaratiQuery(input)) "વોલ્યુમ ઘટાડવામાં આવ્યું છે." else "Volume decreased.",
                actionType = ActionType.VOLUME,
                actionBadge = "🔉 Volume -"
            )
        }

        if (containsAny(input, "mute", "silent", "અવાજ બંધ", "મ્યૂટ કરો", "quiet")) {
            setVolumeMute()
            return CommandExecutionResult(
                spokenText = if (isGujaratiQuery(input)) "વોલ્યુમ મ્યૂટ કર્યું છે." else "Volume muted.",
                actionType = ActionType.VOLUME,
                actionBadge = "🔇 Muted"
            )
        }

        // 6. Timer
        val timerSeconds = extractTimerSeconds(input)
        if (timerSeconds != null && timerSeconds > 0) {
            launchSystemTimer(timerSeconds)
            val minutes = timerSeconds / 60
            val secs = timerSeconds % 60
            val timeDesc = if (minutes > 0 && secs > 0) "$minutes minutes and $secs seconds"
            else if (minutes > 0) "$minutes minutes"
            else "$secs seconds"

            val spoken = if (isGujaratiQuery(input)) {
                "$timeDesc માટે ટાઈમર સેટ કર્યો છે."
            } else {
                "Timer set for $timeDesc."
            }

            return CommandExecutionResult(
                spokenText = spoken,
                actionType = ActionType.TIMER,
                actionBadge = "⏱️ Timer $timeDesc",
                payload = timerSeconds.toString()
            )
        }

        // 7. Math & Calculation
        val mathResult = evaluateMath(input)
        if (mathResult != null) {
            val spoken = if (isGujaratiQuery(input)) {
                "તેનો જવાબ $mathResult છે."
            } else {
                "The answer is $mathResult."
            }
            return CommandExecutionResult(
                spokenText = spoken,
                actionType = ActionType.CALCULATOR,
                actionBadge = "🧮 Result = $mathResult",
                payload = mathResult
            )
        }

        // 8. Open Camera
        if (containsAny(input, "camera", "photo", "કેમેરા", "ફોટો", "take a picture")) {
            val opened = launchCamera()
            return CommandExecutionResult(
                spokenText = if (isGujaratiQuery(input)) "કેમેરા ખોલી રહ્યો છું." else "Opening Camera.",
                actionType = ActionType.OPEN_APP,
                actionBadge = "📷 Camera",
                isSuccess = opened
            )
        }

        // 9. Open Calculator App
        if (containsAny(input, "calculator", "કેલ્ક્યુલેટર")) {
            val opened = launchCalculator()
            return CommandExecutionResult(
                spokenText = if (isGujaratiQuery(input)) "કેલ્ક્યુલેટર ખોલી રહ્યો છું." else "Opening Calculator.",
                actionType = ActionType.OPEN_APP,
                actionBadge = "🧮 Calculator",
                isSuccess = opened
            )
        }

        // 10. Open Settings
        if (containsAny(input, "settings", "સેટિંગ્સ", "સેટિંગ")) {
            launchSettings()
            return CommandExecutionResult(
                spokenText = if (isGujaratiQuery(input)) "સેટિંગ્સ ખોલી રહ્યો છું." else "Opening Settings.",
                actionType = ActionType.OPEN_APP,
                actionBadge = "⚙️ Settings",
                isSuccess = true
            )
        }

        // 11. Open YouTube / WhatsApp / Maps
        if (containsAny(input, "youtube", "યુટ્યુબ")) {
            launchAppByPackage("com.google.android.youtube", "https://youtube.com")
            return CommandExecutionResult(
                spokenText = if (isGujaratiQuery(input)) "યુટ્યુબ ખોલી રહ્યો છું." else "Opening YouTube.",
                actionType = ActionType.OPEN_APP,
                actionBadge = "▶️ YouTube",
                isSuccess = true
            )
        }

        if (containsAny(input, "whatsapp", "વોટ્સએપ")) {
            launchAppByPackage("com.whatsapp", null)
            return CommandExecutionResult(
                spokenText = if (isGujaratiQuery(input)) "વોટ્સએપ ખોલી રહ્યો છું." else "Opening WhatsApp.",
                actionType = ActionType.OPEN_APP,
                actionBadge = "💬 WhatsApp",
                isSuccess = true
            )
        }

        if (containsAny(input, "maps", "map", "મેપ", "નકશો", "location")) {
            launchMaps()
            return CommandExecutionResult(
                spokenText = if (isGujaratiQuery(input)) "મેપ્સ ખોલી રહ્યો છું." else "Opening Maps.",
                actionType = ActionType.OPEN_APP,
                actionBadge = "🗺️ Maps",
                isSuccess = true
            )
        }

        // 12. Phone Call
        val phoneNumber = extractPhoneNumber(input)
        if (phoneNumber != null) {
            launchDialer(phoneNumber)
            val spoken = if (isGujaratiQuery(input)) {
                "$phoneNumber પર કૉલ ડાયલ કરી રહ્યો છું."
            } else {
                "Dialing $phoneNumber."
            }
            return CommandExecutionResult(
                spokenText = spoken,
                actionType = ActionType.CALL,
                actionBadge = "📞 Call $phoneNumber",
                payload = phoneNumber
            )
        }

        // 13. Siri Identity & Persona
        if (containsAny(input, "who are you", "your name", "તમે કોણ છો", "તમારું નામ", "કૌણ છો")) {
            val spoken = if (isGujaratiQuery(input)) {
                "હું સિરી છું, તમારો સ્માર્ટ AI અવાજ સહાયક. હું ઑફલાઇન પણ કામ કરું છું અને સ્ક્રીન બંધ હોય ત્યારે પણ 'હે સિરી' બોલવાથી જાગી જાઉં છું!"
            } else {
                "I am Siri, your smart voice AI assistant. I work completely offline and listen even when your screen is turned off!"
            }
            return CommandExecutionResult(
                spokenText = spoken,
                actionType = ActionType.INFO,
                actionBadge = "✨ Siri Offline AI"
            )
        }

        if (containsAny(input, "how are you", "તમે કેમ છો", "કેમ છો", "kem cho")) {
            val spoken = if (isGujaratiQuery(input)) {
                "હું એકદમ મજામાં છું! તમે કહો, હું તમારી શું મદદ કરી શકું?"
            } else {
                "I'm doing great! How can I help you today?"
            }
            return CommandExecutionResult(
                spokenText = spoken,
                actionType = ActionType.INFO,
                actionBadge = "💬 Conversation"
            )
        }

        if (containsAny(input, "joke", "જોક", "રમુજી", "હસાવો", "funny")) {
            val jokesEn = listOf(
                "Why don't scientists trust atoms? Because they make up everything!",
                "Why did the computer go to the doctor? Because it had a virus!",
                "What did one wall say to the other? I'll meet you at the corner!",
                "Why was 6 afraid of 7? Because 7, 8, 9!"
            )
            val jokesGu = listOf(
                "શિક્ષક: પૃથ્વી ગોળ છે કે ચપટી? વિદ્યાર્થી: સર, પપ્પા કહે છે કે પૃથ્વી પૈસાથી ચાલે છે!",
                "ડૉક્ટર: તમારે દરરોજ ૫ કિલોમીટર ચાલવું પડશે. દર્દી: સાહેબ, હું ૫ દિવસમાં અમદાવાદ પહોંચી જઈશ!",
                "પત્ની: તમે ક્યારેય મારી વાત ધ્યાનથી કેમ નથી સાંભળતા? પતિ: અરે, કંઈ બોલી તું?"
            )
            val joke = if (isGujaratiQuery(input)) jokesGu.random() else jokesEn.random()
            return CommandExecutionResult(
                spokenText = joke,
                actionType = ActionType.JOKE,
                actionBadge = "😄 Joke"
            )
        }

        // 14. Capabilities / Help
        if (containsAny(input, "what can you do", "help", "commands", "તમે શું કરી શકો", "મદદ", "કમાન્ડ")) {
            val spoken = if (isGujaratiQuery(input)) {
                "હું ટોર્ચ ચાલુ-બંધ, બેટરી સ્ટેટસ, સમય-તારીખ, વોલ્યુમ, ટાઈમર, કેલ્ક્યુલેશન, કેમેરા અને એપ્સ ઓપન કરી શકું છું. અને ફોનની સ્ક્રીન બંધ હોય તો પણ 'હે સિરી' થી જવાબ આપું છું!"
            } else {
                "I can toggle flashlight, check battery, tell time and date, set timers, control volume, solve math, open apps, make calls, and listen when screen is off with 'Hey Siri'!"
            }
            return CommandExecutionResult(
                spokenText = spoken,
                actionType = ActionType.INFO,
                actionBadge = "💡 Siri Capabilities"
            )
        }

        // Default Intelligent Fallback
        val defaultSpoken = if (isGujaratiQuery(input)) {
            "મેં તમારો કમાન્ડ સાંભળ્યો: '$input'. હું ઑફલાઇન મોડમાં ટોર્ચ, બેટરી, ટાઇમર, એલાર્મ, કેમેરા કે ગણતરી જેવા કમાન્ડ કરી શકું છું."
        } else {
            "I processed your command: '$input'. In offline mode, I can control flashlight, battery, timers, volume, camera, apps, or answer math questions."
        }

        return CommandExecutionResult(
            spokenText = defaultSpoken,
            actionType = ActionType.INFO,
            actionBadge = "⚡ Offline Intelligence"
        )
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }

    private fun isGujaratiQuery(text: String): Boolean {
        return text.any { it in '\u0A80'..'\u0AFF' } ||
                containsAny(text, "kem cho", "su karo", "tarikh", "vagya", "samay", "karo", "che")
    }

    private fun toggleTorch(enable: Boolean): Boolean {
        val manager = cameraManager ?: return false
        try {
            val cameraId = manager.cameraIdList.firstOrNull { id ->
                val chars = manager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val isBack = chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                hasFlash && isBack
            } ?: manager.cameraIdList.firstOrNull() ?: return false

            manager.setTorchMode(cameraId, enable)
            isTorchOn = enable
            return true
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun getBatteryLevel(): Int {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            85 // reasonable default if emulator lacks battery provider
        }
    }

    private fun isBatteryCharging(): Boolean {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun adjustVolume(direction: Int) {
        try {
            audioManager?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setVolumeMute() {
        try {
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractTimerSeconds(input: String): Int? {
        // match "timer X minutes" / "X minute" / "X seconds" / "૫ મિનિટ"
        val regexMinutes = Pattern.compile("(\\d+)\\s*(min|minute|minutes|મિનિટ|મિનિટો)")
        val matchMin = regexMinutes.matcher(input)
        if (matchMin.find()) {
            val num = matchMin.group(1)?.toIntOrNull() ?: 1
            return num * 60
        }

        val regexSeconds = Pattern.compile("(\\d+)\\s*(sec|second|seconds|સેકન્ડ)")
        val matchSec = regexSeconds.matcher(input)
        if (matchSec.find()) {
            return matchSec.group(1)?.toIntOrNull() ?: 30
        }

        if (input.contains("timer") || input.contains("ટાઈમર")) {
            val digits = Pattern.compile("(\\d+)").matcher(input)
            if (digits.find()) {
                val num = digits.group(1)?.toIntOrNull() ?: 1
                return if (num <= 60) num * 60 else num
            }
        }
        return null
    }

    private fun launchSystemTimer(seconds: Int) {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, "Siri Timer")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractPhoneNumber(input: String): String? {
        if (!input.contains("call") && !input.contains("dial") && !input.contains("કૉલ") && !input.contains("ફોન")) return null
        val matcher = Pattern.compile("(\\+?[0-9]{3,12})").matcher(input)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    private fun launchDialer(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchCamera(): Boolean {
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun launchCalculator(): Boolean {
        val intents = listOf(
            Intent().setClassName("com.google.android.calculator", "com.android.calculator2.Calculator"),
            Intent().setClassName("com.android.calculator2", "com.android.calculator2.Calculator")
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (_: Exception) {}
        }
        return false
    }

    private fun launchSettings() {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchMaps() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=current+location")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchAppByPackage(packageName: String, webFallback: String?) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else if (webFallback != null) {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webFallback)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun evaluateMath(input: String): String? {
        val normalized = input
            .replace("plus", "+")
            .replace("વત્તા", "+")
            .replace("minus", "-")
            .replace("ઓછા", "-")
            .replace("times", "*")
            .replace("multiply", "*")
            .replace("multiplied by", "*")
            .replace("ગુણ્યા", "*")
            .replace("x", "*")
            .replace("divided by", "/")
            .replace("divide", "/")
            .replace("ભાગ્યા", "/")

        val pattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*([+\\-*/])\\s*(\\d+(\\.\\d+)?)")
        val matcher = pattern.matcher(normalized)
        if (matcher.find()) {
            val num1 = matcher.group(1)?.toDoubleOrNull() ?: return null
            val op = matcher.group(3) ?: return null
            val num2 = matcher.group(4)?.toDoubleOrNull() ?: return null

            val result = when (op) {
                "+" -> num1 + num2
                "-" -> num1 - num2
                "*" -> num1 * num2
                "/" -> if (num2 != 0.0) num1 / num2 else return "Cannot divide by zero"
                else -> return null
            }
            return if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                String.format(Locale.ROOT, "%.2f", result)
            }
        }
        return null
    }
}
