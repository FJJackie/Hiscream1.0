package com.test.ssmc.hiscream.Views.View;

import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.test.ssmc.hiscream.R;
import com.test.ssmc.hiscream.Views.utils.BaseActivity;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private int skipTime = 5;//跳过倒计时提示5秒
    private TextView tv_skip;
    Timer timer = new Timer();
    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_skip = findViewById(R.id.tv);//跳过
        tv_skip.setOnClickListener(this);//跳过监听
        timer.schedule(task, 1000, 1000);//等待时间一秒，停顿时间一秒
        /**
         * 正常情况下不点击跳过
         */
        handler = new Handler();
        handler.postDelayed(runnable = new Runnable() {
            @Override
            public void run() {
                //从欢迎界面跳转到首界面
                toWebActivity();
            }
        }, 5000);//延迟5S后发送handler信息
    }

    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() { // UI thread
                @Override
                public void run() {
                    skipTime--;
                    tv_skip.setText(skipTime + " 跳过");
                    if (skipTime < 0) {
                        timer.cancel();
                        tv_skip.setVisibility(View.GONE);//倒计时到0隐藏字体
                    }
                }
            });
        }
    };

    /**
     * 点击跳过
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv:
                //从欢迎界面跳转到首界面
                toWebActivity();
                if (runnable != null) {
                    handler.removeCallbacks(runnable);
                }
                break;
            default:
                break;
        }
    }

    private void toWebActivity(){
        Intent intent=new Intent(MainActivity.this,WebActivity.class);
        startActivity(intent);
        finish();
    }
}