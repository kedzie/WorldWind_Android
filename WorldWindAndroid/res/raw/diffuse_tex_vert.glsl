/*
 * Input vertex attribute defining the surface vertex point in model coordinates. This attribute is specified in
 * TODO.
 */
attribute vec4 vertexPoint;
attribute vec4 aTextureCoord;

/*
 * Input uniform matrix defining the current modelview-projection transform matrix. Maps model coordinates to eye
 * coordinates.
 */
uniform mat4 mvpMatrix;
uniform mat4 texMatrix;
varying vec2 vTextureCoord;

/*
 * OpenGL ES vertex shader entry point. Called for each vertex processed when this shader's program is bound.
 */
void main()
{
    /* Transform the surface vertex point from model coordinates to eye coordinates. */
    gl_Position = mvpMatrix * vertexPoint;
    vTextureCoord = (texMatrix * aTextureCoord).st;
}
