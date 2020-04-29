#version 330

in vec2 outTexCoord;
in vec3 mvVertexNormal;
in vec3 mvVertexPos;
in vec3 meshVertexNormal;

uniform sampler2D tileTexture;
uniform vec3 lightDirection;
uniform vec3 lightColor;
uniform vec2 lightBias;

out vec4 pixelColor;

void main()
{
    vec4 realGroundColor = texture(tileTexture, outTexCoord);
    vec4 finalColor      = realGroundColor;
    float brightness     = max(dot(-lightDirection, meshVertexNormal), 0.0);
    vec3  lighting       = (lightColor * lightBias.x) + (brightness * lightColor * lightBias.y);
          pixelColor     = finalColor * vec4(lighting, 1.0);
}
