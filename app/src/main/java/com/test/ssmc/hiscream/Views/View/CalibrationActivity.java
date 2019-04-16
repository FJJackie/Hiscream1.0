package com.test.ssmc.hiscream.Views.View;

import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Switch;

import com.test.ssmc.hiscream.R;
import com.test.ssmc.hiscream.Views.CameraSurfaceView;
import com.test.ssmc.hiscream.Views.messages.MessageHUB;
import com.test.ssmc.hiscream.Views.messages.MessageListener;
import com.test.ssmc.hiscream.Views.utils.BaseActivity;


public class CalibrationActivity extends BaseActivity implements MessageListener {

    private Camera camera = null;

    //UI控件部分
    private CameraSurfaceView cameraSurfaceView;
    private Button calibrateButton, resetButton;  //校准按钮
    private Switch middleEyePointSwitch, eyePointsSwitch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);
        binding();

        //注册事件监听器
        MessageHUB.get().registerListener(this);
    }


    private void binding() {
        cameraSurfaceView = findViewById(R.id.surface_camera);
        RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(
                (int) (0.95 * this.getResources().getDisplayMetrics().widthPixels),
                (int) (0.6 * this.getResources().getDisplayMetrics().heightPixels));

        layout.setMargins(0, (int) (0.05 * this.getResources()
                .getDisplayMetrics().heightPixels), 0, 0);

        cameraSurfaceView.setLayoutParams(layout);


        calibrateButton = findViewById(R.id.calibrateButton);
        calibrateButton.setOnClickListener((e) -> {
            cameraSurfaceView.calibrate();
            calibrateButton.setBackgroundResource(R.drawable.yellow_button);
        });

        resetButton = findViewById(R.id.resetButton);
        resetButton.setOnClickListener((e) -> {
            cameraSurfaceView.reset();
            calibrateButton.setBackgroundResource(R.drawable.red_button);

        });

        middleEyePointSwitch = findViewById(R.id.middle_eye_point_switch);
        middleEyePointSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                cameraSurfaceView.showMiddleEye(middleEyePointSwitch.isChecked()));

        eyePointsSwitch = findViewById(R.id.eye_points_switch);
        eyePointsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                cameraSurfaceView.showEyePoints(eyePointsSwitch.isChecked()));
    }

    private void resetCam() {
        cameraSurfaceView.reset();
//      @TODO: 2019/4/2
        camera.stopPreview();
//
        camera.setPreviewCallback(null);
        camera.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera = Camera.open(1);
        Camera.Parameters param = camera.getParameters();
        cameraSurfaceView.setCamera(camera);
    }

    @Override
    protected void onPause() {
        super.onPause();
        resetCam();
    }


    @Override
    public void onMessage(int messageID, Object message) {
        switch (messageID) {
            //校准完成后改变button的颜色
            case MessageHUB.DONE_CALIBRATION:
                calibrateButton.setBackgroundResource(R.drawable.green_button);
                break;
            default:
                break;
        }
    }
}
