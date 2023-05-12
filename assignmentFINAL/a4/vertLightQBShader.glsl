#version 430

layout (location=0) in vec3 position;
out vec4 vertColor;

uniform mat4 m_matrix;
uniform mat4 v_matrix;
uniform mat4 p_matrix;

void main(void)
{
    gl_Position = p_matrix * v_matrix * m_matrix * vec4(position,1.0);
	vertColor = vec4(1.0, 0.85, 0.65, 1.0);
} 
