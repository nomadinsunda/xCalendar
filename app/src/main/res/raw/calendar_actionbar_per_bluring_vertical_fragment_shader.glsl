precision mediump float;  // 정확도를 선언하지 않으면 컴파일 에러가 발생한다

uniform sampler2D u_Texture;

uniform float v_Width;
uniform float v_Height;

void main(void)
{
    //uniform float offset[3] = float[]( 0.0, 1.3846153846, 3.2307692308 );
    //uniform float weight[3] = float[]( 0.2270270270, 0.3162162162, 0.0702702703 );
    
    float offset[5];  // discrete offset array
    offset[0] = 0.0;  // discrete offset
    offset[1] = 1.0;  // discrete offset
    offset[2] = 2.0;  // discrete offset
    offset[3] = 3.0;  // discrete offset
    offset[4] = 4.0;  // discrete offset    
    
    float weight[5]; // discrete weight array
    weight[0] = 0.2270270270; // discrete weight
    weight[1] = 0.1945945946; // discrete weight
    weight[2] = 0.1216216216; // discrete weight
    weight[3] = 0.0540540541; // discrete weight
    weight[4] = 0.0162162162; // discrete weight
    
    //float offset[3];  // linear offset array
    //offset[0] = 0.0;
    //offset[1] = 1.3846153846;  // linear offset
    //offset[2] = 3.2307692308;  // linear offset
    
    //float weight[3]; // linear weight array
    //weight[0] = 0.2270270270;    
    //weight[1] = 0.3162162162; // linear weight
    //weight[2] = 0.0702702703; // linear weight
    
    gl_FragColor = texture2D( u_Texture, vec2(gl_FragCoord.x/v_Width, gl_FragCoord.y/v_Height) ) * weight[0];
        
	for (int i = 1; i < 5; i++) { // discrete sampling
	//for (int i = 1; i < 3; i++) { // linear sampling
		gl_FragColor += texture2D( u_Texture, vec2((gl_FragCoord.x + 0.0)/v_Width, (gl_FragCoord.y + offset[i])/v_Height) ) * weight[i];
		gl_FragColor += texture2D( u_Texture, vec2((gl_FragCoord.x + 0.0)/v_Width, (gl_FragCoord.y - offset[i])/v_Height) ) * weight[i];		
	}	
}