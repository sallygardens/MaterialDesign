package com.example.materialdesign.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.commonsware.cwac.camera.CameraHost;
import com.commonsware.cwac.camera.CameraHostProvider;
import com.commonsware.cwac.camera.CameraView;
import com.commonsware.cwac.camera.PictureTransaction;
import com.commonsware.cwac.camera.SimpleCameraHost;
import com.example.materialdesign.R;
import com.example.materialdesign.Utils;
import com.example.materialdesign.adapter.PhotoFiltersAdapter;
import com.example.materialdesign.view.RevealBackgroundView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;
import static android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class TakePhotoActivity extends BaseActivity implements RevealBackgroundView.OnStateChangeListener
         {
           //  CameraHostProvider
    public static final String ARG_REVEAL_START_LOCATION = "reveal_start_location";

    private static final Interpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private static final Interpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private static final int STATE_TAKE_PHOTO = 0;
    private static final int STATE_SETUP_PHOTO = 1;
//    private static final int CAMERA_FRONT=0;
//    private static final int CAMERA_BEHIND=1;
    @BindView(R.id.vRevealBackground)
    RevealBackgroundView vRevealBackground;
    @BindView(R.id.vPhotoRoot)
    View vTakePhotoRoot;
    @BindView(R.id.vShutter)
    View vShutter;
    @BindView(R.id.ivTakenPhoto)
    ImageView ivTakenPhoto;
    @BindView(R.id.vUpperPanel)
    ViewSwitcher vUpperPanel;
    @BindView(R.id.vLowerPanel)
    ViewSwitcher vLowerPanel;
//    @BindView(R.id.cameraView)
//    CameraView cameraView;
    @BindView(R.id.rvFilters)
    RecyclerView rvFilters;
    @BindView(R.id.btnTakePhoto)
    Button btnTakePhoto;
    @BindView(R.id.textureView)
    TextureView textureView;
    @BindView(R.id.controlshot)
    ImageButton cImageButton;
    private boolean pendingIntro;
    private int currentState;
    int beepId;
    //    private File photoPath;
    CameraManager cameramanager;
    CameraDevice cameradevice;
    private Size previewsize;
    //预览屏幕的长宽
    private int surfacewidth;
    private int surfaceheight;
    //打开摄像头的id
    private int cameraid = CameraCharacteristics.LENS_FACING_FRONT;
    //图像捕获
    private ImageReader imagereader;
    //图像预览
    private CameraCaptureSession cameracapturesession;
    private CaptureRequest.Builder previewrequestbuilder;
    //预览请求
    private CaptureRequest previewrequest;
    //从屏幕旋转图片的转向方向
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
//    //camera front shot or behind shot
//    private int Fob=1;
    //判断是否支持闪光灯
    private boolean flashsupports;
    //用于运行不应阻塞的UI任务附加线程
             private  HandlerThread handlerThread;
             private Handler backgroundhandler;
    //文件存储路径
    private File file;
    private final ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg");
            backgroundhandler.post(new ImageSaver(reader.acquireNextImage(),file) );
        }
    };
    private static final String TAG = "camera";

    public static void startCameraFromLocation(int[] startingLocation, Activity startingActivity) {
        Intent intent = new Intent(startingActivity, TakePhotoActivity.class);
        intent.putExtra(ARG_REVEAL_START_LOCATION, startingLocation);
        startingActivity.startActivity(intent);
    }

    private final CameraDevice.StateCallback stateCallback=new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameradevice=camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            if (null != cameradevice)
            cameradevice.close();
            cameradevice=null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameradevice.close();
            cameradevice=null;
        }
    };
    /**
     * 创建TextureViewtextview的周期响应
     */
    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //当TextureView创建完成，打开指定摄像头相机
            openCamera(width, height, cameraid);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    /**
     * 打开指定摄像头ID的相机
     * 初始化相机
     * @param width
     * @param height
     * @param cameraId
     */
    private void openCamera(int width, int height, int cameraId) {
        if (ActivityCompat.checkSelfPermission(TakePhotoActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            return;
        }
        try {
            surfacewidth = width;
            surfaceheight = height;
            CameraCharacteristics characteristics = cameramanager.getCameraCharacteristics(cameraid+"");
            //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;

            // 获取设备方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int totalRotation = sensorToDeviceRotation(characteristics, rotation);
            boolean swapRotation = totalRotation == 90 || totalRotation == 270;
            int rotatedWidth = surfacewidth;
            int rotatedHeight =surfaceheight;
            if (swapRotation) {
                rotatedWidth = surfaceheight;
                rotatedHeight = surfacewidth;
            }
            // 获取最佳的预览尺寸
           previewsize = getPreferredPreviewSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);


//            if (swapRotation) {
//                textureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
//            } else {
//                texture.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
//            }
//            if (mImageReader == null) {
//                // 创建一个ImageReader对象，用于获取摄像头的图像数据,maxImages是ImageReader一次可以访问的最大图片数量
//                mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
//                        ImageFormat.JPEG, 2);
//                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
//            }
//            //检查是否支持闪光灯
//            Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
//            mFlashSupported = available == null ? false : available;
//            mCameraManager.openCamera(mCameraId + "", mStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    /**
     * 设置最佳尺寸
     *
     * @param sizes
     * @param width
     * @param height
     * @return
     */
    private Size getPreferredPreviewSize(Size[] sizes, int width, int height) {
        List<Size> collectorSizes = new ArrayList<>();
        for (Size option : sizes) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    collectorSizes.add(option);
                }
            } else {
                if (option.getHeight() > width && option.getWidth() > height) {
                    collectorSizes.add(option);
                }
            }
        }
        if (collectorSizes.size() > 0) {
            return Collections.min(collectorSizes, new Comparator<Size>() {
                @Override
                public int compare(Size s1, Size s2) {
                    return Long.signum(s1.getWidth() * s1.getHeight() - s2.getWidth() * s2.getHeight());
                }
            });
        }
        return sizes[0];
    }
    //创建预览对话
    private void createCameraPreview() {
        try{
            SurfaceTexture surfaceTexture= textureView.getSurfaceTexture();
            assert surfaceTexture!=null;
            //我们将默认缓冲区的大小配置为我们想要的相机预览的大小。
            surfaceTexture.setDefaultBufferSize(previewsize.getWidth(), previewsize.getHeight());
            // 用来开始预览的输出surface
            Surface surface = new Surface(surfaceTexture);
            //创建预览请求构建器
            previewrequestbuilder = cameradevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //将TextureView的Surface作为相机的预览显示输出
            previewrequestbuilder.addTarget(surface);
            //将TextureView的Surface作为相机的预览显示输出
            previewrequestbuilder.addTarget(surface);
            //在这里，我们为相机预览创建一个CameraCaptureSession。
           cameradevice.createCaptureSession(Arrays.asList(surface, imagereader.getSurface()), new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // 相机关闭时, 直接返回
                            if (null == cameradevice) {
                                return;
                            }
                            //会话准备就绪后，我们开始显示预览。
                            // 会话可行时, 将构建的会话赋给field
                            cameracapturesession = cameraCaptureSession;

                            //相机预览应该连续自动对焦。
                           previewrequestbuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void captureStillPicture() {
        try {
            if (cameradevice == null) {
                return;
            }
            // 创建作为拍照的CaptureRequest.Builder
           previewrequestbuilder = cameradevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 将imageReader的surface作为CaptureRequest.Builder的目标
           previewrequestbuilder.addTarget(imagereader.getSurface());
/*            // 设置自动对焦模式
            mBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 设置自动曝光模式
            mBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);*/
            //设置为自动模式
//            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            //setFlashMode(CONTROL_AE_MODE);
            // 停止连续取景
          cameracapturesession.stopRepeating();
            // 捕获静态图像
           cameracapturesession.capture(previewrequestbuilder.build(), new CameraCaptureSession.CaptureCallback() {
                // 拍照完成时激发该方法
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    //重新打开预览
                    createCameraPreview();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    /**
     * 获取可用设备可用摄像头列表
     */
    private void getCameraId(int ID) {
        try {
            for (String cameraId :cameramanager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameramanager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == ID) {
                    continue;
                }
                 cameraid= Integer.valueOf(cameraId);
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);
        updateStatusBarColor();
        updateState(STATE_TAKE_PHOTO);
        setupRevealBackground(savedInstanceState);
        //setupPhotoFilters();
        // 注册一个观察者来监听视图树，当视图树的布局、视图树的焦点、视图树将要绘制、视图树滚动等发生改变时，ViewTreeObserver都会收到通知
        vUpperPanel.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                vUpperPanel.getViewTreeObserver().removeOnPreDrawListener(this);
                pendingIntro = true;
                vUpperPanel.setTranslationY(-vUpperPanel.getHeight());
                vLowerPanel.setTranslationY(vLowerPanel.getHeight());
                return true;
            }
        });
        initPictureView();
    }

   void initPictureView(){
        cImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cameraid!=CameraCharacteristics.LENS_FACING_FRONT){
                    if(textureView.isAvailable())
                        openCamera(textureView.getHeight(),textureView.getWidth(),cameraid);
                    Log.d(TAG, "yes ");
                    cameraid=CameraCharacteristics.LENS_FACING_BACK;

                }
                else{
                    openCamera(textureView.getHeight(),textureView.getWidth(),cameraid);
                    Log.d(TAG, "ye ");
                    cameraid=CameraCharacteristics.LENS_FACING_FRONT;;

                }
            }
        });
        cameramanager=(CameraManager) getSystemService(Context.CAMERA_SERVICE);

   }
             /**
              * 初试化拍照线程
              */
             public void startBackgroundThread() {
                 handlerThread = new HandlerThread("Camera Background");
                 handlerThread.start();
                 backgroundhandler = new Handler(handlerThread.getLooper());
             }
             public void stopBackgroundThread() {
                 if (handlerThread != null) {
                   handlerThread.quitSafely();
                     try {
                       handlerThread.join();
                        handlerThread = null;
                        backgroundhandler = null;
                     } catch (InterruptedException e) {
                         e.printStackTrace();
                     }
                 }
             }
             /**
              * Closes the current {@link CameraDevice}.
              * 关闭正在使用的相机
              */
             private void closeCamera() {
                 // 关闭捕获会话
                 if (null !=cameracapturesession) {
                    cameracapturesession.close();
                     cameracapturesession = null;
                 }
                 // 关闭当前相机
                 if (null != cameradevice) {
                     cameradevice.close();
                     cameradevice = null;
                 }
                 // 关闭拍照处理器
                 if (null != imagereader) {
                     imagereader.close();
                     imagereader = null;
                 }
             }
    private void updateStatusBarColor() {
        if (Utils.isAndroid5()) {
            getWindow().setStatusBarColor(0xff111111);
        }
    }

    private void setupRevealBackground(Bundle savedInstanceState) {
        vRevealBackground.setFillPaintColor(0xFF16181a);
        vRevealBackground.setOnStateChangeListener(this);
        if (savedInstanceState == null) {
            final int[] startingLocation = getIntent().getIntArrayExtra(ARG_REVEAL_START_LOCATION);
            vRevealBackground.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    vRevealBackground.getViewTreeObserver().removeOnPreDrawListener(this);
                    vRevealBackground.startFromLocation(startingLocation);
                    return true;
                }
            });
        } else {
            vRevealBackground.setToFinishedFrame();
        }
    }

//    private void setupPhotoFilters() {
//        PhotoFiltersAdapter photoFiltersAdapter = new PhotoFiltersAdapter(this);
//        //Item的改变不会影响RecyclerView的宽高的时候可以设置setHasFixedSize(true)
//        rvFilters.setHasFixedSize(true);
//        rvFilters.setAdapter(photoFiltersAdapter);
//        rvFilters.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
//    }

    @Override
    protected void onResume() {
        super.onResume();
        if(textureView.isAvailable()){
            openCamera(textureView.getWidth(), textureView.getHeight(), cameraid);
        }
        else{
            Log.d(TAG, "onResume: cameraclick");
        }
        startBackgroundThread();
        //cameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        stopBackgroundThread();
        //cameraView.onPause();

    }

//    @OnClick(R.id.btnTakePhoto)
//    public void onTakePhotoClick() {
//        btnTakePhoto.setEnabled(false);
//        cameraView.takePicture(true, true);
//        animateShutter();
//    }

//    @OnClick(R.id.btnAccept)
//    public void onAcceptClick() {
//        PublishActivity.openWithPhotoUri(this, Uri.fromFile(photoPath));
//    }

    private void animateShutter() {
        vShutter.setVisibility(View.VISIBLE);
        vShutter.setAlpha(0.f);

        ObjectAnimator alphaInAnim = ObjectAnimator.ofFloat(vShutter, "alpha", 0f, 0.8f);
        alphaInAnim.setDuration(100);
        alphaInAnim.setStartDelay(100);
        alphaInAnim.setInterpolator(ACCELERATE_INTERPOLATOR);

        ObjectAnimator alphaOutAnim = ObjectAnimator.ofFloat(vShutter, "alpha", 0.8f, 0f);
        alphaOutAnim.setDuration(200);
        alphaOutAnim.setInterpolator(DECELERATE_INTERPOLATOR);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(alphaInAnim, alphaOutAnim);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                vShutter.setVisibility(View.GONE);
            }
        });
        animatorSet.start();
    }

    @Override
    public void onStateChange(int state) {
        if (RevealBackgroundView.STATE_FINISHED == state) {
            vTakePhotoRoot.setVisibility(View.VISIBLE);
            if (pendingIntro) {
                startIntroAnimation();
            }
        } else {
            vTakePhotoRoot.setVisibility(View.INVISIBLE);
        }
    }

    private void startIntroAnimation() {
        vUpperPanel.animate().translationY(0).setDuration(400).setInterpolator(DECELERATE_INTERPOLATOR);
        vLowerPanel.animate().translationY(0).setDuration(400).setInterpolator(DECELERATE_INTERPOLATOR).start();
    }

//    @Override
//    public CameraHost getCameraHost() {
//        return new MyCameraHost(this);
//    }

//    class MyCameraHost extends SimpleCameraHost {
//
//        private Camera.Size previewSize;
//
//        public MyCameraHost(Context ctxt) {
//            super(ctxt);
//        }
//
//        @Override
//        public boolean useFullBleedPreview() {
//            return true;
//        }
//
//        @Override
//        public Camera.Size getPictureSize(PictureTransaction xact, Camera.Parameters parameters) {
//            return previewSize;
//        }
//
//        @Override
//        public Camera.Parameters adjustPreviewParameters(Camera.Parameters parameters) {
//            Camera.Parameters parameters1 = super.adjustPreviewParameters(parameters);
//            previewSize = parameters1.getPreviewSize();
//            return parameters1;
//        }
//
//        @Override
//        public void saveImage(PictureTransaction xact, final Bitmap bitmap) {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    showTakenPicture(bitmap);
//                }
//            });
//        }
//
//        @Override
//        public void saveImage(PictureTransaction xact, byte[] image) {
//            super.saveImage(xact, image);
//            photoPath = getPhotoPath();
//        }
//    }

//    private void showTakenPicture(Bitmap bitmap) {
//        vUpperPanel.showNext();
//        vLowerPanel.showNext();
//        ivTakenPhoto.setImageBitmap(bitmap);
//        updateState(STATE_SETUP_PHOTO);
//    }

    @Override
    public void onBackPressed() {
        if (currentState == STATE_SETUP_PHOTO) {
            btnTakePhoto.setEnabled(true);
            vUpperPanel.showNext();
            vLowerPanel.showNext();
            updateState(STATE_TAKE_PHOTO);
        } else {
            super.onBackPressed();
        }
    }

    private void updateState(int state) {
        currentState = state;
        if (currentState == STATE_TAKE_PHOTO) {
            vUpperPanel.setInAnimation(this, R.anim.slide_in_from_right);
            vLowerPanel.setInAnimation(this, R.anim.slide_in_from_right);
            vUpperPanel.setOutAnimation(this, R.anim.slide_out_to_left);
            vLowerPanel.setOutAnimation(this, R.anim.slide_out_to_left);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ivTakenPhoto.setVisibility(View.GONE);
                }
            }, 400);
        } else if (currentState == STATE_SETUP_PHOTO) {
            vUpperPanel.setInAnimation(this, R.anim.slide_in_from_left);
            vLowerPanel.setInAnimation(this, R.anim.slide_in_from_left);
            vUpperPanel.setOutAnimation(this, R.anim.slide_out_to_right);
            vLowerPanel.setOutAnimation(this, R.anim.slide_out_to_right);
            ivTakenPhoto.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 获取设备方向
     *
     * @param characteristics
     * @param deviceOrientation
     * @return
     */
    private static int sensorToDeviceRotation(CameraCharacteristics characteristics, int deviceOrientation) {
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         * 要保存的图片
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         * 图片存储的路径
         */
        private final File mFile;


        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

//        private void showToast(final String text) {
//            runOnUiThread(() -> Toast.makeText(Camera2Activity.this, text, Toast.LENGTH_SHORT).show());
//        }
        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

