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

#if defined(ambientFlag) && defined(separateAmbientFlag)
varying vec3 v_ambientLight;
#endif //separateAmbientFlag

#endif //lightingFlag

#ifdef fogFlag
uniform vec4 u_fogColor;
varying float v_fog;
#endif // fogFlag

#define PERIOD 1.570796
#define GAP 0.2

#define RADIUS_SMALL_MIN 0.2
#define RADIUS_SMALL_MAX 0.35
#define RADIUS_LARGE_MIN 0.4
#define RADIUS_LARGE_MAX 0.5

void main() {
	#if defined(normalFlag) 
		vec3 normal = v_normal;
	#endif // normalFlag

	vec4 diffuse = u_diffuseColor;

	////////////////////////////////////

	float radius = distance(v_diffuseUV, vec2(0.5, 0.5));
	float theta = atan(v_diffuseUV.y - 0.5, v_diffuseUV.x - 0.5);

	////////////////////////////////////

	float dt = fwidth(theta);
	theta = mod(theta, PERIOD);
	float theta2 = mod(theta+dt, PERIOD);

	bool in_theta = theta > GAP;
	bool in_theta2 = theta2 > GAP;

	float theta_contrib = 1.0;
	if (!in_theta && in_theta2) {
		// going into opaque
		theta_contrib = (theta2 - GAP) / dt;

	} else if (in_theta && !in_theta2) {
		// going out of opaque
		theta_contrib = (PERIOD - theta) / dt;
	}

	////////////////////////////////////

	float dr = fwidth(radius);
	float radius2 = radius + dr;

	bool large_r1 = (RADIUS_LARGE_MIN < radius && radius < RADIUS_LARGE_MAX);
	bool large_r2 = (RADIUS_LARGE_MIN < radius2 && radius2 < RADIUS_LARGE_MAX);

	bool small_r1 = (RADIUS_SMALL_MIN < radius && radius < RADIUS_SMALL_MAX);
	bool small_r2 = (RADIUS_SMALL_MIN < radius2 && radius2 < RADIUS_SMALL_MAX);

	float radius_contrib = 1.0;
	if (!large_r1 && large_r2) {
		// going into opaque
		radius_contrib = (radius2 - RADIUS_LARGE_MIN) / dr;
	} else if (large_r1 && !large_r2) {
		// going out of opaque
		radius_contrib = (RADIUS_LARGE_MAX - radius) / dr;
	} else if (!small_r1 && small_r2) {
		// going into opaque
		radius_contrib = (radius2 - RADIUS_SMALL_MIN) / dr;
	} else if (small_r1 && !small_r2) {
		// going out of opaque
		radius_contrib = (RADIUS_SMALL_MAX - radius) / dr;
	}

	////////////////////////////////////

	if (in_theta || in_theta2) {
		if (large_r1 || large_r2) {
			gl_FragColor = vec4(diffuse.rgb, theta_contrib * radius_contrib * v_opacity);
		} else if (small_r1 || small_r2) {
			gl_FragColor = vec4(diffuse.rgb, theta_contrib * radius_contrib * v_opacity * 0.6);
		} else {
			discard;
		}
	} else {
		discard;
	}
}
