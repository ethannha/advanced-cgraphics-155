#version 430

in vec3 varyingNormal;
in vec3 varyingLightDir;
in vec3 varyingVertPos;
in vec3 varyingHalfVector;
in vec4 shadow_coord;
in vec2 tc;
out vec4 color;

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
uniform float intensity;
uniform mat4 m_matrix;
uniform mat4 v_matrix;
uniform mat4 p_matrix;
uniform mat4 norm_matrix;
uniform mat4 shadowMVP;
layout (binding=0) uniform sampler2D s;
layout (binding=1) uniform sampler2DShadow shadowTex;

float lookup(float x, float y)
{  	
	float t = textureProj(shadowTex, shadow_coord + vec4(x * 0.001 * shadow_coord.w,
                                                         y * 0.001 * shadow_coord.w,
                                                         -0.01, 0.0));
	return t;
}

void main(void)
{	
	float shadowFactor=0.0;

	// normalize the light, normal, and view vectors:
	vec3 L = normalize(varyingLightDir);
	vec3 N = normalize(varyingNormal);
	vec3 V = normalize(-v_matrix[3].xyz - varyingVertPos);
	
	// get the angle between the light and surface normal:
	float cosTheta = dot(L,N);
	
	// halfway vector interpolated
	vec3 H = normalize(varyingHalfVector);

	// get angle between the normal and the halfway vector
	float cosPhi = dot(H,N);

	// shadow
	float swidth = 2.5;
	vec2 o = mod(floor(gl_FragCoord.xy), 2.0) * swidth;
	shadowFactor += lookup(-1.5*swidth + o.x,  1.5*swidth - o.y);
	shadowFactor += lookup(-1.5*swidth + o.x, -0.5*swidth - o.y);
	shadowFactor += lookup( 0.5*swidth + o.x,  1.5*swidth - o.y);
	shadowFactor += lookup( 0.5*swidth + o.x, -0.5*swidth - o.y);
	shadowFactor = shadowFactor / 4.0;

	// hi res PCF
/*	float width = 2.5;
	float endp = width * 3.0 + width/2.0;
	for (float m=-endp ; m<=endp ; m=m+width)
	{	for (float n=-endp ; n<=endp ; n=n+width)
		{	shadowFactor += lookup(m,n);
	}	}
	shadowFactor = shadowFactor / 64.0;
*/
	// this would produce normal hard shadows
//	shadowFactor = lookup(0.0, 0.0);

	// compute shadow color, same as ambient
	vec4 shadowColor = globalAmbient * material.ambient + light.ambient * material.ambient;

	// compute ADS contributions (per pixel):
	vec3 ambient = ((globalAmbient * material.ambient) + (light.ambient * material.ambient)).xyz;
	vec3 diffuse = light.diffuse.xyz * material.diffuse.xyz * max(cosTheta,0.0);
	vec3 specular = light.specular.xyz * material.specular.xyz * pow(max(cosPhi,0.0), material.shininess*3.0);
	
	vec4 texColor = texture(s, tc);
	vec4 lightColor = vec4((ambient + (diffuse * intensity) + (specular * intensity)), 1.0);
	
	// texture with light
	color = (texColor * lightColor);

	// Apply shadow color and factor to the final color
    //vec3 finalColor = shadowColor.xyz + shadowFactor * (texColor.xyz * lightColor.xyz);
    //color = vec4(finalColor, 1.0);

	// Output shadowFactor to screen or console
    // Replace "output_location" with the appropriate location for your output
    // For example, gl_FragColor or a custom output variable
    //color = vec4(shadowFactor);  // Output as grayscale value
}
