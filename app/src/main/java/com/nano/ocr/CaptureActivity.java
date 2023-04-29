

package com.nano.ocr;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.nano.ocr.camera.CameraManager;
import com.nano.ocr.camera.ShutterButton;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the text correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 * <p>
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing/
 */
public final class CaptureActivity extends Activity implements SurfaceHolder.Callback,
        ShutterButton.OnShutterButtonListener {

    private static final String TAG = CaptureActivity.class.getSimpleName();

    // Note: These constants will be overridden by any default values defined in preferences.xml.


    /**
     * Whether to use autofocus by default.
     */
    public static final boolean DEFAULT_TOGGLE_AUTO_FOCUS = true;

    /**
     * Whether to initially disable continuous-picture and continuous-video focus modes.
     */
    public static final boolean DEFAULT_DISABLE_CONTINUOUS_FOCUS = true;

    /**
     * Whether to beep by default when the shutter button is pressed.
     */
    public static final boolean DEFAULT_TOGGLE_BEEP = false;


    /**
     * Whether to initially reverse the image returned by the camera.
     */
    public static final boolean DEFAULT_TOGGLE_REVERSED_IMAGE = false;


    /**
     * Whether the light should be initially activated by default.
     */
    public static final boolean DEFAULT_TOGGLE_LIGHT = true;


    /**
     * Flag to enable display of the on-screen shutter button.
     */
    private static final boolean DISPLAY_SHUTTER_BUTTON = true;

    private static State state;
    private static AtomicInteger face= new AtomicInteger(1);
    private enum State {
        PREVIEW,
        PREVIEW_PAUSED,
        SUCCESS,
        DONE
    }


    private static final int PERMISSIONS_REQUEST_CAMERA = 1;

    private CameraManager cameraManager;
    //private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private TextView statusView;
    private View cameraButtonView;
    private View resultView;
    private View progressView;

    private boolean hasSurface;
    private ShutterButton shutterButton;
    private CaptureActivity captureActivity;
    private OnSharedPreferenceChangeListener listener;
   private Button btn_confirm;
   private ImageView bitmapImageView;



    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

       // checkFirstLaunch();

        this.requestPermission();

       // if (isFirstLaunch) {
       //     setDefaultPreferences();
       // }
        captureActivity=this;
        face.set(1);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.capture);
        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        cameraButtonView = findViewById(R.id.camera_button_view);
        resultView = findViewById(R.id.result_view);
        btn_confirm= (Button) findViewById(R.id.btn_confirm);
        statusView = (TextView) findViewById(R.id.status_view);
         bitmapImageView = (ImageView) findViewById(R.id.image_view);
        //handler = null;
        hasSurface = false;

        // Camera shutter button
        if (DISPLAY_SHUTTER_BUTTON) {
            shutterButton = (ShutterButton) findViewById(R.id.shutter_button);
            shutterButton.setOnShutterButtonListener(this);
        }

        cameraManager = new CameraManager(getApplication());
        viewfinderView.setCameraManager(cameraManager);

        state=State.PREVIEW;

        statusView.setVisibility(View.VISIBLE);
        statusView.setText("Please Capture face "+face+" of IDCard");

        // Set listener to change the size of the viewfinder rectangle.
        viewfinderView.setOnTouchListener(new View.OnTouchListener() {
            int lastX = -1;
            int lastY = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = -1;
                        lastY = -1;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int currentX = (int) event.getX();
                        int currentY = (int) event.getY();

                        try {
                            Rect rect = cameraManager.getFramingRect();

                            final int BUFFER = 50;
                            final int BIG_BUFFER = 60;
                            if (lastX >= 0) {
                                // Adjust the size of the viewfinder rectangle. Check if the touch event occurs in the corner areas first, because the regions overlap.
                                if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER))
                                        && ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
                                    // Top left corner: adjust both top and left sides
                                    cameraManager.adjustFramingRect(2 * (lastX - currentX), 2 * (lastY - currentY));
                                   // viewfinderView.removeResultText();
                                } else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER))
                                        && ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
                                    // Top right corner: adjust both top and right sides
                                    cameraManager.adjustFramingRect(2 * (currentX - lastX), 2 * (lastY - currentY));
                                   // viewfinderView.removeResultText();
                                } else if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER))
                                        && ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
                                    // Bottom left corner: adjust both bottom and left sides
                                    cameraManager.adjustFramingRect(2 * (lastX - currentX), 2 * (currentY - lastY));
                                   // viewfinderView.removeResultText();
                                } else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER))
                                        && ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
                                    // Bottom right corner: adjust both bottom and right sides
                                    cameraManager.adjustFramingRect(2 * (currentX - lastX), 2 * (currentY - lastY));
                                   // viewfinderView.removeResultText();
                                } else if (((currentX >= rect.left - BUFFER && currentX <= rect.left + BUFFER) || (lastX >= rect.left - BUFFER && lastX <= rect.left + BUFFER))
                                        && ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
                                    // Adjusting left side: event falls within BUFFER pixels of left side, and between top and bottom side limits
                                    cameraManager.adjustFramingRect(2 * (lastX - currentX), 0);
                                    //viewfinderView.removeResultText();
                                } else if (((currentX >= rect.right - BUFFER && currentX <= rect.right + BUFFER) || (lastX >= rect.right - BUFFER && lastX <= rect.right + BUFFER))
                                        && ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
                                    // Adjusting right side: event falls within BUFFER pixels of right side, and between top and bottom side limits
                                    cameraManager.adjustFramingRect(2 * (currentX - lastX), 0);
                                    //viewfinderView.removeResultText();
                                } else if (((currentY <= rect.top + BUFFER && currentY >= rect.top - BUFFER) || (lastY <= rect.top + BUFFER && lastY >= rect.top - BUFFER))
                                        && ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
                                    // Adjusting top side: event falls within BUFFER pixels of top side, and between left and right side limits
                                    cameraManager.adjustFramingRect(0, 2 * (lastY - currentY));
                                    //viewfinderView.removeResultText();
                                } else if (((currentY <= rect.bottom + BUFFER && currentY >= rect.bottom - BUFFER) || (lastY <= rect.bottom + BUFFER && lastY >= rect.bottom - BUFFER))
                                        && ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
                                    // Adjusting bottom side: event falls within BUFFER pixels of bottom side, and between left and right side limits
                                    cameraManager.adjustFramingRect(0, 2 * (currentY - lastY));
                                    //viewfinderView.removeResultText();
                                }
                            }
                        } catch (NullPointerException e) {
                            Log.e(TAG, "Framing rect not available", e);
                        }
                        v.invalidate();
                        lastX = currentX;
                        lastY = currentY;
                        return true;
                    case MotionEvent.ACTION_UP:
                        lastX = -1;
                        lastY = -1;
                        return true;
                }
                return false;
            }
        });


        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String filename="face1.jpg";
                       if(face.get()==2){
                           filename="face2.jpg";
                       }

                Bitmap bitmap=((BitmapDrawable)bitmapImageView.getDrawable()).getBitmap();
                ImageStorageManager.Companion.saveToInternalStorage(captureActivity,bitmap,filename);

                if(face.get()==2){
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("filename", filename);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                }else{//face=1
                    face.incrementAndGet();
                    resetStatusView();
                }
            }
        });

    }

    public void setImageView(Bitmap bitmap) {

        if (bitmapImageView!=null){
            bitmapImageView.setImageBitmap(bitmap);
          // Turn off capture-related UI elements
            shutterButton.setVisibility(View.GONE);

            cameraButtonView.setVisibility(View.GONE);
            viewfinderView.setVisibility(View.GONE);
            resultView.setVisibility(View.VISIBLE);
            statusView.setVisibility(View.VISIBLE);
            statusView.setText("Please Capture face "+face+" of IDCard");

        }

    }



    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        PERMISSIONS_REQUEST_CAMERA);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(this, "Permission not granted", Toast.LENGTH_LONG).show();
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.requestPermission();

        resetStatusView();

        // Set up the camera preview surface.
        surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        surfaceHolder = surfaceView.getHolder();
        if (!hasSurface) {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        }

    }



    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated()");

        if (holder == null) {
            Log.e(TAG, "surfaceCreated gave us a null surface");
        }

        // Only initialize the camera if the OCR engine is ready to go.
        if (!hasSurface) {
            Log.d(TAG, "surfaceCreated(): calling initCamera()...");
            initCamera(holder);
        }
        hasSurface = true;
    }

    /**
     * Initializes the camera and starts the handler to begin previewing.
     */
    private void initCamera(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "initCamera()");
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        try {

            // Open and initialize the camera
            cameraManager.openDriver(surfaceHolder);

            // Creating the handler starts the preview, which can also throw a RuntimeException.
            cameraManager.startPreview();
            setButtonVisibility(true);
            drawViewfinder();
           // handler = new CaptureActivityHandler(this, cameraManager,false);

        } catch (IOException ioe) {
            ioe.printStackTrace();
            Toast.makeText(this, "Could not initialize camera. Please try restarting device.", Toast.LENGTH_LONG).show();
        } catch (RuntimeException e) {
           e.printStackTrace();
            Toast.makeText(this, "Could not initialize camera. Please try restarting device.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {

        if (cameraManager != null) {
            cameraManager.stopPreview();

            // Stop using the camera, to avoid conflicting with other camera-based apps
            cameraManager.closeDriver();
        }




        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }



    @Override
    protected void onDestroy() {
       // stopHandler();
        super.onDestroy();
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {


            // Exit the app if we're not viewing an OCR result.
          //  if (lastResult == null) {
                setResult(RESULT_CANCELED);
                finish();
                return true;
          //  } else {
                // Go back to previewing in regular OCR mode.
         //       resetStatusView();
         //       if (handler != null) {
          //          handler.sendEmptyMessage(R.id.restart_preview);
          //      }
          //      return true;
          //  }
        } else if (keyCode == KeyEvent.KEYCODE_CAMERA) {

            shutterButtonClick();

            return true;
        } else if (keyCode == KeyEvent.KEYCODE_FOCUS) {
            // Only perform autofocus if user is not holding down the button.
            if (event.getRepeatCount() == 0) {
                cameraManager.requestAutoFocus(500L);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }




    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }


    /**
     * Finds the proper location on the cache dir where we can save files.
     */
    private File getStorageDirectory() {
        return this.getFilesDir();
    }



    /**
     // Turn off capture-related UI elements
     shutterButton.setVisibility(View.GONE);
     cameraButtonView.setVisibility(View.GONE);
     viewfinderView.setVisibility(View.GONE);
     resultView.setVisibility(View.VISIBLE);
     */
    private void restartOcrPreview() {
        // Display the shutter and torch buttons
        setButtonVisibility(true);

        // Draw the viewfinder.
            drawViewfinder();

    }




    /**
     * Resets view elements.
     */
    private void resetStatusView() {
        resultView.setVisibility(View.GONE);

        viewfinderView.setVisibility(View.VISIBLE);
        cameraButtonView.setVisibility(View.VISIBLE);
        if (DISPLAY_SHUTTER_BUTTON) {
            shutterButton.setVisibility(View.VISIBLE);
        }
        setShutterButtonClickable(true);
    }




    @SuppressWarnings("unused")
    void setButtonVisibility(boolean visible) {
        if (shutterButton != null && visible == true && DISPLAY_SHUTTER_BUTTON) {
            shutterButton.setVisibility(View.VISIBLE);
        } else if (shutterButton != null) {
            shutterButton.setVisibility(View.GONE);
        }
    }

    /**
     * Enables/disables the shutter button to prevent double-clicks on the button.
     *
     * @param clickable True if the button should accept a click
     */
    void setShutterButtonClickable(boolean clickable) {
        shutterButton.setClickable(clickable);
    }

    /**
     * Request the viewfinder to be invalidated.
     */
    void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    @Override
    public void onShutterButtonClick(ShutterButton b) {


          shutterButtonClick();


    }

    @Override
    public void onShutterButtonFocus(ShutterButton b, boolean pressed) {
        requestDelayedAutoFocus();
    }

    /**
     * Requests autofocus after a 350 ms delay. This delay prevents requesting focus when the user
     * just wants to click the shutter button without focusing. Quick button press/release will
     * trigger onShutterButtonClick() before the focus kicks in.
     */
    private void requestDelayedAutoFocus() {
        // Wait 350 ms before focusing to avoid interfering with quick button presses when
        // the user just wants to take a picture without focusing.
        cameraManager.requestAutoFocus(350L);
    }


    void shutterButtonClick() {
        // Disable further clicks on this button until OCR request is finished
        if (state == State.PREVIEW) {
            setShutterButtonClickable(false);
            String path = getStorageDirectory() + "/" + "face1.jpg";
            File f=new File(path);
            if(!f.exists()){
               // face=1;

            }else {
               // face=2;
            }

            cameraManager.requestOcrDecode(captureActivity,face.get());
        }
    }




    /**
     * Sets default values for preferences. To be called the first time this app is run.
     */
    private void setDefaultPreferences() {

    }




}
