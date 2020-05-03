#version 330

in vec2 outTexCoord;
in vec3 mvVertexNormal;
in vec3 mvVertexPos;
in vec3 meshVertexNormal;

uniform sampler2D explosionTexture;

uniform vec3 lightDirection;
uniform vec3 lightColor;
uniform vec2 lightBias;
uniform float particleAlpha;
uniform vec3 particleTint;

out vec4 pixelColor;

void main()
{
    vec4  textureColor = texture(explosionTexture, outTexCoord);
    vec4  blendedColor = vec4(textureColor.xyz * particleTint, textureColor.w * particleAlpha);
    float brightness   = max(dot(-lightDirection, meshVertexNormal), 0.0);
    vec3  lighting     = (lightColor * lightBias.x) + (brightness * lightColor * lightBias.y);
          pixelColor   = blendedColor;
}
