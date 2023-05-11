#version 430

layout (location = 3) in vec3 vertPos;
layout (location = 4) in vec2 texCoord;
layout (location = 5) in vec3 vertNormal;
layout (location = 6) in vec3 vertTangent;

out vec3 varyingLightDir;
out vec3 varyingVertPos;
out vec3 varyingNormal;
out vec3 varyingTangent;
out vec3 originalVertex;
out vec2 tc;

layout (binding=2) uniform sampler2D s;
layout (binding=3) uniform sampler2D t;

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
{	varyingVertPos = (v_matrix * vec4(vertPos,1.0)).xyz;
	varyingLightDir = light.position - varyingVertPos;
	tc = texCoord;
	
	originalVertex = vertPos;

	varyingNormal = (norm_matrix * vec4(vertNormal,1.0)).xyz;
	varyingTangent = (norm_matrix * vec4(vertTangent,1.0)).xyz;

	gl_Position = p_matrix * v_matrix * m_matrix * vec4(vertPos,1.0);
}
