package com.bx.carDVR;

import android.app.Application;

/**
 * @author Altair
 * @date :2019.12.28 下午 02:27
 * @description:
 */
public class DvrApplication extends Application {
    private static DvrApplication application;
    private DVRService.RecorderInterface mRecorder;
    private String uploadInfo;
    private boolean isRecord = true;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;

        CrashUtil crashUtil = CrashUtil.getInstance();
        crashUtil.init(this);
    }

    public static DvrApplication getInstance() {
        return application;
    }

    public void setUploadInfo(String info) {
        this.uploadInfo = info;
    }
    public String getUploadInfo() {
        return uploadInfo;
    }
    public void setRecorderInterface(DVRService.RecorderInterface recorderInterface) {
        this.mRecorder = recorderInterface;
    }
    public DVRService.RecorderInterface getRecorderInterface() {
        return mRecorder;
    }

    public void setRecordStatus(boolean isRecord) {
        this.isRecord = isRecord;
    }
    public boolean getRecordStatus() {
        return this.isRecord;
    }
}
