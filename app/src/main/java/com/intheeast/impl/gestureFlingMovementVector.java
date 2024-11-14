package com.intheeast.impl;

public class gestureFlingMovementVector {
	public static final int FLING_NONE_BIG = 0;
	public static final int FLING_XSCALAR_BIG = 1;
	public static final int FLING_YSCALAR_BIG = 2;
	public static final int FLING_BOTH_SAME = 3;
	
	// if 800pixels duration 200ms, 4 pixels per 1 ms -> 4000 pixels per 1s
	//public static final int FLING_VELOCITY_X = 2000; // 4000 / 2
	// if 1230pixels duration 200ms, 6.15 pixels per 1 ms -> 6150 pixels per 1s
	//public static final int FLING_VELOCITY_Y = 3075; // 6150 / 2
	
	public float m_fling_velocity_axis_x;
	public float m_fling_velocity_axis_y;
	public int m_whichBigScalarBetweenXandY;
	
	public KScroller m_scroller;
	public int m_LastFlingX;
	public int m_LastFlingY;
	public float m_moveXscalar;
	public float m_moveYscalar;
	
	public gestureFlingMovementVector() {
		m_whichBigScalarBetweenXandY = FLING_NONE_BIG;
		m_LastFlingX = 0;
		m_LastFlingY = 0;
		m_moveXscalar = 0;
		m_moveYscalar = 0;
		m_fling_velocity_axis_x = 0;
		m_fling_velocity_axis_y = 0;
	}
	
	public void resetFlingMovementVector() {
		m_whichBigScalarBetweenXandY = FLING_NONE_BIG;
		m_LastFlingX = 0;
		m_LastFlingY = 0;
		m_moveXscalar = 0;
		m_moveYscalar = 0;
		m_fling_velocity_axis_x = 0;
		m_fling_velocity_axis_y = 0;
	}
	
	public void setVelocityAxiX(int velocity) {
		m_fling_velocity_axis_x = velocity;
	}
	
	public void setVelocityAxisY(int velocity) {
		m_fling_velocity_axis_y = velocity;
	}
	
	public void setMoveXScalar(float moveXScalar) {
		m_whichBigScalarBetweenXandY = FLING_XSCALAR_BIG;
		m_moveXscalar = moveXScalar;
	}
	
	public void setMoveYScalar(float moveYScalar) {
		m_whichBigScalarBetweenXandY = FLING_YSCALAR_BIG;
		m_moveYscalar = moveYScalar;
	}	
	
	public float getMoveXScalar() {
		return m_moveXscalar;
	}
	
	public float getMoveYScalar() {
		return m_moveYscalar;
	}
	
	public void stopComputeFlingScalar() {
		m_scroller.forceFinished(true);
	}
}
