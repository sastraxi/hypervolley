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

/*
vec3 rgb2hsv(vec3 c)
{
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}
*/

varying MED vec2 v_UV;
// uniform sampler2D u_tex_a, u_tex_b;
// uniform vec2 u_resolution;
uniform float u_anim;

#define TINT vec3(1.0, 0.95, 0.88)
#define TINT_BLEND 0.7
#define VIGNETTE_COLOUR vec4(0.2, 0.17, 0.0, 1.0)
#define VIGNETTE_RADIUS 0.3

#define SPEED 0.03
#define WIDTH 0.3

void main()
{
	// determine our function
	float f_x = v_UV.x + SPEED * u_anim;
	f_x = mod(f_x, WIDTH) / WIDTH;
	if (f_x > 0.5) {
		f_x = 1 - f_x;
	}
	f_x *= 2.0;
	f_x = smoothstep(0.0, 1.0, f_x);

	gl_FragColor = vec4(f_x, f_x, f_x, 1.0);
}
