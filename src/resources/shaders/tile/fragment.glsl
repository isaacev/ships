#version 330

in vec2 outTexCoord;
in vec3 mvVertexNormal;
in vec3 mvVertexPos;
in vec3 meshVertexNormal;

uniform sampler2D tileTexture;
uniform sampler2D overlayTexture;

uniform int isHovered;

uniform vec3 lightDirection;
uniform vec3 lightColor;
uniform vec2 lightBias;

out vec4 pixelColor;

const vec4 TINT_HOVER = vec4(1.0, 1.0, 1.0, 1.0);// white

void main()
{
    vec4 realGroundColor = texture(tileTexture, outTexCoord);
    vec4 finalColor      = realGroundColor;

    if (isHovered == 1) {
        vec4  overlayTint        = TINT_HOVER;
        vec4  overlayColor       = texture(overlayTexture, outTexCoord);
        float overlayWeight      = overlayColor.x;
        vec4  scaledGroundColor  = realGroundColor * (1.0 - overlayWeight);
        vec4  scaledOverlayColor = overlayTint * overlayWeight;
        finalColor         = scaledGroundColor + scaledOverlayColor;
    }

    float brightness     = max(dot(-lightDirection, meshVertexNormal), 0.0);
    vec3  lighting       = (lightColor * lightBias.x) + (brightness * lightColor * lightBias.y);
    pixelColor     = finalColor * vec4(lighting, 1.0);
}
