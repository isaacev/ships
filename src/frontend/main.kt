package frontend

import backend.engine.Engine
import backend.engine.EngineConfigs
import backend.window.WindowConfigs
import frontend.game.Game
import kotlin.system.exitProcess

fun main() {
    try {
        Engine(Game(), object : EngineConfigs {
            override val goalFramesPerSecond = 60
            override val goalUpdatesPerSecond = 30
            override val window = object : WindowConfigs {
                override val title = "Ships"
                override val width = 1280
                override val height = 800
                override val vSync = true
            }
        }).run()
    } catch (ex: Exception) {
        ex.printStackTrace()
        exitProcess(-1)
    }
}
