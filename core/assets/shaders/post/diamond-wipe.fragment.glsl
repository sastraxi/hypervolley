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
uniform vec2 u_resolution;
uniform float u_anim;

#define SPEED 0.1
#define WIDTH 1.5
#define REPEAT 60.0

#define FROM_COLOUR vec3(0.95, 0.88, 0.7)
#define TO_COLOUR vec3(1.0, 0.99, 0.9)

float parameter(vec2 coord)
{
	float f_x = coord.x - 0.3 * coord.y + SPEED * u_anim;
	f_x = mod(f_x, WIDTH) / WIDTH;
	if (f_x > 0.5) {
		f_x = 1.0 - f_x;
	}
	f_x *= 2.0;
	f_x = 0.1 + 0.8 * smoothstep(0.0, 1.0, f_x);
	return f_x;
}

float diamond(vec2 uv, vec2 centre, vec2 size)
{
	float aspect = size.y / size.x;
	vec2 dist = uv - centre;
	if (aspect * abs(dist.x) + abs(dist.y) < size.y) {
		return 1.0;
	}
	return 0.0;
}

void main()
{
	// the grid, in general
	float aspect = u_resolution.x / u_resolution.y;
	float cell_size_x = 1.0 / REPEAT;
	vec2 cell_uv = vec2(cell_size_x, aspect * cell_size_x);
	vec2 half_cell_uv = 0.5 * cell_uv;

	// our specific cell
	vec2 cell_centre = floor(v_UV / cell_uv);
	cell_centre *= cell_uv;
	cell_centre += half_cell_uv;

	// determine our function at the grid square centre
	float f_x = parameter(cell_centre);

	// test to see if we're in the diamond created by that parameter value/grid cell
	float factor = diamond(v_UV, cell_centre, f_x * cell_uv);
	gl_FragColor = vec4(mix(FROM_COLOUR, TO_COLOUR, factor), 1.0);
}
