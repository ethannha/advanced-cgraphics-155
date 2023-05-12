#version 430

layout (location=0) in vec3 position;
out vec4 vertColor;

uniform mat4 v_matrix;
uniform mat4 p_matrix;

const vec4 vertices[6] = vec4[6]
(vec4(0.0,0.0,0.0, 1.0),
vec4( 3.0,0.0,0.0, 1.0),
vec4( 0.0,0.0,0.0, 1.0),
vec4( 0.0,3.0,0.0, 1.0),
vec4( 0.0,0.0,0.0, 1.0),
vec4( 0.0,0.0,3.0, 1.0));

void main(void)
{
    gl_Position = p_matrix * v_matrix * vertices[gl_VertexID];
	if(gl_VertexID==0 || gl_VertexID==1){
        vertColor = vec4(1.0, 0.0, 0.0, 1.0);
    }
    else if(gl_VertexID==2 || gl_VertexID==3){
        vertColor = vec4(0.0, 1.0, 0.0, 1.0);
    }
    else if(gl_VertexID==4 || gl_VertexID==5){
        vertColor = vec4(0.0, 0.0, 1.0, 1.0);
    }
	
} 
