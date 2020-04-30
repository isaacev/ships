#version 330

in vec2 outTexCoord;
in vec3 mvVertexNormal;
in vec3 mvVertexPos;
in vec3 meshVertexNormal;

uniform sampler2D shipTexture;
uniform vec3 hullColor;
uniform vec3 sailColor;
uniform vec3 lightDirection;
uniform vec3 lightColor;
uniform vec2 lightBias;

out vec4 pixelColor;

const vec3 HULL_REGISTRATION = vec3(0x00 / 255.0, 0xAA / 255.0, 0x00 / 255.0);
const vec3 JIB_REGISTRATION  = vec3(0x00 / 255.0, 0xBB / 255.0, 0x00 / 255.0);
const vec3 FORE_REGISTRATION = vec3(0x00 / 255.0, 0xCC / 255.0, 0x00 / 255.0);
const vec3 MAIN_REGISTRATION = vec3(0x00 / 255.0, 0xDD / 255.0, 0x00 / 255.0);
const vec3 FALLBACK_COLOR    = vec3(0xFF / 255.0, 0x00 / 255.0, 0xFF / 255.0);

vec3 applyStyle(vec3 registration)
{
    if (registration == HULL_REGISTRATION) return hullColor;
    if (registration == JIB_REGISTRATION)  return sailColor;
    if (registration == FORE_REGISTRATION) return sailColor;
    if (registration == MAIN_REGISTRATION) return sailColor;

    return FALLBACK_COLOR;
}

void main()
{
    vec4  registrationColor = texture(shipTexture, outTexCoord);
    vec4  finalColor        = vec4(applyStyle(registrationColor.xyz), 1.0);
    float brightness        = max(dot(-lightDirection, meshVertexNormal), 0.0);
    vec3  lighting          = (lightColor * lightBias.x) + (brightness * lightColor * lightBias.y);
    pixelColor        = finalColor * vec4(lighting, 1.0);
}
