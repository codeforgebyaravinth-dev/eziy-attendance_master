package `in`.eziy.attendancemaster.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import `in`.eziy.attendancemaster.data.local.SecurePreferencesManager
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject
import java.util.UUID

class EziyMqttService : Service() {

    private var client: MqttClient? = null
    private val channelId = "eziy_attendance_notifications"

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                channelId,
                "Eziy Attendance Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
        )

        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Eziy Attendance Master")
                .setContentText("Attendance notification listener active")
                .setOngoing(true)
                .build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        connectMqtt()
        return START_STICKY
    }

    private fun connectMqtt() {
        val prefs = SecurePreferencesManager(this)
        val badge = prefs.employeeId

        if (badge.length != 8) return

        Thread {
            try {
                client?.disconnectForcibly()
                val clientId = "eziy-sub-${badge}-${UUID.randomUUID().toString().take(8)}"
                val mqttClient = MqttClient("tcp://mqtt.eziy.in:1883", clientId, null)

                // Match Odoo backend MQTT broker credentials (atc / ahxpp6467c)
                val options = MqttConnectOptions().apply {
                    userName = "atc"
                    password = "ahxpp6467c".toCharArray()
                    isAutomaticReconnect = true
                    isCleanSession = true
                    connectionTimeout = 15
                    keepAliveInterval = 30
                }

                mqttClient.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        try {
                            mqttClient.subscribe(badge, 1)
                            Log.d("EziyMqtt", "Subscribed to MQTT topic: $badge")
                        } catch (e: Exception) {
                            Log.e("EziyMqtt", "MQTT subscription failed: ${e.message}", e)
                        }
                    }

                    override fun connectionLost(cause: Throwable?) {
                        Log.w("EziyMqtt", "MQTT Connection lost: ${cause?.message}")
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        val raw = message?.payload?.toString(Charsets.UTF_8) ?: return
                        Log.d("EziyMqtt", "MQTT Message arrived on $topic: $raw")
                        var title = "Attendance Alert"
                        var text = raw

                        try {
                            val json = JSONObject(raw)
                            val name = json.optString("employee_name", badge)
                            val status = json.optString("status", "Attendance recorded")
                            val stamp = json.optString("timestamp", "")
                            title = "$name — $status"
                            text = if (stamp.isBlank()) status else "$status • $stamp"
                        } catch (_: Exception) {}

                        val nm = getSystemService(NotificationManager::class.java)
                        nm.notify(
                            (System.currentTimeMillis() % 100000).toInt(),
                            NotificationCompat.Builder(this@EziyMqttService, channelId)
                                .setSmallIcon(android.R.drawable.ic_dialog_info)
                                .setContentTitle(title)
                                .setContentText(text)
                                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                                .setAutoCancel(true)
                                .build()
                        )
                    }
                })

                mqttClient.connect(options)
                mqttClient.subscribe(badge, 1)
                client = mqttClient
                Log.d("EziyMqtt", "Connected to mqtt.eziy.in for badge: $badge")
            } catch (e: Exception) {
                Log.e("EziyMqtt", "MQTT connection failed: ${e.message}", e)
            }
        }.start()
    }

    override fun onDestroy() {
        try { client?.disconnect() } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1011
    }
}
