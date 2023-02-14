#version 430

uniform float toggleColor;
in vec4 vertColor;
out vec4 color;

void main(void)
{
	if(toggleColor == 0.0f) {
		color = vec4(0.0, 0.0, 1.0, 1.0);
	}
	else if(toggleColor == 1.0f) {
		color = vertColor;
	}
}