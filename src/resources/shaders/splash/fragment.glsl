#version 330

in vec2 outTexCoord;
in vec3 mvVertexNormal;
in vec3 mvVertexPos;
in vec3 meshVertexNormal;

uniform vec3 color;

out vec4 pixelColor;

void main()
{
    pixelColor = vec4(color, 1.0);
}
