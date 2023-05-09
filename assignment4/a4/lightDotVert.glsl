#version 430

uniform mat4 m_matrix;
uniform mat4 v_matrix;
uniform mat4 p_matrix;
uniform vec3 lightPosition;

layout (location = 0) in vec3 position;

void main()
{
    gl_Position = p_matrix * v_matrix * m_matrix * vec4(lightPosition + position, 1.0);
}
