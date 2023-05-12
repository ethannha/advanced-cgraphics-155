#version 430

layout (location=3) in vec3 position;
layout (location=4) in vec3 normal;
layout (location=5) in vec2 tex_coord;
out vec3 varyingNormal;
out vec3 varyingLightDir;
out vec3 varyingVertPos;
out vec3 varyingHalfVector;
out vec2 tc;

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
layout (binding=1) uniform sampler2D s;

void main(void)
{	
	varyingVertPos = (m_matrix * vec4(position,1.0)).xyz;
	varyingLightDir = light.position - varyingVertPos;
	varyingNormal = (norm_matrix * vec4(normal,1.0)).xyz;
	varyingHalfVector =
		normalize(normalize(varyingLightDir)
		+ normalize(-varyingVertPos)).xyz;

	gl_Position = p_matrix * v_matrix * m_matrix * vec4(position,1.0);
	tc = tex_coord;
}
