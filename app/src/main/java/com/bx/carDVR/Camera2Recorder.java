package com.bx.carDVR;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PorterDuff;import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
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
import android.media.MediaRecorder;
import android.os.Build;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.bx.carDVR.bylym.adas.ADASManager;
import com.bx.carDVR.bylym.model.tools.CheckStorageSpace;

import com.bx.carDVR.bylym.model.NotifyMessageManager;
import com.calmcar.adas.apiserver.AdasServer;
import com.bx.carDVR.bylym.model.tools.DVRTools;

import android.graphics.Bitmap;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.FpsMeter;
import org.opencv.android.Utils;

import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import com.calmcar.adas.apiserver.model.*;
import com.calmcar.adas.apiserver.out.CvCameraViewFrame;

import java.util.Date;

import android.os.Environment;

public class Camera2Recorder implements Recorder ,BackControl{
    public static final String TAG = "DVR-Recorder";
    public static final String TAG_PICTURE = "takePicture";

    private CameraManager mCameraManager;
    private int mCameraID;
    private CameraDevice mCamera;
    private MediaRecorder mRecorder;
    private Surface mUserPreviewSurface;
    private PreviewSurface mPreviewSurfaceForRecord;
    private Surface mAdasSurface;
    private ImageReader mImageReader;
    private ImageReader mPictureImageReader;

    private CameraCaptureSession mCaptureSession;
    private CameraCaptureSession previewCaptureSession;
    private CameraCaptureSession recordCaptureSession;
    private CaptureRequest mTakingPicRequest;
    private RecorderParameters mRecorderParameters;
    private int mCameraConnectionState;
    private OnCameraConnectionStateChangeListener mCameraConnectionStateListener;
    private int mConnectCameraRetry;
    private DVRFileList mFileList;
    private Handler mEventHandler;
    private Handler mHandler;
    private int mRunnableCnt;
    private long mRecorderStartTime;
    private boolean mRecordingStarted;

    private boolean isPreviewing;
    private boolean isTakingPic;
    private boolean isRecording;
    private File mRecordingFile;
    private OnTakePictureFinishListener mOnTakePictureFinishListener;
    private DVRTools dvrTools;
    private String outPath;
    private String currentVideoPath;
    private String photoPath;
    private boolean isLock = false;
    private long lockTime;
    public boolean cameraIsOpen = false;
    protected int FrameWidth = 1280;
    protected int FrameHeight = 720;
    // private Mat mFrameChain;
    //  private JavaCameraFrame mCameraFrame;
    private AdasServer adasServer;
    private byte[] mReadBuffer;
    private boolean isShowADAS = false;
    private CaptureRequest.Builder previewBuild;
    private CaptureRequest previewCaptureRequest;
    private CaptureRequest recordCaptureRequest;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private FpsMeter mFpsMeter;
    private SurfaceHolder mSurfaceHolder;
    private Bitmap mCacheBitmap;
    private Object lock = new Object();
    private List<Surface> previewSurfaces;
    private List<Surface> recordSurfaces;

	 private Range<Integer>[] fpsRanges;


    public Camera2Recorder(Context context, int cameraID, RecorderParameters parameters,
                           HandlerThread handlerThread, AdasServer madasServer) {
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mCameraConnectionState = CAMERA_DISCONNECTED;
        mCameraID = cameraID;
        mRecorderParameters = parameters;
        mEventHandler = new Handler();
        dvrTools = new DVRTools(context);
        adasServer = madasServer;
        startBackgroundThread();
        createHandler(handlerThread);
//        enableFpsMeter();
    }

    private void createHandler(HandlerThread handlerThread) {
        Looper looper;

        do {
            looper = handlerThread.getLooper();
        } while (looper == null);

        mHandler = new Handler(looper);
        mRunnableCnt = 0;
    }

    private void runAtHandlerThread(final Runnable r) {
        Log.d(TAG, "Post to HandlerThread , camera id " + mCameraID);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mHandler) {
                    mRunnableCnt--;
                }

                Log.d(TAG, "Run at HandlerThread , in , camera id " + mCameraID);
                r.run();
                Log.d(TAG, "Run at HandlerThread , out , camera id " + mCameraID);
            }
        });

        synchronized (mHandler) {
            if (mRunnableCnt > 0) {
                Log.d(TAG, "HandlerThread busy , mRunnableCnt = " + mRunnableCnt + " , camera" +
                        " id " + mCameraID);
            }
            mRunnableCnt++;
        }
    }

    private void runRecorder(Runnable r) {
        runAtHandlerThread(r);
    }

    @Override
    public void connectCamera() {
        if (mCameraConnectionState == CAMERA_DISCONNECTED) {
            runRecorder(new Runnable() {
                @Override
                public void run() {
                    mConnectCameraRetry = 10;
                    doConnectingCamera();
                }
            });
        }
    }

    @Override
    public void disconnectCamera() {
        if (mCameraConnectionState == CAMERA_CONNECTED) {
            if (isRecording) {
                stopRecording();
            }

            runRecorder(new Runnable() {
                @Override
                public void run() {
                    mHandler.removeCallbacks(autoConnectingCamera);
                    doDisconnectingCamera();
                }
            });
        }
        releaseCompositeDisposable();
    }

    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(TAG, "DVR open camera ID new  " + mCameraID + " , ok !");
			getFPS();
            cameraIsOpen = true;
            mCamera = camera;
            mConnectCameraRetry = 10;

            //by zrs
            mCameraFrameReady = false;
            /* now we can start update thread */
            Log.d(TAG, "Starting processing thread");
            mStopThread = false;
            /*
            mThread = new Thread(new CameraWorker());
            String name = mThread.getName();
            String id = mThread.getId() + "";
            mThread.start();*/
            //end
            correctRecorderOutputSize();
            initMediaRecorder();
            initImageReader();
            initPictureImageReader();
            createPreviewSurfaceForRecord();
//			getMatchingSize2();
            mEventHandler.post(new Runnable() {
                @Override
                public void run() {
                    onCameraConnected();
                }
            });
        }

        @Override
        public void onClosed(CameraDevice camera) {
            mCamera = null;
            cameraIsOpen = false;
            releasePreviewSurfaceForRecord();
            Settings.Global.putInt(DvrApplication.getInstance().getContentResolver(),
                    Configuration.CAMERA_RECORD_STATUS, 0);
            mEventHandler.post(new Runnable() {
                @Override
                public void run() {
                    onCameraDisconnected();
                }
            });
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            cameraIsOpen = false;
            mConnectCameraRetry = 10;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.w(TAG, "DVR open camera ID new  " + mCameraID + " , fail ! error=" + error);
            Settings.Global.putInt(DvrApplication.getInstance().getContentResolver(),
                    Configuration.CAMERA_RECORD_STATUS, 0);
			cameraIsOpen = false;
            camera.close();
            reConnectCamera();
        }
    };

    private void reConnectCamera(){
        cameraIsOpen = false;
        if (mConnectCameraRetry-- > 0) {
            mHandler.removeCallbacks(autoConnectingCamera);
            mHandler.postDelayed(autoConnectingCamera, 3000);
        }
    }

    private Runnable autoConnectingCamera = new Runnable() {
        @Override
        public void run() {
            doConnectingCamera();
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void doConnectingCamera() {
        Log.d(TAG, "DVR connecting to camera " + mCameraID);

        mEventHandler.post(new Runnable() {
            @Override
            public void run() {
                mCameraConnectionState = CAMERA_CONNECTING;
            }
        });

        try {
            mCameraManager.openCamera(String.valueOf(mCameraID), mCameraStateCallback, mHandler);
            //  mFrameChain = new Mat(FrameHeight + (FrameHeight / 2), FrameWidth, CvType.CV_8UC1);
            //   mCameraFrame = new JavaCameraFrame(mFrameChain, FrameWidth, FrameHeight, ImageFormat.NV21);

            //by zrs
            mFrameChain = new Mat[2];
            mFrameChain[0] = new Mat(FrameHeight + (FrameHeight / 2), FrameWidth, CvType.CV_8UC1);
            mFrameChain[1] = new Mat(FrameHeight + (FrameHeight / 2), FrameWidth, CvType.CV_8UC1);

            mCameraFrame = new JavaCameraFrame[2];
            mCameraFrame[0] = new JavaCameraFrame(mFrameChain[0], FrameWidth, FrameHeight, ImageFormat.NV21);
            mCameraFrame[1] = new JavaCameraFrame(mFrameChain[1], FrameWidth, FrameHeight, ImageFormat.NV21);
            //end

        } catch (CameraAccessException | SecurityException e) {
            e.printStackTrace();
            reConnectCamera();
        }


    }

    private void doDisconnectingCamera() {
        mEventHandler.post(new Runnable() {
            @Override
            public void run() {
                mCameraConnectionState = CAMERA_DISCONNECTING;
            }
        });

        if (mRecorder != null) {
            mRecorder.setOnErrorListener(null);
            mRecorder.release();
            mRecorder = null;
        }
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
    }

    //by zrs
    private boolean mCameraFrameReady = false;
    private Mat[] mFrameChain;
    protected JavaCameraFrame[] mCameraFrame;
    private int mChainIdx = 0;
    private Thread mThread;
    private boolean mStopThread;
    //end

    private void onCameraConnected() {
        Log.d(TAG, "Camera connected , ID " + mCameraID);

        isPreviewing = false;
        isTakingPic = false;
        isRecording = false;

        if (mCameraConnectionState != CAMERA_CONNECTED) {
            mCameraConnectionState = CAMERA_CONNECTED;
            if (mCameraConnectionStateListener != null) {
                mCameraConnectionStateListener.onConnect();
            }
        }
    }

    private void onCameraDisconnected() {
        Log.d(TAG, "Camera disconnected , ID " + mCameraID);

        isPreviewing = false;
        isTakingPic = false;
        isRecording = false;
        //by zrs
        try {
            mStopThread = true;
            Log.d(TAG, "Notify thread");
            synchronized (lock) {
                lock.notify();
            }
            Log.d(TAG, "Wating for thread");
            if (mThread != null)
                mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mThread = null;
        }
        synchronized (lock) {
            if (mFrameChain != null) {
                mFrameChain[0].release();
                mFrameChain[1].release();
            }
            if (mCameraFrame != null) {
                mCameraFrame[0].release();
                mCameraFrame[1].release();
            }
        }
        mCameraFrameReady = false;
        //end

        if (mCameraConnectionState != CAMERA_DISCONNECTED) {
            mCameraConnectionState = CAMERA_DISCONNECTED;
            if (mCameraConnectionStateListener != null) {
                mCameraConnectionStateListener.onDisconnect();
            }
        }
    }

    @Override
    public void onSetValidOutputFilePath(final DVRFileInfo fileInfo) {
        Log.d(TAG, "Valid output file path , camera id " + mCameraID);

        mFileList = new DVRFileList(fileInfo, mCameraID);
    }

    @Override
    public void onSetInvalidOutputFilePath() {
        Log.d(TAG, "Invalid output file path , camera id " + mCameraID);

        mFileList = null;

        if (isRecording) {
            stopRecording();
        }
    }

    private void initMediaRecorder() {
        mRecordingStarted = false;
        mRecorder = new MediaRecorder();
        mRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                if (what == MediaRecorder.MEDIA_ERROR_SERVER_DIED) {
                    Log.e(TAG, "Media server died , camera id " + mCameraID);
                    mr.release();
                    initMediaRecorder();
                }
            }
        });
    }

    private void createPreviewSurfaceForRecord() {
        if (mPreviewSurfaceForRecord == null) {
            mPreviewSurfaceForRecord = new PreviewSurface(32, 24);
        }
    }

    private void releasePreviewSurfaceForRecord() {
        if (mPreviewSurfaceForRecord != null) {
            mPreviewSurfaceForRecord.destroy();
            mPreviewSurfaceForRecord = null;
        }
    }

//    private int  count=0;
    //cyk
    private void  initImageReader() {
        mImageReader = ImageReader.newInstance(
                mRecorderParameters.VIDEO_WIDTH_DEFAULT, mRecorderParameters.VIDEO_HEIGHT_DEFAULT,
                ImageFormat.YUV_420_888, 2);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
//                Log.d(TAG, "Image available, camera id " + mCameraID);
                Image image = reader.acquireNextImage();
                if (isShowADAS) {
//                    int bufferSize = mRecorderParameters.videoWidth * mRecorderParameters.videoHeight * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
//                    mReadBuffer = new byte[bufferSize];
                    int n_image_size = image.getWidth() * image.getHeight() * 3 / 2;
                    mReadBuffer = new byte[n_image_size];
                    long begin = System.currentTimeMillis();
                    System.arraycopy(ImageUtil.getBytesFromImageAsType2(image, ImageUtil.NV21), 0, mReadBuffer, 0, n_image_size);
//                    YuvImage image1 = new YuvImage(mReadBuffer,ImageFormat.NV21,image.getWidth(),image.getHeight(),null);
                    //by zrs
                    //  mFrameChain.put(0, 0, mReadBuffer);
                    //  adasServer.processDataAsyn(mCameraFrame);
                    Log.e(TAG, "send to camerawork ");
                    mFrameChain[mChainIdx].put(0, 0, mReadBuffer);
                    deliverAndDrawFrame(mCameraFrame[mChainIdx]);
                    /*synchronized (lock) {
                        mFrameChain[mChainIdx].put(0, 0, mReadBuffer);
                        mCameraFrameReady = true;
                        lock.notify();
                    }*/
                    //end
                }

                image.close();
            }
        }, mBackgroundHandler);
    }

    private void startBackgroundThread() {
        stopBackgroundThread();
        mBackgroundThread = new HandlerThread("OpenCVCameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread == null)
            return;
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {

        }
    }

    private void initPictureImageReader() {
        mPictureImageReader = ImageReader.newInstance(
                mRecorderParameters.videoWidth, mRecorderParameters.videoHeight,
                ImageFormat.JPEG, 2);
        mPictureImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.d(TAG_PICTURE, "Image available, camera id " + mCameraID);
                Image image = reader.acquireNextImage();

                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                onJpegTaken(bytes, mOnTakePictureFinishListener);
                //Log.d("DVRHomeActivity","bytes="+bytes+",image.getWidth()="+image.getWidth()+",image.getHeight()="+image.getHeight());
                image.close();
            }
        }, mEventHandler);
    }

    private File saveLocalFile(Mat mMat) {
        Bitmap mCacheBitmap = Bitmap.createBitmap(FrameWidth, FrameHeight, Bitmap.Config.RGB_565);
        boolean bmpValid = true;
        try {
            Utils.matToBitmap(mMat, mCacheBitmap);
        } catch (Exception e) {
            bmpValid = false;
        }
        if (bmpValid) {
            File imageFile = getOutputMediaFile(1);
            if (imageFile == null) {
                return null;
            }
            try {
                FileOutputStream filecon = new FileOutputStream(imageFile);
                //图像压缩
                mCacheBitmap.compress(
                        Bitmap.CompressFormat.JPEG, 100,
                        filecon); // 将NV21格式图片，以质量70压缩成Jpeg，并得到JPEG数据流
                filecon.close();
                Log.d("DataPusher", " Yuv file saved to path: " + imageFile.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return imageFile;
        }
        return null;
    }

    private static File

    getOutputMediaFile(int type) {
        File imageFileDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES),
                "MyYuvImageTest");
        if (!imageFileDir.exists()) {
            if (!imageFileDir.mkdirs()) {
                return null;
            }
        }
        // Create a media file name
        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File imageFile;

        imageFile = new File(imageFileDir.getPath() + File.separator + "IMG_" +
                timeStamp + ".jpg");
        return imageFile;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void correctRecorderOutputSize() {
        try {
            StreamConfigurationMap config =
                    mCameraManager.getCameraCharacteristics(String.valueOf(mCameraID)).
                            get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (config == null) {
                return;
            }

            Size[] sizes = config.getOutputSizes(ImageFormat.JPEG);
            if (sizes != null) {
                Size size = sizes[0];
                mRecorderParameters.pictureWidth = size.getWidth();
                mRecorderParameters.pictureHeight = size.getHeight();
                Log.d(TAG,
                        "Camera supported picture size is " + size.getWidth() + "X" + size.getHeight() + ", camera id " + mCameraID);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(CameraCaptureSession session,
                                             CaptureRequest request, long timestamp,
                                             long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }
            };
    private CameraCaptureSession.CaptureCallback mRecordCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(CameraCaptureSession session,
                                             CaptureRequest request, long timestamp,
                                             long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);

                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);


                }
            };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initPreviewCaptureRequest() {
//        if (previewCaptureRequest != null) {
//            return;
//        }
        final Surface previewingSurface = mUserPreviewSurface != null ? mUserPreviewSurface :
                mPreviewSurfaceForRecord.getSurface();
        final Surface takingPicSurface = mPictureImageReader.getSurface();
        if(mAdasSurface == null){
            mAdasSurface = mImageReader.getSurface();
        }
        previewSurfaces = new ArrayList<>();
        previewSurfaces.add(previewingSurface);
        previewSurfaces.add(mAdasSurface);
        previewSurfaces.add(takingPicSurface);
        try {
            previewBuild = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuild.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            previewBuild.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            previewBuild.addTarget(previewingSurface);
            previewBuild.addTarget(mAdasSurface);
            previewCaptureRequest = previewBuild.build();

            CaptureRequest.Builder builder =
                    mCamera.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
            builder.set(CaptureRequest.CONTROL_ENABLE_ZSL, true);
            builder.addTarget(takingPicSurface);
            mTakingPicRequest = builder.build();

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initRecordCaptureRequest() {
//        if (recordCaptureRequest != null) {
//            return;
//        }
        final Surface previewingSurface = mUserPreviewSurface != null ? mUserPreviewSurface :
                mPreviewSurfaceForRecord.getSurface();
        Surface recordSurface=null;
        try {
            recordSurface = mRecorder.getSurface();
        }catch (Exception e){

        }

        final Surface takingPicSurface = mPictureImageReader.getSurface();
//        if(mAdasSurface == null) {
        mAdasSurface = mImageReader.getSurface();
//        }
        Log.d("ssession","initRecordCaptureRequest mAdasSurface:"+ mAdasSurface+"  recordSurface:"+recordSurface);
        recordSurfaces = new ArrayList<>();
        recordSurfaces.add(previewingSurface);
        if(recordSurface!=null){
         recordSurfaces.add(recordSurface);
        }
        recordSurfaces.add(mAdasSurface);
        recordSurfaces.add(takingPicSurface);
        try {
            CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            builder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
//            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,new Range(29,30));
            builder.addTarget(previewingSurface);
            if(recordSurface!=null) {
                builder.addTarget(recordSurface);
            }
            builder.addTarget(mAdasSurface);
            recordCaptureRequest = builder.build();

            CaptureRequest.Builder builder1 =
                    mCamera.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
            builder1.set(CaptureRequest.CONTROL_ENABLE_ZSL, true);
            builder1.addTarget(takingPicSurface);
            mTakingPicRequest = builder1.build();

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void intPictureCaptureRequest() {
        CaptureRequest.Builder builder = null;
        try {
            builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
//            builder.set(CaptureRequest.CONTROL_ENABLE_ZSL, true);
            builder.addTarget(mPictureImageReader.getSurface());
            mTakingPicRequest = builder.build();
        } catch (CameraAccessException e) {
            Log.d(TAG_PICTURE,e.getMessage());
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void createCaptureSession(String name) {
        Log.d("ssession","createCaptureSession");
        Log.d("ag", "createCaptureSession:name " + name);
//        final Surface previewingSurface = mUserPreviewSurface != null ? mUserPreviewSurface :
//                mPreviewSurfaceForRecord.getSurface();
//        final Surface recordSurface = mRecorder.getSurface();
//        final Surface takingPicSurface = mPictureImageReader.getSurface();
//        final Surface mAdasSurface = mImageReader.getSurface();
//        List<Surface> surfaces = new ArrayList<>();
//
//        surfaces.add(previewingSurface);
//        surfaces.add(recordSurface);
//        surfaces.add(mAdasSurface);
//        surfaces.add(takingPicSurface);
        initRecordCaptureRequest();
        CameraCaptureSession.StateCallback callback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                if (mCamera != null) {
//                    if (previewingSurface == mUserPreviewSurface || previewingSurface == mPreviewSurfaceForRecord.getSurface()) {
                    recordCaptureSession = session;
                    try {
//                        CaptureRequest.Builder builder =
//                                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
//                            builder.addTarget(previewingSurface);
//                            builder.addTarget(recordSurface);
//                            builder.addTarget(mAdasSurface);
//                            builder.set(CaptureRequest.CONTROL_AF_MODE,
//                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//                            builder.set(CaptureRequest.CONTROL_AE_MODE,
//                                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                        session.setRepeatingRequest(recordCaptureRequest, mRecordCaptureCallback, mHandler);

//                        builder =
//                                mCamera.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
//                        builder.set(CaptureRequest.CONTROL_ENABLE_ZSL, true);
//                        builder.addTarget(takingPicSurface);
//                        mTakingPicRequest = builder.build();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        startRecorder();
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            }
//            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                Log.e(TAG, "Camera capture session configure failed, camera id " + mCameraID);
            }
        };

        mCaptureSession = null;
        mTakingPicRequest = null;

        try {
            mCamera.createCaptureSession(recordSurfaces, callback, mHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void updatePreview() {
        if (mCaptureSession != null) {
            try {
                mCaptureSession.setRepeatingRequest(previewCaptureRequest, mCaptureCallback, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void updateRecord() {
        if (mCaptureSession != null) {
            try {
                mCaptureSession.setRepeatingRequest(recordCaptureRequest, mCaptureCallback, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

//    private boolean isInit=false;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void createCaptureSessionForPreview() {
        Log.d("ssession","createCaptureSessionForPreview");
        final Surface previewingSurface = mUserPreviewSurface;
//        if(mAdasSurface == null) {
        mAdasSurface = mImageReader.getSurface();
//        }
        final Surface takingPicSurface = mPictureImageReader.getSurface();
        List<Surface> surfaces = new ArrayList<>();

        if(previewingSurface != null) {
            surfaces.add(previewingSurface);
        }

        surfaces.add(mAdasSurface);
        surfaces.add(takingPicSurface);

        Log.d("ssession","createCaptureSessionForPreview mAdasSurface:"+ mAdasSurface);
        CameraCaptureSession.StateCallback callback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                recordCaptureSession=null;
                if (mCamera != null) {
                    if (previewingSurface == mUserPreviewSurface) {
                        mCaptureSession = session;

                        try {
                            CaptureRequest.Builder builder =
                                    mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                            if(previewingSurface != null) {
                                builder.addTarget(previewingSurface);
                            }
//                            if (isShowADAS) {
                            builder.addTarget(mAdasSurface);
                            builder.set(CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            builder.set(CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
//                            }
                            CaptureRequest request = builder.build();
                            session.setRepeatingRequest(request, mCaptureCallback, mHandler);

                            // taking Picture
                            builder =
                                    mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                            builder.set(CaptureRequest.CONTROL_ENABLE_ZSL, true);
                            builder.addTarget(takingPicSurface);
                            mTakingPicRequest = builder.build();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                Log.e(TAG, "Camera capture session configure failed, camera id " + mCameraID);
            }

            @Override
            public void onClosed(CameraCaptureSession session) {
                super.onClosed(session);
                Log.d(TAG, "onClosed: ");
            }
        };

        mCaptureSession = null;
        mTakingPicRequest = null;

        try {
            Log.d(TAG, "createCaptureSessionForPreview:camerId " + mCamera.getId());
            mCamera.createCaptureSession(surfaces, callback, mHandler);
        } catch (Exception e) {
//            e.printStackTrace();
            Log.e(TAG, "CameraAccessException: " + e.getMessage() + " " + e);
        }
    }

    private void closeCaptureSession() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
            mTakingPicRequest = null;
        }
    }

    private void prepareRecorder(File outputFile) {
        try {
            Log.d("test", "prepareRecorder: outputFile new  " + outputFile.getAbsolutePath());
            mRecorder.reset();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER); //设置用于录制的音源
            mRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mRecorder.setOutputFormat(mRecorderParameters.outputFormat);
            mRecorder.setOutputFile(outputFile.getAbsolutePath());
            //设置audio的编码格式
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setVideoEncoder(mRecorderParameters.videoEncoder);
            mRecorder.setVideoSize(mRecorderParameters.videoWidth, mRecorderParameters.videoHeight);
            mRecorder.setVideoFrameRate(mRecorderParameters.videoFrameRate);
            mRecorder.setVideoEncodingBitRate(mRecorderParameters.videoEncodingBitRate);
            mRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mr, int what, int extra) {
                    Log.d("info", "onInfo:what " + what + " extra " + extra);
                }

            });

            Log.d("test", "prepareRecorder:setOnInfoListener ");
            mRecorder.prepare();

//            MyFileObserver fb = new MyFileObserver("/storage/0000-0000/DVR-BX/1/video/20180101/");
//            Log.d(LOG_TAG, "addFilePath prepareRecorder: " );
//            fb.startWatching();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startRecorder() {
        if (mRecordingStarted) {
            return;
        }

        try {
            mRecorder.start();
            mRecordingStarted = true;
            mRecorderStartTime = System.nanoTime();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void stopRecorder() {
        Log.d("aa", "aaaac stopRecorder: ");
        if (!mRecordingStarted) {
            return;
        }

        mRecordingStarted = false;

        /*while (System.nanoTime() - mRecorderStartTime < 200000000) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {

            }
        }*/

        try {
            mRecorder.stop();
            dvrTools.tianji(currentVideoPath);
            if (isLock) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("aadfd", "run: lockTime " + lockTime);
                        dvrTools.toFileClip(lockTime, dvrTools.frontFileList, outPath);
                        isLock = false;
                    }
                }, 6000);
            }


        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private int startRecording() {
        Log.d("aa", "aaaac startRecording: ");
        int result;

        do {
            if (mCameraConnectionState != CAMERA_CONNECTED) {
                result = RECORDING_RST_FAIL_NO_CAMERA;
                break;
            }

            if (isRecording) {
                //result = RECORDING_RST_FAIL_CURRENTLY_RECORDING;
                //break;
            }

            if (mFileList == null || !isOutputFilePathValid()) {
                result = RECORDING_RST_FAIL_CARD_INVALID;
                break;
            }


            if (!isRecording) {
                runRecorder(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void run() {
                        if (mCamera != null) {
                            Log.d(TAG, "Start recording , camera id " + mCameraID);
//                            mCamera.startWaterMark(); //开始在图像上显示时间日期

                            try {
                                Log.d("test", "aaaac: newVideoFile");
                                Settings.Global.putInt(DvrApplication.getInstance().getContentResolver(),
                                        Configuration.CAMERA_RECORD_STATUS, 1);
                                final File file = mFileList.newVideoFile();
                                currentVideoPath = file.getAbsolutePath();
                                outPath = currentVideoPath.substring(0,
                                        currentVideoPath.lastIndexOf("/"));
                                Log.d("test", "startRecording:path " + currentVideoPath + " " +
                                        "outPath " + outPath);
                                if (file == null) {
//                                    result = RECORDING_RST_FAIL_CARD_INVALID;
                                    Log.w(TAG, "Create a video file fail !");
//                                    return RECORDING_RST_FAIL_CARD_INVALID;
                                }
                                prepareRecorder(file);
                            } catch (RuntimeException e) {
                                e.printStackTrace();
                                return;
                            }

                            createCaptureSession("stopRecorder");
                            //获取录像surface有时为空
//                            mHandler.postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                    createCaptureSession("stopRecorder");
//                                }
//                            },10);
                        }
                    }
                });
            } else {
                runRecorder(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void run() {
                        if (mCamera != null) {
                            Log.d(TAG, "Restart recording , camera id " + mCameraID);

                            try {
                                stopRecorder();
                                Log.d("test", "aaaac: newVideoFile1");
                                final File file = mFileList.newVideoFile();
                                currentVideoPath = file.getAbsolutePath();
                                outPath = currentVideoPath.substring(0,
                                        currentVideoPath.lastIndexOf("/"));
                                Log.d("test", "startRecording:path " + currentVideoPath + " " +
                                        "outPath " + outPath);
                                prepareRecorder(file);
                            } catch (RuntimeException e) {
                                e.printStackTrace();
                                return;
                            }
                            createCaptureSession("stopRecorder");
                        }
                    }
                });
            }

//            mRecordingFile = file;
            isRecording = true;
            result = RECORDING_RST_SUCCESSFUL;
            DvrApplication.getInstance().setRecordStatus(true);
        } while (false);

        return result;
    }

    private void stopRecording() {
        if (mCameraConnectionState != CAMERA_CONNECTED) {
            return;
        }

        if (!isRecording) {
            return;
        }

        runRecorder(new Runnable() {
            @Override
            public void run() {
                if (mCamera != null) {
                    Log.d(TAG, "Stop recording , camera id " + mCameraID);

                    try {
                        stopRecorder();
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }

                    if (mUserPreviewSurface != null) {
                        runRecorder(new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                            @Override
                            public void run() {
                                Log.d("ssession","mCamera:"+mCamera+ " mUserPreviewSurface:"+mUserPreviewSurface);
                                if (mCamera != null) { // //&& mCaptureSession == null  && mUserPreviewSurface != null
                                        createCaptureSessionForPreview();
                                }
                            }
                        });
                    }else {
                        closeCaptureSession();
                    }
                }
            }
        });

        isRecording = false;
    }

    private void takePicture(final OnTakePictureFinishListener listener) {
        boolean takingPic = false;
        int result = TAKE_PIC_RST_FAIL;
        Log.d(TAG_PICTURE,"mCameraConnectionState != CAMERA_CONNECTED="+(mCameraConnectionState != CAMERA_CONNECTED)+ "isTakingPic="+isTakingPic);
        do {
            if (mCameraConnectionState != CAMERA_CONNECTED) {
                result = TAKE_PIC_RST_FAIL_NO_CAMERA;
                break;
            }

            if (mFileList == null || !isOutputFilePathValid()) {
                result = TAKE_PIC_RST_FAIL_CARD_INVALID;
                break;
            }

            if (isPreviewing == false) {
                result = TAKE_PIC_RST_FAIL;
                break;
            }

            if (isRecording == true) {
                //result = TAKE_PIC_RST_FAIL_CURRENTLY_RECORDING;
                //break;
            }

            if (isTakingPic == true) {
                result = TAKE_PIC_RST_FAIL;
                break;
            }

            takingPic = true;
            isTakingPic = true;
            mOnTakePictureFinishListener = listener;

            runRecorder(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void run() {
                    if (mCamera != null) {
                        Log.d(TAG_PICTURE, "Take picture , camera id " + mCameraID);
                        if (recordCaptureSession != null) {
                            try {
                                intPictureCaptureRequest();
                                recordCaptureSession.capture(mTakingPicRequest, mCaptureCallback,
                                        mHandler);
                            } catch (Exception e) {
                                isTakingPic=false;
                                Log.d(TAG_PICTURE,e.getMessage());
                                e.printStackTrace();
                            }
                        }else{
                            isTakingPic=false;
                        }
                    }else{
                        isTakingPic = false;
                    }
                }
            });
        } while (false);

        if (listener != null && !takingPic) {
            listener.onFinish(result, null);
        }
    }

    private void onJpegTaken(byte[] data, OnTakePictureFinishListener listener) {
        int result = TAKE_PIC_RST_FAIL;
        photoPath = null;
        Log.d(TAG_PICTURE,"data size:"+data.length);
        do {
            if (isTakingPic == false) {
                result = TAKE_PIC_RST_FAIL;
                break;
            }

            isTakingPic = false;

            if (mCameraConnectionState != CAMERA_CONNECTED) {
                result = TAKE_PIC_RST_FAIL_NO_CAMERA;
                break;
            }

            if (mFileList == null || !isOutputFilePathValid()) {
                result = TAKE_PIC_RST_FAIL_CARD_INVALID;
                break;
            }

            File file = mFileList.newPictureFile();
            if (file != null) {
                Log.d(TAG_PICTURE,"path:"+file.getAbsolutePath());
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(file);
                    outputStream.write(data);
                    result = TAKE_PIC_RST_SUCCESSFUL;
                    photoPath = file.getAbsolutePath();
                } catch (IOException e) {
                    result = TAKE_PIC_RST_FAIL;
                    Log.d(TAG_PICTURE,e.getMessage());
                    e.printStackTrace();
                } finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            result = TAKE_PIC_RST_FAIL;
                            Log.d(TAG_PICTURE,e.getMessage());
                        }
                    }
                }
            } else {
                result = TAKE_PIC_RST_FAIL_CARD_INVALID;
                Log.w(TAG_PICTURE, "Create a picture file fail !");
            }
        } while (false);

        if (listener != null) {
            listener.onFinish(result, photoPath);
        }
    }

    @Override
    public void cameraStartPreview(final SurfaceHolder surfaceHolder) {
        if (mCameraConnectionState != CAMERA_CONNECTED) {
            return;
        }

        Log.d(TAG, "Camera start preview , camera id " + mCameraID);

        runRecorder(new Runnable() {
            @Override
            public void run() {
                if (mCamera != null) {
                    Surface surface = surfaceHolder.getSurface();
                    mSurfaceHolder = surfaceHolder;
                    if (surface != null && surface.isValid()) {
                        mUserPreviewSurface = surface;
                        if (mRecordingStarted) {
                            createCaptureSession("cameraStartPreview");
                        } else {
                            createCaptureSessionForPreview();
                        }
                    }
                }
            }
        });

        isPreviewing = true;
    }

    @Override
    public void cameraStartPreview(final SurfaceTexture surfaceTexture) {
        if (mCameraConnectionState != CAMERA_CONNECTED) {
            return;
        }

        Log.d(TAG, "Camera start preview , camera id " + mCameraID);

        runRecorder(new Runnable() {
            @Override
            public void run() {
                if (mCamera != null) {
                    if (surfaceTexture != null && !surfaceTexture.isReleased()) {
                        mUserPreviewSurface = new Surface(surfaceTexture);
                        if (mRecordingStarted) {
                            createCaptureSession("cameraStartPreview");
                        } else {
                            createCaptureSessionForPreview();
                        }
                    }
                }
            }
        });

        isPreviewing = true;
    }

    @Override
    public void cameraStopPreview() {
        if (mCameraConnectionState != CAMERA_CONNECTED) {
            return;
        }

        if (isPreviewing) {
            isPreviewing = false;
            Log.d(TAG, "Camera stop preview , camera id " + mCameraID);

            runRecorder(new Runnable() {
                @Override
                public void run() {
                    if (mCamera != null) {
                        if (mUserPreviewSurface != null) {
                            mUserPreviewSurface = null;
                            if (mRecordingStarted) {
                                createCaptureSession("cameraStopPreview");
                            } else {
                                closeCaptureSession();
                            }
                        }
                    }
                }
            });
        }
    }


    @Override
    public void cameraTakePicture(OnTakePictureFinishListener listener) {
        takePicture(listener);
    }

    @Override
    public int cameraStartRecording() {
        return startRecording();
    }

    @Override
    public void cameraStopRecording() {
        stopRecording();
    }

    @Override
    public int getCameraConnectionState() {
        return mCameraConnectionState;
    }

    @Override
    public void setOnCameraConnectionStateChangeListener(OnCameraConnectionStateChangeListener listener) {
        mCameraConnectionStateListener = listener;
    }

    @Override
    public int getCameraID() {
        return mCameraID;
    }

    @Override
    public DVRFileList getFileList() {
        return mFileList;
    }

    @Override
    public void lockCurrentRecordingFile(boolean b, long time) {
        isLock = b;
        lockTime = time;
        Log.d("test", "lockCurrentRecordingFile: " + currentVideoPath);
    }

    @Override
    public boolean isOutputFilePathValid() {
        return (mFileList != null);
    }

    @Override
    public boolean isRecording() {
        return isRecording;
    }

    @Override
    public boolean isPreviewing() {
        return isPreviewing;
    }

    @Override
    public boolean isCameraOpen() {
        return cameraIsOpen;
    }

    @Override
    public void setADASIsOpen(boolean show) {
        int state = show ? 1 : 0;
        Settings.Global.putInt(DvrApplication.getInstance().getContentResolver(),
                Configuration.CAMERA_ADAS_STATUS, state);
        this.isShowADAS = show;
    }

    @Override
    public boolean getADASIsOpen() {
        return isShowADAS;
    }

    @Override
    public void createNewSession() {

    }

    private void releaseCompositeDisposable() {
        dvrTools.release();
    }

    @SuppressLint("NewApi")
    private Size getMatchingSize2() {
        Size selectSize = null;
        try {
            for (final String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size[] sizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
                DisplayMetrics displayMetrics = DvrApplication.getInstance().getResources()
                        .getDisplayMetrics(); //因为我这里是将预览铺满屏幕,所以直接获取屏幕分辨率
                int deviceWidth = displayMetrics.widthPixels; //屏幕分辨率宽
                int deviceHeigh = displayMetrics.heightPixels; //屏幕分辨率高
                Log.e(TAG, "getMatchingSize2: 屏幕密度宽度=" + deviceWidth + " 屏幕密度高度 "
                        + deviceHeigh + " cameraid " + cameraId);
                /**
                 * 循环40次,让宽度范围从最小逐步增加,找到最符合屏幕宽度的分辨率,
                 * 你要是不放心那就增加循环,肯定会找到一个分辨率,不会出现此方法返回一个null的Size的情况
                 * ,但是循环越大后获取的分辨率就越不匹配
                 */
//				for (int j = 1; j < 41; j++) {
                for (int i = 0; i < sizes.length; i++) { //遍历所有Size
                    Size itemSize = sizes[i];
                    Log.e(TAG, "当前itemSize 宽=" + itemSize.getWidth() + "高=" + itemSize.getHeight());
                    //判断当前Size高度小于屏幕宽度+j*5  &&  判断当前Size高度大于屏幕宽度-j*5  &&  判断当前Size宽度小于当前屏幕高度
                    if (itemSize.getHeight() < (deviceWidth) && itemSize.getHeight() > (deviceWidth)) {
                        if (selectSize != null) { //如果之前已经找到一个匹配的宽度
                            if (Math.abs(deviceHeigh - itemSize.getWidth()) < Math.abs(deviceHeigh - selectSize.getWidth())) { //求绝对值算出最接近设备高度的尺寸
                                selectSize = itemSize;
                                continue;
                            }
                        } else {
                            selectSize = itemSize;
                        }

                    }
                }

//        Canvas var5;
//        if (var2 && mCacheBitmap != null && (var5 = mSurfaceHolder.lockCanvas()) != null) {
//            var5.drawColor(0, PorterDuff.Mode.CLEAR);
//            if (mScale != 0.0F) {
//                var5.drawBitmap(mCacheBitmap, new Rect(0, 0, mCacheBitmap.getWidth(),
//                        mCacheBitmap.getHeight()), new Rect((int) (((float) var5.getWidth()
//                        - mScale * (float) mCacheBitmap.getWidth()) / 2.0F),
//                        (int) (((float) var5.getHeight() - mScale * (float) mCacheBitmap.getHeight()) / 2.0F),
//                        (int) (((float) var5.getWidth() - mScale * (float) mCacheBitmap.getWidth()) / 2.0F +
//                                mScale * (float) mCacheBitmap.getWidth()),
//                        (int) (((float) var5.getHeight() - mScale * (float) mCacheBitmap.getHeight()) / 2.0F
//                                + mScale * (float) mCacheBitmap.getHeight())), (Paint) null);
//            } else {
//                var5.drawBitmap(mCacheBitmap, new Rect(0, 0, mCacheBitmap.getWidth(),
//                        mCacheBitmap.getHeight()), new Rect(
//                        (var5.getWidth() - mCacheBitmap.getWidth()) / 2,
//                        (var5.getHeight() - mCacheBitmap.getHeight()) / 2,
//                        (var5.getWidth() - mCacheBitmap.getWidth()) / 2 + mCacheBitmap.getWidth(),
//                        (var5.getHeight() - mCacheBitmap.getHeight()) / 2
//                                + mCacheBitmap.getHeight()), (Paint) null);
//            }
//
//            if (mFpsMeter != null) {
//                mFpsMeter.measure();
//                mFpsMeter.draw(var5, 20.0F, 30.0F);
//            }
//
//            mSurfaceHolder.unlockCanvasAndPost(var5);
//        }


                if (selectSize != null) { //如果不等于null 说明已经找到了 跳出循环
                    break;
    }}

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
//        Log.e(LOG_TAG, "getMatchingSize2: 选择的分辨率宽度=" + selectSize.getWidth());
//        Log.e(LOG_TAG, "getMatchingSize2: 选择的分辨率高度=" + selectSize.getHeight());
        return selectSize;
    }
	
	@SuppressLint("NewApi")
    private void getFPS() {
        CameraCharacteristics characteristics = null;
        try {
            characteristics = mCameraManager.getCameraCharacteristics(String.valueOf(mCameraID));
            fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


    private class CameraWorker implements Runnable {
        @Override
        public void run() {
            do {
                Log.d(TAG, "run:CameraWorker ");
                //by zrs
                boolean hasFrame = false;
                synchronized (lock) {
                    try {
                        while (!mCameraFrameReady && !mStopThread) {
                            Log.e(TAG, "camerawork lock wait begin ");
                            lock.wait();
                            Log.e(TAG, "camerawork lock wait end ");
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mCameraFrameReady) {
                        mChainIdx = 1 - mChainIdx;
                        mCameraFrameReady = false;
                        hasFrame = true;
                    }
                }

                if (!mStopThread && hasFrame) {
                    if (!mFrameChain[1 - mChainIdx].empty()) {
                        deliverAndDrawFrame(mCameraFrame[1 - mChainIdx]);
                    }
                }
                //end
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (!mStopThread);
            Log.d(TAG, "Finish processing thread");
        }
    }

    //end
    protected void deliverAndDrawFrame(CvCameraViewFrame var1) {
        Mat var4 = null;
        if (NotifyMessageManager.getInstance().isCvCameraViewFrame()) {
            var4 = var1.rgba();
            Log.d(TAG, "deliverAndDrawFrame: null");
        } else {
            Log.d(TAG, "deliverAndDrawFrame: onCameraFrame");
            NotifyMessageManager.getInstance().onCameraFrame(var1);
        }
//        if (mListener != null) {
//            var4 = mListener.onCameraFrame(var1);
//        } else {
//            var4 = var1.rgba();
//        }
        float mScale = 0.0F;
        boolean var2 = true;

        if (var4 != null) {
            try {
                Utils.matToBitmap(var4, mCacheBitmap);
            } catch (Exception var3) {
                Log.e("CameraBridge", "Mat type: " + var4);
                Log.e("CameraBridge", "Bitmap type: " + mCacheBitmap.getWidth() + "*"
                        + mCacheBitmap.getHeight());
                Log.e("CameraBridge", "Utils.matToBitmap() throws an exception: "
                        + var3.getMessage());
                var2 = false;
            }
        }

//        Canvas var5;
//        if (var2 && mCacheBitmap != null && (var5 = mSurfaceHolder.lockCanvas()) != null) {
//            var5.drawColor(0, PorterDuff.Mode.CLEAR);
//            if (mScale != 0.0F) {
//                var5.drawBitmap(mCacheBitmap, new Rect(0, 0, mCacheBitmap.getWidth(),
//                        mCacheBitmap.getHeight()), new Rect((int) (((float) var5.getWidth()
//                        - mScale * (float) mCacheBitmap.getWidth()) / 2.0F),
//                        (int) (((float) var5.getHeight() - mScale * (float) mCacheBitmap.getHeight()) / 2.0F),
//                        (int) (((float) var5.getWidth() - mScale * (float) mCacheBitmap.getWidth()) / 2.0F +
//                                mScale * (float) mCacheBitmap.getWidth()),
//                        (int) (((float) var5.getHeight() - mScale * (float) mCacheBitmap.getHeight()) / 2.0F
//                                + mScale * (float) mCacheBitmap.getHeight())), (Paint) null);
//            } else {
//                var5.drawBitmap(mCacheBitmap, new Rect(0, 0, mCacheBitmap.getWidth(),
//                        mCacheBitmap.getHeight()), new Rect(
//                        (var5.getWidth() - mCacheBitmap.getWidth()) / 2,
//                        (var5.getHeight() - mCacheBitmap.getHeight()) / 2,
//                        (var5.getWidth() - mCacheBitmap.getWidth()) / 2 + mCacheBitmap.getWidth(),
//                        (var5.getHeight() - mCacheBitmap.getHeight()) / 2
//                                + mCacheBitmap.getHeight()), (Paint) null);
//            }
//
//            if (mFpsMeter != null) {
//                mFpsMeter.measure();
//                mFpsMeter.draw(var5, 20.0F, 30.0F);
//            }
//
//            mSurfaceHolder.unlockCanvasAndPost(var5);
//        }
    }

    //by zrs
    public interface CvCameraViewListener2 {
        /**
         * This method is invoked when camera preview has started. After this method is invoked
         * the frames will start to be delivered to client via the onCameraFrame() callback.
         *
         * @param width  -  the width of the frames that will be delivered
         * @param height - the height of the frames that will be delivered
         */
        public void onCameraViewStarted(int width, int height);

        /**
         * This method is invoked when camera preview has been stopped for some reason.
         * No frames will be delivered via onCameraFrame() callback after this method is called.
         */
        public void onCameraViewStopped();

        /**
         * This method is invoked when delivery of the frame needs to be done.
         * The returned values - is a modified frame which needs to be displayed on the screen.
         * TODO: pass the parameters specifying the format of the frame (BPP, YUV or RGB and etc)
         */
        public Mat onCameraFrame(CvCameraViewFrame inputFrame);
    }

    @Override
    public void addCmd(int cmd) {
//        if(cmd == Ma)
    }

}