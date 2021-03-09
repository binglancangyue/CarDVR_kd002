package com.bx.carDVR.bylym;

import android.content.Context;

import org.opencv.android.CameraBridgeViewBase;

public class TestSurFaceView extends CameraBridgeViewBase {
    public TestSurFaceView(Context context, int i) {
        super(context, i);
    }

    @Override
    protected boolean connectCamera(int i, int i1) {
        return false;
    }

    @Override
    protected void disconnectCamera() {

    }
}
