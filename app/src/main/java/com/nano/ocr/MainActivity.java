package com.nano.ocr;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.nano.ocr.attribute.ApiResponseDto;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public static Bitmap photo_capture;

    private Button mBtnCameraView;

    private int ACTIVITY_REQUEST_CODE = 1;

    private View scrollView;
    private View loadingLayout;
    private TextView tvResult;
    private ImageView ivPhoto;
private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context=this;
        // view declaration
        mBtnCameraView = (Button) findViewById(R.id.btn_camera);
        //mEditOcrResult = (EditText) findViewById(R.id.edit_ocrresult);

        scrollView = findViewById(R.id.main_layout);
        loadingLayout = findViewById(R.id.loading_layout);
        ivPhoto = findViewById(R.id.view_photo);
        tvResult = findViewById(R.id.text_result);


        mBtnCameraView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // on button click

                // Camera pop up the screen
                Intent mIttCamera = new Intent(MainActivity.this, com.nano.ocr.CaptureActivity.class);
                startActivityForResult(mIttCamera, ACTIVITY_REQUEST_CODE);

            }
        });


    }




    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK)
        {
            if(requestCode== ACTIVITY_REQUEST_CODE)
            {
                // Received OCR result output
                scrollView.setVisibility(View.VISIBLE);
                String ocr=data.getStringExtra("STRING_OCR_RESULT");

                //tvResult.setText(ocr);
                ivPhoto.setImageBitmap(photo_capture);
               // Log.e(TAG, "OCR extractedmats :: "+extractedmats.size());
                //new AsyncTess().execute(MainActivity.extractedmats);

                //send photo to backend
                Log.e(TAG, "calling backend :: "+photo_capture);
                new AsyncOcr().execute(photo_capture);

            }
        }
    }



    private class AsyncOcr extends AsyncTask< Bitmap, Integer, IdCardEntityDto> {

        @Override
        protected void onPreExecute() {
            loadingLayout.setVisibility(View.VISIBLE);
        }
        @Override
        protected IdCardEntityDto doInBackground( Bitmap... lists) {
            //Tesseract OCR Perform
            IdCardEntityDto resp= null;
            Bitmap bitmap=lists[0];

                try {
                    String base64=NetUtils.toBase64(bitmap);
                    Log.e(TAG, "base64 :: "+base64);
                    //call service
                    String URL_="http://192.168.8.100:8282/api/v1/ocr/idcard";
                    String[] params= new String[]{URL_,"capture",base64 };
                    WvHttpResponse response=NetUtils.getServerResponse(URL_,params,60000,"json",null);
                    Log.e(TAG, "api response " +response);
                    if(response!=null){

                        Gson gson = new Gson();
                        ApiResponseDto resp1 = gson.fromJson(response.getResponse(), ApiResponseDto.class);
                        resp=  resp1.getData();
                        Log.e(TAG, "api response resp" +resp);
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "cropped part data error " + e.getMessage());
                }finally {

                }
           return resp;
        }

        protected void onPostExecute(IdCardEntityDto iddto) {
            //Change button properties after completion and print the result
            loadingLayout.setVisibility(View.GONE);
            if(iddto!=null) {
                String results="";
               /*if(iddto.getFirstName()!=null) results+="First Name :"+iddto.getFirstName()+"/n";
                if(iddto.getMiddleName()!=null) results+="Middle Name :"+iddto.getMiddleName()+"/n";
                if(iddto.getFirstName()!=null)results+="Last Name :"+iddto.getLastName()+"/n";
                if(iddto.getCountry()!=null) results+="Country :"+iddto.getCountry()+"/n";
                if(iddto.getGender()!=null)results+="Gender :"+iddto.getGender()+"/n";
                if(iddto.getDob()!=null)results+="Date of Birth :"+iddto.getDob()+"/n";
                if(iddto.getIdNumber()!=null)results+="IdNumber :"+iddto.getIdNumber()+"/n";
                if(iddto.getHeight()!=null)results+="Height :"+iddto.getHeight()+"/n";
                if(iddto.getOccupation()!=null)results+="Occupation :"+iddto.getOccupation()+"/n";
                if(iddto.getPob()!=null)results+="Place of Birth :"+iddto.getPob()+"/n";*/

                results=iddto.getSignature();

                tvResult.setText(results);
                Log.e(TAG, "photophoto " + iddto.getPhoto());

                if (iddto.getPhoto()!=null){
                    try {
                        String encodedImage=iddto.getPhoto();
                        String dec=Utils.Base64UrlDecode(encodedImage);
                        byte[] decodedString = Base64.decode(dec, Base64.DEFAULT);
                        Log.e(TAG, "decdecdec " + dec);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        ivPhoto.setImageBitmap(decodedByte);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }

        }

    }


    public String read_file(Context context, String filename) {
        try {
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (FileNotFoundException e) {
            return "";
        } catch (UnsupportedEncodingException e) {
            return "";
        } catch (IOException e) {
            return "";
        }
    }


}