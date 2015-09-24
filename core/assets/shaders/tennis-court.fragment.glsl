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
    const vec4 bitShifts = vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 160581375.0);
    return step(v_shadowMapUv.z, dot(texture2D(u_shadowTexture, v_shadowMapUv.xy + offset), bitShifts));//+(1.0/255.0));	
}

float getShadow() 
{
	return (//getShadowness(vec2(0,0)) + 
			getShadowness(vec2(u_shadowPCFOffset, u_shadowPCFOffset)) +
			getShadowness(vec2(-u_shadowPCFOffset, u_shadowPCFOffset)) +
			getShadowness(vec2(u_shadowPCFOffset, -u_shadowPCFOffset)) +
			getShadowness(vec2(-u_shadowPCFOffset, -u_shadowPCFOffset))) * 0.25;
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

}
