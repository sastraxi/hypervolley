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

#if defined(specularTextureFlag) || defined(specularColorFlag)
#define specularFlag
#endif

#ifdef normalFlag
varying vec3 v_normal;
#endif //normalFlag

#if defined(colorFlag)
varying vec4 v_color;
#endif

#ifdef blendedFlag
varying float v_opacity;
#ifdef alphaTestFlag
varying float v_alphaTest;
#endif //alphaTestFlag
#endif //blendedFlag

#if defined(diffuseTextureFlag) || defined(specularTextureFlag)
#define textureFlag
#endif

#ifdef diffuseTextureFlag
varying MED vec2 v_diffuseUV;
#endif

#ifdef specularTextureFlag
varying MED vec2 v_specularUV;
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

#ifdef lightingFlag
varying vec3 v_lightDiffuse;

#if	defined(ambientLightFlag) || defined(ambientCubemapFlag) || defined(sphericalHarmonicsFlag)
#define ambientFlag
#endif //ambientFlag

#ifdef specularFlag
varying vec3 v_lightSpecular;
#endif //specularFlag

#ifdef shadowMapFlag
uniform sampler2D u_shadowTexture;
uniform float u_shadowPCFOffset;
varying vec3 v_shadowMapUv;
#define separateAmbientFlag

float getShadowness(vec2 offset)
{
    return step(v_shadowMapUv.z, texture2D(u_shadowTexture, v_shadowMapUv.xy + offset));
}

float getShadowOld()
{
	return (//getShadowness(vec2(0,0)) +
			getShadowness(vec2(u_shadowPCFOffset, u_shadowPCFOffset)) +
			getShadowness(vec2(-u_shadowPCFOffset, u_shadowPCFOffset)) +
			getShadowness(vec2(u_shadowPCFOffset, -u_shadowPCFOffset)) +
			getShadowness(vec2(-u_shadowPCFOffset, -u_shadowPCFOffset))) * 0.25;
}

/* ************************************************************************** */
/* http://developer.download.nvidia.com/whitepapers/2008/PCSS_Integration.pdf */
/* ************************************************************************** */

#define NEAR_PLANE 1
#define LIGHT_WORLD_SIZE 0.005
#define LIGHT_FRUSTUM_WIDTH 1

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
    float blockerSum = 0;
    numBlockers = 0;

    for (int i = 0; i < BLOCKER_SEARCH_NUM_SAMPLES; ++i)
    {
        float shadowMapDepth = texture2D(u_shadowTexture, uv + poissonDiskBlocker[i] * searchWidth);

 		if (shadowMapDepth < zReceiver) {
 			blockerSum += shadowMapDepth;
 			numBlockers++;
 		}
 	}
 	avgBlockerDepth = blockerSum / numBlockers;
}

float PCF_Filter(vec2 uv, float zReceiver, float filterRadiusUV)
{
	float sum = 0.0;
	for ( int i = 0; i < PCF_NUM_SAMPLES; ++i )
	{
 		vec2 offset = poissonDiskPCF[i] * filterRadiusUV;
 		// TODO rotate poisson disk "randomly" (but actually based on uv coord hash)
		// TODO incorporate reasonable biasing
 		sum += step(zReceiver, texture2D(u_shadowTexture, uv + offset));
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
 	float filterRadiusUV = penumbraRatio * LIGHT_SIZE_UV; // * NEAR_PLANE / eye_z;

 	// STEP 3: filtering
 	return PCF_Filter(uv, zReceiver, filterRadiusUV);
}

float getShadow()
{
	// return getShadowOld();
	return PCSS(u_shadowTexture, v_shadowMapUv.xy, v_shadowMapUv.z);
	// return PCF_Filter(v_shadowMapUv.xy, v_shadowMapUv.z, 0.001);
}

#endif //shadowMapFlag

#if defined(ambientFlag) && defined(separateAmbientFlag)
varying vec3 v_ambientLight;
#endif //separateAmbientFlag

#endif //lightingFlag

#ifdef fogFlag
uniform vec4 u_fogColor;
varying float v_fog;
#endif // fogFlag

/**
 * Create the tennis court figure.
 */
#define COURT_HALF_WIDTH 0.35
#define COURT_HALF_HEIGHT 0.3
#define CENTRE vec2(0.5, 0.5)
#define CLAMP_START 51.4
#define BACKGROUND_COLOUR 	vec4(0.8, 0.8, 0.8, 1.0)
#define PATTERN_COLOUR    	vec4(0.88, 0.88, 0.88, 1.0)
#define PERIOD 0.029

vec4 getCourt(float aspect, vec2 uv)
{
    vec2 courtUV = (uv - CENTRE) / vec2(COURT_HALF_WIDTH, COURT_HALF_HEIGHT);
    courtUV = 0.5 * (courtUV + vec2(1,1));

    vec2 Exy = fwidth(courtUV);

    if (courtUV.x > 0.0 && courtUV.x < 1.0 && courtUV.y > 0.0 && courtUV.y < 1.0)
    {
        // two circles: one at the bottom-right (aspect,0) and one at the top-left (0,1)
       	courtUV.x *= aspect;

        float E = 0.5 * (Exy.x + Exy.y);

        float c1x = aspect - courtUV.x;
        float c1y = courtUV.y;
        float c1r = sqrt(c1x * c1x + c1y * c1y);
        bool c1 = mod(c1r, PERIOD) < (0.5 * PERIOD);
        bool c1next = mod(c1r + E, PERIOD) < (0.5 * PERIOD);
        float c1s = clamp(CLAMP_START - c1r, 0.0, 1.0);
        if (c1 != c1next) {
            float edge = floor((c1r + E) / (0.5*PERIOD)) * 0.5*PERIOD;
            if (c1) {
                c1s *= ((edge - c1r) / E);
            } else {
                c1s *= ((c1r + E - edge) / E);
            }
        }

        float c2x = courtUV.x;
        float c2y = 1.0 - courtUV.y;
        float c2r = sqrt(c2x * c2x + c2y * c2y);
        bool c2 = mod(c2r, PERIOD) < (0.5 * PERIOD);
        bool c2next = mod(c2r + E, PERIOD) < (0.5 * PERIOD);
        float c2s = clamp(CLAMP_START - c2r, 0.0, 1.0);
        if (c2 != c2next) {
            float edge = floor((c2r + E) / (0.5*PERIOD)) * 0.5*PERIOD;
            if (c2) {
                c2s *= ((edge - c2r) / E);
            } else {
                c2s *= ((c2r + E - edge) / E);
            }
        }

        // base colour
        if ((c1 || c1next) && c1r < c2r) {
            return mix(BACKGROUND_COLOUR, PATTERN_COLOUR, c1s);
        } else if ((c2 || c2next) && c2r < c1r) {
            return mix(BACKGROUND_COLOUR, PATTERN_COLOUR, c2s);
        } else {
            return BACKGROUND_COLOUR;
        }

    } else {
    	return BACKGROUND_COLOUR;
    }
}

void main() {
	#if defined(normalFlag)
		vec3 normal = v_normal;
	#endif // normalFlag

	// draw the tennis court
	vec4 diffuse = getCourt(TENNIS_COURT_ASPECT, v_diffuseUV);

	#if (!defined(lightingFlag))
		gl_FragColor.rgb = diffuse.rgb;
	#elif (!defined(specularFlag))
		#if defined(ambientFlag) && defined(separateAmbientFlag)
			#ifdef shadowMapFlag
				gl_FragColor.rgb = (diffuse.rgb * (v_ambientLight + getShadow() * v_lightDiffuse));
				//gl_FragColor.rgb = texture2D(u_shadowTexture, v_shadowMapUv.xy);
			#else
				gl_FragColor.rgb = (diffuse.rgb * (v_ambientLight + v_lightDiffuse));
			#endif //shadowMapFlag
		#else
			#ifdef shadowMapFlag
				gl_FragColor.rgb = getShadow() * (diffuse.rgb * v_lightDiffuse);
			#else
				gl_FragColor.rgb = (diffuse.rgb * v_lightDiffuse);
			#endif //shadowMapFlag
		#endif
	#else
		#if defined(specularTextureFlag) && defined(specularColorFlag)
			vec3 specular = texture2D(u_specularTexture, v_specularUV).rgb * u_specularColor.rgb * v_lightSpecular;
		#elif defined(specularTextureFlag)
			vec3 specular = texture2D(u_specularTexture, v_specularUV).rgb * v_lightSpecular;
		#elif defined(specularColorFlag)
			vec3 specular = u_specularColor.rgb * v_lightSpecular;
		#else
			vec3 specular = v_lightSpecular;
		#endif

		#if defined(ambientFlag) && defined(separateAmbientFlag)
			#ifdef shadowMapFlag
				gl_FragColor.rgb = (diffuse.rgb * (getShadow() * v_lightDiffuse + v_ambientLight)) + specular;
				//gl_FragColor.rgb = texture2D(u_shadowTexture, v_shadowMapUv.xy);
			#else
				gl_FragColor.rgb = (diffuse.rgb * (v_lightDiffuse + v_ambientLight)) + specular;
			#endif //shadowMapFlag
		#else
			#ifdef shadowMapFlag
				gl_FragColor.rgb = getShadow() * ((diffuse.rgb * v_lightDiffuse) + specular);
			#else
				gl_FragColor.rgb = (diffuse.rgb * v_lightDiffuse) + specular;
			#endif //shadowMapFlag
		#endif
	#endif //lightingFlag

	#ifdef fogFlag
		gl_FragColor.rgb = mix(gl_FragColor.rgb, u_fogColor.rgb, v_fog);
	#endif // end fogFlag

	#ifdef blendedFlag
		gl_FragColor.a = diffuse.a * v_opacity;
		#ifdef alphaTestFlag
			if (gl_FragColor.a <= v_alphaTest)
				discard;
		#endif
	#else
		gl_FragColor.a = 1.0;
	#endif

	// gl_FragColor.rg = v_shadowMapUv.xy;
	// gl_FragColor.rgb = texture2D(u_shadowTexture, v_shadowMapUv.xy).rrr;

}
