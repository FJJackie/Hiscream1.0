package com.test.ssmc.hiscream.Views.View;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.test.ssmc.hiscream.R;
import com.test.ssmc.hiscream.Views.utils.BaseActivity;

public class SettingActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        binding();
    }
    private void binding(){
        Button calibrationButton=findViewById(R.id.calibration_setting);
        calibrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(SettingActivity.this,CalibrationActivity.class);
                startActivity(intent);
            }
        });
    }
}
