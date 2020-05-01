#version 330

in vec2 outTexCoord;
in vec3 mvVertexNormal;
in vec3 mvVertexPos;
in vec3 meshVertexNormal;

uniform sampler2D tileTexture;
uniform sampler2D overlayTexture;

uniform int overlayChoice;
uniform float overlayRotation;
uniform vec3 overlayTint;

uniform vec3 lightDirection;
uniform vec3 lightColor;
uniform vec2 lightBias;

out vec4 pixelColor;

const int CIRCLE_OVERLAY = 0;
const int TERMINUS_OVERLAY = 1;
const int STRAIGHT_OVERLAY = 2;
const int OBTUSE_OVERLAY = 3;

vec2 rotateUV(vec2 uv, float radians, vec2 center)
{
    return vec2(
        cos(radians) * (uv.x - center.x) + sin(radians) * (uv.y - center.y) + center.x,
        cos(radians) * (uv.y - center.y) - sin(radians) * (uv.x - center.x) + center.y
    );
}

vec2 overlayTexCoord(int whichOverlay, vec2 tileTexCoord)
{
    vec2 scaledTexCoord = tileTexCoord / 2.0;
    if (whichOverlay == TERMINUS_OVERLAY) {
        scaledTexCoord.x += 0.5;
    } else if (whichOverlay == STRAIGHT_OVERLAY) {
        scaledTexCoord.y += 0.5;
    } else if (whichOverlay == OBTUSE_OVERLAY) {
        scaledTexCoord.x += 0.5;
        scaledTexCoord.y += 0.5;
    }
    return scaledTexCoord;
}

vec2 overlayCenter(int whichOverlay)
{
         if (whichOverlay == TERMINUS_OVERLAY) return vec2(3.0 / 4.0, 1.0 / 4.0);
    else if (whichOverlay == STRAIGHT_OVERLAY) return vec2(1.0 / 4.0, 3.0 / 4.0);
    else if (whichOverlay == OBTUSE_OVERLAY)   return vec2(3.0 / 4.0, 3.0 / 4.0);
    else                                       return vec2(1.0 / 4.0, 1.0 / 4.0);
}

void main()
{
    vec4 realGroundColor = texture(tileTexture, outTexCoord);
    vec4 finalColor      = realGroundColor;

    if (overlayChoice >= 0) {
        vec2  overlayCoord       = overlayTexCoord(overlayChoice, outTexCoord);
        vec2  overlayCoordRot    = rotateUV(overlayCoord, overlayRotation, overlayCenter(overlayChoice));
        vec4  overlayColor       = texture(overlayTexture, overlayCoordRot);

        float overlayWeight      = overlayColor.x;
        vec4  scaledGroundColor  = realGroundColor * (1.0 - overlayWeight);
        vec4  scaledOverlayColor = vec4(overlayTint, 1.0) * overlayWeight;
        finalColor         = scaledGroundColor + scaledOverlayColor;
    }

    float brightness     = max(dot(-lightDirection, meshVertexNormal), 0.0);
    vec3  lighting       = (lightColor * lightBias.x) + (brightness * lightColor * lightBias.y);
    pixelColor     = finalColor * vec4(lighting, 1.0);
}
