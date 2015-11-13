attribute vec3 a_position;
attribute vec2 a_texCoord0;

uniform mat4 u_worldTrans;
uniform mat4 u_projViewTrans;
uniform vec2 u_resolution;

uniform sampler2D u_texture;

varying vec2 v_UV;

void main() {
	gl_Position = vec4(a_position, 1.0);
	v_UV = a_texCoord0;
}
