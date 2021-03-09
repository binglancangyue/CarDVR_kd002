package com.bx.carDVR.bylym.adas;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.bx.carDVR.bylym.adas.camera2video.CameraActivity;


public class SplashActivity extends AppCompatActivity {

    private static final String[] PERMISSION_REQUESTS =
                   {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_PHONE_STATE,
                           Manifest.permission.RECORD_AUDIO
                   };


    private ProReqPermission  proReqPermission;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      //  setContentView(R.layout.activity_splash);
        proReqPermission=new ProReqPermission(this);
        checkPermission();
    }


    private void startHomeActivity(){
        Intent intent=new Intent(this, CameraActivity.class);
        startActivity(intent);
        this.finish();
    }

    public void checkPermission() {
        //检查
        boolean isAllGranted = proReqPermission.checkPermissionAllGranted(PERMISSION_REQUESTS);
        //通过
        if (isAllGranted) {
            startHomeActivity();
            return;
        }
        //请求
        proReqPermission.requestPermission(PERMISSION_REQUESTS,ProReqPermission.REQUEST_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions,  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == ProReqPermission.REQUEST_CODE) {
            boolean isAllGranted = true;
            // 判断是否所有的权限都已经授予了
            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }

            if (isAllGranted) {
                startHomeActivity();
            } else {
                proReqPermission.openAppDetails();
            }
        }
    }
}
