package com.intheeast.etc;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class EUtil {

	public static float dipToPixels(Context context, float dipValue) {
	    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
	    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
	}
}
