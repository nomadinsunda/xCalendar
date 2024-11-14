package com.intheeast.impl;

public class gestureScrollMovementVector {
	public static final int SCROLLING_NONE_BIG = 0;
	public static final int SCROLLING_XSCALAR_BIG = 1;
	public static final int SCROLLING_YSCALAR_BIG = 2;
	public static final int SCROLLING_BOTH_SAME = 3;	
	
	public int m_whichBigScalarBetweenXandY;
	public int m_KickoffScroll_x1;
	public float m_scroll_previous_x;
	public int m_KickoffScroll_y1;
	public float m_scroll_previous_y;
	
	public float m_moveXscalar;
	public float m_moveYscalar;
	
	public float m_reloadingCur_X;
	public float m_reloadingCur_Y;
	public float m_reloadingPrv_X;
	public float m_reloadingPrv_Y;	
	
	boolean m_Reset;
	
	public gestureScrollMovementVector() {
		m_whichBigScalarBetweenXandY = SCROLLING_NONE_BIG;
		m_KickoffScroll_x1 = 0;
		m_scroll_previous_x = 0;
		m_KickoffScroll_y1 = 0;
		m_scroll_previous_y = 0;
		m_moveXscalar = 0;
		m_moveYscalar = 0;
		//m_Reset = true;
	}
	
	public void resetScrollMovementVector() {
		m_whichBigScalarBetweenXandY = SCROLLING_NONE_BIG;
		m_KickoffScroll_x1 = 0;
		m_scroll_previous_x = 0;
		m_KickoffScroll_y1 = 0;
		m_scroll_previous_y = 0;
		m_moveXscalar = 0;
		m_moveYscalar = 0;
	}
	
	public void setMoveXScalar(float moveXScalar) {
		m_whichBigScalarBetweenXandY = SCROLLING_XSCALAR_BIG;
		m_moveXscalar = moveXScalar;
	}
	
	public void setMoveYScalar(float moveYScalar) {
		m_whichBigScalarBetweenXandY = SCROLLING_YSCALAR_BIG;
		m_moveYscalar = moveYScalar;
	}	
	
	public float getMoveXScalar() {
		return m_moveXscalar;
	}
	
	public float getMoveYScalar() {
		return m_moveYscalar;
	}
}
