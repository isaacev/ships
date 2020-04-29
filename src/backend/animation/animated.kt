package backend.animation

import backend.engine.Duration

interface Animated {
    fun nextFrame(delta: Duration)
}
