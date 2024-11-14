package com.intheeast.gl;

public class EyePosition {
	public float eyeX;
	public float eyeY;
	public float eyeZ; 
	public float lookAtX;
	public float lookAtY; 
	public float lookAtZ;
	public float upX; 
	public float upY; 
	public float upZ;
    
    public EyePosition() {
    	this.eyeX = 0;
    	this.eyeY = 0;
    	this.eyeZ = 0;
    	this.lookAtX = 0;
    	this.lookAtY = 0; 
    	this.lookAtZ = 0;
    	this.upX = 0;
    	this.upY = 0;
    	this.upZ = 0;
    }
    
    public EyePosition(float eyeX, float eyeY, float eyeZ, 
                       float lookAtX, float lookAtY, float lookAtZ,
                       float upX, float upY, float upZ) {
    	this.eyeX = eyeX;
    	this.eyeY = eyeY;
    	this.eyeZ = eyeZ;
    	this.lookAtX = lookAtX;
    	this.lookAtY = lookAtY; 
    	this.lookAtZ = lookAtZ;
    	this.upX = upX;
    	this.upY = upY;
    	this.upZ = upZ;
    }
    
    public void setEyeX(float eyeX) {
    	this.eyeX = eyeX;    	
    }
    
    public void setEyeY(float eyeY) {
    	this.eyeY = eyeY;    	
    }
    
    public void setEyeZ(float eyeZ) {
    	this.eyeZ = eyeZ;    	
    }
    
    public void setEyeXYZ(float eyeX, float eyeY, float eyeZ) {
    	this.eyeX = eyeX;
    	this.eyeY = eyeY;
    	this.eyeZ = eyeZ;
    }
    
    public void setLookAtX(float lookAtX) {
    	this.lookAtX = lookAtX;
    }
    
    public void setLookAtY(float lookAtY) {
    	this.lookAtY = lookAtY;
    }
    
    public void setLookAtZ(float lookAtZ) {
    	this.lookAtZ = lookAtZ;
    }
    
    public void setLookAtXYZ(float lookAtX, float lookAtY, float lookAtZ) {
    	this.lookAtX = lookAtX;
    	this.lookAtY = lookAtY; 
    	this.lookAtZ = lookAtZ;
    }

    public void setUpXYZ(float upX, float upY, float upZ) {
    	this.upX = upX;
    	this.upY = upY;
    	this.upZ = upZ;
    }             
}
