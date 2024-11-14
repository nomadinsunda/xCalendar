package com.intheeast.easing;

public class Cubic {
    
	// 물체의 움직임이 서서히 시작해서 빨라지는 것 : acceleration
    public static float easeIn (float t, float b , float c, float d) {
            return c*(t/=d)*t*t + b;
    }
    
    // 움직임이 멈출 때 끝에 이르러 변화의 정도가 서서히 감소하는 것. 촬영기나 물체가 움직임이다가 정지상태가 될 때 서서히 멈추는 것 : deceleration
    public static float easeOut (float t, float b , float c, float d) {
            return c*((t=t/d-1)*t*t + 1) + b;
    }
    
    // 물체의 움직임이 급격해지는 것을 피하기 위하여 천천히 움직이기 시작하여 서서히 속도가 빨라진 뒤 다시 점차 속도가 줄어들면서 정지되는 것
    public static float easeInOut (float t, float b , float c, float d) {
            if ((t/=d/2) < 1) return c/2*t*t*t + b;
            return c/2*((t-=2)*t*t + 2) + b;
    }

}
