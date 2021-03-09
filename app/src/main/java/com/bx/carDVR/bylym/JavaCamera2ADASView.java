package com.bx.carDVR.bylym;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.ViewGroup.LayoutParams;

import com.calmcar.adas.apiserver.model.JavaCameraFrame;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * This class is an implementation of the Bridge View between OpenCV and Java Camera.
 * This class relays on the functionality available in base class and only implements
 * required functions:
 * connectCamera - opens Java camera and sets the PreviewCallback to be delivered.
 * disconnectCamera - closes the camera and stops preview.
 * When frame is delivered via callback from Camera - it processed via OpenCV to be
 * converted to RGBA32 and then passed to the external callback for modifications if required.
 */

@TargetApi(21)
public class JavaCamera2ADASView extends CameraBridgeViewBase {

    private static final String LOGTAG = "JavaCamera2View";

    private ImageReader mImageReader;
    private int mPreviewFormat = ImageFormat.YUV_420_888;

    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private String mCameraID;
    private android.util.Size mPreviewSize = new android.util.Size(-1, -1);

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private int  YUV_420_888_PIXEL_STRIDE=1;

    public JavaCamera2ADASView(Context context, int cameraId) {
        super(context, cameraId);
        mCameraIndex =CAMERA_ID_BACK;
    }

    public JavaCamera2ADASView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCameraIndex =CAMERA_ID_BACK;
    }

    private void startBackgroundThread() {
        Log.i(LOGTAG, "startBackgroundThread");
        stopBackgroundThread();
        mBackgroundThread = new HandlerThread("OpenCVCameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        Log.i(LOGTAG, "stopBackgroundThread");
        if (mBackgroundThread == null)
            return;
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(LOGTAG, "stopBackgroundThread", e);
        }
    }

    protected boolean initializeCamera() {
        Log.i(LOGTAG, "initializeCamera");
        CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            String camList[] = manager.getCameraIdList();
            if (camList.length == 0) {
                Log.e(LOGTAG, "Error: camera isn't detected.");
                return false;
            }
            if (mCameraIndex == CameraBridgeViewBase.CAMERA_ID_ANY) {
                mCameraID = camList[0];
            } else {
                for (String cameraID : camList) {
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);
                    if ((mCameraIndex == CameraBridgeViewBase.CAMERA_ID_BACK &&
                            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) ||
                            (mCameraIndex == CameraBridgeViewBase.CAMERA_ID_FRONT &&
                                    characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                    ) {
                        mCameraID = cameraID;
                        break;
                    }
                }
            }
            if (mCameraID != null) {
                Log.i(LOGTAG, "Opening camera: " + mCameraID);
                manager.openCamera(mCameraID, mStateCallback, mBackgroundHandler);
            }
            return true;
        } catch (CameraAccessException e) {
            Log.e(LOGTAG, "OpenCamera - Camera Access Exception", e);
        } catch (IllegalArgumentException e) {
            Log.e(LOGTAG, "OpenCamera - Illegal Argument Exception", e);
        } catch (SecurityException e) {
            Log.e(LOGTAG, "OpenCamera - Security Exception", e);
        }
        return false;
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
        }

    };

    private void createCameraPreviewSession() {
        final int w = mPreviewSize.getWidth(), h = mPreviewSize.getHeight();
        Log.i(LOGTAG, "createCameraPreviewSession(" + w + "x" + h + ")");
        if (w < 0 || h < 0)
            return;
        try {
            if (null == mCameraDevice) {
                Log.e(LOGTAG, "createCameraPreviewSession: camera isn't opened");
                return;
            }
            if (null != mCaptureSession) {
                Log.e(LOGTAG, "createCameraPreviewSession: mCaptureSession is already started");
                return;
            }

            mImageReader = ImageReader.newInstance(w, h, mPreviewFormat, 2);
            mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    if (image == null)
                        return;

                    // sanity checks - 3 planes
                    Image.Plane[] planes = image.getPlanes();
                    assert (planes.length == 3);
                    assert (image.getFormat() == mPreviewFormat);

                    //判断
                    int uvPixelStride=planes[1].getPixelStride();
                    int uvRowStride=planes[1].getRowStride();
                    if(uvPixelStride==1 ||uvPixelStride==2) {
                        ByteBuffer y_plane = planes[0].getBuffer();
                        ByteBuffer u_plane = planes[1].getBuffer();
                        ByteBuffer v_plane = planes[2].getBuffer();

                        byte[] yData=new byte[y_plane.remaining()];
                        byte[] uData=new byte[u_plane.remaining()];
                        byte[] vData=new byte[v_plane.remaining()];
                        y_plane.get(yData);
                        u_plane.get(uData);
                        v_plane.get(vData);

                        //临时存储uv数据的
                        byte uBytes[] = new byte[w * h / 4];
                        byte vBytes[] = new byte[w * h / 4];

                        int uIndex = 0;
                        int vIndex = 0;

                        int srcIndex = 0;
                        for (int j = 0; j < h / 2; j++) {
                            for (int k = 0; k < w / 2; k++) {
                                uBytes[uIndex++] = uData[srcIndex];
                                srcIndex += uvPixelStride;
                            }
                            if (uvPixelStride == 2) {
                                srcIndex += uvRowStride - w;
                            } else if (uvPixelStride== 1) {
                                srcIndex += uvRowStride- w/ 2;
                            }
                        }

                        //根据pixelsStride取相应的数据
                        srcIndex = 0;
                        for (int j = 0; j < h/ 2; j++) {
                            for (int k = 0; k < w / 2; k++) {
                                vBytes[vIndex++] = vData[srcIndex];
                                srcIndex += uvPixelStride;
                            }
                            if (uvPixelStride== 2) {
                                srcIndex += uvRowStride - w;
                            } else if (uvPixelStride== 1) {
                                srcIndex += uvRowStride - w/ 2;
                            }
                        }

                        byte[] yuvBytes = new byte[w * h * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
                        int type=2;
                        switch (type){
                            case 2://nv21
                                int dstIndex=0;
                                System.arraycopy(yData,0,yuvBytes,dstIndex, w*h);
                                dstIndex=w*h;
                                for (int i = 0; i < vBytes.length; i++) {
                                    yuvBytes[dstIndex++] = vBytes[i];
                                    yuvBytes[dstIndex++] = uBytes[i];
                                }

                                break;
                        }

                        Mat yuvMat= new Mat(mFrameHeight + (mFrameHeight/2), mFrameWidth, CvType.CV_8UC1);
                        yuvMat.put(0,0,yuvBytes);
                        JavaCameraFrame tempFrame = new JavaCameraFrame(yuvMat, w, h,ImageFormat.NV21);
                        deliverAndDrawFrame((CvCameraViewFrame) tempFrame);
                        yuvMat.release();
                        tempFrame.release();
                    }
                    image.close();
                }
            }, mBackgroundHandler);
            Surface surface = mImageReader.getSurface();

            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            mCameraDevice.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            Log.i(LOGTAG, "createCaptureSession::onConfigured");
                            if (null == mCameraDevice) {
                                return; // camera is already closed
                            }
                            mCaptureSession = cameraCaptureSession;
                            try {
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
                                Log.i(LOGTAG, "CameraPreviewSession has been started");
                            } catch (Exception e) {
                                Log.e(LOGTAG, "createCaptureSession failed", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Log.e(LOGTAG, "createCameraPreviewSession failed");
                        }
                    },
                    null
            );
        } catch (CameraAccessException e) {
            Log.e(LOGTAG, "createCameraPreviewSession", e);
        }
    }

    @Override
    protected void disconnectCamera() {
        Log.i(LOGTAG, "closeCamera");
        try {
            CameraDevice c = mCameraDevice;
            mCameraDevice = null;
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != c) {
                c.close();
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } finally {
            stopBackgroundThread();
        }
    }

    boolean calcPreviewSize(final int width, final int height) {
        Log.i(LOGTAG, "calcPreviewSize: " + width + "x" + height);
        if (mCameraID == null) {
            Log.e(LOGTAG, "Camera isn't initialized!");
            return false;
        }
        CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraID);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            int bestWidth = 0, bestHeight = 0;
            float aspect = (float) width / height;
            android.util.Size[] sizes = map.getOutputSizes(SurfaceHolder.class);
            bestWidth = sizes[0].getWidth();
            bestHeight = sizes[0].getHeight();
            for (android.util.Size sz : sizes) {
                int w = sz.getWidth(), h = sz.getHeight();
                Log.d(LOGTAG, "trying size: " + w + "x" + h);
                if (w==width &&  h==height) {
                    bestWidth = w;
                    bestHeight = h;
                    mPreviewSize = new android.util.Size(bestWidth, bestHeight);
                    return true;
                }
            }
            Log.i(LOGTAG, "camera2 not support frame size : " + width + "x" + height);
            return false;
//            Log.i(LOGTAG, "best size: " + bestWidth + "x" + bestHeight);
//            assert(!(bestWidth == 0 || bestHeight == 0));
//            if (mPreviewSize.getWidth() == bestWidth && mPreviewSize.getHeight() == bestHeight)
//                return false;
//            else {
//                mPreviewSize = new android.util.Size(bestWidth, bestHeight);
//                return true;
//            }
        } catch (CameraAccessException e) {
            Log.e(LOGTAG, "calcPreviewSize - Camera Access Exception", e);
        } catch (IllegalArgumentException e) {
            Log.e(LOGTAG, "calcPreviewSize - Illegal Argument Exception", e);
        } catch (SecurityException e) {
            Log.e(LOGTAG, "calcPreviewSize - Security Exception", e);
        }
        return false;
    }

    @Override
    protected boolean connectCamera(int width, int height) {
        Log.i(LOGTAG, "setCameraPreviewSize(" + width + "x" + height + ")");
        startBackgroundThread();
        initializeCamera();
        try {
            boolean needReconfig = calcPreviewSize(mFrameWidth ,mFrameHeight);
//            mFrameWidth = mPreviewSize.getWidth();
//            mFrameHeight = mPreviewSize.getHeight();

            if ((getLayoutParams().width == LayoutParams.MATCH_PARENT) && (getLayoutParams().height == LayoutParams.MATCH_PARENT))
                mScale = Math.min(((float)height)/mFrameHeight, ((float)width)/mFrameWidth);
            else
                mScale = 0;

            AllocateCache();

            if (needReconfig) {
                if (null != mCaptureSession) {
                    Log.d(LOGTAG, "closing existing previewSession");
                    mCaptureSession.close();
                    mCaptureSession = null;
                }
                createCameraPreviewSession();
            }
        } catch (RuntimeException e) {
            throw new RuntimeException("Interrupted while setCameraPreviewSize.", e);
        }
        return true;
    }



    public void setFrameSize(int frameWidth, int frameHeight) {
        mFrameWidth = frameWidth;
        mFrameHeight = frameHeight;
    }


}
