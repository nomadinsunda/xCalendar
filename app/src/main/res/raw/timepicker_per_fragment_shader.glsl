precision mediump float;       	// Set the default precision to medium. We don't need as high of a 
								// precision in the fragment shader.
uniform sampler2D u_Texture;    // The input texture.
  
varying vec2 v_TexCoordinate;   // Interpolated texture coordinate per fragment.
varying float alphaValue;

uniform bool u_isChangedAlpha;

// The entry point for our fragment shader.
void main()                    		
{                            
	vec4 baseColor = texture2D(u_Texture, v_TexCoordinate);
	if (u_isChangedAlpha)
    	baseColor.a = alphaValue;
        	
    gl_FragColor = baseColor;                                   		
}                                                                     	

