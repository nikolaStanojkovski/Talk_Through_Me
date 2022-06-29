package mk.ukim.finki.talkthroughme.util

class Constants {
    companion object {
        const val TTS_INFERENCE_BROADCAST_NAME = "tts_inference_update"
        const val TTS_SERVICE_BROADCAST_NAME = "tts_service_update"
        const val SYNTHESIZE_FUNCTION_NAME = "synthesize_android"
        const val STATIC_MODEL_FUNCTION_NAME = "get_static_model"
        const val VOCODER_FUNCTION_NAME = "get_static_vocoder"
        const val MAIN_FUNCTION_NAME = "main"

        const val INFERENCE_TEXT = "textToSpeak"
        const val CLOSE_PROGRESS_DIALOG = "closeProgressDialog"
        const val INFERENCE_FINISHED = "inferenceFinished"
        const val FIRST_APPLICATION_RUN = "firstrun"

        const val WAV_EXTENSION = ".wav"
        const val PNG_EXTENSION = ".png"

        const val DEPRECATION = "DEPRECATION"
        const val WIFI = "WIFI"
        const val MOBILE = "MOBILE"
    }
}