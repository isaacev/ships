package backend

import org.joml.Vector3f

typealias Units = Float
typealias Degrees = Float

typealias Color = Vector3f

fun color(r: Int, g: Int, b: Int): Color {
    return Color(r / 255f, g / 255f, b / 255f)
}

fun color(hex: Int): Color {
    val r = (hex and 0xff0000) ushr 16
    val g = (hex and 0x00ff00) ushr 8
    val b = (hex and 0x0000ff)
    return color(r, g, b)
}
