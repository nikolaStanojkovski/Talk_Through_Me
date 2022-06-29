package mk.ukim.finki.talkthroughme

import android.app.AlertDialog
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import mk.ukim.finki.talkthroughme.ui.ProgressDialog
import mk.ukim.finki.talkthroughme.util.Constants
import mk.ukim.finki.talkthroughme.util.InternetUtils
import mk.ukim.finki.talkthroughme.util.tts.TTSService
import mk.ukim.finki.talkthroughme.util.tts.TTSUtils
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var progressDialog: AlertDialog
    private lateinit var ttsServiceReceiver: BroadcastReceiver
    private lateinit var buttonSpeak: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonSpeak = findViewById(R.id.btnSpeak)
        sharedPreferences = getSharedPreferences(
            applicationContext.resources.getText(R.string.app_id).toString(),
            MODE_PRIVATE
        )

        if (TTSUtils.model == null && TTSUtils.synthesize_object == null && TTSUtils.vocoder == null) {
            if (checkFirstRun()) {
                if (InternetUtils.hasActiveInternetConnection(this)) {
                    sharedPreferences.edit().putBoolean(Constants.FIRST_APPLICATION_RUN, false)
                        .apply()
                    initModels()
                } else {
                    progressDialog = ProgressDialog(this).build()
                    progressDialog.show()
                    Handler().postDelayed({
                        finish()
                        exitProcess(0)
                    }, 3000)
                }
            } else {
                initModels()
            }
        }

        buttonSpeak.setOnClickListener {
            buttonSpeak.isEnabled = false
            buttonSpeak.isClickable = false
            sendTTSUpdate(findViewById<TextView>(R.id.inputText).text.toString())
        }
    }

    private fun initModels() {
        progressDialog = ProgressDialog(this).build()
        progressDialog.show()
        receiveTTSInferenceUpdate()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                ttsServiceReceiver,
                IntentFilter(Constants.TTS_SERVICE_BROADCAST_NAME)
            )
        startTTSService()
    }

    private fun checkFirstRun(): Boolean {
        return (sharedPreferences.getBoolean(Constants.FIRST_APPLICATION_RUN, true))
    }

    private fun receiveTTSInferenceUpdate() {
        ttsServiceReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.extras != null) {
                    val bundle: Bundle = intent.extras!!

                    val closeProgressDialog: Boolean =
                        bundle.getBoolean(Constants.CLOSE_PROGRESS_DIALOG, false)
                    val inferenceFinished: Boolean =
                        bundle.getBoolean(Constants.INFERENCE_FINISHED, false)
                    if (closeProgressDialog) {
                        progressDialog.dismiss()
                    }
                    if (inferenceFinished) {
                        buttonSpeak.isEnabled = true
                        buttonSpeak.isClickable = true
                    }
                }
            }
        }
    }

    private fun startTTSService() {
        val serviceIntent = Intent(this, TTSService::class.java)
        startService(serviceIntent)
    }

    private fun sendTTSUpdate(textToSpeak: String) {
        val serviceIntent = Intent("tts_inference_update")
        serviceIntent.putExtra("textToSpeak", textToSpeak)

        LocalBroadcastManager.getInstance(this).sendBroadcast(serviceIntent)
    }

    override fun onDestroy() {
        super.onDestroy()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(ttsServiceReceiver)
    }
}