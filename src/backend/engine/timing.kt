package backend.engine

typealias Milliseconds = Float

class Timer {
    var lastLoopTime: Milliseconds = timeNow

    val timeNow: Milliseconds
        get() = System.nanoTime() / 1_000_000f

    val elapsedTime: Milliseconds
        get() {
            val now = timeNow
            val elapsed = now - lastLoopTime
            lastLoopTime = now
            return elapsed
        }
}

data class Duration(val total: Milliseconds, private var remaining: Milliseconds = total) {
    fun isExhausted(): Boolean {
        return remaining <= 0f
    }

    fun request(request: Milliseconds): Milliseconds {
        return if (request > remaining) {
            val claimed = remaining
            remaining = 0f
            claimed
        } else {
            remaining -= request
            request
        }
    }

    fun requestAll(): Milliseconds {
        return request(remaining)
    }

    fun fresh(): Duration {
        return Duration(total)
    }
}
