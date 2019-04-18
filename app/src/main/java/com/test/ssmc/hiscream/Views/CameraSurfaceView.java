package com.test.ssmc.hiscream.Views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.FaceDetector.Face;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import com.test.ssmc.hiscream.Views.View.UpDateWebViewInterface;
import com.test.ssmc.hiscream.Views.measurement.Point;
import com.test.ssmc.hiscream.Views.messages.MeasurementStepMessage;
import com.test.ssmc.hiscream.Views.messages.MessageHUB;
import com.test.ssmc.hiscream.Views.utils.Util;

import java.util.ArrayList;
import java.util.List;

public class CameraSurfaceView extends SurfaceView implements Callback,
        Camera.PreviewCallback {
    private static final String TAG = "CameraSurfaceView";
    /**
     * Represents the standard height of a peace of a4 paper e.g. 29.7cm
     */
    public static final int CALIBRATION_DISTANCE_A4_MM = 294;

    public static final int CALIBRATION_MEASUREMENTS = 10;

    public static final int AVERAGE_THREASHHOLD = 5;

    /**
     * 在校准点测量出的人脸到设备的
     * Measured distance at calibration point
     */
    private float _distanceAtCalibrationPoint = -1;

    /**
     * 实时获得的人眼间距的平均值
     * 像素点为单位
     */
    private float _currentAvgEyeDistance = -1;

    /**
     * 实时计算出的人脸到设备的距离
     * in cm
     */
    private float _currentDistanceToFace = -1;

    private final SurfaceHolder mHolder;

    private Camera mCamera;

    private Face _foundFace = null;

    private int _threshold = CALIBRATION_MEASUREMENTS;//阈值、临界值

    private FaceDetectionThread _currentFaceDetectionThread;//人脸检测线程

    private List<Point> _points;//人脸双眼距离、设备和用户距离 组成的点的List

    protected final Paint _middlePointColor = new Paint();
    protected final Paint _eyeColor = new Paint();

    private Size _previewSize;

    //校准状态标志
    private boolean _calibrated = false;
    private boolean _calibrating = false;
    private int _calibrationsLeft = -1;

    private float heightRatio = 0f;
    private float widthRatio = 0f;


    public CameraSurfaceView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        _middlePointColor.setARGB(100, 200, 0, 0);
        _middlePointColor.setStyle(Paint.Style.FILL);
        _middlePointColor.setStrokeWidth(2);

        _eyeColor.setColor(Color.GREEN);

        mHolder = getHolder();
        mHolder.addCallback(this);
    }
    public CameraSurfaceView(final Context context) {
        super(context);
        _middlePointColor.setARGB(100, 200, 0, 0);
        _middlePointColor.setStyle(Paint.Style.FILL);
        _middlePointColor.setStrokeWidth(2);

        _eyeColor.setColor(Color.GREEN);

        mHolder = getHolder();
        mHolder.addCallback(this);
    }
    public void setCamera(final Camera camera) {
        mCamera = camera;

        if (mCamera != null) {
            requestLayout();

            Parameters params = mCamera.getParameters();
            camera.setDisplayOrientation(90);
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Parameters.FOCUS_MODE_AUTO)) {
                // set the focus mode
                params.setFocusMode(Parameters.FOCUS_MODE_AUTO);
                // set Camera parameters
                mCamera.setParameters(params);
            }
        }
    }

    /**
     * Variables for the onDraw method, in order to prevent variable allocation
     * to slow down the sometimes heavily called onDraw method
     */
    private final PointF _middlePoint = new PointF();
    private final Rect _trackingRectangle = new Rect();

    private final static int RECTANGLE_SIZE = 20;
    private boolean _showEyes = false;
    private boolean _showTracking = true;

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(final Canvas canvas) {
        if (_foundFace != null) {
            _foundFace.getMidPoint(_middlePoint);

            Log.d(TAG, "middlePoint= " + _middlePoint.x + " : " + _middlePoint.y);
            Log.d(TAG, "_foundFace.eyesDistance()= " + _foundFace.eyesDistance());

            // portrait mode! 肖像模式
            float heightRatio = canvas.getHeight() / (float) _previewSize.width;//previewSize.width以像素为单位
            float widthRatio = canvas.getWidth() / (float) _previewSize.height;
            this.heightRatio = heightRatio;
            this.widthRatio = widthRatio;

            //调试用
            Log.d(TAG, "previewSize.width= " + _previewSize.width);
            Log.d(TAG, "previewSize.height= " + _previewSize.height);
            Log.d(TAG, "canvas.getWidth()= " + canvas.getWidth());
            Log.d(TAG, "canvas.getHeight()= " + canvas.getHeight());

            int realX = (int) (_middlePoint.x * widthRatio);
            int realY = (int) (_middlePoint.y * heightRatio);

            //调试用
            Log.d(TAG, "Drawcall Real :" + realX + " : " + realY);

            //眼球距中点的距离
            int halfEyeDist = (int) (widthRatio * _foundFace.eyesDistance() / 2);

            //调试用
            Log.d(TAG, "halfEyeDis= " + halfEyeDist);

            //双眼中点定位
            if (_showTracking) {
                // Middle point
                _trackingRectangle.left = realX - RECTANGLE_SIZE;
                _trackingRectangle.top = realY - RECTANGLE_SIZE;
                _trackingRectangle.right = realX + RECTANGLE_SIZE;
                _trackingRectangle.bottom = realY + RECTANGLE_SIZE;
                canvas.drawRect(_trackingRectangle, _middlePointColor);
            }
            //左右眼追踪定位
            if (_showEyes) {
                // Left eye
                _trackingRectangle.left = realX - halfEyeDist - RECTANGLE_SIZE;
                _trackingRectangle.top = realY - RECTANGLE_SIZE;
                _trackingRectangle.right = realX - halfEyeDist + RECTANGLE_SIZE;
                _trackingRectangle.bottom = realY + RECTANGLE_SIZE;
                canvas.drawRect(_trackingRectangle, _eyeColor);

                // Right eye
                _trackingRectangle.left = realX + halfEyeDist - RECTANGLE_SIZE;
                _trackingRectangle.top = realY - RECTANGLE_SIZE;
                _trackingRectangle.right = realX + halfEyeDist + RECTANGLE_SIZE;
                _trackingRectangle.bottom = realY + RECTANGLE_SIZE;
                canvas.drawRect(_trackingRectangle, _eyeColor);
            }
        }
    }

    public void reset() {
        _distanceAtCalibrationPoint = -1;
        _currentAvgEyeDistance = -1;
        _calibrated = false;
        _calibrating = false;
        _calibrationsLeft = -1;
    }

    /**
     * Sets this current EYE distance to be the distance of a peace of a4 paper
     * e.g. 29,7cm
     */
    public void calibrate() {
        if (!_calibrating || !_calibrated) {
            _points = new ArrayList<Point>();
            _calibrating = true;
            _calibrationsLeft = CALIBRATION_MEASUREMENTS;
            _threshold = CALIBRATION_MEASUREMENTS;
        }
    }

    private void doneCalibrating() {
        Log.d(TAG, "doneCalibrating: ");
        _calibrated = true;
        _calibrating = false;
        _currentFaceDetectionThread = null;

        _threshold = AVERAGE_THREASHHOLD;

        _distanceAtCalibrationPoint = _currentAvgEyeDistance;
        MessageHUB.get().sendMessage(MessageHUB.DONE_CALIBRATION, null);
    }

    public boolean isCalibrated() {
        return _calibrated || _calibrating;
    }

    public void showMiddleEye(final boolean on) {
        _showTracking = on;
    }

    public void showEyePoints(final boolean on) {
        _showEyes = on;
    }

    private void updateMeasurement(final Face currentFace) {
        Log.i(TAG, "updateMeasurement(Face)");
        if (currentFace == null) {
            return;
        }

        //获取人脸数据
        _foundFace = _currentFaceDetectionThread.getCurrentFace();
        float eyesDistance = _foundFace.eyesDistance();//测得的双眼距离
        float deviceDistance = CALIBRATION_DISTANCE_A4_MM * (_distanceAtCalibrationPoint / eyesDistance);//测得的设备和用户的距离
        _points.add(new Point(eyesDistance, deviceDistance));
        //_points只保存 _threshold = 10 个人脸的Point
        while (_points.size() > _threshold) {
            _points.remove(0);
        }

        float sum = 0;
        for (Point p : _points) {
            sum += p.getEyeDistance();
        }
        _currentAvgEyeDistance = sum / _points.size();//计算得到 当前人眼间距的平均值
        _currentDistanceToFace = CALIBRATION_DISTANCE_A4_MM
                * (_distanceAtCalibrationPoint / _currentAvgEyeDistance);
        Log.d(TAG, "updateMeasurement1: " + _currentAvgEyeDistance);
        Log.d(TAG, "updateMeasurement2: " + _distanceAtCalibrationPoint);
        _currentDistanceToFace = Util.MM_TO_CM(_currentDistanceToFace);

        MeasurementStepMessage message = new MeasurementStepMessage();
        message.setConfidence(currentFace.confidence());
        message.setCurrentAvgEyeDistance(_currentAvgEyeDistance);
        message.setDistToFace(_currentDistanceToFace);
        message.setEyesDistance(currentFace.eyesDistance());
        message.setMeasurementsLeft(_calibrationsLeft);
        message.setProcessTimeForLastFrame(_processTimeForLastFrame);

        currentFace.getMidPoint(_middlePoint);///
        message.setMiddlePoint(_middlePoint);///
        message.setRealX((int) (_middlePoint.x * widthRatio));///
        message.setRealY((int) (_middlePoint.y * heightRatio));///
        message.setHalfEyeDist((int) (widthRatio * _foundFace.eyesDistance() / 2));///

        Log.d(TAG, "_currentDistanceToFace= " + _currentDistanceToFace);

        //发送更新UI的信息
        MessageHUB.get().sendMessage(MessageHUB.MEASUREMENT_STEP, message);
    }

    private long _lastFrameStart = System.currentTimeMillis();
    private float _processTimeForLastFrame = -1;

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        if (_calibrationsLeft == -1)
            return;
        //正在检测
        if (_calibrationsLeft > 0) {
            // Doing calibration !

            if (_currentFaceDetectionThread != null
                    && _currentFaceDetectionThread.isAlive()) {
                //丢弃当前的人脸 直接返回
                // Drop Frame

                return;
            }

            // No face detection started or already finished
            _processTimeForLastFrame = System.currentTimeMillis()
                    - _lastFrameStart;
            _lastFrameStart = System.currentTimeMillis();

            if (_currentFaceDetectionThread != null) {
                _calibrationsLeft--;
                updateMeasurement(_currentFaceDetectionThread.getCurrentFace());

                if (_calibrationsLeft == 0) {
                    doneCalibrating();
                    invalidate();
                    return;
                }
            }

            _currentFaceDetectionThread = new FaceDetectionThread(data,
                    _previewSize);
            _currentFaceDetectionThread.start();

            invalidate();
        } else {
            // Simple Measurement

            if (_currentFaceDetectionThread != null
                    && _currentFaceDetectionThread.isAlive()) {
                // Drop Frame
                return;
            }

            // No face detection started or already finished
            //没有已开启的或者已结束的线程
            _processTimeForLastFrame = System.currentTimeMillis()
                    - _lastFrameStart;
            _lastFrameStart = System.currentTimeMillis();
            //更新测量
            if (_currentFaceDetectionThread != null)
                updateMeasurement(_currentFaceDetectionThread.getCurrentFace());

            _currentFaceDetectionThread = new FaceDetectionThread(data,
                    _previewSize);
            _currentFaceDetectionThread.start();
            invalidate();
        }
    }

    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        synchronized (this) {
            // This allows us to make our own drawBitmap
            this.setWillNotDraw(false);
        }
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder) {
//        mCamera.release();
//        mCamera = null;
    }

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format,
                               final int width, final int height) {

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        Parameters parameters = mCamera.getParameters();
        _previewSize = parameters.getPreviewSize();

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            mCamera.setPreviewCallback(this);

        } catch (Exception e) {
            Log.d("This", "Error starting camera preview: " + e.getMessage());
        }
    }
}
