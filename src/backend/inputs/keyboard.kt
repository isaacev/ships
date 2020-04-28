package backend.inputs

import backend.window.Window

typealias KeyCode = Int

/**
 * The DiscreteKey class is a tiny state-machine that tracks the state of a
 * single key so that an action is only performed when the key is pressed
 * for the first time and not when the key is held down.
 */
class DiscreteKey(private val keyCode: KeyCode) {
    private var isBeingPressed = false
    private var isAvailableToUse = false

    fun update(window: Window) {
        if (window.isKeyPressed(keyCode) && !isBeingPressed) {
            isBeingPressed = true
            isAvailableToUse = true
        } else if (window.isKeyReleased(keyCode)) {
            isBeingPressed = false
        }
    }

    fun use(): Boolean {
        return if (isAvailableToUse) {
            isAvailableToUse = false
            true
        } else {
            false
        }
    }
}
