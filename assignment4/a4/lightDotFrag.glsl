#version 430

uniform vec4 lightDotColor;
out vec4 fragColor;

void main()
{
    fragColor = lightDotColor;
}
