#version 430

in vec2 tc;
out vec4 color;
out vec4 tex;

uniform mat4 mv_matrix;
uniform mat4 p_matrix;
layout (binding=0) uniform sampler2D s;

void main(void)
{	
    color = vec4(0.0, 1.0, 0.0, 1.0);
    tex = texture(s,tc);
}
