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

const int CIRCLE_OVERLAY   = 0;
const int TERMINUS_OVERLAY = 1;
const int STRAIGHT_OVERLAY = 2;
const int OBTUSE_OVERLAY   = 3;
const int START_OVERLAY    = 4;

const vec2 CIRCLE   = vec2(0.0, 0.0);
const vec2 TERMINUS = vec2(1.0, 0.0);
const vec2 STRAIGHT = vec2(0.0, 1.0);
const vec2 OBTUSE   = vec2(1.0, 1.0);
const vec2 START    = vec2(2.0, 0.0);

vec2 rotateUV(vec2 uv, float radians, vec2 center)
{
    return vec2(
        cos(radians) * (uv.x - center.x) + sin(radians) * (uv.y - center.y) + center.x,
        cos(radians) * (uv.y - center.y) - sin(radians) * (uv.x - center.x) + center.y
    );
}

vec2 overlayTexCoord(int whichOverlay, vec2 tileTexCoord)
{
    vec2 scaledTexCoord = tileTexCoord / 4.0;
    return scaledTexCoord;
}

vec2 overlayOffset(int whichOverlay)
{
         if (whichOverlay == TERMINUS_OVERLAY) return TERMINUS;
    else if (whichOverlay == STRAIGHT_OVERLAY) return STRAIGHT;
    else if (whichOverlay == OBTUSE_OVERLAY)   return OBTUSE;
    else if (whichOverlay == START_OVERLAY)    return START;
    else                                       return CIRCLE;
}

vec2 overlayCenter(int whichOverlay)
{
    return overlayOffset(whichOverlay) / 4.0 + vec2(0.125, 0.125);
}

void main()
{
    vec4 realGroundColor = texture(tileTexture, outTexCoord);
    vec4 finalColor      = realGroundColor;

    if (overlayChoice >= 0) {
        vec2  overlayCoord       = (outTexCoord / 4.0) + (overlayOffset(overlayChoice) / 4.0);
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
