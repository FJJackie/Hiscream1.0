package com.test.ssmc.hiscream.Views.View;

import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Button;

import com.test.ssmc.hiscream.Views.messages.MeasurementStepMessage;
import com.test.ssmc.hiscream.Views.messages.MessageHUB;
import com.test.ssmc.hiscream.Views.messages.MessageListener;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import static android.content.Context.SENSOR_SERVICE;

public class CalibrationProvider implements MessageListener, SensorEventListener {

    private static final String TAG = "CalibrationProvider";

    public static final String CAM_SIZE_WIDTH = "intent_cam_size_width";
    public static final String CAM_SIZE_HEIGHT = "intent_cam_size_height";
    public static final String AVG_NUM = "intent_avg_num";
    public static final String PROBANT_NAME = "intent_probant_name";
    private static final int BRIGHTNESS_FACTOR_DEFAULT = 1;
    private static final int FONT_SIZE_FACTOR_DEFAULT = 1;

    //UI控件部分
    Camera _cam;
    //TextView _currentDistanceView;
    Button _calibrateButton;

    private Sensor sensor;
    private double eBrightness;//环境亮度值

    private int count = 0;

    private float _currentDevicePosition;
    private int _cameraHeight;
    private int _cameraWidth;
    private int _avgNum;
    private float distToFace;
    private float brightnessValue;///环境光亮度值
    private int fontSizeFactor = FONT_SIZE_FACTOR_DEFAULT;///后期可取消这两个值
    private int bFactor = BRIGHTNESS_FACTOR_DEFAULT;///

    private float lastDistToFace = -1f;
    private int lastRealX = 0;

    private final static DecimalFormat _decimalFormater = new DecimalFormat("0.0");
    //像素密度参考值
    final int refDpi = 160;
    //2018/12/06
    //参考字体大小
    final int refFontSize = 20;

    //20190325 update
    UpDateWebViewInterface upDateWebViewInterface;

    private DisplayMetrics displayMetrics;

    //@todo 20190331
    SensorManager sensorManager;

    CalibrationProvider(UpDateWebViewInterface upDateWebViewInterface) {

        //callback webView接口回调
        this.upDateWebViewInterface = upDateWebViewInterface;

        //注册事件监听
        sensorManager = (SensorManager) upDateWebViewInterface.getContext().getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT); // 获取光线传感器
        if(sensor != null){
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        //注册事件监听器
        MessageHUB.get().registerListener(this);
    }


    public void setDisplayMetrics(DisplayMetrics displayMetrics) {
        this.displayMetrics = displayMetrics;
    }

    @Override
    public void onMessage(final int messageID, final Object message) {
        Log.i(TAG, "onMessage(int,Object)");
        switch (messageID) {
            case MessageHUB.MEASUREMENT_STEP:
                updateUI((MeasurementStepMessage) message);
                break;
             case MessageHUB.WAIT_MEASUREMENT:
                 upDateWebViewInterface.showWaiting();
            default:
                break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.i(TAG, "onSensorChanged(SensorEvent)");
        //获取环境亮度值
        float[] values = event.values;
            switch (event.sensor.getType()) {
                //动态更新光线信息
                case Sensor.TYPE_LIGHT:
                    eBrightness = values[0];
                    brightnessValue = values[0];
                    break;
            }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "onAccuracyChanged(Sensor,int)");
    }

    /**
     * 2018/12/06
     * 获取理想的字体大小
     *
     * @param message 传入message参数 获取人脸到屏幕的距离
     * @return 理想字体的大小
     */
    public float getIdealTextSize(final MeasurementStepMessage message) {
        //对应公式4-12
        float fontRatio = message.getDistToFace() / 29.7f;
        float idealTextSize = fontRatio * refFontSize;
        return idealTextSize;
    }

//    /**
//     * 2018/12/06
//     * 获得当前设备的一个DisplayMetrics对象
//     * @return DisplayMetrics 对象
//     */
//    public DisplayMetrics getDisplayMetrics(){
//        DisplayMetrics metrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
//        return metrics;
//    }

    /**
     * 2018/12/06
     * get the screen size
     * 获取屏幕对角线尺寸 单位 inch
     *
     * @return 屏幕对角线长度 单位 inch
     */
    public double getScreenSizeInch() {
//        DisplayMetrics metrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

        //dm: density(显示屏的逻辑密度)
        //参考dpi = 160
        //final int refDpi = 160;
        double dm = displayMetrics.density * refDpi;
        //接下来的代码是获得屏幕尺寸后计算屏幕的实际大小
        //利用勾股定理求得屏幕的对角线长度 单位Inch
        //y = metrics.widthPixels/dm = 屏幕的物理宽度 下面的y同理
        double x = Math.pow(displayMetrics.widthPixels / dm, 2);
        double y = Math.pow(displayMetrics.heightPixels / dm, 2);
        final double screenInch = Math.sqrt(x + y);

        return screenInch;
    }


    /**
     * 2018/12/07
     * 获取最佳亮度值 B
     * 论文公式 4-3
     *
     * @param message MeasurementStepMessage
     * @return
     */
    public double getOptimalBrightness(MeasurementStepMessage message) {

//        DisplayMetrics metrics = getDisplayMetrics();
        final double screenSizeInch = getScreenSizeInch();
        double heightPixel = displayMetrics.heightPixels;
        double widthPixel = displayMetrics.widthPixels;

        //论文公式4-3   Dmax：对应论文中的Dmax
        //疑问：widthPixel?? 与论文不对应       公式2-7
        final double Dmax = screenSizeInch /
                (Math.sqrt(Math.pow((1.0 * widthPixel / heightPixel), 2) + 1) * widthPixel * Math.tan(1 / 60.0 * Math.PI / 180));
        //获取的人脸到设备的距离
        distToFace = message.getDistToFace();

        //l0 目标亮度值
        //l0 ??? 为什么等于eBrightness+30
        double bBrightness = eBrightness + 30;
        //计算目标亮度 公式4-7
        //bBrightness:屏幕本身最佳亮度      eBrightness:环境亮度   iBrightness:当前阅读距离下目标亮度
        double iBrightness = eBrightness + Math.pow((distToFace / Dmax), 2) * (bBrightness - eBrightness);
        Log.d(TAG, "getOptimalBrightness: 环境亮度：" + eBrightness);
        Log.d(TAG, "getOptimalBrightness: 屏幕背景亮度：" + iBrightness);
        return iBrightness;
    }

    public int getOptimalFontSize(MeasurementStepMessage message) {
//        DisplayMetrics metrics = getDisplayMetrics();
        int fontSize = (int) (5.5 * message.getDistToFace() * displayMetrics.densityDpi / (6000 * 2.54 * 0.45));
        return fontSize;
    }

    /**
     * Update the UI params.
     * 此方法需要被重构
     * 重构思路
     * 先拆解成多个方法 把以下任务分开
     * <p>
     * 1 计算屏幕的实际大小可以作为初始化的内容一开始就计算出来 以后直接使用
     * 2 计算maxD的方法
     * 3 计算目标亮度的方法
     *
     * @param message
     */
    public void updateUI(final MeasurementStepMessage message) {
        Log.i(TAG, "updateUI(MeasurementStepMessage)");

        //_currentDistanceView.setText(_decimalFormater.format(message.getDistToFace()) + " cm");
        //设置理想字体大小
        //_currentDistanceView.setTextSize(getIdealTextSize(message));
        /*
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        //接下来的代码是获得屏幕尺寸后计算屏幕的实际大小

        //dm: density(显示屏的逻辑密度)
        //参考dpi = 160
        final int refDpi = 160;
        double dm = metrics.density * refDpi;

        //利用勾股定理求得屏幕的对角线长度 单位Inch
        //y = metrics.widthPixels/dm = 屏幕的物理宽度 下面的y同理
        double x = Math.pow(metrics.widthPixels / dm, 2);
        double y = Math.pow(metrics.heightPixels / dm, 2);*/
        /*
        DisplayMetrics metrics = getDisplayMetrics();
        final double screenSizeInch = getScreenSizeInch();
        double heightPixel = metrics.heightPixels;
        double widthPixel = metrics.widthPixels;

        //论文公式4-3   Dmax：对应论文中的MaxD
        //疑问：widthPixel?? 与论文不对应
        final double Dmax = screenSizeInch / (Math.sqrt(Math.pow((1.0 * widthPixel / heightPixel), 2) + 1) * widthPixel * Math.tan(1 / 60.0 * Math.PI / 180));
        //获取的人脸到设备的距离
        double DistoFace = message.getDistToFace();

        //l0 目标亮度值
        //l0 ??? 为什么等于lb+30
        double l0 = lb + 30;
        //计算最佳亮度 公式4-7
        */


        //屏幕亮度调整到目标亮度
        double iBrightness = getOptimalBrightness(message);
        float brightness = iBrightness / 255 >= 1 ? 1 : (float) iBrightness / 255;
        // TODO: 2019/3/31 更新亮度
        Log.d(TAG, "updateUI: 最终屏幕亮度"+brightness);
        upDateWebViewInterface.updateBrightness(brightness);

        //更改网页显示效果
        /**
         * 调整字体大小的代码部分
         * @param webView 判断webView是否为空
         */
        /*20190325 更改
        if (webView != null) {
            //根据距离经过计算设置字体大小
            //算法5-3 44页??
            //公式不一致
            int fontSize = getOptimalFontSize(message);
            if (fontSize > 0) {
                //改变网页内容 字体大小和前后景反差颜色
                changeFontSizeAndContrast(fontSize);
                //ui提示字体改变
                Toast.makeText(CalibrationActivity.this, "fontsize: " + fontSize, Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "webView is null");
        }
        */

        //计算获得字体大小
        int fontSize = getOptimalFontSize(message);
        Log.d(TAG, "updateUI: get font size: " + fontSize);
        if (fontSize > 0) {
            //改变字体大小
            Log.d(TAG, "updateUI: change font size: " + fontSize);
            changeFontSizeAndContrast(fontSize);
            BigDecimal bg = new BigDecimal(distToFace);
            double distToFaceWeb = bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            upDateWebViewInterface.ShowSnackbar("font size:" + fontSize + "  " + "distance:" + distToFaceWeb + "cm");
        }
//        /**计算角度
//         *
//         */
//        double angle = 0;
//        if (lastDistToFace > 0) {
//            String string;
//            if (TextUtils.isEmpty(string = etEyeDistance.getText().toString().trim()) == false) {
//
//                float eyeDistance = Float.valueOf(string);
//                // 公式4-15?
//                angle = Math.atan(eyeDistance * Math.abs(message.getRealX() - lastRealX) / (message.getHalfEyeDist() * 2) / message.getDistToFace());
//
//                Log.d(TAG, "message.getRealX()= " + message.getRealX());
//                Log.d(TAG, "lastRealX= " + lastRealX);
//                Log.d(TAG, "eye distance(p)= " + message.getHalfEyeDist() * 2);
//                Log.d(TAG, "distance to face(cm)= " + message.getDistToFace());
////                tvMoveDistance.setText("move dist(p): " + Math.abs(message.getRealX() - lastRealX));
////                tvEyeDistance.setText("eye dist(p): " + message.getHalfEyeDist() * 2);
//            }
//        }
//        lastDistToFace = message.getDistToFace();
//        lastRealX = message.getRealX();
//
//        Log.i(TAG, "angle= " + angle);
////        tvAngle.setText("angle: " + String.valueOf(angle));
    }
//
//    private void resetCam() {
//        _cam.stopPreview();
//        _cam.setPreviewCallback(null);
//        _cam.release();
//    }

//    /**
//     * 改变网页的背景和前景。bg和fg代表背景和前景色，可以是英文颜色名，也可以是rgb值（#RRGGBB）
//     * 未被使用
//     *
//     * @param bg background
//     * @param fg foreground
//     */
//    /*private void changeFgAndBg(String bg, String fg) {
//        webView.getSettings().setJavaScriptEnabled(true);
//        String js = "javascript:" +
//                "var sheet = document.getElementsByTagName('style');"+
//                "if(sheet.length==0) sheet =document.createElement('style');"+
//                "else sheet = document.getElementsByTagName('style')[0];"+
//                "sheet.innerHTML='* { color : " + fg + " !important;background: " + bg + "!important}';"+
//                "document.body.appendChild(sheet);";
//        webView.loadUrl(js);
//    }
//    */

    /**
     * 改变网页的字体大小
     * 未被使用
     *
     * @param fontsize
     */
    /*
    private void changeFontSize(int fontsize) {
        webView.getSettings().setJavaScriptEnabled(true);
        String js = "javascript:(function(){ var css = '* { font-size : " + fontsize + "px !important ; }';var style = document.getElementsByTagName('style');if(style.length==0){style = document.createElement('style');}else{style = document.getElementsByTagName('style')[0];}        if (style.styleSheet){ style.style.styleSheet.cssText=css;}else{style.appendChild(document.createTextNode(css));} document.getElementsByTagName('head')[0].appendChild(style);})()";
        //String js ="javascript:"+"var sheet = document.getElementsByTagName('style');if(sheet.length==0) sheet =document.createElement('style');else sheet = document.getElementsByTagName('style')[0];sheet.innerHTML='* { font-size : "+fontsize+"px !important;}；document.body.appendChild(sheet)';document.body.appendChild(sheet);";
        webView.loadUrl(js);
    }
    */
    /**
    这个方法使用js代码改变了字体和背景
    依据传入的字体大小改变网页前景和背景颜色
     */
    private void changeFontSizeAndContrast(int fontsize) {
        //公式4-19 由字体换算出对比度阈值
        String js = "javascript:(function(){  var contrast = -0.0425*" + fontsize + "+0.85; " +
                "    var body = document.getElementsByTagName('body')[0]; " +
                "var bgL =0;" +
                "    var eps = 1; " +
                "    var fgr,fgg,fgb,result,fgl; " +
                "    console.log('bgL'+bgL); " +
                "    for(var b = 0;b<=255;b++){ " +
                "        for (var r =0;r<=255;r++){ " +
                "            for(var  g= 0; g<=255;g++){ " +
                //公式4-17 根据RGB值计算相对色彩亮度
                "                fgL =0.2126*r+0.7152*g+0.0722*b; " +
                "                result =Math.abs(contrast-Math.abs(fgL/255)); " +
                //获得最小的eps
                "                if(result<eps){ " +
                "                    eps = result; " +
                "                    fgr = r; " +
                "                    fgg = g; " +
                "                    fgb = b; " +
                "                } " +
                "            } " +
                "        } " +
                "    };" +

                "var sheet = document.getElementsByTagName('style'); " +
                "    if(sheet.length==0)  " +
                "    sheet =document.createElement('style'); " +
                "    else  " +
                "    sheet = document.getElementsByTagName('style')[0]; " +
                "    sheet.innerHTML=' * { color : rgb('+fgr+','+fgg+','+fgb+') !important; background: black !important;font-size: " + fontsize + "px !important}';  " +
                "    document.body.appendChild(sheet);}" +
                " )()";
        //webView.loadUrl(js);
        upDateWebViewInterface.LoadJsView(js);//alert('finish');
    }
    /*
    private class MyOnclickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                //访问网页
                case R.id.go_to:
                    Log.i(TAG, "Button go to has been clicked");
                    String urlString = url.getText().toString().trim();
                    if (webView != null) {
                        if (TextUtils.isEmpty(urlString) == false) {
                            webView.loadUrl(urlString);
                        } else {
                            StringBuilder sb = new StringBuilder();
                            sb.append("<html>");
                            sb.append("<head>");
                            sb.append("<title>Welcome !</title>");
                            sb.append("</head>");
                            sb.append("<body>");

                            sb.append("<p style=\"font-family:Calibri\">");
                            sb.append("The Bible (from Koine Greek τὰ βιβλία, tà biblía, \"the books\"[1]) is a collection of sacred texts or scriptures that Jews and Christians consider to be a product of divine inspiration and a record of the relationship between God and humans.\n" +
                                    "Many different authors contributed to the Bible. And what is regarded as canonical text differs depending on traditions and groups; a number of Bible canons have evolved, with overlapping and diverging contents.[2] The Christian Old Testament overlaps with the Hebrew Bible and the Greek Septuagint; the Hebrew Bible is known in Judaism as the Tanakh. The New Testament is a collection of writings by early Christians, believed to be mostly Jewish disciples of Christ, written in first-century Koine Greek. These early Christian Greek writings consist of narratives, letters, and apocalyptic writings. Among Christian denominations there is some disagreement about the contents of the canon, primarily the Apocrypha, a list of works that are regarded with varying levels of respect.\n" +
                                    "Attitudes towards the Bible also differ amongst Christian groups. Roman Catholics, Anglicans and Eastern Orthodox Christians stress the harmony and importance of the Bible and sacred tradition, while Protestant churches focus on the idea of sola scriptura, or scripture alone. This concept arose during the Protestant Reformation, and many denominations today support the use of the Bible as the only source of Christian teaching.\n" +
                                    "With estimated total sales of over 5 billion copies, the Bible is widely considered to be the best-selling book of all time.[3][4] It has estimated annual sales of 100 million copies,[5][6] and has been a major influence on literature and history, especially in the West where the Gutenberg Bible was the first mass-printed book. The Bible was the first book ever printed using movable type.");
                            sb.append("</p>");

                            sb.append("</body>");
                            sb.append("</html>");
                            webView.loadData(sb.toString(), "text/html", "utf-8");
                        }
                    } else {
                        Log.e(TAG, "Go ot error");
                    }
                    break;
                //设置字体
                case R.id.font_size_ok:
                    Log.i(TAG, "Button fontSizeOk has been clicked");

                    String fontSizeString = fontSize.getText().toString().trim();
                    if (fontSizeString != null
                            && TextUtils.isEmpty(fontSizeString) == false
                            && webView != null) {
                        WebSettings settings = webView.getSettings();
                        settings.setTextZoom(Integer.parseInt(fontSizeString));
                    } else {
                        Log.e(TAG, "zoom error");
                    }
                    break;

                case R.id.size_factor_ok:
                    Log.i(TAG, "size_factor_ok has been clicked");
                    String sizeFactorString = sizeFactor.getText().toString().trim();
                    if (sizeFactorString != null
                            && TextUtils.isEmpty(sizeFactorString) == false) {
                        fontSizeFactor = Integer.parseInt(sizeFactorString);
                    }
                    break;

                case R.id.brightness_ok:
                    //亮度值调整
                    Log.i(TAG, "brightness_ok has been clicked.");
                    String brightnessString = brightness.getText().toString().trim();
                    if (TextUtils.isEmpty(brightnessString) == false) {
                        setBrightness(Float.parseFloat(brightnessString));
                    } else {
                    }
                    break;
                //设置亮度因素
                case R.id.brightness_factor_ok:
                    Log.i(TAG, "brightness_factor_ok: has been clicked");
                    //亮度因素调整
                    String brightnessFactorString = brightnessFactor.getText().toString().trim();
                    if (TextUtils.isEmpty(brightnessFactorString) == false) {
                        bFactor = Integer.parseInt(brightnessFactorString);
                    }
                    break;

                default:

            }
        }
    }
    */
//    private class MySensorEventListener implements SensorEventListener {
//
//        @Override
//        public void onSensorChanged(SensorEvent sensorEvent) {
//            float[] values = sensorEvent.values;
//            switch (sensorEvent.sensor.getType()) {
//                //动态更新光线信息
//                case Sensor.TYPE_LIGHT:
//                    brightnessValue = values[0];
//                    break;
//            }
//        }
//
//        @Override
//        public void onAccuracyChanged(Sensor sensor, int i) {
//        }
//    }
}
