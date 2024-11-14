package com.intheeast.easing;

public class SigmoidCurve {
	// sigmoid curve 
	// f(x) = 1 / (1 + e^-x)
	// x=[-10~10], y=[0~1]		
	// x=-10, 1/(1+e^10) = 0.00004539786
	// x=10, 1/(1+e^-10) = 0.99995460213
	// x=[-6~6], y=[0~1]		
	// x=-6, 1/(1+e^6) = 0.00247262315
	// x=6, 1/(1+e^-6) = 0.99752737684
	// (scalar_time / total_time)를 어떻게 [-6 ~ 6]으로 치환하는가???
	// x=[-5~5], y=[0~1]
	// x = ((scalar_time / total_time) * 10) - 5
	// x = (spentTimeRatio * 10) - 5;
	// x = (spentTimeRatio * 12) - 6;
	// apply_time = 1 / ( 1 + Math.exp(-x) );
		
	public static float  ease(float scalar, float total) {
		float apply_time = 0;
		float spentTimeRatio = scalar / total;
		double x = (spentTimeRatio * 12) - 6;
		apply_time = (float)( 1 / ( 1 + Math.exp(-x) ) );
		return apply_time;		
	}
}
