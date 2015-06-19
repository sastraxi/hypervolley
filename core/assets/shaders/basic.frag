#ifdef GL_ES
	precision mediump float;
#endif

varying vec4 colour;
varying float height;

void main() {
	gl_FragColor = colour * (1.0f - (1.0f - height) * 0.3f);
}
