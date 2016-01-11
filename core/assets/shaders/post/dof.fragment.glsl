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

varying MED vec2 v_UV;
uniform sampler2D u_texture;
uniform sampler2D u_blurred;

#define NUM_TAPS 12
const vec2 poisson[] = {
	vec2( 0.6604091, 0.4742445),
	vec2( 0.7948614, -0.4471891),
	vec2( 0.4493439, -0.021661),
	vec2( -0.07716497, 0.264531),
	vec2( 0.2304726, 0.7877784),
	vec2( -0.4126254, 0.7543818),
	vec2( 0.1667066, -0.4643931),
	vec2( -0.3443508, -0.426068),
	vec2( -0.5895757, 0.2109648),
	vec2( -0.9567321, -0.1419154),
	vec2( -0.1509167, -0.9429354),
	vec2( 0.4075699, -0.9050737)
};

uniform vec2 u_pixel_texture;
uniform vec2 u_pixel_blurred;

const float maxCoC = 10.0;
const float radiusScale = 0.1;

// in our alpha channel, 0 is minimum blur and 1 is maximum
vec4 poissonDOF(vec2 uv)
{
	vec4 sum = vec4(0.0, 0.0, 0.0, 0.0);
	float discRadius, discRadiusLow, centerDepth;

	sum = texture2D(u_texture, uv);
	centerDepth = sum.a;
	discRadius = sum.a * maxCoC;
	discRadiusLow = discRadius * radiusScale;

	sum = vec4(0.0, 0.0, 0.0, 0.0);
	for (int t = 0; t < NUM_TAPS; ++t)
	{
		// compute tap texture coordinates
		vec2 coordLow = uv + (u_pixel_blurred * poisson[t] * discRadiusLow);
		vec2 coordHigh = uv + (u_pixel_texture * poisson[t] * discRadius);

		// fetch high-res tap
		vec4 tapLow = texture2D(u_blurred, coordLow);
		vec4 tapHigh = texture2D(u_texture, coordHigh);

		// max low- and hi-res taps based on tap blurriness
		float tapBlur = tapHigh.a;
		vec4 tap = mix(tapHigh, tapLow, tapBlur);

		// "smart" blur ignores taps that are closer than the center tap and in focus
		tap.a = (tap.a >= centerDepth) ? 1.0 : tap.a;

		// accumulate
		sum.rgb += tap.rgb * tap.a;
		sum.a += tap.a;
	}
	return (sum / sum.a);
}

void main() {
    vec4 sum = poissonDOF(v_UV);
    gl_FragColor = vec4(sum.rgb, 1.0);
}
