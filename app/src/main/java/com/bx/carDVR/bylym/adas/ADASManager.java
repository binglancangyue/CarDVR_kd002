package com.bx.carDVR.bylym.adas;

import android.content.Context;
import android.graphics.ImageFormat;
import android.os.Looper;
import android.util.Log;

import com.bx.carDVR.DvrApplication;
import com.bx.carDVR.bylym.model.NotifyMessageManager;
import com.calmcar.adas.apiserver.AdasConf;
import com.calmcar.adas.apiserver.AdasServer;
import com.calmcar.adas.apiserver.model.JavaCameraFrame;
import com.calmcar.adas.apiserver.out.ActiveSuccessListener;
import com.calmcar.adas.apiserver.out.CvCameraViewFrame;
import com.calmcar.adas.apiserver.out.DetectInitSuccessListener;
import com.calmcar.adas.gps.LocationTickListener;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import static com.bx.carDVR.bylym.activity.DVRHomeActivity.MODULE_VALUE;

/**
 * @author: Administrator
 * @date: 2021/3/9
 * @description:
 */
public class ADASManager {
    private static final String LOG_TAG = "ADASManager";
    private AdasServer adasServer;
    private Context mContext;
    private Mat mFrameChain;
    protected JavaCameraFrame mCameraFrame;
    protected int FrameWidth = 1280;
    protected int FrameHeight = 720;
    private static ADASManager adasManager;

    public static ADASManager getInstance() {
        if (adasManager == null) {
            synchronized (ADASManager.class) {
                if (adasManager == null) {
                    adasManager = new ADASManager();
                }
            }
        }
        return adasManager;
    }

    public ADASManager() {
        adasServer = new AdasServer(DvrApplication.getInstance());
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                initADAS();
            }
        }).start();
    }

    private void initADAS() {
        adasServer.setActiveSuccessListener(new ActiveSuccessListener() {
            @Override
            public void onActiveCallBack(String type) {
                String type0 = type + "";
                Log.d(LOG_TAG, "onActiveCallBack:type " + type0);
                //type :1 首次激活成功  10 已经激活  2  未经授权，激活失败 3  网络错误，激活失败
            }
        });

        adasServer.setDetectInitSuccessListener(new DetectInitSuccessListener() {
            @Override
            public void onInitSuccess() {

                /** adasServer.processDataAsyn(new com.calmcar.adas.apiserver.model.JavaCameraFrame( new Mat(480 + (480 / 2),
                 640, CvType.CV_8UC1), 640,
                 480, ImageFormat.NV21));**/
                Log.d(LOG_TAG, "onInitSuccess"); //cyk success

            }
        });
        adasServer.initConf(1280, 720);
        adasServer.setAdasServerRunMode(MODULE_VALUE);//设置室内模式 室内模式一定写成0，室外模式一定写成1

        Log.d(LOG_TAG, "initADAS: " + AdasConf.VP_X + " " + AdasConf.VP_Y);
        if (AdasConf.VP_X < 0 || AdasConf.VP_Y < 0) {
            adasServer.setVPPara(640, 360);
        }
        adasServer.setAdasServerModuleState(true, 0);
        adasServer.setFrontCarWarnSencitivity(5);
        adasServer.setLaneWarnSensitivity(0.74f);
        adasServer.startServer(new LocationTickListener() {
            @Override
            public void onTickArrive(double v, double v1, double v2) {
//                Log.d(LOG_TAG, "onTickArrive: " + v2);
//                Toast.makeText(DvrApplication.getInstance(), "速度" + v2, Toast.LENGTH_SHORT).show();
                adasServer.updateCarSpeed(v2);
            }
        });
    }

    public AdasServer getAdasServer() {
        return adasServer;
    }

    public void stopADAS() {
        if (adasServer != null) {
            adasServer.serverStop();
        }
    }

    public void initJavaCameraFrame() {
        mFrameChain = new Mat(FrameHeight + (FrameHeight / 2), FrameWidth, CvType.CV_8UC1);
        mCameraFrame = new JavaCameraFrame(mFrameChain, FrameWidth, FrameHeight, ImageFormat.NV21);
    }

    public void releaseCameraFrame() {
        if (mFrameChain != null) {
            mFrameChain.release();
        }
        if (mCameraFrame != null) {
            mCameraFrame.release();
        }
    }

    /**
     * 传递数据给ADAS
     *
     * @param bytes
     */
    public void putDada(byte[] bytes) {
        if (mFrameChain != null) {
            mFrameChain.put(0, 0, bytes);
            deliverAndDrawFrame(mCameraFrame);
        }
    }

    //end
    protected void deliverAndDrawFrame(CvCameraViewFrame var1) {
        Mat var4 = null;
        if (NotifyMessageManager.getInstance().isCvCameraViewFrame()) {
            var4 = var1.rgba();
            Log.d(LOG_TAG, "deliverAndDrawFrame: null");
        } else {
            Log.d(LOG_TAG, "deliverAndDrawFrame: onCameraFrame");
            NotifyMessageManager.getInstance().onCameraFrame(var1);
        }
    }

}
