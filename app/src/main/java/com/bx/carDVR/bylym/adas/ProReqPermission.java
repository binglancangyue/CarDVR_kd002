package com.bx.carDVR.bylym.adas;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

public class ProReqPermission {
    public  static final int  REQUEST_CODE =111;
    private Activity holdActivity;
    public ProReqPermission(Activity holdActivity) {
        this.holdActivity = holdActivity;
    }
    /**
     * 检查是否拥有指定的所有权限
     */
   public  boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(holdActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                // 只要有一个权限没有被授予, 则直接返回 false
                return false;
            }
        }
        return true;
    }




    public void  requestPermission(String[] permissions,int requestCode){
        //请求
        ActivityCompat.requestPermissions(
                holdActivity,
                permissions,
                requestCode
        );
    }


    /**
     * 打开 APP 的详情设置
     */
    public  void openAppDetails() {
        AlertDialog.Builder builder = new AlertDialog.Builder(holdActivity);
        builder.setMessage("APP 应用需要授权 ，请到 “应用信息 -> 权限” 中授予！");
        builder.setPositiveButton("去手动授权", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + holdActivity.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                holdActivity.startActivity(intent);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }






}
