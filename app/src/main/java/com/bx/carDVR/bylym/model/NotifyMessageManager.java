package com.bx.carDVR.bylym.model;

import com.bx.carDVR.Camera2Recorder;
import com.bx.carDVR.bylym.model.listener.OnDelayRecordingTimeListener;
import com.bx.carDVR.bylym.model.listener.OnDialogCallBackListener;
import com.bx.carDVR.bylym.model.listener.OnLocationListener;
import com.bx.carDVR.bylym.model.listener.OnNavContentResolverListener;
import com.calmcar.adas.apiserver.model.CdwDetectInfo;
import com.calmcar.adas.apiserver.model.LdwDetectInfo;
import com.calmcar.adas.apiserver.out.CvCameraViewFrame;

/**
 * @author Altair
 * @date :2020.03.26 下午 04:28
 * @description: 回调管理类
 */
public class NotifyMessageManager {
    private OnDialogCallBackListener mDialogCallBackListener;
    private OnDialogCallBackListener.OnShowFormatDialogListener mShowFormatDialogListener;
    private OnLocationListener mOnLocationListener;
    private Camera2Recorder.CvCameraViewListener2 cvCameraViewListener2;
	private OnDelayRecordingTimeListener mRecordingTimeListener;
	private OnNavContentResolverListener mResolverListener;

    public static NotifyMessageManager getInstance() {
        return SingletonHolder.sInstance;
    }

    private static class SingletonHolder {
        private static final NotifyMessageManager sInstance = new NotifyMessageManager();
    }
	 public void setOnDelayRecordingTimeListener(OnDelayRecordingTimeListener listener) {
        this.mRecordingTimeListener = listener;
    }
    public void setDialogCallBackListener(OnDialogCallBackListener listener) {
        this.mDialogCallBackListener = listener;
    }

    public void setShowFormatDialogListener(OnDialogCallBackListener.OnShowFormatDialogListener listener) {
        this.mShowFormatDialogListener = listener;
    }

    public void setOnNavContentResolverListener(OnNavContentResolverListener listener) {
        this.mResolverListener = listener;
    }

    public void updateDVRUI(int type) {
        if (mDialogCallBackListener != null) {
            mDialogCallBackListener.updateDVRUI(type);
        }
    }
    public void updateDVRUI(int type, LdwDetectInfo ldwDetectInfo, CdwDetectInfo cdwDetectInfo) {
        if (mDialogCallBackListener != null) {
            mDialogCallBackListener.updateDVRUI(type, ldwDetectInfo, cdwDetectInfo);
        }
    }

    public void showFormatDialog() {
        if (mShowFormatDialogListener != null) {
            mShowFormatDialogListener.showFormatDialog();
        }
    }

    public boolean showFormatDialogListener() {
        if (mShowFormatDialogListener == null) {
            return true;
        }
        return false;
    }

    public void setOnLocationListener(OnLocationListener listener) {
        this.mOnLocationListener = listener;
    }

    public void gpsSpeedChange() {
        if (mOnLocationListener == null) {
            return;
        }
        mOnLocationListener.gpsSpeedChanged();
    }

    public void setCvCameraViewListener(Camera2Recorder.CvCameraViewListener2 listener) {
        this.cvCameraViewListener2 = listener;
    }

    public void onCameraFrame(CvCameraViewFrame inputFrame) {
        if (cvCameraViewListener2 != null) {
            cvCameraViewListener2.onCameraFrame(inputFrame);
        }
    }

    public Boolean isCvCameraViewFrame() {
        if (cvCameraViewListener2 == null) {
            return true;
        } else {
            return false;
        }
    }
	
	 public void startLocation() {
        if (mOnLocationListener == null) {
            return;
        }
        mOnLocationListener.gpsSpeedChanged();
    }

    public void delayRecordingTime() {
        if (mRecordingTimeListener == null) {
            return;
        }
        mRecordingTimeListener.delayRecordingTime();
    }

    public void ContentResolverChange(int type,boolean isOpen) {
        if (mResolverListener != null) {
            mResolverListener.contentResolverChange(type, isOpen);
        }
    }

}
