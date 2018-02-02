package com.iotina.iotina_blue.Camera;

/**
 * Created by Rohit on 14/01/18.
 */

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.iotina.iotina_blue.R;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.R.attr.logoDescription;
import static android.R.attr.width;
import static com.iotina.iotina_blue.R.drawable.camera;
import static com.iotina.iotina_blue.R.id.screen;

public class Camera extends AppCompatActivity {
    private static final String TAG = "AndroidCameraApi";
    private ImageButton takePictureButton;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    CameraCharacteristics characteristics = null;

    //seekbar
    private SeekBar seekBar_g,seekBar_r,seekBar_e,seekBar_ss,seekBar_iso;

    //ImageButton
    private  ImageButton imageButton_g, imageButton_r,imageButton_e,imageButton_ss,imageButton_iso;
    boolean showIcon = false;
    private EditText editText_r,editText_g,editText_ss,editText_e,editText_iso;
    private FrameLayout frame;

    int Iso_matter= 400;
    Long ss_matter = 100000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camer_layout);
        textureView = (TextureView) findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        takePictureButton = (ImageButton) findViewById(R.id.btn_takepicture);
        frame = (FrameLayout)findViewById(R.id.frame);
        assert takePictureButton != null;
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

        imageButton_g = (ImageButton)findViewById(R.id.imageButton_green);
        imageButton_r = (ImageButton)findViewById(R.id.imageButton_red);
        imageButton_e = (ImageButton)findViewById(R.id.imageButton_effect);
        imageButton_ss = (ImageButton)findViewById(R.id.imageButton_ss);
        imageButton_iso = (ImageButton)findViewById(R.id.imageButton_iso);

        editText_e = (EditText)findViewById(R.id.textView_e);
        editText_g = (EditText)findViewById(R.id.textView_g);
        editText_r = (EditText)findViewById(R.id.textView_r);
        editText_ss = (EditText)findViewById(R.id.textView_ss);
        editText_iso = (EditText)findViewById(R.id.textView_iso);

        Toast.makeText(Camera.this,"Toast enable",Toast.LENGTH_LONG).show();

        hideEditText();

        frame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(Camera.this, "frame clicked", Toast.LENGTH_LONG).show();
                toggleIcons();
            }
        });

        //seekbar handler
        seekBarHandler();
        ImageButtonHandler();
        editTextHandler();
        //setCameraProperties();
    }
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            //invert image
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }

    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(Camera.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
           // captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            //  captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF);


            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            final File file = new File(Environment.getExternalStorageDirectory()+"/pic.jpeg");



            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                            addExof(file);
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(Camera.this, "Saved:" + file, Toast.LENGTH_SHORT).show();



                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;

            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());

            Matrix matrix = new Matrix();

            //Toast.makeText(this,"size : "+imageDimension.getHeight()+" width :"+imageDimension.getWidth(),Toast.LENGTH_LONG).show();
            // matrix.postRotate(270, (float) Display.g()/2, (float)imageDimension.getHeight()/2 );
            //textureView.setTransform(matrix);

            textureView.setRotation(180);


            Surface surface = new Surface(texture);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            //captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY,1600);
            captureRequestBuilder.addTarget(surface);
           // captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 800);
            // seekBar_iso.setProgress(50);



            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(Camera.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            characteristics = manager.getCameraCharacteristics(cameraId);



            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            Range<Long> range2 = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            Long max1;
            Long min1;
            if(range2 != null) {
                max1 = range2.getUpper();//10000
                min1 = range2.getLower();//100
                 Toast.makeText(Camera.this,"Upper range shutter :- "+max1,Toast.LENGTH_LONG).show();
                Toast.makeText(Camera.this, "Lower range shutter :- "+min1, Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(Camera.this,"ISO sensitivity not supported",Toast.LENGTH_SHORT).show();
            }


            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(Camera.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
        captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, Iso_matter);
        captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, ss_matter );
        //captureRequestBuilder.set(
             //   CaptureRequest.CONTROL_AF_MODE,
               // CameraMetadata.CONTROL_AF_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(Camera.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private  void seekBarHandler() {
        seekBar_g = (SeekBar)findViewById(R.id.seekBar_green);
        seekBar_r = (SeekBar)findViewById(R.id.seekBar_red);
        seekBar_e = (SeekBar)findViewById(R.id.seekBar_effect);
        seekBar_ss = (SeekBar)findViewById(R.id.seekBar_ss);
        seekBar_iso = (SeekBar)findViewById(R.id.seekBar_iso);

        seekBar_g.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //send data to green light
                editText_g.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        seekBar_r.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //set red listner
                editText_r.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekBar_e.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //effect operation
                editText_e.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekBar_ss.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //ss shutter led
                editText_ss.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                try {

                    int progress = seekBar.getProgress();
                    Range<Long> range2 = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
                    Long max1 = 10000000L;
                    Long min1 = 10000L;
                    if(range2 != null) {
                        max1 = range2.getUpper();//10000
                        min1 = range2.getLower();//100
                    } else {
                        Toast.makeText(Camera.this,"shutter speed not supported",Toast.LENGTH_SHORT).show();
                    }


                     Long ss = ((progress * (max1 - min1)) / 100 + min1) - 1;




                    ss_matter = ss;
                    updatePreview();
                    //Toast.makeText(Camera.this,"ISO :- "+ iso,Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(Camera.this, "error :- "+e.getMessage().toString(),Toast.LENGTH_LONG).show();
                }

            }
        });


        seekBar_iso.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //iso changes log
                editText_iso.setText(String.valueOf(progress));
                // Range<Integer> range2 = CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE;


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                try {

                    int progress = seekBar.getProgress();
                    Range<Integer> range2 = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
                    int max1 = 1000;
                    int min1 = 100;
                    if(range2 != null) {
                        max1 = range2.getUpper();//10000
                        min1 = range2.getLower();//100
                    } else {
                        Toast.makeText(Camera.this,"ISO sensitivity not supported",Toast.LENGTH_SHORT).show();
                    }


                    int iso = ((progress * (max1 - min1)) / 100 + min1) - 1;


                    Log.d("ISO number ", "iso upper value"+iso);

                    Iso_matter = iso;
                    updatePreview();
                    Toast.makeText(Camera.this,"ISO :- "+ iso,Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(Camera.this, "error :- "+e.getMessage().toString(),Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    private  void ImageButtonHandler() {


        imageButton_g.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(seekBar_g.getVisibility() == View.VISIBLE) {
                    hideSeekbar();
                } else {
                    hideSeekbar();
                    seekBar_g.setVisibility(View.VISIBLE);
                }
            }
        });


        imageButton_g.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                editText_g.setVisibility(View.VISIBLE);
                return true;
            }
        });

        imageButton_r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(seekBar_r.getVisibility() == View.VISIBLE) {
                    hideSeekbar();
                } else {
                    hideSeekbar();
                    seekBar_r.setVisibility(View.VISIBLE);
                }
            }
        });

        imageButton_r.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                editText_r.setVisibility(View.VISIBLE);
                return true;
            }
        });

        imageButton_e.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(seekBar_e.getVisibility() == View.VISIBLE) {
                    hideSeekbar();
                } else {
                    hideSeekbar();
                    seekBar_e.setVisibility(View.VISIBLE);
                }
            }
        });

        imageButton_e.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                editText_e.setVisibility(View.VISIBLE);
                return true;
            }
        });

        imageButton_ss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(seekBar_ss.getVisibility() == View.VISIBLE) {
                    hideSeekbar();
                } else {
                    hideSeekbar();
                    seekBar_ss.setVisibility(View.VISIBLE);
                }
            }
        });

        imageButton_ss.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                editText_ss.setVisibility(View.VISIBLE);
                return true;
            }
        });

        imageButton_iso.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(seekBar_iso.getVisibility() == View.VISIBLE) {
                    hideSeekbar();
                } else {
                    hideSeekbar();
                    seekBar_iso.setVisibility(View.VISIBLE);
                }
            }
        });
        imageButton_iso.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                editText_iso.setVisibility(View.VISIBLE);
                return true;
            }
        });
    }

    private void hideSeekbar() {
        seekBar_g.setVisibility(View.INVISIBLE);
        seekBar_r.setVisibility(View.INVISIBLE);
        seekBar_e.setVisibility(View.INVISIBLE);
        seekBar_ss.setVisibility(View.INVISIBLE);
        seekBar_iso.setVisibility(View.INVISIBLE);
    }

    private void toggleIcons() {
        if(showIcon) {
            imageButton_g.setVisibility(View.INVISIBLE);
            imageButton_r.setVisibility(View.INVISIBLE);
            imageButton_e.setVisibility(View.INVISIBLE);
            imageButton_ss.setVisibility(View.INVISIBLE);
            imageButton_iso.setVisibility(View.INVISIBLE);
            hideSeekbar();
            hideEditText();
            showIcon = false;
        } else {
            imageButton_g.setVisibility(View.VISIBLE);
            imageButton_r.setVisibility(View.VISIBLE);
            imageButton_e.setVisibility(View.VISIBLE);
            imageButton_ss.setVisibility(View.VISIBLE);
            imageButton_iso.setVisibility(View.VISIBLE);
            showIcon = true;
        }
    }

    private void hideEditText() {
        editText_iso.setVisibility(View.INVISIBLE);
        editText_g.setVisibility(View.INVISIBLE);
        editText_r.setVisibility(View.INVISIBLE);
        editText_ss.setVisibility(View.INVISIBLE);
        editText_e.setVisibility(View.INVISIBLE);


    }


    private  void editTextHandler() {
        editText_e.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL
                        && event.getAction() == KeyEvent.ACTION_DOWN) {
                    int a = Integer.parseInt(editText_e.getText().toString());
                    seekBar_e.setProgress(a);
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    seekBar_e.refreshDrawableState();
                }
                return true;
            }
        });

        editText_r.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL
                        && event.getAction() == KeyEvent.ACTION_DOWN) {
                    int a = Integer.parseInt(editText_r.getText().toString());
                    seekBar_r.setProgress(a);
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    seekBar_r.refreshDrawableState();
                }
                return true;
            }
        });

        editText_g.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL
                        && event.getAction() == KeyEvent.ACTION_DOWN) {
                    int a = Integer.parseInt(editText_g.getText().toString());
                    seekBar_g.setProgress(a);
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    seekBar_g.refreshDrawableState();
                }
                return true;
            }
        });

        editText_ss.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL
                        && event.getAction() == KeyEvent.ACTION_DOWN) {
                    int a = Integer.parseInt(editText_ss.getText().toString());
                    seekBar_ss.setProgress(a);
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    seekBar_ss.refreshDrawableState();
                }
                return true;
            }
        });

        editText_iso.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL
                        && event.getAction() == KeyEvent.ACTION_DOWN) {
                    int a = Integer.parseInt(editText_iso.getText().toString());
                    seekBar_iso.setProgress(a);
                    seekBar_iso.refreshDrawableState();
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                return true;
            }
        });
    }


    private  void addExof(File file) {
        //meta data
        try {
            ExifInterface exof = new ExifInterface(file.getPath());
            Toast.makeText(Camera.this, " "+file.getPath(),Toast.LENGTH_LONG).show();

            exof.setAttribute("Done", "Donebabay");
            exof.saveAttributes();

            exof.setAttribute("led red",
                    String.valueOf(seekBar_r.getProgress()));
            exof.saveAttributes();

            exof.setAttribute("led effect",
                    String.valueOf(seekBar_e.getProgress()));

            exof.setAttribute("led green",
                    String.valueOf(seekBar_g.getProgress()));

            exof.saveAttributes();

            exof.setAttribute("led iso",
                    String.valueOf(seekBar_iso.getProgress()));

            exof.saveAttributes();

            exof.setAttribute("led shutter speed",
                    String.valueOf(seekBar_ss.getProgress()));

            exof.saveAttributes();

            exof.setAttribute(ExifInterface.TAG_ARTIST,"ROhit chahar");
            exof.saveAttributes();

        } catch (IOException e) {
            Log.d("EOF" , "Eof exception : " +e.getMessage());
            Toast.makeText(Camera.this, "error exof : "+e.getMessage(),Toast.LENGTH_LONG).show();
        } finally {

        }
    }





}
