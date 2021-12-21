package ro.upt.sma.tensorflowlitedemo

import android.annotation.SuppressLint
import android.content.res.AssetManager
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.experimental.and


class TFLiteClassifier(private val inputSize: Int) : Classifier {

    private lateinit var interpreter: Interpreter
    private lateinit var labelList: List<String>

    private val byteBuffer =
        ByteBuffer.allocateDirect(BATCH_SIZE * inputSize * inputSize * PIXEL_SIZE)

    init {
        byteBuffer.order(ByteOrder.nativeOrder())
    }

    companion object {
        @Throws(IOException::class)
        fun create(
            assetManager: AssetManager,
            modelPath: String,
            labelPath: String,
            inputSize: Int
        ): Classifier {

            val classifier = TFLiteClassifier(inputSize)
            classifier.interpreter = Interpreter(classifier.loadModelFile(assetManager, modelPath))
            classifier.labelList = classifier.loadLabels(assetManager, labelPath)

            return classifier
        }

        private const val MAX_RESULTS = 5
        private const val BATCH_SIZE = 1
        private const val PIXEL_SIZE = 3
        private const val THRESHOLD = 0.1f
    }

    override fun recognize(bitmap: Bitmap): List<Recognition> {
        // Step one is to convert the bitmap to a byte buffer.
        val bitmapToByteBuffer = convertBitmapToByteBuffer(bitmap);

        val result = Array(1) { ByteArray(labelList.size) }

        // Step two will run the interpreter using the byte buffer as input and use the 'result' variable to return the output.
        interpreter.run(bitmapToByteBuffer, result);

        // Last step is to return the result in a sorted order.
        return getSortedResult(result);

        return emptyList() // can be deleted
    }

    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    @Throws(IOException::class)
    private fun loadLabels(assetManager: AssetManager, labelPath: String): List<String> {
        val labels = mutableListOf<String>()

        val reader = BufferedReader(InputStreamReader(assetManager.open(labelPath)))

        var line: String? = reader.readLine()
        while (!line.isNullOrEmpty()) {
            labels.add(line)
            line = reader.readLine()
        }

        reader.close()

        return labels
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        byteBuffer.clear()

        val intValues = IntArray(inputSize * inputSize)

        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val value = intValues[pixel++]
                byteBuffer.put((value shr 16 and 0xFF).toByte())
                byteBuffer.put((value shr 8 and 0xFF).toByte())
                byteBuffer.put((value and 0xFF).toByte())
            }
        }

        return byteBuffer
    }

    @SuppressLint("DefaultLocale")
    private fun getSortedResult(labelProbArray: Array<ByteArray>): List<Recognition> {
        return labelProbArray[0]
            .mapIndexed { index, bytes ->
                val confidence = (bytes and 0xff.toByte()) / 255.0f
                val label = if (index < labelList.size) labelList[index] else "UFO"
                Recognition(index.toString(), label, confidence)
            }
            .filter { it.confidence > THRESHOLD }
            .sortedByDescending { it.confidence }
            .take(MAX_RESULTS)
    }


}