attribute vec3 a_position;
//attribute vec3 a_normal;
attribute vec4 a_color;
//attribute vec2 a_texCoord0;

uniform mat4 u_worldTrans;
uniform mat4 u_projViewTrans;

varying vec2 position;

void main() {
	gl_Position = u_projViewTrans * u_worldTrans * vec4(a_position, 1.0);
	position = a_position.xy; // un-project
}
