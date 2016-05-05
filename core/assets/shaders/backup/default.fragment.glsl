#ifdef GL_ES
#define LOWP lowp
#define MED mediump
#define HIGH highp
precision mediump float;
#else
#define MED
#define LOWP
#define HIGH
#endif

////////////////////////////////////////////////////////////////////////////////////
////////// POSITION ATTRIBUTE - FRAGMENT
////////////////////////////////////////////////////////////////////////////////////
#define nop() {}

varying vec4 v_position;
vec4 g_position = vec4(0.0, 0.0, 0.0, 1.0);
#define pullPosition() (g_position = v_position)

////////////////////////////////////////////////////////////////////////////////////
////////// COLOR ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec4 v_color;
vec4 g_color = vec4(1.0, 1.0, 1.0, 1.0);
#define pullColor()	(g_color = v_color)

////////////////////////////////////////////////////////////////////////////////////
////////// NORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec3 v_normal;
vec3 g_normal = vec3(0.0, 0.0, 1.0);
#define pullNormal() (g_normal = v_normal)

////////////////////////////////////////////////////////////////////////////////////
////////// BINORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec3 v_binormal;
vec3 g_binormal = vec3(0.0, 0.0, 1.0);
#define pullBinormal() (g_binormal = v_binormal)

////////////////////////////////////////////////////////////////////////////////////
////////// TANGENT ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec3 v_tangent;
vec3 g_tangent = vec3(1.0, 0.0, 0.0);
#define pullTangent() (g_tangent = v_tangent)

////////////////////////////////////////////////////////////////////////////////////
////////// TEXCOORD0 ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
varying vec2 v_texCoord0;
vec2 g_texCoord0 = vec2(0.0, 0.0);
#define pullTexCoord0() (g_texCoord0 = v_texCoord0)


// Uniforms which are always available
uniform mat4 u_projViewTrans;

uniform mat4 u_worldTrans;

uniform vec4 u_cameraPosition;

uniform mat3 u_normalMatrix;

// Other uniforms
#ifdef blendedFlag
uniform float u_opacity;
#else
const float u_opacity = 1.0;
#endif

#ifdef alphaTestFlag
uniform float u_alphaTest;
#else
const float u_alphaTest = 0.0;
#endif

#ifdef shininessFlag
uniform float u_shininess;
#else
const float u_shininess = 20.0;
#endif


#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef specularColorFlag
uniform vec4 u_specularColor;
#endif

#ifdef specularTextureFlag
uniform sampler2D u_specularTexture;
#endif

#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
#endif

#if defined(diffuseTextureFlag) || defined(specularTextureFlag)
#define textureFlag
#endif

#if defined(specularTextureFlag) || defined(specularColorFlag)
#define specularFlag
#endif

#if defined(specularFlag) || defined(fogFlag)
#define cameraPositionFlag
#endif


#ifdef shadowMapFlag
uniform sampler2D u_shadowTexture;
varying vec3 v_shadowMapUv;
#define separateAmbientFlag

/* ************************************************************************** */
/* http://developer.download.nvidia.com/whitepapers/2008/PCSS_Integration.pdf */
/* ************************************************************************** */

#define NEAR_PLANE 20
#define LIGHT_WORLD_SIZE 0.6
#define LIGHT_FRUSTUM_WIDTH 1000
#define PCSS_BIAS 0.01

// Assuming that LIGHT_FRUSTUM_WIDTH == LIGHT_FRUSTUM_HEIGHT
#define LIGHT_SIZE_UV (LIGHT_WORLD_SIZE / LIGHT_FRUSTUM_WIDTH)

#define BLOCKER_SEARCH_NUM_SAMPLES 16
const vec2 poissonDiskBlocker[] = {
    vec2( -0.94201624, -0.39906216 ),
    vec2( 0.94558609, -0.76890725 ),
    vec2( -0.094184101, -0.92938870 ),
    vec2( 0.34495938, 0.29387760 ),
    vec2( -0.91588581, 0.45771432 ),
    vec2( -0.81544232, -0.87912464 ),
    vec2( -0.38277543, 0.27676845 ),
    vec2( 0.97484398, 0.75648379 ),
    vec2( 0.44323325, -0.97511554 ),
    vec2( 0.53742981, -0.47373420 ),
    vec2( -0.26496911, -0.41893023 ),
    vec2( 0.79197514, 0.19090188 ),
    vec2( -0.24188840, 0.99706507 ),
    vec2( -0.81409955, 0.91437590 ),
    vec2( 0.19984126, 0.78641367 ),
    vec2( 0.14383161, -0.14100790 )
};

#define PCF_NUM_SAMPLES 25
const vec2 poissonDiskPCF[] = {
    vec2( -0.7505342, 0.5912911 ),
    vec2( -0.6739749, 0.2554671 ),
    vec2( -0.2951576, 0.6270834 ),
    vec2( -0.371296, 0.1871899 ),
    vec2( -0.7291721, -0.08390427 ),
    vec2( -0.4435159, -0.2342117 ),
    vec2( -0.5003734, -0.6009152 ),
    vec2( -0.09212996, -0.005192649 ),
    vec2( -0.004650356, -0.5252756 ),
    vec2( 0.376779, 0.04625643 ),
    vec2( -0.0335904, 0.4419256 ),
    vec2( 0.1714043, -0.2117357 ),
    vec2( -0.9311763, -0.3311462 ),
    vec2( -0.2234885, -0.8848045 ),
    vec2( 0.1046409, -0.9366635 ),
    vec2( -0.9846501, 0.1148771 ),
    vec2( 0.4942736, 0.3433943 ),
    vec2( 0.9006424, -0.02421155 ),
    vec2( 0.7949737, 0.4719028 ),
    vec2( 0.6461979, -0.3156413 ),
    vec2( 0.3756832, 0.7479037 ),
    vec2( 0.4115427, -0.8772281 ),
    vec2( 0.05674819, 0.9628649 ),
    vec2( 0.6714596, -0.6865104 ),
    vec2( 0.3584906, -0.4865665 )
};


float PenumbraSize(float zReceiver, float zBlocker) //Parallel plane estimation
{
    return (zReceiver - zBlocker) / zBlocker;
}

void FindBlocker(out float avgBlockerDepth, out float numBlockers, vec2 uv, float zReceiver)
{
    // This uses similar triangles to compute what
    // area of the shadow map we should search
    float searchWidth = LIGHT_SIZE_UV * (zReceiver - NEAR_PLANE) / zReceiver;
    searchWidth *= 1.0;
    float blockerSum = 0;
    numBlockers = 0;

    for (int i = 0; i < BLOCKER_SEARCH_NUM_SAMPLES; ++i)
    {
        float shadowMapDepth = texture2D(u_shadowTexture, uv + poissonDiskBlocker[i] * searchWidth).r - PCSS_BIAS;

 		if (shadowMapDepth < zReceiver) {
 			blockerSum += shadowMapDepth;
 			numBlockers++;
 		}
 	}
 	avgBlockerDepth = blockerSum / numBlockers;
}

#define XOR_SCALE 40000
float PCF_Filter(vec2 uv, float zReceiver, float filterRadiusUV)
{
	float sum = 0.0;
	for ( int i = 0; i < PCF_NUM_SAMPLES; ++i )
	{
 		vec2 offset = poissonDiskPCF[i] * filterRadiusUV;

 		// rotate poisson disk sample "randomly" (but actually based on uv coord hash, so it's temporally stable)
 		/*
        float angle = 0.6 * ((int(XOR_SCALE * uv.x) ^ int(XOR_SCALE * uv.y)) + 500.0 * uv.x * uv.y);
        float ca = cos(angle);
        float sa = sin(angle);
        offset = vec2(ca*offset.x - sa*offset.y, sa*offset.x + ca*offset.y);
        */

 		sum += step(zReceiver, texture2D(u_shadowTexture, uv + offset).r);
 	}
 	return sum / PCF_NUM_SAMPLES;
}

float PCSS(sampler2D shadowMapTex, vec2 uv, float eye_z)
{
 	float zReceiver = eye_z;

 	// STEP 1: blocker search
 	float avgBlockerDepth = 0;
 	float numBlockers = 0;
 	FindBlocker(avgBlockerDepth, numBlockers, uv, zReceiver);

    // early-out if there are no occluders (saves filtering)
 	if (numBlockers < 1) {
 		return 1.0;
 	}

 	// STEP 2: penumbra size
 	float penumbraRatio = PenumbraSize(zReceiver, avgBlockerDepth);
 	float filterRadiusUV = penumbraRatio * LIGHT_SIZE_UV * NEAR_PLANE / eye_z;

 	// STEP 3: filtering
 	return PCF_Filter(uv, zReceiver, filterRadiusUV);
}

float getShadow()
{
	return PCSS(u_shadowTexture, v_shadowMapUv.xy, v_shadowMapUv.z);
}

#endif //shadowMapFlag

#if defined(diffuseTextureFlag) && defined(diffuseColorFlag)
#define fetchColorDiffuseTD(texCoord, defaultValue) texture2D(u_diffuseTexture, texCoord) * u_diffuseColor
#elif defined(diffuseTextureFlag)
#define fetchColorDiffuseTD(texCoord, defaultValue) texture2D(u_diffuseTexture, texCoord)
#elif defined(diffuseColorFlag)
#define fetchColorDiffuseTD(texCoord, defaultValue) u_diffuseColor
#else
#define fetchColorDiffuseTD(texCoord, defaultValue) (defaultValue)
#endif


#define fetchColorDiffuseD(defaultValue) fetchColorDiffuseTD(g_texCoord0, defaultValue)
#define fetchColorDiffuse() fetchColorDiffuseD(vec4(1.0))

#if defined(diffuseTextureFlag) || defined(diffuseColorFlag)
#define applyColorDiffuse(baseColor) ((baseColor) * fetchColorDiffuse())
#else
#define applyColorDiffuse(baseColor) (baseColor)
#endif


#if defined(specularTextureFlag) && defined(specularColorFlag)
#define fetchColorSpecularTD(texCoord, defaultValue) (texture2D(u_specularTexture, texCoord).rgb * u_specularColor.rgb)
#elif defined(specularTextureFlag)
#define fetchColorSpecularTD(texCoord, defaultValue) texture2D(u_specularTexture, texCoord).rgb
#elif defined(specularColorFlag)
#define fetchColorSpecularTD(texCoord, defaultValue) u_specularColor.rgb
#else
#define fetchColorSpecularTD(texCoord, defaultValue) (defaultValue)
#endif


#define fetchColorSpecularD(defaultValue) fetchColorSpecularTD(g_texCoord0, defaultValue)
#define fetchColorSpecular() fetchColorSpecularD(vec3(0.0))

#if defined(specularTextureFlag) || defined(specularColorFlag)
#define applyColorSpecular(intensity) ((intensity) * fetchColorSpecular())
#define addColorSpecular(baseColor, intensity)	((baseColor) + applyColorSpecular(intensity))
#else
#define applyColorSpecular(intensity) (vec3(0.0))
#define addColorSpecular(baseColor, intensity)	(baseColor)
#endif

varying vec3 v_lightDir;
varying vec3 v_lightCol;
varying vec3 v_viewDir;
#ifdef environmentCubemapFlag
varying vec3 v_reflect;
#endif

#ifdef environmentCubemapFlag
uniform samplerCube u_environmentCubemap;
#endif

#ifdef reflectionColorFlag
uniform vec4 u_reflectionColor;
#endif

varying vec3 v_ambientLight;

#define saturate(x) clamp( x, 0.0, 1.0 )

#ifdef pointLightsFlag
struct PointLight
{
vec3 color;
vec3 position;
float intensity;
};
uniform PointLight u_pointLights[numPointLights];
varying vec3 v_pointDir[numPointLights];
#endif

void main() {
	pullColor();
	pullTexCoord0();

	vec4 diffuse = applyColorDiffuse(g_color);
	vec3 specular = fetchColorSpecular();

	#ifdef normalTextureFlag
	vec4 N = vec4(normalize(texture2D(u_normalTexture, g_texCoord0).xyz * 2.0 - 1.0), 1.0);
	#ifdef environmentCubemapFlag
	vec3 reflectDir = normalize(v_reflect + (vec3(0.0, 0.0, 1.0) - N.xyz));
	#endif
	#else
	vec4 N = vec4(0.0, 0.0, 1.0, 1.0);
	#ifdef environmentCubemapFlag
	vec3 reflectDir = normalize(v_reflect);
	#endif
	#endif

	vec3 L = normalize(v_lightDir);
	vec3 V = normalize(v_viewDir);
	vec3 H = normalize(L + V);
	float NL = dot(N.xyz, L);
	float NH = max(0.0, dot(N.xyz, H));

	float specOpacity = 1.0; //(1.0 - diffuse.w);
	float spec = min(1.0, pow(NH, 10.0) * specOpacity);
	float selfShadow = saturate(4.0 * NL);

	#ifdef environmentCubemapFlag
	vec3 environment = textureCube(u_environmentCubemap, reflectDir).rgb;
	specular *= environment;
	#ifdef reflectionColorFlag
	diffuse.rgb = saturate(vec3(1.0) - u_reflectionColor.rgb) * diffuse.rgb + environment * u_reflectionColor.rgb;
	#endif
	#endif
	vec3 directionalContribution = (v_lightCol * diffuse.rgb) * NL;

	vec3 pointContribution = vec3(0.0, 0.0, 0.0);

	#ifdef shadowMapFlag
		gl_FragColor = vec4(saturate((directionalContribution + pointContribution) * getShadow()), diffuse.w);
	#else
		gl_FragColor = vec4(saturate(directionalContribution + pointContribution), diffuse.w);
	#endif
	gl_FragColor.rgb += v_ambientLight * diffuse.rgb;
	gl_FragColor.rgb += (selfShadow * spec) * specular;
}