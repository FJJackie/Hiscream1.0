package com.test.ssmc.hiscream.Views.View;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.test.ssmc.hiscream.R;
import com.test.ssmc.hiscream.Views.CameraSurfaceView;
import com.test.ssmc.hiscream.Views.messages.MessageHUB;
import com.test.ssmc.hiscream.Views.messages.MessageListener;


public class CalibrationDialog extends Dialog implements DialogInterface {
    public CalibrationDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_calivration);
    }

}
