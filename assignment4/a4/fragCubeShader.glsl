#version 430

in vec3 tc;
out vec4 fragColor;

uniform mat4 v_matrix;
uniform mat4 p_matrix;
layout (binding = 4) uniform samplerCube samp;

void main(void)
{
	fragColor = texture(samp,tc);
}
