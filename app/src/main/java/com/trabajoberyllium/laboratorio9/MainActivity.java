package com.trabajoberyllium.laboratorio9;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button foto, video;
    PreviewView interfaz;
    private ImageCapture tomarFoto;
    private VideoCapture grabarVideo;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Botones, vista de capturas, añadirle la funcion de escucha onClick a botones
        foto = findViewById(R.id.foto);
        foto.setOnClickListener(this);
        video = findViewById(R.id.video);
        video.setOnClickListener(this);

        interfaz = findViewById(R.id.interfaz);

        // Procedimiento camera x
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, getExecutor());

    }

    Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    @SuppressLint("RestrictedApi")
    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(interfaz.getSurfaceProvider());

        // Tomar foto
        tomarFoto = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        // Grabar video
        grabarVideo = new VideoCapture.Builder()
                .setVideoFrameRate(30)
                .build();

        // Mantener el ciclo de vida
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, tomarFoto, grabarVideo);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.foto:
                startActivity(new Intent(this, mostrarFoto.class));
                break;
            case R.id.video:
                if (video.getText() == "Grabar Video"){
                    video.setText("Detener Video");
                    capturarVideo();
                } else {
                    video.setText("Grabar Video");
                    grabarVideo.stopRecording();
                }
                break;
        }
    }

    @SuppressLint("RestrictedApi")
    private void capturarVideo() {
        if (grabarVideo != null) {

            long timestamp = System.currentTimeMillis();

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");

            String pp = contentValues.getAsString("_display_name").toString();
            Log.e("Info", "nombre del video: " + pp);

            try {
                // En caso no se tengan permisos correspondientes en la aplicacion
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                grabarVideo.startRecording(
                        new VideoCapture.OutputFileOptions.Builder(
                                getContentResolver(),
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                        ).build(),
                        getExecutor(),
                        new VideoCapture.OnVideoSavedCallback() {
                            @Override
                            public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                                Toast.makeText(MainActivity.this, "Video guardado", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                                Toast.makeText(MainActivity.this, "Error al guardar: " + message, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}