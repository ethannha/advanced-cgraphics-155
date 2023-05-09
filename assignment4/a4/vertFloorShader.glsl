#version 430

layout (location=0) in vec3 position;
layout (location=1) in vec2 texCoord;
out vec2 tc;

uniform mat4 m_matrix;
uniform mat4 v_matrix;
uniform mat4 p_matrix;

void main(void)
{
	gl_Position = p_matrix * v_matrix * m_matrix * vec4(position,1.0);
	tc = texCoord;
} 
