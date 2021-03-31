package com.bx.carDVR.bylym.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bx.carDVR.AdasDrawView3;
import com.bx.carDVR.Camera2Recorder;
import com.bx.carDVR.Configuration;
import com.bx.carDVR.DVRFileList;
import com.bx.carDVR.DVRService;
import com.bx.carDVR.DvrApplication;
import com.bx.carDVR.R;
import com.bx.carDVR.Recorder;
import com.bx.carDVR.SettingInfo;
import com.bx.carDVR.bylym.TestSurFaceView;
import com.bx.carDVR.bylym.model.NotifyMessageManager;
import com.bx.carDVR.bylym.model.listener.OnDelayRecordingTimeListener;
import com.bx.carDVR.bylym.model.listener.OnDialogCallBackListener;
import com.bx.carDVR.bylym.model.listener.OnNavContentResolverListener;
import com.bx.carDVR.bylym.model.tools.DVRTools;
import com.bx.carDVR.bylym.model.tools.DialogTool;
import com.bx.carDVR.bylym.model.tools.FunctionTool;
import com.bx.carDVR.bylym.model.tools.RequestPermissionTool;
import com.bx.carDVR.bylym.model.tools.ToastTool;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

//cyk add 20200429
import com.calmcar.adas.apiserver.AdasConf;
import com.calmcar.adas.apiserver.out.CvCameraViewFrame;
import com.calmcar.adas.apiserver.AdasServer;
import com.calmcar.adas.apiserver.model.CdwDetectInfo;
import com.calmcar.adas.apiserver.model.FrontCarInfo;
import com.calmcar.adas.apiserver.model.LdwDetectInfo;
import com.calmcar.adas.apiserver.out.ActiveSuccessListener;
import com.calmcar.adas.apiserver.out.DetectInitSuccessListener;
import com.calmcar.adas.gps.LocationTickListener;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;

import java.text.DecimalFormat;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
//cyk add 20200429

/**
 * @author Altair
 * @date :2019.12.28 下午 03:23
 * @description:
 */
public class DVRHomeActivity extends AppCompatActivity implements View.OnClickListener,
        OnDialogCallBackListener, OnDialogCallBackListener.OnShowFormatDialogListener,
        AdasServer.CameraDataProcessCallBack, Camera2Recorder.CvCameraViewListener2, OnDelayRecordingTimeListener
        , OnNavContentResolverListener {
    private FrameLayout mFrameLayout;
    public static final String TAG = "DVRHomeActivity";
    private Context mContext;
    private CvCameraViewFrame mCvCameraViewFrame;
    private static final int SUB_PAGE_PICTURE = 0;
    private static final int SUB_PAGE_VIDEO = 1;
    private static final int SUB_PAGE_PLAY = 2;

    public static final int MODULE_VALUE = 1;//设置室内模式 室内模式一定写成0，室外模式一定写成1
    private boolean isOutModuleOpen = true;

    private static final int VIDEO_NUM = Configuration.CAMERA_NUM;
    //    private Handler mHandler = new Handler();shinei
    private InnerHandler mHandler;
    private ServiceConnection mSrvConnection;
    private DVRService.DVRSrvBinder mDVRSrvBinder;
    private DVRService.SettingInterface mSetting;
    private DVRService.RecorderInterface mRecorder;
    private DVRService.OnAutoRecordingStateChangeListener mAutoRecordingStateListener;
    private DVRService.OnCameraSignalStateChangeListener mCameraSignalStateListener;
    private Recorder.OnCameraConnectionStateChangeListener mCameraConnectionStateListener;
    private BroadcastReceiver mStorageDeviceConnectionReceiver;

    private int curSubPage;
    private boolean isRecording;
    private int mCurVideoId;
    private int otherVideoId;

    private DVRFileList mList;
    private Map<String, List<String>> mVideoList;
    private Map<String, List<String>> mPicList;
    private ArrayList<String> mVideoListArray;
    private ArrayList<String> mPicListArray;
    private ArrayList<String> mFileListArray;

    private int curRecordingTime = 0;
    private int curRecordingTime2 = 0;
    private Timer mRecordingTimer;
    private SurfaceView mVideoView;
    private SurfaceView mBackVideoView;
    private View mVideoBlackView;
    private View mNoSignalView;

    private View mRecordingDispView;
    private TextView mRecordingTimeView;

    private MediaPlayer mMediaPlayer;
    private Bitmap mBitmap;
    private ImageView imgBtnLock;
    private ImageView imgBtnTakePicture;
    private ImageView imgBtnRecord;
    private ImageView imgBtnADAS;
    private ImageView imgBtnMicrophone;
    private ImageView imgBtnSettings;

    private ImageButton imgBtnOperator;
    private ImageView ivRecordingIcon;
    private TextView tvRecordingTime;
    private FunctionTool mFunctionTool;
    //cyk add 20200429
    private static final int INITIAL_REQUEST = 1337;
    private AdasServer adasServer;
    private WarnSpeakManager warnSpeakerManager;
    private AdasDrawView3 adasDrawView;
    private TextView tvMapInfo;
    private int frameNum = 0;
    private CompositeDisposable compositeDisposable;

    private String[] permissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};
    public static final int REQUEST_CAMERA_CODE = 1000;
    private RequestPermissionTool requestPermissionTool;


    //cyk add 20200429
    private static final String ACTION_SHOW_NAVIGATION = "com.bx.carDVR.show.navigation";
    private DialogTool mDialogTool;
    private boolean isSOSEnable = false;
    private boolean isDoubleClick = false;

    //校准
    private Button btn_check_line;
    private LinearLayout ln_up_down, ln_left_right;
    private View v_up_down, v_left_right;
    private Button btn_set_line_ok;

    private int desLastX, desLastY;
    private int deadStartX;
    private int deadEndX;
    private RelativeLayout center_conf_rela;
    //    float mScale=0;
    private int deadStartY;
    private int deadEndY;
    ArrayList<VideoWriter> videoWriterArrayList;

    private int adjustAdas = View.GONE;
    private TextView tvGpsInfo;
    private TextView tvCarSpeed;
    private String gpsInfo;
    private String carSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            adjustAdas = savedInstanceState.getInt("adjustAdas");
            Log.d(TAG, "adjustAdas: " + (View.VISIBLE == adjustAdas));
        }

        Log.d(TAG, "onCreate: ");
        //cyk add 20200429
        setContentView(R.layout.activity_car_dvr_test);
//        startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
//        Window window = getWindow();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
//                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
//            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
//            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//            window.setStatusBarColor(0x00000000);
//            window.setNavigationBarColor(0x00000000);
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
//        }
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        this.mContext = this;
        mHandler = new InnerHandler(this);
        warnSpeakerManager = new WarnSpeakManager(mContext);
        mFunctionTool = new FunctionTool();
        mDialogTool = new DialogTool();
        compositeDisposable = new CompositeDisposable();
//        startService();
        requestPermissionTool = new RequestPermissionTool(mContext);
        requestPermissionTool.initPermission(permissions, this);
        NotifyMessageManager.getInstance().setDialogCallBackListener(this);
        NotifyMessageManager.getInstance().setOnDelayRecordingTimeListener(this);
        NotifyMessageManager.getInstance().setOnNavContentResolverListener(this);
        initView();
        initNewView();

        requestPermissionTool = new RequestPermissionTool(mContext);
        requestPermissionTool.initPermission(permissions, this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (center_conf_rela != null) {
            outState.putInt("adjustAdas", center_conf_rela.getVisibility());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onProcessBack(LdwDetectInfo ldwDetectInfo, CdwDetectInfo cdwDetectInfo) {
        checkAdasWarnInfo(ldwDetectInfo, cdwDetectInfo);
        adasDrawView.drawBitmap(ldwDetectInfo, cdwDetectInfo);
    }

    private void initView() {
        curSubPage = SUB_PAGE_VIDEO;
        adasDrawView = findViewById(R.id.adasDrawView);
        adasDrawView.setVisibility(View.VISIBLE);
        tvGpsInfo = findViewById(R.id.tv_gps_info);
        tvCarSpeed = findViewById(R.id.tv_car_speed);
//        setGesture();
        tvMapInfo = findViewById(R.id.position_text_view);
//        mFrameLayout = findViewById(R.id.fl_camera);
//        if (mVideoPageParent == null) {
//            View parent = getLayoutInflater().inflate(R.layout.camera_layout,null );
//            View parent = getLayoutInflater().inflate(R.layout.camera_layout, mFrameLayout, true);
//            mVideoPageParent = (ViewGroup) parent;
        initVideoView();
        imgBtnMicrophone.setSelected(!mFunctionTool.isMicrophoneMute());
        //        }
//        mFrameLayout.addView(mVideoPageParent);

        if (mRecorder != null && mRecorder.getCameraConnectionState(mCurVideoId) == Recorder.CAMERA_CONNECTED) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    addPreviewVideoView();
                }
            }, 200);
        }
    }


    private LocationTickListener locationTickListener = new LocationTickListener() {
        @Override
        public void onTickArrive(final double longitude, final double latitude, final double rate) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateStateView(longitude, latitude, rate);
                }
            });

        }
    };

    public void updateStateView(double longitude, double latitude, double rate) {
        DecimalFormat df1 = new DecimalFormat("######0.0");
        DecimalFormat df2 = new DecimalFormat("######0.00000");
        if (adasServer.isValidLocationState()) {
            tvMapInfo.setText("经度：" + df2.format(longitude) + "\n" + "纬度：" + df2.format(latitude) + "\n"
                    + "速度：" + df1.format(rate) + "km/h");
            adasServer.updateCarSpeed(rate);
        } else {
            tvMapInfo.setText("");
        }
    }

    public void setAdasView(boolean isOpen) {
        Log.d("carAdas", "adasView:" + adasDrawView + "  mRecorder:" + mRecorder + " isOpen:" + isOpen);
        if (adasDrawView == null || imgBtnADAS == null) {
            return;
        }
        if (isOpen) {
            imgBtnADAS.setSelected(true);
            adasDrawView.setVisibility(View.VISIBLE);
            mRecorder.isOpenADAS(1, true);
        } else {
            adasDrawView.setVisibility(View.GONE);
            mRecorder.isOpenADAS(1, false);
            imgBtnADAS.setSelected(false);
        }
    }

    private void toLock() {
        if (isSOSEnable) {
//                    imgBtnLock.setSelected(true);


            mRecorder.lockFile(false);
        } else {
            ToastTool.showToast(R.string.please_open_record);
        }
    }

    private void openVoice() {
        boolean open = imgBtnMicrophone.isSelected();
        isOpen(open, imgBtnMicrophone);
        mFunctionTool.setMicrophoneMute(open);
    }

    @Override
    public void onClick(View view) {
        hideNavigationBar();
        switch (view.getId()) {
            case R.id.btn_lock:
                toLock();
                break;
            case R.id.btn_take_picture:
                if (Configuration.ONLY_TAKE_PHOTOS_WHILE_RECORDING && isRecording) {
                    onClickTakePicture();
                }
                break;
            case R.id.btn_record:
                onClickRecordingVideo();
                break;
            case R.id.btn_adas:
                startADAS();
                break;
            case R.id.btn_microphone:
                boolean open = imgBtnMicrophone.isSelected();
                isOpen(open, imgBtnMicrophone);
                mFunctionTool.setMicrophoneMute(open);
                break;
            case R.id.btn_settings:
                if (isRecording || mRecorder.isCurrentlyAutoRecording()) {
                    mDialogTool.showSettingDialog(mContext);
                } else {
                    sendBroadcastForHideNavigationBar();
                }
                break;
            case R.id.id_btn_set_line_ok:
                mDVRSrvBinder.setShowADASLine(false);
                float fcX = (desLastX) / mScaleWidth;
                float fcY = (desLastY) / mScaleHeight;
                Log.d(TAG, "onClick:fcX " + (int) fcX + " fcY " + (int) fcY);
                adasServer.setVPPara((int) fcX, (int) fcY);
                center_conf_rela.setVisibility(View.GONE);
                break;
            case R.id.video_preview:
                doubleClick();
                break;
//            default:
//                Log.d("aaa", "onClick:SUB_PAGE_SET ");
//                switchToSubPage(SUB_PAGE_SET);
//                break;
        }
    }

    private void isOpen(boolean isOpen, ImageView imageView) {
        imageView.setSelected(!isOpen);
    }

    private void startADAS() {
        boolean isOpen = imgBtnADAS.isSelected();
        isOpen(isOpen, imgBtnADAS);
        if (isOpen) {
            center_conf_rela.setVisibility(View.GONE);
        }
        if (mDVRSrvBinder != null) {
            setAdasView(!isOpen);
//                    startRecording();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ");
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(option);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
//            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
//        imgBtnMicrophone.setSelected(!mFunctionTool.isMicrophoneMute());
//        Log.d(TAG, "onStart: mFunctionTool.isMicrophoneMute() " + mFunctionTool.isMicrophoneMute());
        /*if(mHandler != null){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideNavigationBar();
                }
            },500);
        }*/
        hideNavigationBar();
        startService();
        if (Settings.Global.getInt(getContentResolver(), Configuration.CAMERA_ADAS_STATUS, 0) == 1) {
            updateIconState(1, true);
        } else {
            updateIconState(1, false);
        }
        if (Settings.Global.getInt(getContentResolver(), Configuration.CAMERA_RECORD_STATUS, 0) == 1) {
            updateIconState(0, true);
        } else {
            updateIconState(0, false);
        }
        if (Settings.Global.getInt(getContentResolver(), Configuration.CAMERA_MIC_STATUS, 0) == 1) {
            updateIconState(2, true);
        } else {
            updateIconState(2, false);
        }
        Settings.Global.putInt(mContext.getContentResolver(), Configuration.CAMERA_NAV_COLOR, 1);
    }

    public void startService() {
        Intent intent = new Intent(this, DVRService.class);
        mSrvConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mDVRSrvBinder = (DVRService.DVRSrvBinder) service;

                if (mDVRSrvBinder == null) {
                    Log.w(TAG, "binder is null !");
                    return;
                }

                DVRService service1 = mDVRSrvBinder.getService();
                service1.setSpeedCallBack(new DVRService.SpeedCallback() {
                    @Override
                    public void onSpeedChange(double speed) {
                        if (adasServer != null && isOutModuleOpen) {
                            Log.d("adasSpeed", "adasspeed:adasServer" + speed);
                            adasServer.updateCarSpeed(speed);
                        }
                    }
                });
                Log.d("adasSpeed", "setcallback succuss");

                onDVRServiceConnected();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("carAdas", "mRecorder:" + mRecorder);
                        if (mRecorder != null) {
                            setAdasView(mRecorder.getIsOpenADAS(1));
                        }
                    }
                });
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                onDVRServiceDisconnected();
            }
        };

        startService(intent);
        bindService(intent, mSrvConnection, Context.BIND_AUTO_CREATE);
        registerReceiver();
//        hideNavigationBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        if (mRecorder != null && mRecorder.isCurrentlyAutoRecording()) {
            onAutoRecordingStart();
        }
        if (mRecorder != null) {
            imgBtnRecord.setSelected(mRecorder.isCurrentlyAutoRecording());
        }
        NotifyMessageManager.getInstance().setShowFormatDialogListener(this);
        Log.d(TAG, "onResume: " + getApplicationContext().getResources().getDisplayMetrics().densityDpi);
    }

    @Override
    protected void onPause() {
//        stopRecording();
//        stopRecordingTimer();
        super.onPause();
        Log.d(TAG, "onPause: ");
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addDataScheme("file");
        mStorageDeviceConnectionReceiver = new StorageDeviceConnectionReceiver();
        registerReceiver(mStorageDeviceConnectionReceiver, filter);
    }

    private void showNavigationBar(boolean show) {
        Intent intent = new Intent();
        intent.setAction(ACTION_SHOW_NAVIGATION);
        intent.putExtra("show", show);
//        intent.setComponent(new ComponentName("com.android.systemui",
//                "com.android.systemui.navigationbar.NavigationBar"));
        Log.d(TAG, "showNavigationBar: show " + show);
//        sendBroadcast(intent);
    }

    private void hideNavigationBar() {
        if (!Configuration.IS_3IN) {
            return;
        }
        int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        uiFlags |= 0x00001000;
//        getWindow().getDecorView().setSystemUiVisibility(uiFlags);
    }

    private void showNavigationBar() {
        if (!Configuration.IS_3IN) {
            return;
        }
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void onDVRServiceConnected() {
        mDVRSrvBinder.notifyRecordingComponent(this);
        mSetting = mDVRSrvBinder.getSettingInterface();
        mRecorder = mDVRSrvBinder.getRecorderInterface();
        DvrApplication.getInstance().setRecorderInterface(mRecorder);
        initADAS1();
//        toInitADAS();
        adasServer.setCameraDataProcessCallBack(this);
        NotifyMessageManager.getInstance().setCvCameraViewListener(this);
        mAutoRecordingStateListener = new DVRService.OnAutoRecordingStateChangeListener() {
            @Override
            public void onStart() {
                onAutoRecordingStart();
            }

            @Override
            public void onStop() {
                onAutoRecordingStop();
            }
        };
        mCameraSignalStateListener = new DVRService.OnCameraSignalStateChangeListener() {
            @Override
            public void onSignalGot() {
                onCameraSignalGot();
            }

            @Override
            public void onSignalLost() {
                onCameraSignalLost();
            }
        };
        mCameraConnectionStateListener = new Recorder.OnCameraConnectionStateChangeListener() {
            @Override
            public void onConnect() {
                onCameraConnected();
            }

            @Override
            public void onDisconnect() {
                onCameraDisconnected();
            }
        };

        mRecorder.registerOnAutoRecordingStateChangeListener(mAutoRecordingStateListener);
        mRecorder.registerOnCameraConnectionStateChangeListener(mCurVideoId,
                mCameraConnectionStateListener);
        mRecorder.registerOnCameraSignalStateChangeListener(mCurVideoId,
                mCameraSignalStateListener);

        //add camera0 start
        mRecorder.registerOnCameraConnectionStateChangeListener(0,
                mCameraConnectionStateListener);
        mRecorder.registerOnCameraSignalStateChangeListener(0,
                mCameraSignalStateListener);
        //end

        if (mRecorder.getCameraConnectionState(mCurVideoId) == Recorder.CAMERA_CONNECTED) {
            onCameraConnected();
        }
//        if (Configuration.DVR_OPEN_MANUAL_ADJUSTMENT) {
        initNewView();
//        }
    }

    private void initADAS1() {
        if (adasServer == null) {
            adasServer = mDVRSrvBinder.getADASServer();
        }
        adasServer.setCameraDataProcessCallBack(this);

        warnSpeakerManager.initVideoPlayers(R.raw.car_out_line, R.raw.front_car_warn_level_one
                , R.raw.front_car_warn_level_two, R.raw.front_car_launch, R.raw.adas_start_warn
        );
    }

    private void initADAS() {
        if (adasServer == null) {
            adasServer = mDVRSrvBinder.getADASServer();
        }

        adasServer.setActiveSuccessListener(new ActiveSuccessListener() {
            @Override
            public void onActiveCallBack(String type) {
                String type0 = type + "";
                Log.d(TAG, "onActiveCallBack:type " + type0);
                //type :1 首次激活成功  10 已经激活  2  未经授权，激活失败 3  网络错误，激活失败
            }
        });

        adasServer.setDetectInitSuccessListener(new DetectInitSuccessListener() {
            @Override
            public void onInitSuccess() {

                /** adasServer.processDataAsyn(new com.calmcar.adas.apiserver.model.JavaCameraFrame( new Mat(480 + (480 / 2),
                 640, CvType.CV_8UC1), 640,
                 480, ImageFormat.NV21));**/
                Log.d(TAG, "onInitSuccess"); //cyk success

            }
        });
        adasServer.setCameraDataProcessCallBack(this);
        adasServer.initConf(1280, 720);
        adasServer.setAdasServerRunMode(MODULE_VALUE);//设置室内模式 室内模式一定写成0，室外模式一定写成1

        if (AdasConf.VP_X < 0 || AdasConf.VP_Y < 0) {
            adasServer.setVPPara(640, 360);
        }

        //setCvCameraViewListener(this);
        warnSpeakerManager.initVideoPlayers(R.raw.car_out_line, R.raw.front_car_warn_level_one
                , R.raw.front_car_warn_level_two, R.raw.front_car_launch, R.raw.adas_start_warn
        );
//        adasServer.startServer(locationTickListener);//调用gps方法
//        updateCarSpeed(0);
        adasServer.setAdasServerModuleState(true, 0);
        adasServer.setFrontCarWarnSencitivity(5);
        adasServer.setLaneWarnSensitivity(0.74f);
    }

    private void onDVRServiceDisconnected() {
        if (mRecorder != null) {
            mRecorder.unregisterOnAutoRecordingStateChangeListener(mAutoRecordingStateListener);
            mRecorder.unregisterOnCameraConnectionStateChangeListener(mCurVideoId,
                    mCameraConnectionStateListener);
            mRecorder.unregisterOnCameraSignalStateChangeListener(mCurVideoId,
                    mCameraSignalStateListener);
            if (mRecorder.getCameraConnectionState(mCurVideoId) == Recorder.CAMERA_CONNECTED) {
                onCameraDisconnected();
                Log.d(TAG, "onDVRServiceDisconnected: ");
            }
            //add start
            if (Configuration.CAMERA_NUM == 2) {
                int otherCameraId;
                if (mCurVideoId == 0) {
                    otherCameraId = 1;
                } else {
                    otherCameraId = 0;
                }
                mRecorder.unregisterOnCameraConnectionStateChangeListener(otherCameraId,
                        mCameraConnectionStateListener);
                mRecorder.unregisterOnCameraSignalStateChangeListener(otherCameraId,
                        mCameraSignalStateListener);
            }
            //end
        }
        if (mDVRSrvBinder != null) {
            mDVRSrvBinder.cancelRecordingComponent(this);
            mDVRSrvBinder = null;
        }
        mSetting = null;
        mRecorder = null;
    }

    private void addPreviewVideoView() {
        if (curSubPage != SUB_PAGE_PICTURE && curSubPage != SUB_PAGE_VIDEO) {
            return;
        }

        if (mVideoView != null) {
            mVideoView.setVisibility(View.VISIBLE);
            //add back camera start
            if (Configuration.CAMERA_NUM == 2) {
                mBackVideoView.setVisibility(View.VISIBLE);
            } else {
                mBackVideoView.setVisibility(View.GONE);
            }
            //end
        }
    }

    private void onCameraConnected() {
        if (curSubPage == SUB_PAGE_PICTURE || curSubPage == SUB_PAGE_VIDEO) {
            if (mVideoView != null) {
                mVideoView.setVisibility(View.INVISIBLE);
                if (Configuration.CAMERA_NUM == 2) {
                    mBackVideoView.setVisibility(View.INVISIBLE);
                }
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        addPreviewVideoView();
                    }
                }, 200);
            }
        }

        if (mRecorder != null && mRecorder.isCurrentlyAutoRecording()) {
            onAutoRecordingStart();
        }
    }

    private void onCameraDisconnected() {
        if (curSubPage == SUB_PAGE_PICTURE || curSubPage == SUB_PAGE_VIDEO) {
            showBlackVideo();
            if (mVideoView != null) {
                mVideoView.setVisibility(View.INVISIBLE);
                if (Configuration.CAMERA_NUM == 2) {
                    mBackVideoView.setVisibility(View.INVISIBLE);
                }
            }

            mRecordingDispView.setVisibility(View.GONE);
//            mRecordingBtn.setImageResource(R.drawable.recording_selector);
        }

        stopRecording();
        stopRecordingTimer();
    }

    private void onCameraSignalGot() {
        if (curSubPage == SUB_PAGE_PICTURE || curSubPage == SUB_PAGE_VIDEO) {
            showBlackVideo();
            mNoSignalView.setVisibility(View.INVISIBLE);
            mHandler.postDelayed(removeBlackVideo, 500);
        }
    }

    private void onCameraSignalLost() {
        if (curSubPage == SUB_PAGE_PICTURE || curSubPage == SUB_PAGE_VIDEO) {
            showBlackVideo();
            mNoSignalView.setVisibility(View.VISIBLE);
        }
    }

    private void onAutoRecordingStart() {
//        isRecording = false;
        isRecording = true;
        if (curSubPage == SUB_PAGE_PICTURE || curSubPage == SUB_PAGE_VIDEO) {
            mRecordingDispView.setVisibility(View.VISIBLE);
//            mRecordingBtn.setImageResource(R.drawable.stop_record_selector);
            Log.d(TAG, "onAutoRecordingStart: ");
            mHandler.sendEmptyMessage(2);
            startRecordingTimer();
        }
    }

    private void onAutoRecordingStop() {
        isRecording = false;
        stopRecordingTimer();

        if (curSubPage == SUB_PAGE_PICTURE || curSubPage == SUB_PAGE_VIDEO) {
            mRecordingDispView.setVisibility(View.GONE);
            imgBtnRecord.setSelected(false);
        }
    }

    private void showBlackVideo() {
        mHandler.removeCallbacks(removeBlackVideo);
//        mVideoBlackView.setVisibility(View.VISIBLE);
    }

    private Runnable removeBlackVideo = new Runnable() {
        @Override
        public void run() {
            if (curSubPage != SUB_PAGE_PICTURE && curSubPage != SUB_PAGE_VIDEO) {
                return;
            }

            mVideoBlackView.setVisibility(View.INVISIBLE);
        }
    };

    private void onStorageDeviceUnmounted(final String devPath) {
        if (mSetting != null && mSetting.isDeviceMatchingSetPath(devPath)) {
            stopRecording();
            stopRecordingTimer();

            if (curSubPage == SUB_PAGE_PLAY) {
                updateListInfo();
            }
        }
    }

    private void updateListInfo() {
        boolean hasList = true;
        if (mRecorder != null) {
            mList = mRecorder.getFileList(mCurVideoId);
            if (mList == null) {
                hasList = false;
            }
        } else {
            mList = null;
            hasList = false;
        }
//        mListSwitcher.removeAllViews();
        if (hasList) {
            mVideoList = mList.getVideoList();
            mPicList = mList.getPictureList();
        }
    }

    private void formatCard() {
        if (mSetting != null) {
            int strId = -1;
            int result = mSetting.formatStorage();
            Log.d(TAG, "formatCard:result " + result);
            if (result == DVRService.FORMATTING_RST_FAIL_CURRENTLY_RECORDING) {
                strId = R.string.cannot_formatting;
            } else if (result == DVRService.FORMATTING_RST_FAIL_CARD_INVALID) {
                strId = R.string.card_not_exist;
            } else if (result == DVRService.FORMATTING_RST_FAIL) {
                strId = R.string.formatting_fail;
            }

            if (strId != -1) {
                ToastTool.showToast(strId);
            }
        }
    }

    private void onClickTakePicture() {
        if (mRecorder != null) {
            Log.d(TAG, "onClickTakePicture:mCurVideoId " + mCurVideoId);
            mRecorder.cameraTakePicture(mCurVideoId, new Recorder.OnTakePictureFinishListener() {
                @Override
                public void onFinish(int result, String photoPath) {
                    int strId = -1;

                    if (result == Recorder.TAKE_PIC_RST_SUCCESSFUL) {
                        strId = R.string.photo_saved;
                    } else if (result == Recorder.TAKE_PIC_RST_FAIL_NO_CAMERA) {
                        strId = R.string.camera_not_found;
                    } else if (result == Recorder.TAKE_PIC_RST_FAIL_CARD_INVALID) {
                        strId = R.string.card_not_exist;
                    } else if (result == Recorder.TAKE_PIC_RST_FAIL_CURRENTLY_RECORDING) {
                        strId = R.string.cannot_take_pic;
                    } else if (result == Recorder.TAKE_PIC_RST_FAIL) {
                        strId = R.string.take_pic_fail;
                    }
                    Log.d(TAG, "onFinish: strId： " + result + " photoPath " + photoPath);
                    if (strId != -1) {
                        ToastTool.showToast(strId);

                    }
                }
            });

            if (Configuration.CAMERA_NUM == 2) {
                int cameraBackId;
                if (mCurVideoId == 1) {
                    cameraBackId = 0;
                } else {
                    cameraBackId = 1;
                }
                mRecorder.cameraTakePicture(cameraBackId,
                        new Recorder.OnTakePictureFinishListener() {
                            @Override
                            public void onFinish(int result, String photoPath) {

                            }
                        });
            }
        } else {
            Log.d(TAG, "onClickTakePicture:mRecorder != null ");
        }
    }

    private void onClickRecordingVideo() {
        if (mRecorder != null) {
            Log.d(TAG, "onClickRecordingVideo: " + mRecorder.isCurrentlyAutoRecording());
            if (mRecorder.isCurrentlyAutoRecording()) {
                mDialogTool.showStopRecordingDialog(mContext);
//                mSetting.stopAutoRecording();
//                Toast.makeText(this, R.string.auto_recording, Toast.LENGTH_SHORT).show();
            } else {
                if (isRecording) {
                    if (curRecordingTime >= 2) {
//                        stopRecording();
                        mDialogTool.showStopRecordingDialog(mContext);
                    }
                } else {
                    startRecording();
                }
            }
        } else {
            ToastTool.showToast(R.string.camera_not_found);
        }
    }

    private void startRecording() {
        if (!isRecording && mRecorder != null) {
            int strId = -1;
            int result = mRecorder.cameraStartRecording(mCurVideoId);
            //add start
            int cameraBackId;
            if (mCurVideoId == 1) {
                cameraBackId = 0;
            } else {
                cameraBackId = 1;
            }
            if (Configuration.CAMERA_NUM == 2) {
                mRecorder.cameraStartRecording(cameraBackId);
            }
            //end
            if (result == Recorder.RECORDING_RST_SUCCESSFUL) {
                isRecording = true;
                curRecordingTime = 0;
                curRecordingTime2 = 0;
                mRecordingDispView.setVisibility(View.VISIBLE);
//                mRecordingBtn.setImageResource(R.drawable.stop_record_selector);
                mHandler.sendEmptyMessage(2);
                startRecordingTimer();
            } else if (result == Recorder.RECORDING_RST_FAIL_NO_CAMERA) {
                strId = R.string.camera_not_found;
            } else if (result == Recorder.RECORDING_RST_FAIL_CARD_INVALID) {
                strId = R.string.card_not_exist;
            } else if (result == Recorder.RECORDING_RST_FAIL_CURRENTLY_RECORDING) {
                strId = R.string.cannot_recording;
            } else if (result == Recorder.RECORDING_RST_FAIL) {
                strId = R.string.recording_fail;
            }

            if (strId != -1) {
                ToastTool.showToast(strId);
            }
        }
    }

    private void stopRecording() {
        if (isRecording) {
            if (mRecorder != null) {
                ToastTool.hideLongToast();
                mRecorder.cameraStopRecording(mCurVideoId);
                //add start
                int cameraBackId;
                if (mCurVideoId == 1) {
                    cameraBackId = 0;
                } else {
                    cameraBackId = 1;
                }
                if (Configuration.CAMERA_NUM == 2) {
                    mRecorder.cameraStopRecording(cameraBackId);
                }
                //emd
            }
            imgBtnRecord.setSelected(false);
            isRecording = false;
            mRecordingDispView.setVisibility(View.GONE);
//            mRecordingBtn.setImageResource(R.drawable.recording_selector);
            stopRecordingTimer();
        }
    }

    private void startRecordingTimer() {
        mHandler.sendEmptyMessage(2);
        Log.d(TAG, "startRecordingTimer: ");
        if (mRecordingTimer == null) {
            imgBtnRecord.setSelected(true);
            mRecordingTimer = new Timer();
            mRecordingTimer.schedule(new TimerTask() {
                private Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
//                        adasServer.updateCarSpeed(60.0);
                        updateRecordingTime();
//                        refreshRecordingIcon();
                    }
                };

                @Override
                public void run() {
                    mHandler.removeCallbacks(runnable);
                    mHandler.post(runnable);
                }
            }, 0, 1000);
            if (!mRecorder.isCurrentlyAutoRecording()) {
                mRecordingTimer.schedule(new TimerTask() {
                    private Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            if (isRecording) {
                                curRecordingTime++;
                                curRecordingTime2++;
                                if (mSetting != null && mRecorder != null) {
                                    if (curRecordingTime >= (mSetting.settingGetRecordingTime() / SettingInfo.TIME_INTERVAL_1_MINUTE * 60)) {
                                        //mRecorder.cameraStopRecording(mCurVideoId);
                                        Log.d(TAG, "run: curRecordingTime " + curRecordingTime);
                                        curRecordingTime = 0;
                                        curRecordingTime2 = 0;
                                        if (mRecorder.cameraStartRecording(mCurVideoId) != Recorder.RECORDING_RST_SUCCESSFUL) {
                                            stopRecording();
                                        }
                                    }
                                }
                            }
                        }
                    };

                    @Override
                    public void run() {
                        mHandler.removeCallbacks(runnable);
                        mHandler.post(runnable);
                    }
                }, 300, 1000);
            }
        }
    }

    private void stopRecordingTimer() {
        if (mRecordingTimer != null) {
            mRecordingTimer.cancel();
            mRecordingTimer = null;
        }
    }

    private void updateRecordingTime() {
       /* if (isRecording) {
            String time = String.format("%02d:%02d", curRecordingTime / 60 % 60,
                    curRecordingTime % 60);
            mRecordingTimeView.setText(time);
        } else if (mRecorder != null && mRecorder.isCurrentlyAutoRecording()) {
            int time = mRecorder.getCurrentRecordingTime(mCurVideoId);
            String times = String.format("%02d:%02d", time / 60 % 60, time % 60);
            mRecordingTimeView.setText(times);
        }*/


        if (mRecorder != null && mRecorder.isCurrentlyAutoRecording()) {
            int time = mRecorder.getCurrentRecordingTime(mCurVideoId);
            String times = String.format("%02d:%02d", time / 60 % 60, time % 60);
            mRecordingTimeView.setText(times);
//            Log.d(TAG, "updateRecordingTime:isCurrentlyAutoRecording ");
        } else {
            if (isRecording) {
                String time = String.format("%02d:%02d", curRecordingTime2 / 60 % 60,
                        curRecordingTime2 % 60);
                mRecordingTimeView.setText(time);
            }
        }
    }

    private void refreshRecordingIcon() {
        if (isRecording) {
            /*if ((curRecordingTime % 2) == 0) {
                mRecordingBtn.setImageResource(R.drawable.stop_record_selector);
            } else {
                mRecordingBtn.setImageResource(R.drawable.recording_selector);
            }*/
        } else if (mRecorder != null && mRecorder.isCurrentlyAutoRecording()) {
           /* int time = mRecorder.getCurrentRecordingTime(mCurVideoId);
            if ((time % 2) == 0) {
                mRecordingBtn.setImageResource(R.drawable.stop_record_selector);
            } else {
                mRecordingBtn.setImageResource(R.drawable.recording_selector);
            }*/
        }
    }

    private boolean switchVideo(int idx) {
        if (idx == mCurVideoId || idx < 0 || idx >= VIDEO_NUM) {
            return false;
        }

        if (isRecording) {
            return false;
        }

        if (mRecorder != null && mRecorder.isCurrentlyAutoRecording()) {
            return false;
        }

        if (curSubPage == SUB_PAGE_PICTURE || curSubPage == SUB_PAGE_VIDEO) {
            showBlackVideo();
            if (mVideoView != null) {
                mVideoView.setVisibility(View.INVISIBLE);
                mBackVideoView.setVisibility(View.INVISIBLE);
                if (mRecorder != null && mRecorder.getCameraConnectionState(idx) == Recorder.CAMERA_CONNECTED) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            addPreviewVideoView();
                        }
                    }, 10);
                }
            }
        }

        Log.d(TAG, "switchVideo idx: " + idx);
        if (mRecorder != null) {
            mRecorder.unregisterOnCameraConnectionStateChangeListener(mCurVideoId,
                    mCameraConnectionStateListener);
            mRecorder.unregisterOnCameraSignalStateChangeListener(mCurVideoId,
                    mCameraSignalStateListener);

            //add by lym
            if (Configuration.CAMERA_NUM == 2) {
                mRecorder.unregisterOnCameraConnectionStateChangeListener(idx,
                        mCameraConnectionStateListener);
                mRecorder.unregisterOnCameraSignalStateChangeListener(idx,
                        mCameraSignalStateListener);
                mRecorder.registerOnCameraConnectionStateChangeListener(mCurVideoId,
                        mCameraConnectionStateListener);
                mRecorder.registerOnCameraSignalStateChangeListener(mCurVideoId,
                        mCameraSignalStateListener);
            }
            //end

            mRecorder.registerOnCameraConnectionStateChangeListener(idx,
                    mCameraConnectionStateListener);
            mRecorder.registerOnCameraSignalStateChangeListener(idx, mCameraSignalStateListener);
        }

        mCurVideoId = idx;
        return true;
    }

    private void initVideoView() {
//        View parent = mVideoPageParent;
        FrameLayout parent = findViewById(R.id.preview_parent);
        parent.setOnClickListener(this);
        mVideoView = findViewById(R.id.video_preview);
        mVideoView.setOnClickListener(this);
//        mVideoView.setFrameSize(1280, 720);//960*540
//        mVideoView.setVisibility(SurfaceView.VISIBLE);
////        mVideoView.setCvCameraViewListener(this);
////        mVideoView.setOnClickListener(this);
////        mVideoView.setMaxFrameSize(1280, 720);//864*480
////        mVideoView.enableFpsMeter();

        mBackVideoView = findViewById(R.id.video_back_preview);
        mVideoView.setVisibility(View.INVISIBLE);
        mBackVideoView.setVisibility(View.INVISIBLE);
        initCameraBtn();
        mVideoView.getHolder().setKeepScreenOn(true);
        mBackVideoView.getHolder().setKeepScreenOn(true);
        mVideoView.setOnClickListener(this);
        mVideoView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surfaceChanged:mCurVideoId " + mCurVideoId);
                Log.d("ssession", "surfaceChanged mRecorder:" + mRecorder + "  format:" + format + "  width:" + width + " height:" + height + " id:" + this);
                if (mRecorder != null) {
                    if (width > 0 && height > 0) {
                        mRecorder.cameraStartPreview(mCurVideoId, holder);
                        showBlackVideo();
                        if (mRecorder.isOpenCamera(mCurVideoId)) {
                            mHandler.postDelayed(removeBlackVideo, 500);
                            mNoSignalView.setVisibility(View.INVISIBLE);
                        } else {
                            mNoSignalView.setVisibility(View.VISIBLE);
                        }
//                        if (mRecorder.cameraIsSignalNormal(mCurVideoId)) {
//                            mHandler.postDelayed(removeBlackVideo, 500);
//                            mNoSignalView.setVisibility(View.INVISIBLE);
//                        } else {
//                            mNoSignalView.setVisibility(View.VISIBLE);
//                        }
                    }
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d("ssession", "surfaceDestroyed mRecorder:" + mRecorder + "id:" + this);
                if (mRecorder != null) {
                    Log.d(TAG, "surfaceDestroyed:mCurVideoId " + mCurVideoId);
                    mRecorder.cameraStopPreview(mCurVideoId);
                }
                showBlackVideo();
            }

        });

        if (Configuration.CAMERA_NUM == 2) {
            mBackVideoView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {

                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                           int height) {
                    Log.d("ssession", "mBackVideoView surfaceChanged mRecorder:" + mRecorder + "  format:" + format + "  width:" + width + " height:" + height + " id:" + this);
                    if (mRecorder != null) {
                        if (width > 0 && height > 0) {
                            if (mCurVideoId == 0) {
                                otherVideoId = 1;
                            } else {
                                otherVideoId = 0;
                            }
                            Log.d(TAG, "surfaceChanged:otherVideoId " + otherVideoId);
                            mRecorder.cameraStartPreview(otherVideoId, holder);

                            showBlackVideo();
//                        Log.d(TAG, "surfaceChanged:mCurVideoId: " +
//                                mRecorder.cameraIsSignalNormal(mCurVideoId) +
//                                " cameraId: " + mRecorder.cameraIsSignalNormal(otherVideoId));
                            if (mRecorder.cameraIsSignalNormal(otherVideoId)) {
                                mBackVideoView.setVisibility(View.VISIBLE);
                            } else {
                                mBackVideoView.setVisibility(View.GONE);
                            }
                        }
                    }
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    Log.d("ssession", "surfaceDestroyed mRecorder:" + mRecorder + "id:" + this);
                    Log.d(TAG, "surfaceDestroyed:otherVideoId " + otherVideoId);
                    if (mRecorder != null) {
                        mRecorder.cameraStopPreview(otherVideoId);
                    }
                    showBlackVideo();
                }
            });
        }

        mVideoBlackView = findViewById(R.id.video_black);
        mNoSignalView = findViewById(R.id.no_signal_str);
        mVideoBlackView.setVisibility(View.INVISIBLE);

        mRecordingDispView = findViewById(R.id.recording_disp);
        mRecordingTimeView = findViewById(R.id.recording_time);

        if (VIDEO_NUM > 1) {
/*            int[] btn_ids = {R.id.button_ch1, R.id.button_ch2, R.id.button_ch3, R.id.button_ch4};
            for (int i = 0; i < VIDEO_NUM; i++) {
                final int idx = i;
                mSelectVideoBtns[i] = parent.findViewById(btn_ids[i]);
                mSelectVideoBtns[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mCurVideoId != idx) {
                            int lastId = mCurVideoId;
                            if (switchVideo(idx)) {
                                mSelectVideoBtns[lastId].setSelected(false);
                                mSelectVideoBtns[idx].setSelected(true);
                            }
                        }
                    }
                });
                mSelectVideoBtns[i].setVisibility(View.VISIBLE);
            }
            parent.findViewById(R.id.select_video_key).setVisibility(View.VISIBLE);
            mSelectVideoBtns[mCurVideoId].setSelected(true);*/

            mBackVideoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int id;
                    if (mCurVideoId != 0) {
                        id = 0;
                    } else {
                        id = 1;
                    }
                    switchVideo(id);
                }
            });
        }
    }

    private void initCameraBtn() {
        imgBtnLock = findViewById(R.id.btn_lock);
        imgBtnTakePicture = findViewById(R.id.btn_take_picture);
        imgBtnRecord = findViewById(R.id.btn_record);
        imgBtnADAS = findViewById(R.id.btn_adas);
        imgBtnMicrophone = findViewById(R.id.btn_microphone);
        imgBtnSettings = findViewById(R.id.btn_settings);
        if (Configuration.IS_3IN) {
            imgBtnLock.setVisibility(View.VISIBLE);
        } else {
            imgBtnLock.setVisibility(View.GONE);
        }
//        ivRecordingIcon = findViewById(R.id.iv_recording_icon);
//        tvRecordingTime = findViewById(R.id.tv_recording_time);
        imgBtnLock.setOnClickListener(this);
        imgBtnTakePicture.setOnClickListener(this);
        imgBtnRecord.setOnClickListener(this);
        imgBtnADAS.setOnClickListener(this);
        imgBtnMicrophone.setOnClickListener(this);
        imgBtnSettings.setOnClickListener(this);
        closeSOS();
    }

    @Override
    public void updateDVRUI(int type) {
        if (type == 0 || type == 2) {
            if (mRecorder != null) {
                if (mRecorder.isCurrentlyAutoRecording()) {
                    mSetting.stopAutoRecording();
                } else {
                    if (isRecording) {
                        if (curRecordingTime >= 2) {
                            stopRecording();
                        }
                    }
                }
            }
            if (mHandler != null) {
                mHandler.sendEmptyMessage(1);
            } else {
            }
            if (type == 2) {
                sendBroadcastForHideNavigationBar();
            }
        } else {
            formatCard();
        }
    }

    @Override
    public void updateGPSInfo(String info, String speed) {
        gpsInfo = info;
        carSpeed = speed;
        mHandler.sendEmptyMessage(3);
    }

    private void updateGPSInfo() {
        tvGpsInfo.setText(gpsInfo);
        tvCarSpeed.setText(carSpeed);
    }

    @Override
    public void updateDVRUI(int type, LdwDetectInfo ldwDetectInfo, CdwDetectInfo cdwDetectInf) {
        if (type == 3) {
            adasDrawView.drawBitmap(ldwDetectInfo, cdwDetectInf);
        }
    }

    private void sendBroadcastForHideNavigationBar() {
        Intent intent = new Intent(Configuration.ACTION_SHOW_SETTING_WINDOW);
        intent.putExtra("isHideNavigationBar", true);
        sendBroadcast(intent);
    }

    @Override
    public void showFormatDialog() {
        mDialogTool.showFormatDialog(mContext);
    }

    @Override
    public void delayRecordingTime() {
        Log.d(TAG, "delayRecordingTime: ");
        if (!isRecording) {
            return;
        }
        if (curRecordingTime >= mRecorder.getRecordTime() - DVRTools.CUT_AFTER_DURATION) {
            curRecordingTime -= DVRService.COUNT_DOWN;
        } else {
            curRecordingTime = mRecorder.getRecordTime() - DVRService.COUNT_DOWN;
        }
    }

    @Override
    public void contentResolverChange(int type, boolean isOpen) {
        updateIconState(type, isOpen);
    }

    private class StorageDeviceConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_REMOVED)
                    || action.equals(Intent.ACTION_MEDIA_EJECT)) {
                Uri uri = intent.getData();
                if (uri != null) {
                    String path = uri.getPath();
                    onStorageDeviceUnmounted(path);
                }
            }
        }
    }

    private void openSOS() {
        isSOSEnable = true;
        imgBtnLock.setImageResource(R.drawable.selector_sos);
    }

    private void closeSOS() {
        isSOSEnable = false;
        imgBtnLock.setImageResource(R.drawable.icon_dvr_sos_close);
    }

    private static class InnerHandler extends Handler {
        private final WeakReference<DVRHomeActivity> activityWeakReference;
        private DVRHomeActivity mDvrHomeActivity;

        private InnerHandler(DVRHomeActivity popupWindow) {
            this.activityWeakReference = new WeakReference<>(popupWindow);
            mDvrHomeActivity = activityWeakReference.get();
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int number = msg.what;
            if (number == 1) {
                mDvrHomeActivity.closeSOS();
            }
            if (number == 2) {
                mDvrHomeActivity.openSOS();
            }
            if (number == 3) {
                mDvrHomeActivity.updateGPSInfo();
            }
            removeMessages(number);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermissionDismiss = false;//有权限没有通过
        if (REQUEST_CAMERA_CODE == requestCode) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == -1) {
                    hasPermissionDismiss = true;
                }
            }
            //如果有权限没有被允许
            if (hasPermissionDismiss) {
                requestPermissionTool.showPermissionDialog();//跳转到系统设置权限页面，或者直接关闭页面，不让他继续访问
            } else {
                //全部权限通过，可以进行下一步操作
            }
        }
    }


    @SuppressLint("CheckResult")
    public void toInitADAS() {
        compositeDisposable.add(Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> emitter) throws Exception {
                adasServer.startServer();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {

                    }
                }));
    }


    @Override
    protected void onStop() {
        super.onStop();
        Settings.Global.putInt(mContext.getContentResolver(), Configuration.CAMERA_NAV_COLOR, 0);
        NotifyMessageManager.getInstance().setShowFormatDialogListener(null);
        showNavigationBar();
        Log.d(TAG, "onStop: ");
        stopRecording();
        stopRecordingTimer();
        unregisterReceiver(mStorageDeviceConnectionReceiver);
        unbindService(mSrvConnection);
        onDVRServiceDisconnected();
        Log.d(TAG, "onStop: after");
    }

    public void checkAdasWarnInfo(LdwDetectInfo ldwDetectInfo, CdwDetectInfo cdwDetectInfo) {
        Log.d("adasdeal", "cdwDetectInfo=" + cdwDetectInfo + ",ldwDetectInfo=" + ldwDetectInfo);
        if (ldwDetectInfo != null) {

            Log.d("adasdeal", "cdwDetectInfo.getDetectState=" + ldwDetectInfo.getDetectState());
            if (ldwDetectInfo.getDetectState() == 2 || ldwDetectInfo.getDetectState() == 3) {
                warnSpeakerManager.carOutLine();
            }
        }

        if (cdwDetectInfo != null) {
            FrontCarInfo frontCarInfo = cdwDetectInfo.getFrontCarInfo();
            if (frontCarInfo != null) {
                switch (frontCarInfo.getFrontCarStateType()) {
                    case 1:
                        warnSpeakerManager.frontCarSafeDistance();
                        break;
                    case 2:
                        warnSpeakerManager.frontCarCrash();
                        break;
                    case 3:
                        warnSpeakerManager.frontCarLaunchWarn();
                        break;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }

        if (warnSpeakerManager != null) {
            warnSpeakerManager.stop();
            warnSpeakerManager = null;
        }
        if (mDialogTool != null) {
            mDialogTool.dismissDialog();
        }
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable.clear();
            compositeDisposable = null;
        }
    }

    //手动校准
    float mScaleWidth, mScaleHeight;

    private void initNewView() {
        if (adasServer == null) {
            return;
        }
        center_conf_rela = findViewById(R.id.center_conf_rela);
        btn_set_line_ok = findViewById(R.id.id_btn_set_line_ok);
        ln_up_down = findViewById(R.id.id_lin_up_down);
        ln_left_right = findViewById(R.id.id_lin_left_right);
        v_up_down = findViewById(R.id.id_view_up_down);
        v_left_right = findViewById(R.id.id_view_left_right);

        setGestureListener(this);//手势监听
        btn_set_line_ok.setOnClickListener(this);
        //计算平移距离
//        if(adjustAdas == View.VISIBLE) {
        center_conf_rela.setVisibility(adjustAdas);
        initNew(DVRHomeActivity.this);
//        }
        imgBtnADAS.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                center_conf_rela.setVisibility(View.VISIBLE);
                initNew(DVRHomeActivity.this);
                return true;
            }
        });
    }

    /**
     * 手动调整adas十字位置
     *
     * @param context
     */
    private void initNew(Context context) {
        int width = mVideoView.getMeasuredWidth();
        int height = mVideoView.getMeasuredHeight();
//        if(AdasConf.IN_FRAME_HEIGHT>0){
//            mScale = Math.min(((float)height)/AdasConf.IN_FRAME_HEIGHT, ((float)width)/AdasConf.IN_FRAME_WIDTH);
//        }else{
//            mScale = 0;
//        }
        mScaleWidth = ((float) width) / AdasConf.IN_FRAME_WIDTH;
        mScaleHeight = ((float) height) / AdasConf.IN_FRAME_HEIGHT;

        float desWidth = AdasConf.IN_FRAME_WIDTH * mScaleWidth;
        float desHeight = AdasConf.IN_FRAME_HEIGHT * mScaleHeight;

        deadStartX = 150;
        deadEndX = (int) (desWidth - 150);

        deadStartY = 100;
        deadEndY = (int) (desHeight - 100);

        FrameLayout.LayoutParams relaParams = (FrameLayout.LayoutParams) center_conf_rela.getLayoutParams(); //取控件textView当前的布局参数 linearParams.height = 20;// 控件的高强制设成20

        relaParams.width = (int) desWidth;// 控件的宽强制设成30
        relaParams.height = (int) desHeight;// 控件的宽强制设成30

        center_conf_rela.setLayoutParams(relaParams); //使设置好的布局参数应用到控件
//        center_conf_rela.setVisibility(View.VISIBLE);
        int[] oldVPPara = adasServer.getVPPara();
        Log.d("adasvp", "x:" + oldVPPara[0] + " y:" + oldVPPara[1]);
        if (oldVPPara[0] > 0) {
            desLastX = (int) (oldVPPara[0] * mScaleWidth);
            desLastY = (int) (oldVPPara[1] * mScaleHeight);
            ViewGroup.MarginLayoutParams margin = new ViewGroup.MarginLayoutParams(ln_left_right.getLayoutParams());
            margin.setMargins(desLastX - margin.width / 2, 0, (int) (desWidth - desLastX) - margin.width / 2, 0);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(margin);
            ln_left_right.setLayoutParams(layoutParams);

            ViewGroup.MarginLayoutParams margin1 = new ViewGroup.MarginLayoutParams(ln_up_down.getLayoutParams());
            margin1.setMargins(0, desLastY - margin1.height / 2, 0, (int) (desHeight - desLastY) - margin1.height / 2);
            RelativeLayout.LayoutParams layoutParams1 = new RelativeLayout.LayoutParams(margin1);
            ln_up_down.setLayoutParams(layoutParams1);

        } else {
            desLastX = (int) desWidth / 2;
            desLastY = (int) desHeight / 2;

            ViewGroup.MarginLayoutParams margin = new ViewGroup.MarginLayoutParams(ln_left_right.getLayoutParams());
            margin.setMargins((int) (desWidth / 2) - margin.width / 2, 0, ((int) (desWidth / 2) - margin.width / 2), 0);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(margin);
            ln_left_right.setLayoutParams(layoutParams);
            ViewGroup.MarginLayoutParams margin1 = new ViewGroup.MarginLayoutParams(ln_up_down.getLayoutParams());
            margin1.setMargins(0, (int) (desHeight / 2) - margin1.height / 2, 0, (int) (desHeight / 2) - margin1.height / 2);
            RelativeLayout.LayoutParams layoutParams1 = new RelativeLayout.LayoutParams(margin1);
            ln_up_down.setLayoutParams(layoutParams1);
        }

    }

    public void setGesture() {
        Log.d("adastouch", "setGesture");
        adasDrawView.setOnTouchListener(new View.OnTouchListener() {
            private int beginX, beginY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                Log.d("adastouch", "x=" + event.getRawX() + "  y=" + event.getRawY());
                int dx, dy;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        beginX = (int) event.getRawX();
                        beginY = (int) event.getRawY();
                        boolean isIn = adasDrawView.isInIndicator(beginX, beginY);
                        Log.d("adastouch", "setGesture" + isIn);
                        return isIn;
                    case MotionEvent.ACTION_MOVE:
//                        dx=(int)event.getRawX()-beginX;
//                        dy=(int)event.getRawY()-beginY;
                        adasDrawView.setVPPra((int) event.getRawX(), (int) event.getRawY(), adasServer);
                        break;
                    case MotionEvent.ACTION_UP:
//                        dx=(int)event.getRawX()-beginX;
//                        dy=(int)event.getRawY()-beginY;
                        adasDrawView.setVPPra((int) event.getRawX(), (int) event.getRawY(), adasServer);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        adasDrawView.setVPPra(0, 0, adasServer);
                        break;
                }
                return true;
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setGestureListener(Context context) {
        ln_left_right.setOnTouchListener(new View.OnTouchListener() {
            private int lastX;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v_left_right.setBackgroundColor(Color.parseColor("#0000FF"));
                        // 按下屏幕的操作
                        lastX = (int) event.getRawX();//获取触摸事件触摸位置的原始X坐标
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // 在屏幕上移动的操作
                        int dx = (int) event.getRawX() - lastX;
                        int l = v.getLeft() + dx;
                        int b = v.getBottom();
                        int r = v.getRight() + dx;
                        int t = v.getTop();

                        if (l < deadStartX) {
                            l = deadStartX;
                            r = deadStartX + v.getWidth();
                        }

                        if (r > deadEndX) {
                            r = deadEndX;
                            l = deadEndX - v.getWidth();
                        }

                        v.layout(l, t, r, b);
                        lastX = (int) event.getRawX();//获取触摸事件触摸位置的原始X坐标
                        desLastX = v.getLeft() + 20;
                        v.postInvalidate();
//                        btn_set_line_ok.setVisibility(View.VISIBLE);
                        break;
                    case MotionEvent.ACTION_UP:
                        v_left_right.setBackgroundColor(Color.parseColor("#00FF00"));
                        lastX = (int) event.getRawX();//获取触摸事件触摸位置的原始X坐标
                        desLastX = v.getLeft() + 20;
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        // 手势撤消的操作
                        // 一般认为不能由用户主动触发。
                        // 系统在运行到一定程度下无法继续响应你的后续动作时会产生此事件
                        break;
                    default:
                        break;
                }
                // 这个返回值如果是false的话，那么它只会接受到第一个ACTION_DOWN的效果，
                // 后面的它认为没有触发，所以要想继续监听后续事件，需要返回值为true
                return true;
            }
        });

        ln_up_down.setOnTouchListener(new View.OnTouchListener() {
            private int lastY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v_up_down.setBackgroundColor(Color.parseColor("#0000FF"));
                        // 按下屏幕的操作
                        lastY = (int) event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // 在屏幕上移动的操作
                        int dy = (int) event.getRawY() - lastY;
                        int l = v.getLeft();
                        int b = v.getBottom() + dy;
                        int r = v.getRight();
                        int t = v.getTop() + dy;

                        if (t < deadStartY) {
                            t = deadStartY;
                            b = deadStartY + v.getHeight();
                        }
                        if (b > deadEndY) {
                            b = deadEndY;
                            t = deadEndY - v.getHeight();
                        }
                        v.layout(l, t, r, b);
                        lastY = (int) event.getRawY();
                        desLastY = v.getTop() + 20;
                        v.postInvalidate();
                        break;
                    case MotionEvent.ACTION_UP:
                        v_up_down.setBackgroundColor(Color.parseColor("#00FF00"));
                        lastY = (int) event.getRawY();
                        desLastY = v.getTop() + 20;
                        // 离开屏幕的操作
                        break;
                    case (MotionEvent.ACTION_CANCEL):
                        // 手势撤消的操作
                        // 一般认为不能由用户主动触发。
                        // 系统在运行到一定程度下无法继续响应你的后续动作时会产生此事件
                        break;
                    default:
                        break;
                }
                // 这个返回值如果是false的话，那么它只会接受到第一个ACTION_DOWN的效果，
                // 后面的它认为没有触发，所以要想继续监听后续事件，需要返回值为true
                return true;
            }
        });
    }

    //手动校准-----------------------------------------------end
    @Override
    public void onCameraViewStarted(int i, int i1) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat mMat = inputFrame.rgba();
        //传送数据到检测服务
        try {
            adasServer.processDataAsyn(inputFrame);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        Log.d("adasdeal", "onCameraFrame: ");
        return mMat;
    }

    private void updateIconState(int type, boolean isOpen) {
        if (type == 0) {
            imgBtnRecord.setSelected(isOpen);
            if (isOpen) {
                mHandler.sendEmptyMessage(2);
            } else {
                mHandler.sendEmptyMessage(1);
            }
        }
        if (type == 1) {
            updateADAS(isOpen);
        }
        if (type == 2) {
            imgBtnMicrophone.setSelected(isOpen);
        }
        if (type == 3) {
            if (center_conf_rela != null) {
                Log.d(TAG, "updateIconState:center_conf_rela");
                center_conf_rela.setVisibility(View.VISIBLE);
                initNew(DVRHomeActivity.this);
            } else {
                Log.d(TAG, "updateIconState:center_conf_rela=null ");
            }
        }
    }

    private void updateADAS(boolean isOpen) {
        imgBtnADAS.setSelected(isOpen);
        if (adasDrawView == null || imgBtnADAS == null) {
            return;
        }
        if (isOpen) {
            if (center_conf_rela != null) {
                center_conf_rela.setVisibility(View.GONE);
            }
            adasDrawView.setVisibility(View.VISIBLE);

        } else {
            adasDrawView.setVisibility(View.GONE);
        }
    }
/*    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }*/

    /* @Override
     public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {
         Mat mMat = cvCameraViewFrame.rgba();
         //传送数据到检测服务
         adasServer.processDataAsyn((CvCameraViewFrame) cvCameraViewFrame);
         return mMat;
     }*/
    private static final long DOUBLE_TIME = 300;
    private static long lastClickTime = 0;

    private void doubleClick() {
        Log.d(TAG, "doubleClick: ");
        long currentTimeMillis = System.currentTimeMillis();
        long time = currentTimeMillis - lastClickTime;
        lastClickTime = currentTimeMillis;
        if (time < DOUBLE_TIME) {
            Log.d(TAG, "doubleClick:go ");
            onClickTakePicture();
        }
    }

}
