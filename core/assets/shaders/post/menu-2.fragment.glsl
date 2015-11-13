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

vec4 blur13(sampler2D image, vec2 uv, vec2 resolution, vec2 direction)
{
    vec4 color = vec4(0.0);
    vec2 off1 = vec2(1.411764705882353) * direction;
    vec2 off2 = vec2(3.2941176470588234) * direction;
    vec2 off3 = vec2(5.176470588235294) * direction;
    color += texture2D(image, uv) * 0.1964825501511404;
    color += texture2D(image, uv + (off1 / resolution)) * 0.2969069646728344;
    color += texture2D(image, uv - (off1 / resolution)) * 0.2969069646728344;
    color += texture2D(image, uv + (off2 / resolution)) * 0.09447039785044732;
    color += texture2D(image, uv - (off2 / resolution)) * 0.09447039785044732;
    color += texture2D(image, uv + (off3 / resolution)) * 0.010381362401148057;
    color += texture2D(image, uv - (off3 / resolution)) * 0.010381362401148057;
    return color;
}

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

varying MED vec2 v_UV;
uniform sampler2D u_texture;
uniform vec2 u_resolution;
uniform float u_anim;

#define TINT vec3(1.0, 0.95, 0.88)
#define TINT_BLEND 0.7
#define VIGNETTE_COLOUR vec4(0.2, 0.17, 0.0, 1.0)
#define VIGNETTE_RADIUS 0.3

void main()
{
    // finish the blur
    vec4 sum = vec4(0.0);

    //our original texcoord for this fragment
    vec2 tc = v_UV;
    float amt = 1.5 * u_anim / u_resolution.y;

    sum += texture2D(u_texture, vec2(tc.x, tc.y - 13.0*amt)) * 0.018036;
    sum += texture2D(u_texture, vec2(tc.x, tc.y - 12.0*amt)) * 0.021042;
    sum += texture2D(u_texture, vec2(tc.x, tc.y - 11.0*amt)) * 0.024249;
    sum += texture2D(u_texture, vec2(tc.x, tc.y - 10.0*amt)) * 0.027601;
    sum += texture2D(u_texture, vec2(tc.x, tc.y - 9.0*amt)) * 0.031032;
    sum += texture2D(u_texture, vec2(tc.x, tc.y - 8.0*amt)) * 0.034462;
    sum += texture2D(u_texture, vec2(tc.x, tc.y - 7.0*amt)) * 0.037801;
    sum += texture2D(u_texture, vec2(tc.x, tc.y - 6.0*amt)) * 0.040956;
    sum += texture2D(u_texture, vec2(tc.x, tc.y - 5.0*amt)) * 0.043831;
    sum += texture2D(u_texture, vec2(tc.x, tc.y - 4.0*amt)) * 0.046332;
    sum += texture2D(u_texture, vec2(tc.x, tc.y - 3.0*amt)) * 0.048376;
    sum += texture2D(u_texture, vec2(tc.x, tc.y - 2.0*amt)) * 0.049891;
    sum += texture2D(u_texture, vec2(tc.x, tc.y - 1.0*amt)) * 0.050822;

    vec4 centerTexel = texture2D(u_texture, vec2(tc.x, tc.y));
    sum += centerTexel * 0.051137;

    sum += texture2D(u_texture, vec2(tc.x, tc.y + 13.0*amt)) * 0.018036;
    sum += texture2D(u_texture, vec2(tc.x, tc.y + 12.0*amt)) * 0.021042;
    sum += texture2D(u_texture, vec2(tc.x, tc.y + 11.0*amt)) * 0.024249;
    sum += texture2D(u_texture, vec2(tc.x, tc.y + 10.0*amt)) * 0.027601;
    sum += texture2D(u_texture, vec2(tc.x, tc.y + 9.0*amt)) * 0.031032;
    sum += texture2D(u_texture, vec2(tc.x, tc.y + 8.0*amt)) * 0.034462;
    sum += texture2D(u_texture, vec2(tc.x, tc.y + 7.0*amt)) * 0.037801;
    sum += texture2D(u_texture, vec2(tc.x, tc.y + 6.0*amt)) * 0.040956;
    sum += texture2D(u_texture, vec2(tc.x, tc.y + 5.0*amt)) * 0.043831;
    sum += texture2D(u_texture, vec2(tc.x, tc.y + 4.0*amt)) * 0.046332;
    sum += texture2D(u_texture, vec2(tc.x, tc.y + 3.0*amt)) * 0.048376;
    sum += texture2D(u_texture, vec2(tc.x, tc.y + 2.0*amt)) * 0.049891;
    sum += texture2D(u_texture, vec2(tc.x, tc.y + 1.0*amt)) * 0.050822;

    //discard alpha for our simple demo, multiply by vertex color and return
    vec3 blurred = sum.rgb;

    // de-saturate
    vec3 desaturated = vec3(mix(blurred.rgb, TINT, TINT_BLEND * u_anim * u_anim));

	// vignette
	float intensity = clamp(distance(tc, vec2(0.5, 0.5)) - VIGNETTE_RADIUS, 0.0, 1.0);
	gl_FragColor = mix(vec4(desaturated, 1.0), VIGNETTE_COLOUR, intensity * u_anim);
}
