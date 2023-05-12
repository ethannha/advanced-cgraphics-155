#version 430

layout (location = 7) in vec3 position;
layout (location = 8) in vec3 normal;
out vec3 vNormal;
out vec3 vVertPos;

uniform mat4 mv_matrix;
uniform mat4 p_matrix;
uniform mat4 norm_matrix;
layout (binding = 4) uniform samplerCube t;

void main(void)
{
	vVertPos = (mv_matrix * vec4(position,1.0)).xyz;
	vNormal = (norm_matrix * vec4(normal,1.0)).xyz;
	gl_Position = p_matrix * mv_matrix * vec4(position,1.0);
}
