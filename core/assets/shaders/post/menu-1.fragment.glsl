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
  vec2 off1 = 1.411764705882353 * direction;
  vec2 off2 = 3.2941176470588234 * direction;
  vec2 off3 = 5.176470588235294 * direction;
  color += texture2D(image, uv) * 0.1964825501511404;
  color += texture2D(image, uv + (off1 / resolution)) * 0.2969069646728344;
  color += texture2D(image, uv - (off1 / resolution)) * 0.2969069646728344;
  color += texture2D(image, uv + (off2 / resolution)) * 0.09447039785044732;
  color += texture2D(image, uv - (off2 / resolution)) * 0.09447039785044732;
  color += texture2D(image, uv + (off3 / resolution)) * 0.010381362401148057;
  color += texture2D(image, uv - (off3 / resolution)) * 0.010381362401148057;
  return color;
}

vec4 blur5(sampler2D image, vec2 uv, vec2 resolution, vec2 direction) {
  vec4 color = vec4(0.0);
  vec2 off1 = vec2(1.3333333333333333) * direction;
  color += texture2D(image, uv) * 0.29411764705882354;
  color += texture2D(image, uv + (off1 / resolution)) * 0.35294117647058826;
  color += texture2D(image, uv - (off1 / resolution)) * 0.35294117647058826;
  return color;
}

varying MED vec2 v_UV;
uniform sampler2D u_texture;
uniform vec2 u_resolution;
uniform float u_anim;

void main() {
    //this will be our RGBA sum
    vec4 sum = vec4(0.0);

    //our original texcoord for this fragment
    vec2 tc = v_UV;
    float amt = 1.5 * u_anim / u_resolution.x;

    sum += texture2D(u_texture, vec2(tc.x - 13.0*amt, tc.y)) * 0.018036;
    sum += texture2D(u_texture, vec2(tc.x - 12.0*amt, tc.y)) * 0.021042;
    sum += texture2D(u_texture, vec2(tc.x - 11.0*amt, tc.y)) * 0.024249;
    sum += texture2D(u_texture, vec2(tc.x - 10.0*amt, tc.y)) * 0.027601;
    sum += texture2D(u_texture, vec2(tc.x - 9.0*amt, tc.y)) * 0.031032;
    sum += texture2D(u_texture, vec2(tc.x - 8.0*amt, tc.y)) * 0.034462;
    sum += texture2D(u_texture, vec2(tc.x - 7.0*amt, tc.y)) * 0.037801;
    sum += texture2D(u_texture, vec2(tc.x - 6.0*amt, tc.y)) * 0.040956;
    sum += texture2D(u_texture, vec2(tc.x - 5.0*amt, tc.y)) * 0.043831;
    sum += texture2D(u_texture, vec2(tc.x - 4.0*amt, tc.y)) * 0.046332;
    sum += texture2D(u_texture, vec2(tc.x - 3.0*amt, tc.y)) * 0.048376;
    sum += texture2D(u_texture, vec2(tc.x - 2.0*amt, tc.y)) * 0.049891;
    sum += texture2D(u_texture, vec2(tc.x - 1.0*amt, tc.y)) * 0.050822;

    vec4 centerTexel = texture2D(u_texture, vec2(tc.x, tc.y));
    sum += centerTexel * 0.051137;

    sum += texture2D(u_texture, vec2(tc.x + 13.0*amt, tc.y)) * 0.018036;
    sum += texture2D(u_texture, vec2(tc.x + 12.0*amt, tc.y)) * 0.021042;
    sum += texture2D(u_texture, vec2(tc.x + 11.0*amt, tc.y)) * 0.024249;
    sum += texture2D(u_texture, vec2(tc.x + 10.0*amt, tc.y)) * 0.027601;
    sum += texture2D(u_texture, vec2(tc.x + 9.0*amt, tc.y)) * 0.031032;
    sum += texture2D(u_texture, vec2(tc.x + 8.0*amt, tc.y)) * 0.034462;
    sum += texture2D(u_texture, vec2(tc.x + 7.0*amt, tc.y)) * 0.037801;
    sum += texture2D(u_texture, vec2(tc.x + 6.0*amt, tc.y)) * 0.040956;
    sum += texture2D(u_texture, vec2(tc.x + 5.0*amt, tc.y)) * 0.043831;
    sum += texture2D(u_texture, vec2(tc.x + 4.0*amt, tc.y)) * 0.046332;
    sum += texture2D(u_texture, vec2(tc.x + 3.0*amt, tc.y)) * 0.048376;
    sum += texture2D(u_texture, vec2(tc.x + 2.0*amt, tc.y)) * 0.049891;
    sum += texture2D(u_texture, vec2(tc.x + 1.0*amt, tc.y)) * 0.050822;

    gl_FragColor = vec4(sum.rgb, 1.0);
}
