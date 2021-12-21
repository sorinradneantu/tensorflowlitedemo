package ro.upt.sma.tensorflowlitedemo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast
import io.fotoapparat.Fotoapparat
import kotlinx.android.synthetic.main.activity_capture.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CaptureActivity : AppCompatActivity() {

    private lateinit var classifier: Classifier

    private lateinit var fotoapparat: Fotoapparat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)

        classifier = TFLiteClassifier.create(
            assets, Companion.MODEL_PATH,
            Companion.LABEL_PATH, Companion.INPUT_SIZE
        )

        fotoapparat = Fotoapparat(
            context = this,
            view = cmv_camera_preview
        )

        cmv_camera_preview.setOnClickListener { _ ->
            GlobalScope.launch((Dispatchers.Default)) {
                val bitmap: Bitmap = fotoapparat.takePicture().toBitmap().await().bitmap

                // Rescale the bitmap to INPUT_SIZE width and height using the Bitmap.createScaledBitmap method.
                val newBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
                // Run the recognizer which will return the recognitions.
                val rec = classifier.recognize(newBitmap);
                withContext(Dispatchers.Main) {
                    // Show the recognitions using the common Toast widget. Make use of joinToString method to concat multiple items.
                    val showRec = rec.joinToString {
                            rec ->
                        rec.label + " " + rec.confidence;
                    }
                    Toast.makeText(applicationContext, showRec, Toast.LENGTH_LONG).show();
                }

            }
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 123)
        }

    }

    override fun onResume() {
        super.onResume()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fotoapparat.start()
        }
    }

    override fun onPause() {
        super.onPause()

        fotoapparat.stop()
    }

    companion object {
        private const val MODEL_PATH = "mobilenet_v1_0.5_224_quant.tflite"
        private const val LABEL_PATH = "labels_mobilenet_quant_v1_224.txt"
        private const val INPUT_SIZE = 224
    }

}
