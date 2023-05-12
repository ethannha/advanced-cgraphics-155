#version 430

layout (location=9) in vec4 vertPos;
layout (location=10) in vec4 vertNormal;

out vec3 varyingOriginalNormal;

struct PositionalLight
{	vec4 ambient;
	vec4 diffuse;
	vec4 specular;
	vec3 position;
};
struct Material
{	vec4 ambient;  
	vec4 diffuse;  
	vec4 specular;  
	float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 m_matrix;
uniform mat4 v_matrix;
uniform mat4 p_matrix;
uniform mat4 norm_matrix;

void main(void)
{	varyingOriginalNormal = (norm_matrix * vertNormal).xyz;
	gl_Position = m_matrix * vertPos;
}
