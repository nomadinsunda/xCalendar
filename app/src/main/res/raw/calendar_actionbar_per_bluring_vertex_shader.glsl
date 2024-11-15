precision mediump float;

//attribute vec2 a_TexCoordinate; // Per-vertex texture coordinate information we will pass in. 		

//varying vec2 v_TexCoordinate;   // This will be passed into the fragment shader.			
attribute vec3 a_Position;		// Per-vertex position information we will pass in.   				


// The entry point for our vertex shader.  
void main()                                                 	
{                
	//v_TexCoordinate = a_TexCoordinate;                                         
	gl_Position = vec4(a_Position, 1.0);
}