#ifdef GL_ES
	precision mediump float;
#endif

#define CIRCLE_MIN_DISTANCE 10f
#define CIRCLE_MAX_DISTANCE 12f

#define POINTS_NUM 4
#define POINTS_MIN_DISTANCE 7f
#define POINTS_MAX_DISTANCE 13f
#define POINTS_ARC_WIDTH 0.1f

uniform vec2 u_centre;
uniform float u_radius;

varying vec2 position;

void main() {
	vec2 p = position - u_centre;
	float r = length(p);
	float angle = atan(p.y, p.x);
	if (r < CIRCLE_MIN_DISTANCE && r > CIRCLE_MIN_DISTANCE) {
		gl_FragColor = vec4(0.4, 0.6, 1.0, 1.0);
	} else {
		discard;
	}
}
