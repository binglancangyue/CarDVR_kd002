package com.bx.carDVR;

public class Configuration {
    public static final boolean CAMERA2 = true;

    public static final int CAMERA_NUM = 1;
        public static final int[] CAMERA_IDS = {0, 1, 6, 7};
//    public static final int[] CAMERA_IDS = {0, 1, 6, 7};

    public static final boolean[] IS_USB_CAMERA = {false, false, false, false}; //是否是USB摄像头

    public static final boolean ENABLE_CAR_REVERSE = false;

    public static final boolean ONLY_TAKE_PHOTOS_WHILE_RECORDING = true;

    public static final String ACTION_CLOSE_DVR = "com.bx.carDVR.action_close";
    public static final String ACTION_SET_DVR_RECORD_TIME = "com.android.systemui.SET_DVR_RECORD_TIME";
    public static final String ACTION_SHOW_SETTING_WINDOW = "com.android.systemui.show_setting_window";
    public static final String ACTION_SET_G_SENSOR_LEVEL = "com.android.systemui.SET_G_SENSOR_LEVEL";
    public static final String ACTION_SET_ADAS_LEVEL = "com.android.systemui.SET_ADAS_LEVEL";
    public static final String ACTION_UPLOAD_VIDEO = "com.bx.carDVR.action.UPLOAD_VIDEO";
    public static final String ACTION_FORMAT_SD_CARD = "com.android.systemui.FORMAT_SD_CARD";
    public static final String ACTION_UPLOAD = "bixin.action.shangchuan";
    public static final String ACTION_START_SOS = "com.bx.carDVR.action.start_sos";
    public static final String ACTION_SYSTEMUI_CMD = "com.bx.carDVR.action.systemui_cmd";

    public static final String DVR_COLLISION = "Dvr_collision";
    public static final String DVR_ADAS = "Dvr_ADAS";
    public static final String ACTION_STOP_RECORD = "com.bixin.bxvideolist.action.stop_recording";
	public static final boolean IS_3IN = true;
    public static final boolean DVR_OPEN_MANUAL_ADJUSTMENT = false;// 手动调整ADAS准星
	
	public static final String ACTION_DVR_STATE = "com.bx.carDVR.action_dvr_state";
    public static final String ACTION_SHOW_STOP_RECORDING_DIALOG = "com.bx.carDVR.action.show_dialog";
    /*
    public static final int CAMERA_NUM = 4; //sofar 4 avin camera
    public static final int[] CAMERA_IDS = {4, 5, 6, 7};

    public static final boolean[] IS_USB_CAMERA = {false, false, false, false}; //是否是USB摄像头

    public static final boolean ENABLE_CAR_REVERSE = true;

     */

    /*
    public static final int CAMERA_NUM = 1; //tianyu 1 usb camera
    public static final int[] CAMERA_IDS = {0, 5, 6, 7};

    public static final boolean[] IS_USB_CAMERA = {true, false, false, false}; //是否是USB摄像头

    public static final boolean ENABLE_CAR_REVERSE = false;
    */

    public static final String CAMERA_RECORD_STATUS = "camera_record_status";
    public static final String CAMERA_MIC_STATUS = "camera_mic_status";
    public static final String CAMERA_ADAS_STATUS = "camera_adas_status";
    public static final String CAMERA_NAV_COLOR = "camera_nav_color";

}
