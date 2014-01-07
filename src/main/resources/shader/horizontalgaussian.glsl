uniform sampler2D tex;

const float step = 1.0 / 512.0;
const float half_taps = 3.0;

void main( )
{
	vec4 color = vec4(0,0,0,0);
		
	float sum = 0.0;
	for( float x = -half_taps; x < half_taps; x++ )
	{
		color += texture2D(tex, gl_TexCoord[0].st + x * vec2(step,0) ) * (abs(x) - half_taps - 1.0);		
		sum += (abs(x) - half_taps - 1.0);
	} 
	
	gl_FragColor = color / (sum / 1.2);
}