package com.test.ssmc.hiscream;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.test.ssmc.hiscream.Views.CameraSurfaceView;
import com.test.ssmc.hiscream.Views.View.UpDateWebViewInterface;
import com.test.ssmc.hiscream.Views.messages.MessageHUB;

public class MyService extends Service {
    public boolean isWork=false;
    private MyBinder binder = new MyBinder();
    private CameraSurfaceView cameraSurfaceView;
    private Camera camera;
    private WindowManager windowManager;
    private View view;
    public class MyBinder extends Binder {
        public MyService getService() {
            return MyService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return  binder;
    }
    //每次startService的时候都会调用这个函数
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initView();
        isWork=true;
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onDestroy() {
        isWork=false;
        super.onDestroy();
    }

    public void releaseView(){
        cameraSurfaceView.reset();
        camera.setPreviewCallback(null);
        camera.release();
        if (windowManager != null) windowManager.removeView(view);
    }
    //启动cameraSurfaceView检测时给cameraSurfaceView设置前置相机
    private void initCamera(){
        camera=Camera.open(1);
        cameraSurfaceView.setCamera(camera);
    }

    //申请开启浮窗权限，获取权限后就开启悬窗
    private void initView() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse( "package:"+"com.test.ssmc.hiscream"));  //应用的包名，可直接跳转到这个应用的悬浮窗设置；
                startActivity(intent);
                openWindow();
            } else {
                openWindow();
            }
        }else {
            openWindow();
        }
    }

    //开启悬浮窗，并调用camerasufaceview的检测
    private void openWindow(){
        windowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.height = 1;//ViewGroup.LayoutParams.WRAP_CONTENT;
        layoutParams.width = 1;//ViewGroup.LayoutParams.WRAP_CONTENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        layoutParams.flags =  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        layoutParams.format = PixelFormat.TRANSLUCENT;  //透明
        layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;  //右上角显示
        view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.floatingview,null);
        view.setVisibility(View.VISIBLE);
        cameraSurfaceView=view.findViewById(R.id.floatingCameraSurfaceView);
        initCamera();
        windowManager.addView(view,layoutParams);
        MessageHUB.get().sendMessage(MessageHUB.WAIT_MEASUREMENT,null);
        cameraSurfaceView.calibrate();
    }
}
