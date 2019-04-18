package com.test.ssmc.hiscream.Views.measurement;

/**
* 双眼距离、设备和用户的距离组成的点
* */
public class Point {
	//测得的双眼距离
	private final float _eyeDistance;
	//测得的设备和用户的距离
	private final float _deviceDistance;

	public Point(final float eyeDistance, final float deviceDistance) {
		_eyeDistance = eyeDistance;
		_deviceDistance = deviceDistance;
	}

	/**
	 * in mm
	 * 
	 * @return
	 */
	public float getEyeDistance() {
		return _eyeDistance;
	}

	/**
	 * in mm
	 * 
	 * @return
	 */
	public float getDeviceDistance() {
		return _deviceDistance;
	}
}
