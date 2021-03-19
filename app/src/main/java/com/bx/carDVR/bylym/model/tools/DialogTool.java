package com.bx.carDVR.bylym.model.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bx.carDVR.Configuration;
import com.bx.carDVR.DvrApplication;
import com.bx.carDVR.R;
import com.bx.carDVR.bylym.model.NotifyMessageManager;

import java.lang.reflect.Field;

/**
 * @author Altair
 * @date :2020.05.29 下午 04:47
 * @description:
 */
public class DialogTool {
    private AlertDialog stopRecordDialog;
    private AlertDialog formatSDCardDialog;
    private AlertDialog settingDialog;

/*    public void showStopRecordingDialog(Context context) {
        if (stopRecordDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.dialog_title_stop_recording);
            builder.setMessage(R.string.dialog_message_stop_recording);
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismissStopRecordDialog();
                }
            });
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismissStopRecordDialog();
                    NotifyMessageManager.getInstance().updateDVRUI(0);
                }
            });
            stopRecordDialog = builder.create();
        }
        showDialog(stopRecordDialog);
    }*/

    public void showStopRecordingDialog(Context context) {
        if (stopRecordDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = View.inflate(context, R.layout.dialog_layout, null);
          /*  LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
            layoutParams.width = 280;*/
            builder.setView(view);
            TextView title = view.findViewById(R.id.tv_dialog_title);
            TextView message = view.findViewById(R.id.tv_dialog_message);
            TextView negativeButton = view.findViewById(R.id.tv_dialog_cancel);
            TextView positiveButton = view.findViewById(R.id.tv_dialog_ok);
            title.setText(R.string.dialog_title_stop_recording);
            message.setText("錄影停止期間\n一鍵理賠通知功能暫停。");
            negativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissStopRecordDialog();
                }
            });
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissStopRecordDialog();
                    NotifyMessageManager.getInstance().updateDVRUI(0);

                }
            });
            stopRecordDialog = builder.create();
            formatSDCardDialog.getWindow().setType(
                    (WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
        }
        showAlertDialog(stopRecordDialog);
//        showDialog(stopRecordDialog);
    }

/*    public void showFormatDialog(Context context) {
        if (formatSDCardDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.dialog_title_format);
            builder.setMessage(R.string.dialog_message_format_sd_card);
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismissFormatDialog();
                }
            });
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismissFormatDialog();
                    NotifyMessageManager.getInstance().updateDVRUI(1);
                }
            });
            formatSDCardDialog = builder.create();
        }
        showDialog(formatSDCardDialog);
    }*/

    public void showFormatDialog(Context context) {
        if (formatSDCardDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = View.inflate(context, R.layout.dialog_layout, null);
          /*  LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
            layoutParams.width = 280;*/
            builder.setView(view);
            TextView title = view.findViewById(R.id.tv_dialog_title);
            TextView message = view.findViewById(R.id.tv_dialog_message);
            TextView negativeButton = view.findViewById(R.id.tv_dialog_cancel);
            TextView positiveButton = view.findViewById(R.id.tv_dialog_ok);
            title.setText(R.string.dialog_title_format);
            message.setText(R.string.dialog_message_format_sd_card);
            negativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissFormatDialog();
//                    InterfaceCallBackManagement.getInstance().updateView(1,false);
                }
            });
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissFormatDialog();

                }
            });
            formatSDCardDialog = builder.create();
        }
        showAlertDialog(formatSDCardDialog);
    }

/*    public void showSettingDialog(Context context) {
        if (settingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.dialog_title_setting);
            builder.setMessage(R.string.dialog_message_setting_stop_recording);
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismissSettingDialog();
                }
            });
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismissSettingDialog();
                    NotifyMessageManager.getInstance().updateDVRUI(2);
                }
            });
            settingDialog = builder.create();
        }
        showDialog(settingDialog);
    }*/

    public void showSettingDialog(Context context) {
        if (settingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = View.inflate(context, R.layout.dialog_layout, null);
          /*  LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
            layoutParams.width = 280;*/
            builder.setView(view);
            TextView title = view.findViewById(R.id.tv_dialog_title);
            TextView message = view.findViewById(R.id.tv_dialog_message);
            TextView negativeButton = view.findViewById(R.id.tv_dialog_cancel);
            TextView positiveButton = view.findViewById(R.id.tv_dialog_ok);
            title.setText(R.string.dialog_title_setting);
            message.setText(R.string.dialog_message_setting_stop_recording);
            negativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissSettingDialog();
                }
            });
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissSettingDialog();
                    NotifyMessageManager.getInstance().updateDVRUI(2);

                }
            });
            settingDialog = builder.create();
        }
        showAlertDialog(settingDialog);
    }

    public void showDialog(AlertDialog alertDialog) {
        if (!alertDialog.isShowing()) {
            if (Configuration.IS_3IN) {
                focusNotAle(alertDialog.getWindow());
                alertDialog.show();
//                setDialogTextSize(alertDialog);
                hideNavigationBar(alertDialog.getWindow());
                clearFocusNotAle(alertDialog.getWindow());
            } else {
                alertDialog.show();
            }
        }
    }

    private void showAlertDialog(AlertDialog alertDialog) {
        focusNotAle(alertDialog.getWindow());
        alertDialog.show();
        WindowManager.LayoutParams params =
                alertDialog.getWindow().getAttributes();
        params.width = 500;
        alertDialog.getWindow().setAttributes(params);
        hideNavigationBar(alertDialog.getWindow());
        clearFocusNotAle(alertDialog.getWindow());

    }


    public void dismissStopRecordDialog() {
        if (stopRecordDialog != null) {
            stopRecordDialog.dismiss();
        }
    }

    public void dismissSettingDialog() {
        if (settingDialog != null) {
            settingDialog.dismiss();
        }
    }

    public void dismissFormatDialog() {
        if (formatSDCardDialog != null) {
            formatSDCardDialog.dismiss();
        }
    }

    public void dismissDialog() {
        dismissStopRecordDialog();
        dismissFormatDialog();
        dismissSettingDialog();
        settingDialog = null;
        stopRecordDialog = null;
        formatSDCardDialog = null;
    }

    /**
     * dialog 需要全屏的时候用，和clearFocusNotAle() 成对出现
     * 在show 前调用  focusNotAle   show后调用clearFocusNotAle
     *
     * @param window
     */
    public void focusNotAle(Window window) {
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }

    /**
     * dialog 需要全屏的时候用，focusNotAle() 成对出现
     * 在show 前调用  focusNotAle   show后调用clearFocusNotAle
     *
     * @param window
     */
    public void clearFocusNotAle(Window window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }

    public void hideNavigationBar(Window window) {
//        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
//        window.getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
//            @Override
//            public void onSystemUiVisibilityChange(int visibility) {
//                int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
//                        //布局位于状态栏下方
//                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
//                        //全屏
//                        View.SYSTEM_UI_FLAG_FULLSCREEN |
//                        //隐藏导航栏
//                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
//                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
//                if (Build.VERSION.SDK_INT >= 19) {
//                    uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
//                } else {
//                    uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
//                }
//                window.getDecorView().setSystemUiVisibility(uiOptions);
//            }
//        });
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        View decorView = window.getDecorView();
        int option = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(option);
        window.setNavigationBarColor(Color.TRANSPARENT);
    }

    public void hideNavigationBar2(Window window){
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        View decorView = window.getDecorView();
        int option = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(option);
    }

    public void setDialogTextSize(AlertDialog builder) {
        Button button_negative = builder.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button button_positive = builder.getButton(AlertDialog.BUTTON_POSITIVE);
        button_negative.setTextSize(27);
        button_positive.setTextSize(27);
        builder.getWindow().setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) button_positive.getLayoutParams();
        LinearLayout.LayoutParams layoutParams1 = (LinearLayout.LayoutParams) button_positive.getLayoutParams();
        layoutParams.height = 80;
        layoutParams.width = 90;
        layoutParams.setMargins(0, 0, 5, 0);
        layoutParams.gravity = Gravity.CENTER;
        layoutParams1.gravity = Gravity.CENTER;
        layoutParams1.height = 80;
        layoutParams1.width = 90;
        button_negative.setLayoutParams(layoutParams);
        button_positive.setLayoutParams(layoutParams1);
        try {
            //获取mAlert对象
            Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
            mAlert.setAccessible(true);
            Object mAlertController = mAlert.get(builder);

            //获取mTitleView并设置大小颜色
            Field mTitle = mAlertController.getClass().getDeclaredField("mTitleView");
            mTitle.setAccessible(true);
            TextView mTitleView = (TextView) mTitle.get(mAlertController);
            if (mTitleView != null) {
                mTitleView.setTextSize(30);
            }

            //获取mMessageView并设置大小颜色
            Field mMessage = mAlertController.getClass().getDeclaredField("mMessageView");
            mMessage.setAccessible(true);
            TextView mMessageView = (TextView) mMessage.get(mAlertController);
            if (mMessageView != null) {
                mMessageView.setTextSize(27);
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

}
