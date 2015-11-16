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
uniform float u_anim;

#define MAX_VIGNETTE_INTENSITY 0.53
#define VIGNETTE_RADIUS 0.39

void main()
{
    vec2 tc = v_UV;
    tc.y += 0.08;
    if (tc.y < 0.5) tc.y = 0.5; // only do a strong vignette on the top half
    float dst = distance(tc, vec2(0.5, 0.5)) * (1 - u_anim);

	// vignette
	float intensity = clamp((dst - VIGNETTE_RADIUS) / 0.3, 0.0, 1.0);
	intensity = pow(intensity, 1.3);
	gl_FragColor = vec4(0f, 0f, 0f, intensity * MAX_VIGNETTE_INTENSITY);
}
