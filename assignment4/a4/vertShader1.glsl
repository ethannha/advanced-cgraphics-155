#version 430

layout (location=0) in vec3 position;

uniform mat4 shadowMVP;

void main(void)
{	gl_Position = shadowMVP * vec4(position,1.0);
}
