package com.example.camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private SquareOverlayView squareOverlayView;
    private Button openCameraButton;
    private Interpreter tfliteInterpreter;
    private TextView resultTextView, confidenceTextView;
    int imageSize = 32;
    int numClasses = 8;
    float threshold = 1.0f;

    String acapulco,ampalaya, bayabas, katakataka, lagundi, oregano, sambong, error;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Resources resources = getResources();
        acapulco = resources.getString(R.string.acapulco);
        ampalaya = resources.getString(R.string.amplaya);
        bayabas = resources.getString(R.string.bayabas);
        katakataka = resources.getString(R.string.katakataka);
        lagundi = resources.getString(R.string.lagundi);
        oregano = resources.getString(R.string.oregano);
        sambong = resources.getString(R.string.sambong);
        error = resources.getString(R.string.error);

        previewView = findViewById(R.id.previewView);
        SquareOverlayView squareOverlayView = findViewById(R.id.squareOverlayView);
        FrameLayout frameLayout = findViewById(R.id.frameLayout);

        frameLayout.post(new Runnable() {
            @Override
            public void run() {
                int frameLayoutWidth = frameLayout.getWidth();
                int frameLayoutHeight = frameLayout.getHeight();

                int overlaySize = Math.min(frameLayoutWidth, frameLayoutHeight);

                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) squareOverlayView.getLayoutParams();
                layoutParams.width = overlaySize;
                layoutParams.height = overlaySize;
                layoutParams.gravity = Gravity.CENTER;
                squareOverlayView.setLayoutParams(layoutParams);
            }
        });



        openCameraButton = findViewById(R.id.openCameraButton);
        openCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previewView.setVisibility(View.VISIBLE);
                squareOverlayView.setVisibility(View.VISIBLE);
                startCamera();
            }
        });

        ImageView capturedImageView = new ImageView(this);

        try {
            tfliteInterpreter = new Interpreter(loadModelFile(), null);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd("CNN.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                PreviewView previewView = findViewById(R.id.previewView);
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageCapture imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                openCameraButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        imageCapture.takePicture(ContextCompat.getMainExecutor(MainActivity.this), new ImageCapture.OnImageCapturedCallback() {
                            @Override
                            public void onCaptureSuccess(ImageProxy image) {
                                super.onCaptureSuccess(image);

                                Bitmap bitmap = imageToBitmap(image);
                                showCapturedImageDialog(bitmap);

                                image.close();
                            }

                            @Override
                            public void onError(ImageCaptureException exception) {
                                super.onError(exception);
                                exception.printStackTrace();
                            }
                        });
                    }
                });
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private Bitmap imageToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ImageProxy.PlaneProxy plane = planes[0];
        ByteBuffer buffer = plane.getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }


    private void showCapturedImageDialog(Bitmap bitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_captured_image, null);
        ImageView imageView = dialogView.findViewById(R.id.dialogImageView);
        imageView.setImageBitmap(bitmap);

        // Center align the captured image in the ImageView
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // Process the image with TensorFlow Lite model
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(imageSize, imageSize, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0.0f, 255.0f))
                .build();
        TensorImage inputImage = TensorImage.fromBitmap(bitmap);
        inputImage = imageProcessor.process(inputImage);

        // Run inference on the processed image
        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(new int[]{1, numClasses}, DataType.FLOAT32);
        tfliteInterpreter.run(inputImage.getBuffer(), outputBuffer.getBuffer().rewind());

        // Get the predicted class index
        float[] probabilities = outputBuffer.getFloatArray();
        int maxIndex = 0;
        float maxValue = probabilities[0];
        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > maxValue) {
                maxValue = probabilities[i];
                maxIndex = i;
            }
        }

        String[] classes = new String[]{"acapulco", "ampalaya", "bayabas", "katakataka", "lagundi", "oregano", "sambong", "error"};

        // Display the result in the TextView
        resultTextView = dialogView.findViewById(R.id.resultTextView);
        confidenceTextView = dialogView.findViewById(R.id.confidenceTextView);

        StringBuilder resultBuilder = new StringBuilder();
        if (maxValue > threshold) {
            if (maxIndex >= 0 && maxIndex < classes.length) {
                resultBuilder.append("Matched! Class: ").append(classes[maxIndex]);
            } else {
                resultBuilder.append("Matched! Unknown Class");
            }
            resultTextView.setText(resultBuilder.toString());

            StringBuilder confidenceBuilder = new StringBuilder();
            confidenceBuilder.append("Confidences:\n");
            for (int i = 0; i < numClasses; i++) {
                float confidencePercentage = probabilities[i] * 10;
                if (confidencePercentage < 0) {
                    confidencePercentage = 0;
                }
                confidenceBuilder.append(classes[i]).append(": ").append(String.format("%.2f", confidencePercentage)).append("%\n");
            }
            confidenceTextView.setText(confidenceBuilder.toString());


        } else {
            resultBuilder.append("Not matched!");
            resultTextView.setText(resultBuilder.toString());
            confidenceTextView.setText("");
        }

        builder.setView(dialogView);
        builder.setPositiveButton("OK", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }
}