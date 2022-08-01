package com.example.text_scanner;

import static android.Manifest.permission.CAMERA;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.Locale;

public class ScannerActivity extends AppCompatActivity {

    private TextView resultIV;
    private Button snapbtn;
    private Button detect_btn;
    private Button speak_btn;
    private Bitmap imageBitmap;
    static final int REQUEST_IMAGE_CAPTURE=1;
    private ImageView captureIV;
    private TextToSpeech textToSpeech;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        captureIV=findViewById(R.id.image);
        resultIV=findViewById(R.id.detectedText);
        snapbtn=findViewById(R.id.snapbtn);
        detect_btn=findViewById(R.id.detectbtn);
        speak_btn=findViewById(R.id.detectbtn);
        textToSpeech=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i!=TextToSpeech.ERROR){
                    textToSpeech.setLanguage(Locale.UK);
                }

            }
        });
        detect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detectText();

            }
        });
        snapbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkPermission()){
                    captureImage();
                }
                else{
                    requestPermission();
                }
                detectText();
            }
        });

        speak_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detectText();
                String text= resultIV.getText().toString();
                textToSpeech.speak(text,TextToSpeech.QUEUE_FLUSH,null);
            }
        });
    }

    @Override
    protected void onPause() {
        if(textToSpeech!=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onPause();
    }

    private boolean checkPermission(){
        int cameraPermission= ContextCompat.checkSelfPermission(getApplicationContext(),CAMERA);
        return cameraPermission== PackageManager.PERMISSION_GRANTED;
    }
    private void requestPermission(){
        int PermissionCode=200;
        ActivityCompat.requestPermissions(this,new String[]{CAMERA},PermissionCode);
    }
    private void captureImage(){
        Intent takePicture=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePicture.resolveActivity(getPackageManager())!=null){
            startActivityForResult(takePicture,REQUEST_IMAGE_CAPTURE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length>0){
            boolean cameraPermission=grantResults[0]==PackageManager.PERMISSION_GRANTED;
            if(cameraPermission){
                Toast.makeText(this,"Permission Granted....",Toast.LENGTH_SHORT).show();
                captureImage();
            }
            else{
                Toast.makeText(this,"Permission Denied....",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_IMAGE_CAPTURE&& resultCode==RESULT_OK){
            Bundle extras=data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            captureIV.setImageBitmap(imageBitmap);
        }
    }

    private void detectText(){
        InputImage image=InputImage.fromBitmap(imageBitmap,0);
        TextRecognizer recognizer= TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        Task<Text> result=recognizer.process(image).addOnSuccessListener(new OnSuccessListener<Text>() {
            @Override
            public void onSuccess(Text text) {
                StringBuilder result=new StringBuilder();
                for(Text.TextBlock block:text.getTextBlocks()){
                    String blockText= block.getText();
                    Point[] blockCornerPoint= block.getCornerPoints();
                    Rect blockFrame=block.getBoundingBox();
                    for(Text.Line line: block.getLines()){
                        String LineText=line.getText();
                        Point[] lineCornerPoint= line.getCornerPoints();
                        Rect lineRect=line.getBoundingBox();
                        for(Text.Element element:line.getElements()){
                            String elementText= element.getText();
                            result.append(elementText);
                        }
                        resultIV.setText(blockText);
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ScannerActivity.this,"failed to get text from image.."+e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });
    }
}

