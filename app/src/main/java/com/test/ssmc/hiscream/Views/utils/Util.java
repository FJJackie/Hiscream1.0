package com.test.ssmc.hiscream.Views.utils;


public class Util {
	/*
	单位转换
	 */
	public static float CM_TO_MM(final float cm) {
		return cm * 10;
	}

	public static float CM_TO_MM(final int cm) {
		return cm * 10.0f;
	}

	public static float MM_TO_CM(final float mm) {
		return mm / 10;
	}

	public static float MM_TO_CM(final int mm) {
		return mm / 10.0f;
	}
}
