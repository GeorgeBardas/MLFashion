package com.mlfashion.mlfashion;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.util.ArrayUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseCloudModelSource;
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModelSource;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 42;

    public void searchFile(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    public static Bitmap doGreyscale(Bitmap src) {
        // constant factors
        final double GS_RED = 0.299;
        final double GS_GREEN = 0.587;
        final double GS_BLUE = 0.114;

        Bitmap bmOut = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        int A, R, G, B;
        int pixel;

        int width = src.getWidth();
        int height = src.getHeight();

        for(int x = 0; x < width; ++x) {
            for(int y = 0; y < height; ++y) {
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);
                R = G = B = (int)(GS_RED * R + GS_GREEN * G + GS_BLUE * B);
                bmOut.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }

        // return final image
        return bmOut;
    }

    public Bitmap processImage(Uri uri){
        Bitmap bitmap = null;

        try{
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        }catch (FileNotFoundException f){
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
        }catch (IOException e){
            Toast.makeText(this, "IO Exception", Toast.LENGTH_SHORT).show();
        }

        bitmap = bitmap.createScaledBitmap(bitmap, 28, 28, true);

        TextView pixelsValuesTV = findViewById(R.id.pixels_values);
        bitmap = doGreyscale(bitmap);

        float[][][] processed_image = new float[1][28][28];

        for(int i=0;i<28;i++){
            for(int k=0;k<28;k++){
                processed_image[0][i][k] = (float)Color.red(bitmap.getPixel(i, k)) / 255;
                if (processed_image[0][i][k] == 1.0) processed_image[0][i][k] = 0;
//                pixelsValuesTV.append(processed_image[0][i][k] + " ");
            }
            pixelsValuesTV.append(System.lineSeparator());
        }

        predict(processed_image);

        return bitmap;
    }

    public void predict(float input[][][]){
        final String[] class_names = {"T-shirt", "Trouser", "Pullover", "Dress", "Coat",
                "Sandal", "Shirt", "Sneaker", "Bag", "Ankle boot"};

        final FirebaseModelOptions options = new FirebaseModelOptions.Builder()
                .setCloudModelName("fashion-detector")
//                .setLocalModelName("fashion-detector")
                .build();

        FirebaseModelInterpreter firebaseInterpreter = null;

        try{
            firebaseInterpreter =
                    FirebaseModelInterpreter.getInstance(options);
        } catch (FirebaseMLException e){
            String error = "FirebaseMLException - 1 " + e;
            Toast.makeText(this, error + "", Toast.LENGTH_SHORT).show();
        }

        FirebaseModelInputOutputOptions inputOutputOptions = null;

        try {
            inputOutputOptions = new FirebaseModelInputOutputOptions.Builder()
                            .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 28, 28})
                            .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 10})
                            .build();
        } catch (FirebaseMLException e){
            String error = "FirebaseMLException - 2 " + e;
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        }

        try{
            FirebaseModelInputs inputs = new FirebaseModelInputs.Builder()
                    .add(input)
                    .build();

            firebaseInterpreter.run(inputs, inputOutputOptions)
                    .addOnSuccessListener(new OnSuccessListener<FirebaseModelOutputs>() {
                        @Override
                        public void onSuccess(FirebaseModelOutputs firebaseModelOutputs) {
                            float[][] output = firebaseModelOutputs.getOutput(0);
                            float[] probabilities = output[0];
                            float maxi = 0, max = probabilities[0];

                            TextView probabilitiesTV = findViewById(R.id.probabilities);
                            for(int i=0;i<probabilities.length;i++){
//                                probabilitiesTV.append(probabilities[i] + "");
//                                probabilitiesTV.append(System.lineSeparator());
                                if (probabilities[i] > max){
                                    max = probabilities[i];
                                    maxi = i;
                                }
                            }
                            Toast.makeText(MainActivity.this, "P: " + maxi, Toast.LENGTH_SHORT).show();
                            probabilitiesTV.setText(class_names[(int)maxi]);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(MainActivity.this, "FAIL", Toast.LENGTH_SHORT).show();
                }
            });



        }catch (FirebaseMLException f){
            Toast.makeText(MainActivity.this, "ML ERROR", Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FirebaseApp.initializeApp(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseModelDownloadConditions.Builder conditionsBuilder =
                new FirebaseModelDownloadConditions.Builder();
//                        .requireCharging()
//                        .requireDeviceIdle()
//                        .requireWifi();

        FirebaseModelDownloadConditions conditions = conditionsBuilder.build();

        FirebaseCloudModelSource cloudSource = new FirebaseCloudModelSource.Builder("fashion-detector")
                .enableModelUpdates(true)
                .setInitialDownloadConditions(conditions)
                .setUpdatesDownloadConditions(conditions)
                .build();

        FirebaseModelManager.getInstance().registerCloudModelSource(cloudSource);

        FirebaseLocalModelSource localSource =
                new FirebaseLocalModelSource.Builder("fashion-detector")
                        .setAssetFilePath("model.tflite")
                        .build();
        FirebaseModelManager.getInstance().registerLocalModelSource(localSource);

        findViewById(R.id.select_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchFile();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == requestCode && resultCode == Activity.RESULT_OK){
            Uri uri = null;
            uri = data.getData();
            Toast.makeText(this, uri + "", Toast.LENGTH_SHORT).show();
            ImageView imageView = findViewById(R.id.imageView);
            imageView.setImageBitmap(processImage(uri));
        }
    }
}

