void main()
{
	gl_FrontColor = gl_Color;
	gl_BackColor = gl_Color;
		
	gl_Position = gl_Vertex;
	float a_cos = cos( gl_Normal.x );
	float a_sin = sin( gl_Normal.x );
	gl_Position.x = (gl_Vertex.x * a_cos - gl_Vertex.y * a_sin) * gl_Vertex.z;
	gl_Position.y = (gl_Vertex.y * a_cos + gl_Vertex.x * a_sin) * gl_Vertex.z;
	gl_Position.x += gl_Normal.y;
	gl_Position.y += gl_Normal.z;	
	gl_Position = gl_ModelViewProjectionMatrix * gl_Position;
} 