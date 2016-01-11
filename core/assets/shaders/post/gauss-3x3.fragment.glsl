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
uniform vec2 u_inv_resolution;

const float coeff[] = {
	0.0625,	0.125,	0.0625,
	0.125,	0.25,	0.125,
	0.0625,	0.125,	0.0625
};

vec4 gauss33(sampler2D image, vec2 uv, vec2 inv_resolution) {
	vec4 result = vec4(0.0, 0.0, 0.0, 0.0);
	vec2 texCoord;

	for(int i=0;i<3;i++) {
		for(int j=0;j<3;j++) {
			texCoord = uv + vec2((i-1)*inv_resolution.x,(j-1)*inv_resolution.y);
			result += coeff[i*3 + j] * texture2D(image, texCoord);
		}
	}

	return result;
}

void main() {
    //this will be our RGBA sum
    gl_FragColor = gauss33(u_texture, v_UV, u_inv_resolution);
}
