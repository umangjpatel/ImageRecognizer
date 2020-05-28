package io.github.umangjpatel.imagerecognizer;

import android.annotation.SuppressLint;

import androidx.camera.core.ImageProxy;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.nio.FloatBuffer;
import java.util.Objects;

public class Classifier {

    private Module mModule;
    private FloatBuffer mInputTensorBuffer;
    private Tensor mInputTensor;

    private static final int INPUT_TENSOR_WIDTH = 224, INPUT_TENSOR_HEIGHT = 224;

    public Classifier(String modelPath) {
        mModule = Module.load(modelPath);
        mInputTensorBuffer =
                Tensor.allocateFloatBuffer(3 * INPUT_TENSOR_WIDTH * INPUT_TENSOR_HEIGHT);
        mInputTensor = Tensor.fromBlob(mInputTensorBuffer, new long[]{1, 3, INPUT_TENSOR_HEIGHT, INPUT_TENSOR_WIDTH});
    }

    private int argMax(float[] inputs) {
        int maxIndex = -1;
        float maxvalue = 0.0f;
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] > maxvalue) {
                maxIndex = i;
                maxvalue = inputs[i];
            }
        }
        return maxIndex;
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    public int predict(ImageProxy image, int rotation) {
        TensorImageUtils.imageYUV420CenterCropToFloatBuffer(
                Objects.requireNonNull(image.getImage()),
                rotation,
                224, 224,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB,
                mInputTensorBuffer, 0);
        Tensor output = mModule.forward(IValue.from(mInputTensor)).toTensor();
        float[] scores = output.getDataAsFloatArray();
        return argMax(scores);
    }

}