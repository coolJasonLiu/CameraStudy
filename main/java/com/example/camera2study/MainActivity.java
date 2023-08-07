package com.example.camera2study;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
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
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String[] Permission_need =
            {Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO,
                    Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION};
    //后置摄像头信息
    private String backCameraId;
    private CameraCharacteristics backCharacteristics;
    //前置摄像头信息
    private String frontCameraId;
    private CameraCharacteristics frontCharacteristics;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession mCaptureSession;
    private com.example.camera2study.textureView camera_preview;
    private List<Surface> surfaceList = new ArrayList<>();
    private CaptureRequest.Builder requestBuilder;
    private CameraCharacteristics cameraCharacteristics;
    private Surface previewSurface;//用于预览的surface
    private SurfaceTexture mSurfaceTexture;
    private Button btn_take;
    private TextView take;
    private TextView recode;
    private TextView bph;
    private TextView dj;
    private TextView kmsj;
    private TextView iso;
    private TextView seek_dj_tx;
    private TextView seek_kmsj_tx;
    private TextView seek_iso_tx;
    private TextView open;
    private boolean flag = false;
    private boolean btn_flag = false;
    private Size size;
    private ImageReader imageReader;
    private Surface jpegPreviewSurface;//拍照时用于获取数据的surface
    private CaptureRequest.Builder captureImageRequestBuilder;//用于拍照的 CaptureRequest.Builder对象
    private String[] km_time = {"1/500", "1/250", "1/125", "1/60", "1/30", "1/15", "1/8", "1/4", "1/2", "1", "2", "4", "8", "16"};
    private long[] km_time_in = {2000000, 4000000, 8000000, 10000000, 20000000, 40000000, 125000000, 250000000, 500000000, 1000000000, 2000000000, 4000000000L, 8000000000L, 16000000000L};
    private CaptureRequest.Builder setBuilder;
    private boolean isOpen = false;
    private Surface previewDataSurface;
    private Surface mediaSurface;
    private MediaRecorder mMediaRecorder = new MediaRecorder();
    private CaptureRequest.Builder mPreviewBuilder;
    private SeekBar seek_dj;
    private SeekBar seek_kmsj;
    private SeekBar seek_iso;
    private LinearLayout dd;
    private LinearLayout zidong;
    private LinearLayout bcd;
    private LinearLayout yy;
    private LinearLayout qt;
    private LinearLayout yt;
    private LinearLayout ly_setAll;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkRequiredPermissions();
        initCamera();
        openCamera();
        //ActionBar actionBar = getSupportActionBar();
        //actionBar.hide();// 去掉标题栏
        initView();
        initEvent();
        camera_preview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                mSurfaceTexture = surfaceTexture;
            }
            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            }
            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                return false;
            }
            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
            }
        });
    }
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.open) {
            if (!isOpen) {
                startPreview();
                isOpen = true;
                open.setText("开始预览");
            }
        } else if (id == R.id.btn_take) {
            System.out.println("awdawdawdawdadw开拍！");
            takePic();
        }
    }

    /**
     * 开启预览
     * 1.startPreview()
     * 2.previewDataSurface用于接受数据、接受预览, getPreviewSurface()
     * 3.jpegPreviewSurface用于获取数据、保存预览, getImgReader()
     * 4.释放相机会话，closeSession()
     * */
    //创建一个用于接收预览数据的surface，防止用于预览的surface在拍照后卡顿
    private void getPreviewSurface() {
        ImageReader previewDataImageReader = ImageReader.newInstance(camera_preview.getWidth(), camera_preview.getHeight(), ImageFormat.YUV_420_888, 5);
        previewDataImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image image = imageReader.acquireNextImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    Image.Plane yPlane = planes[0];
                    Image.Plane uPlane = planes[1];
                    Image.Plane vPlane = planes[2];
                    ByteBuffer yBuffer = yPlane.getBuffer(); // Data from Y channel
                    ByteBuffer uBuffer = uPlane.getBuffer(); // Data from U channel
                    ByteBuffer vBuffer = vPlane.getBuffer(); // Data from V channel
                }
                if (image != null) {
                    image.close();
                }
            }
        }, null);
        previewDataSurface = previewDataImageReader.getSurface();
    }
    //使用ImageReader创建一个用于保存照片的surface
    private void getImgReader() {
        //创建ImageReader，用于创建保存预览的surface
        imageReader = ImageReader.newInstance(camera_preview.getWidth(), camera_preview.getHeight(), ImageFormat.JPEG, 5);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image image = imageReader.acquireLatestImage();
                new Thread(new ImageSaver(image)).start();//在子线程保存照片
            }
        }, null);
        jpegPreviewSurface = imageReader.getSurface();
    }
    //保存照片
    private static class ImageSaver implements Runnable {
        private Image mImage;
        public ImageSaver(Image image) {
            mImage = image;
        }
        @Override
        public void run() {
            System.out.println("wdawdawdad" + mImage);
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            File mImageFile = new File(Environment.getExternalStorageDirectory() + "/DCIM/myPicture.jpg");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mImageFile);
                fos.write(data, 0, data.length);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImageFile = null;
                if (fos != null) {
                    try {
                        fos.close();
                        fos = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    private void closeSession(){
        if (mCaptureSession != null){
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }//释放相机会话
    private void startPreview(){
        closeSession();
        getPreviewSurface();
        getImgReader();
        //设置预览页面1,获取相机支持的size并和surfaceTexture比较选出适合的,用于预览流
        size = new Size(1920,1080);
        mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());//设置surface的大小，控件texture大小要与surface匹配
        previewSurface = new Surface(mSurfaceTexture);
        //
        surfaceList.add(previewSurface);
        surfaceList.add(previewDataSurface);
        surfaceList.add(jpegPreviewSurface);
        try {
            requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //CaptureRequest必须指定一个或多个surface,可以多次调用方法添加
            requestBuilder.addTarget(previewSurface);
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
            requestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE);
            requestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            requestBuilder.addTarget(previewDataSurface);
            //创建CaptureSession
            cameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCaptureSession = cameraCaptureSession;
                    try {
                        mCaptureSession.setRepeatingRequest(requestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        if (setBuilder != null){
            try {
                setBuilder = null;
                setBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 开启预览
     * */

    /**
     * 拍照
     * */
    private static final SparseIntArray ORIENTATION = new SparseIntArray();
    //设置屏幕方向
    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }
    private void takePic() {
        try {
            //获取屏幕方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            try{
                captureImageRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            }catch(CameraAccessException e){
                e.printStackTrace();
            }
            captureImageRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
            captureImageRequestBuilder.addTarget(previewDataSurface);
            captureImageRequestBuilder.addTarget(jpegPreviewSurface);
            mCaptureSession.capture(captureImageRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.d("TAG","拍照");
    }
    /**
     * 拍照
     * */

    /**
     * 初始化数据, initView, initEvent
     * */
    private void initView() {
        camera_preview = findViewById(R.id.camera_preview);
        btn_take = findViewById(R.id.btn_take);
        open = findViewById(R.id.open);
    }
    private void initEvent() {
        //take.setOnClickListener(this);
        btn_take.setOnClickListener(this);
        open.setOnClickListener(this);
    }

    /**
     * 初始化相机
     * 1.获取实例
     * 2.获取设备列表, initCamera
     * 3.获取各个摄像头可控等级, isHardwareLevelSupported
     * 4.申请检查相机权限, checkRequiredPermissions, isPermission
     * 5.筛选前后摄像头
     */
    //初始化摄像头
    private void initCamera(){
        //创建CameraManager实例
        cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        //获取相机设备ID列表
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            for (String cameraId : cameraIdList) { //检查设备可控等级和筛选前后摄像头
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (isHardwareLevelSupported(characteristics)) {
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                        backCameraId = cameraId;
                        backCharacteristics = characteristics;
                        break;
                    } else if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                        frontCameraId = cameraId;
                        frontCharacteristics = characteristics;
                        break;
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    //检查该cameraID的可控等级是否达到INFO_SUPPORTED_HARDWARE_LEVEL_FULL或以上,initCamera()里使用
    private boolean isHardwareLevelSupported(CameraCharacteristics characteristics) {
        int requiredLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        int[] levels = {CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3};
        for (int i = 0; i < 5; i++) {
            if (requiredLevel == levels[i]) {
                if (i > 2) {
                    return true;
                }
            }
        }
        return false;
    }
    //申请相机权限
    private boolean checkRequiredPermissions() {
        if (!isPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(Permission_need, 1);//如果权限未授权，则申请授权
            shouldShowRequestPermissionRationale("该权限将用于手机拍照录像和存储功能，若拒绝则运行像地狱");//显示权限信息
        }
        return isPermission();
    }
    //检查相机权限
    public boolean isPermission() {
        for (String permission : Permission_need) {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }
    /**
     * 初始化相机
     * 1.获取实例
     * 2.获取设备列表
     * 3.获取各个摄像头可控等级
     * 4.申请检查相机权限
     * 5.筛选前后摄像头
     */

    /**
     * 打开相机
     * 1.handler开启相机
     * 2.相机回调
     * 3.
     */
    //在handler中开启相机
    Handler cameraHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        checkRequiredPermissions();
                        Toast.makeText(MainActivity.this, "开个相机权限行不行？", Toast.LENGTH_SHORT).show();
                    } else {
                        cameraManager.openCamera(backCameraId, mStateCallback, null);
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
            if (msg.what == 1) {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        checkRequiredPermissions();
                        Toast.makeText(MainActivity.this, "开个相机权限行不行？", Toast.LENGTH_SHORT).show();
                    } else {
                        cameraManager.openCamera(frontCameraId, mStateCallback, null);
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    private void openCamera() {
        if (backCameraId != null) {
            Message message_back = Message.obtain();
            message_back.what = 0;
            cameraHandler.sendMessage(message_back);
        } else if (frontCameraId != null) {
            Message message_front = Message.obtain();
            message_front.what = 1;
            cameraHandler.sendMessage(message_front);
        } else {
            Toast.makeText(this, "宁的摄像头有、问题", Toast.LENGTH_SHORT).show();
        }
    }
    //创建相机回调
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            //打开成功，可以获取CameraDevice对象
            cameraDevice = camera;
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            //断开连接
        }
        @Override
        public void onError(@NonNull CameraDevice camera, final int error) {
            //发生异常
        }
    };
    /**
     * 打开相机
     * 1.handler开启相机
     * 2.相机回调
     */

}