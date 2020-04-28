package backend.engine

import backend.Managed
import backend.inputs.Mouse
import backend.window.Window
import backend.window.WindowConfigs

private enum class State {
    Booting,
    Loading,
    Running,
    Closing,
}

interface EngineConfigs {
    val goalFramesPerSecond: Int
    val goalUpdatesPerSecond: Int
    val window: WindowConfigs
}

interface GameLike: Managed {
    fun load()

    fun input(window: Window, mouse: Mouse)

    fun updateState(mouse: Mouse)

    fun updateAnimation(delta: Duration)

    fun render(window: Window)
}

class Engine(private val game: GameLike, private val configs: backend.engine.EngineConfigs): Runnable, Managed {
    private var state = State.Booting
    private val window = Window(configs.window)
    private val mouse = Mouse(window)
    private val timer = Timer()

    init {
        // Tell the game to load any assets it needs
        println("-- L O A D I N G --")
        state = State.Loading
        game.load()

        // System-level configuration
        System.setProperty("java.awt.headless", "true")
    }

    override fun run() {
        println("-- R U N N I N G --")
        state = State.Running

        try {
            var elapsedTime: Milliseconds
            var accumulator: Milliseconds = 0f
            val interval: Milliseconds = 1_000f / configs.goalUpdatesPerSecond

            while (!window.shouldClose() && state == State.Running) {
                // Update timing variables each cycle:
                elapsedTime = timer.elapsedTime
                accumulator += elapsedTime

                // Let the game record any input info it needs:
                handleInput()

                // For the time leftover between the start of the loop
                // cycle and the expiration of the current interval,
                // keep updating the game state:
                while (accumulator >= interval) {
                    updateState()
                    accumulator -= interval
                }

                // When the end current interval has been reached,
                // update the game's animation state using the number
                // of passed milliseconds...
                updateAnimation(Duration(interval))

                // ...then render the next frame:
                renderFrame()

                // If the game does NOT have vertical sync (vSync) enabled,
                // manually delay until the next interval can begin:
                if (window.doesNotHaveVerticalSync()) {
                    manualSync()
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            free()
        }
    }

    override fun free() {
        println("-- C L O S I N G --")
        state = State.Closing
        game.free()
    }

    private fun handleInput() {
        mouse.input(window)
        game.input(window, mouse)
    }

    private fun updateState() {
        game.updateState(mouse)
    }

    private fun updateAnimation(delta: Duration) {
        game.updateAnimation(delta)
    }

    private fun renderFrame() {
        game.render(window)
        window.update()
    }

    private fun manualSync() {
        val loopSlot = 1f / configs.goalFramesPerSecond
        val endTime = timer.lastLoopTime + loopSlot
        while (timer.timeNow < endTime) {
            try {
                Thread.sleep(1)
            } catch (ex: InterruptedException) {
                // Ignore the exception
            }
        }
    }
}
