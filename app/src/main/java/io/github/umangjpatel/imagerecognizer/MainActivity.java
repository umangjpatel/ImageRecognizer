package io.github.umangjpatel.imagerecognizer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private PreviewView mCameraPreviewView;
    private AppCompatTextView mCameraClassTextView;

    private ExecutorService mCameraExecutor;
    private Classifier mClassifier;

    private ProcessCameraProvider mCameraProvider;
    private ListenableFuture<ProcessCameraProvider> mCameraProviderListenableFuture;

    private static int mLensFacing = CameraSelector.LENS_FACING_BACK;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        mCameraExecutor = Executors.newSingleThreadExecutor();
        wireUpWidgets();
        mClassifier = new Classifier(Utils.assetFilePath(this, "resnet18.pt"));
        mCameraPreviewView.post(this::setupCameraX);
    }

    private void wireUpWidgets() {
        mCameraPreviewView = findViewById(R.id.camera_preview_view);
        mCameraClassTextView = findViewById(R.id.camera_class_text_view);
    }

    private void setupCameraX() {
        if (mCameraProvider != null)
            mLensFacing = hasBackCamera() ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT;
        mCameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);
        mCameraProviderListenableFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    mCameraProvider = mCameraProviderListenableFuture.get();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
                bindCameraUseCases();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        int rotation = mCameraPreviewView.getDisplay().getRotation();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(mLensFacing)
                .build();

        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(224, 224))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(mCameraExecutor, image -> {
            int value = mClassifier.predict(image, rotation);
            String predictedClass = Utils.IMAGENET_CLASSES[value];
            Log.v("PREDICTION", predictedClass);
            runOnUiThread(() -> mCameraClassTextView.setText(predictedClass));
            image.close();
        });
        mCameraProvider.unbindAll();
        Camera camera = mCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        preview.setSurfaceProvider(mCameraPreviewView.createSurfaceProvider(camera.getCameraInfo()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraExecutor.shutdown();
    }

    public static Intent getIntent(Context packageContext) {
        return new Intent(packageContext, MainActivity.class);
    }

    private boolean hasBackCamera() {
        boolean hasCamera = false;
        try {
            hasCamera = mCameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA);
        } catch (CameraInfoUnavailableException e) {
            e.printStackTrace();
        }
        return hasCamera;
    }

//    private boolean hasFrontCamera() {
//        boolean hasCamera = false;
//        try {
//            hasCamera = mCameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA);
//        } catch (CameraInfoUnavailableException e) {
//            e.printStackTrace();
//        }
//        return hasCamera;
//    }
}
