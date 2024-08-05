package wisp.questgiver

/**
 * Use by calling `Global.getSector().getListenerManager().addListener(this);`
 * Remove when done by calling `Global.getSector().getListenerManager().removeListener(this);`
 *
 * Absolutely not stolen from Nexerelin. How dare you.
 */
interface ModPluginEventListener {
    fun onGameLoad(isNewGame: Boolean) {}
    fun beforeGameSave() {}
    fun afterGameSave() {}
    fun onGameSaveFailed() {}
    fun onNewGameAfterProcGen() {}
    fun onNewGameAfterEconomyLoad() {}
    fun onNewGameAfterTimePass() {}
}