package mk.ukim.finki.talkthroughme.util.tts

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mk.ukim.finki.talkthroughme.R
import mk.ukim.finki.talkthroughme.util.Constants
import mk.ukim.finki.talkthroughme.util.NotificationUtils
import java.io.File


class TTSService : Service() {

    private lateinit var ttsReceiver: BroadcastReceiver
    private var notificationReference: NotificationCompat.Builder? = null

    override fun onBind(intent: Intent?): IBinder {
        receiveInferenceUpdate()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(ttsReceiver, IntentFilter(Constants.TTS_INFERENCE_BROADCAST_NAME))
        startModel()

        return Binder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        receiveInferenceUpdate()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(ttsReceiver, IntentFilter(Constants.TTS_INFERENCE_BROADCAST_NAME))
        startModel()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startModel() {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        val context = this

        CoroutineScope(Dispatchers.IO).launch {
            val py: Python = Python.getInstance()

            val synthesizeObject = py.getModule(Constants.SYNTHESIZE_FUNCTION_NAME)
            val model = synthesizeObject.callAttr(Constants.STATIC_MODEL_FUNCTION_NAME)
            val vocoder = synthesizeObject.callAttr(Constants.VOCODER_FUNCTION_NAME)

            TTSUtils.synthesize_object = synthesizeObject
            TTSUtils.model = model
            TTSUtils.vocoder = vocoder

            withContext(Dispatchers.Main) {
                notificationReference = NotificationUtils.showNotification(context)
                sendProgressUpdate()
            }
        }
    }

    private fun inferenceModel(textValue: String) {
        if (notificationReference != null) {
            NotificationUtils.updateNotificationProgress(applicationContext, notificationReference!!, false)
        }

        TTSUtils.synthesize_object!!.callAttr(
            Constants.MAIN_FUNCTION_NAME,
            textValue,
            TTSUtils.model,
            TTSUtils.vocoder
        )

        Toast.makeText(
            applicationContext,
            resources.getText(R.string.audio_generated_message),
            Toast.LENGTH_LONG
        ).show()

        if (notificationReference != null) {
            NotificationUtils.updateNotificationProgress(applicationContext, notificationReference!!, true)
        }
        playFile(textValue)
    }

    private fun playFile(textValue: String) {
        val path = getExternalFilesDir(null)
        val audioFile = File(path, "../${textValue}${Constants.WAV_EXTENSION}")
        val spectrogramFile = File(path, "../${textValue}${Constants.PNG_EXTENSION}")

        if (audioFile.exists()) {
            MediaPlayer().apply {
                setDataSource(audioFile.path)
                prepare()
                start()
                setOnCompletionListener {
                    it.release()
                    audioFile.delete()
                    if (spectrogramFile.exists()) {
                        spectrogramFile.delete()
                    }
                    sendInferenceUpdate()
                }
            }
        }
    }

    private fun receiveInferenceUpdate() {
        ttsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.extras != null) {
                    val bundle: Bundle = intent.extras!!

                    val textToSpeak: String? = bundle.getString(Constants.INFERENCE_TEXT)
                    if (!textToSpeak.isNullOrEmpty()) {
                        inferenceModel(textToSpeak)
                    }
                }
            }

        }
    }

    private fun sendProgressUpdate() {
        val activityIntent = Intent(Constants.TTS_SERVICE_BROADCAST_NAME)
        activityIntent.putExtra(Constants.CLOSE_PROGRESS_DIALOG, true)

        LocalBroadcastManager.getInstance(this).sendBroadcast(activityIntent)
    }

    private fun sendInferenceUpdate() {
        val activityIntent = Intent(Constants.TTS_SERVICE_BROADCAST_NAME)
        activityIntent.putExtra(Constants.INFERENCE_FINISHED, true)

        LocalBroadcastManager.getInstance(this).sendBroadcast(activityIntent)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (notificationReference != null) {
            NotificationUtils.cancelNotification()
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(ttsReceiver)
    }
}