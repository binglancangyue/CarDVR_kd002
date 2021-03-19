package com.bx.carDVR.bylym.model.listener;

import com.calmcar.adas.apiserver.model.CdwDetectInfo;
import com.calmcar.adas.apiserver.model.LdwDetectInfo;

/**
 * @author Altair
 * @date :2020.05.29 下午 05:20
 * @description:
 */
public interface OnDialogCallBackListener {
    void updateDVRUI(int type, LdwDetectInfo ldwDetectInfo, CdwDetectInfo cdwDetectInf);

    void updateDVRUI(int type);

    void updateGPSInfo(String info, String speed);

    interface OnShowFormatDialogListener {
        void showFormatDialog();
    }
}
