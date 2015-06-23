#ifdef GL_ES
	precision mediump float;
#endif

uniform vec2 u_centre;
uniform float u_radius;

varying vec2 position;

void main() {
	if (distance(position, u_centre) < u_radius) {
		gl_FragColor = vec4(0.4, 0.6, 1.0, 1.0);
	} else {
		discard;
	}
}
