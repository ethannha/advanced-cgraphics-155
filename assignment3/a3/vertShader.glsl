#version 430

layout (location=0) in vec3 position;
layout (location=1) in vec2 tex_coord;
layout (binding=0) uniform sampler2D s;
layout (location = 2) in vec3 normal;


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
uniform mat4 mv_matrix;
uniform mat4 p_matrix;
uniform mat4 norm_matrix;
uniform float textureScale;

void main(void)
{
	varyingVertPos = (mv_matrix * vec4(position,1.0)).xyz;
	varyingLightDir = light.position - varyingVertPos;
	varyingNormal = (norm_matrix * vec4(normal,1.0)).xyz;
	
	varyingHalfVector = normalize(normalize(varyingLightDir) + normalize(-varyingVertPos)).xyz;

	gl_Position = p_matrix * mv_matrix * vec4(position,1.0);
	tc = tex_coord * textureScale;
	
} 