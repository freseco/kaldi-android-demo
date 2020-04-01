package org.kaldi.demo;


import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;

public class LinternaSingleton {

    private static LinternaSingleton instance;
    //Para encdencer la cámara
    private CameraManager mCameraManager;
    private String mCameraId;
    private  boolean flashlight=false;

    private final Context context;

    private static LinternaSingleton sSoleInstance;

    private LinternaSingleton(Context mContext){
        context=mContext;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraId = mCameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }  //private constructor.

    public static LinternaSingleton getInstance(Context mContext){
        if (sSoleInstance == null){ //if there is no instance available... create new one
            sSoleInstance = new LinternaSingleton(mContext);
        }

        return sSoleInstance;
    }


    public void turnOnFlashLight() {

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mCameraManager.setTorchMode(mCameraId, true);
                flashlight=true;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void turnOffFlashLight() {

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mCameraManager.setTorchMode(mCameraId, false);
                flashlight=false;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}