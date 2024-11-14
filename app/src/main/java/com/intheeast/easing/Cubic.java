package com.intheeast.easing;

public class Cubic {
    
	// ��ü�� �������� ������ �����ؼ� �������� �� : acceleration
    public static float easeIn (float t, float b , float c, float d) {
            return c*(t/=d)*t*t + b;
    }
    
    // �������� ���� �� ���� �̸��� ��ȭ�� ������ ������ �����ϴ� ��. �Կ��⳪ ��ü�� �������̴ٰ� �������°� �� �� ������ ���ߴ� �� : deceleration
    public static float easeOut (float t, float b , float c, float d) {
            return c*((t=t/d-1)*t*t + 1) + b;
    }
    
    // ��ü�� �������� �ް������� ���� ���ϱ� ���Ͽ� õõ�� �����̱� �����Ͽ� ������ �ӵ��� ������ �� �ٽ� ���� �ӵ��� �پ��鼭 �����Ǵ� ��
    public static float easeInOut (float t, float b , float c, float d) {
            if ((t/=d/2) < 1) return c/2*t*t*t + b;
            return c/2*((t-=2)*t*t + 2) + b;
    }

}
