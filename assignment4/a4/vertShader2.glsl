#version 430

layout (location=0) in vec3 position;
layout (location=1) in vec2 tex_coord;
layout (location=2) in vec3 normal;
out vec3 varyingNormal;
out vec3 varyingLightDir;
out vec3 varyingVertPos;
out vec3 varyingHalfVector;
out vec4 shadow_coord;
out vec2 tc;

struct PositionalLight
{	vec4 ambient, diffuse, specular;
	vec3 position;
};
struct Material
{	vec4 ambient, diffuse, specular;
	float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 m_matrix;
uniform mat4 v_matrix;
uniform mat4 p_matrix;
uniform mat4 norm_matrix;
uniform mat4 shadowMVP;
layout (binding=0) uniform sampler2D s;
layout (binding=1) uniform sampler2DShadow shadowTex;

void main(void)
{	
	varyingVertPos = (m_matrix * vec4(position,1.0)).xyz;
	varyingLightDir = light.position - varyingVertPos;
	varyingNormal = (norm_matrix * vec4(normal,1.0)).xyz;
	varyingHalfVector = normalize(varyingLightDir-varyingVertPos).xyz;

	shadow_coord = shadowMVP * vec4(vertPos,1.0);

	gl_Position = p_matrix * v_matrix * m_matrix * vec4(position,1.0);
	tc = tex_coord;
}
