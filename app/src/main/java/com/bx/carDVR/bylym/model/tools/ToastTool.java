package com.bx.carDVR.bylym.model.tools;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bx.carDVR.Configuration;
import com.bx.carDVR.DvrApplication;

import java.util.Timer;
import java.util.TimerTask;


public class ToastTool {
    private static Toast toast = null;
    private static Toast longToast = null;
    private static Timer timer;

    public static ToastTool getInstance() {
        return ToastTool.SingletonHolder.sInstance;
    }

    private static class SingletonHolder {
        private static final ToastTool sInstance = new ToastTool();
    }

    public static void showToast(int text) {
        if (toast == null) {
            toast = Toast.makeText(DvrApplication.getInstance(), text, Toast.LENGTH_SHORT);
//            toast.setGravity(Gravity.BOTTOM, 0, 50);

            LinearLayout linearLayout = (LinearLayout) toast.getView();
            TextView messageTextView = (TextView) linearLayout.getChildAt(0);
            if (Configuration.IS_3IN) {
                messageTextView.setTextSize(24);
                toast.setGravity(Gravity.BOTTOM, 0, 50);
            }
        } else {
            toast.setText(text);
            toast.setDuration(Toast.LENGTH_SHORT);
        }
        toast.show();
    }

    public static void showToast(String text) {
        if (toast == null) {
            toast = Toast.makeText(DvrApplication.getInstance(), text, Toast.LENGTH_SHORT);
            LinearLayout linearLayout = (LinearLayout) toast.getView();
            TextView messageTextView = (TextView) linearLayout.getChildAt(0);
            if (Configuration.IS_3IN) {
                messageTextView.setTextSize(24);
                toast.setGravity(Gravity.BOTTOM, 0, 50);
            }
        } else {
            toast.setText(text);
            toast.setDuration(Toast.LENGTH_SHORT);
        }
        toast.show();
    }

    @SuppressLint("ShowToast")
    public static void showLongToast(int text) {
        if (longToast == null) {
            longToast = Toast.makeText(DvrApplication.getInstance(), text, Toast.LENGTH_SHORT);
            LinearLayout linearLayout = (LinearLayout) longToast.getView();
            TextView messageTextView = (TextView) linearLayout.getChildAt(0);
            if (Configuration.IS_3IN) {
                messageTextView.setTextSize(24);
                longToast.setGravity(Gravity.BOTTOM, 0, 50);
            }
        } else {
            longToast.setText(text);
            longToast.setDuration(Toast.LENGTH_SHORT);
        }
        hideLongToast();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                longToast.show();
            }
        }, 0, 5000);
    }

    public static void hideLongToast() {
//        if (longToast != null) {
//            longToast.cancel();
//        }
        if (timer != null) {
            timer.cancel();
        }
    }

    public void release() {
        if (toast != null) {
            toast.cancel();
            toast = null;
        }
        if (longToast != null) {
            longToast.cancel();
            longToast = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

}
