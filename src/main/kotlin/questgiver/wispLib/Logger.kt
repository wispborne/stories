package wisp.questgiver.wispLib

import org.apache.log4j.Level
import org.apache.log4j.Logger
import wisp.questgiver.Questgiver.game

class DebugLogger {
    var level: Level = Level.ALL

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getLogger(): Logger {
        val callingClassName =
        // Protect against people running the game on newer JREs
            // that have removed this method.
            if (getJavaVersion() < 9) {
                runCatching { getCallerClassName(3) }
                    .onFailure { }
                    .getOrNull()
            } else null

        return Logger.getLogger(callingClassName ?: "")
            .apply { level = Level.ALL }
    }

    private fun getCallerClassName(depth: Long): String? =
        if (getJavaVersion() < 9) null
//            runCatching {
//                sun.reflect.Reflection.getCallerClass(depth.toInt()).name
//            }
//                .onFailure { }
//                .getOrNull()
        else StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
            .walk { frames ->
                frames.skip(depth)
                    .findFirst()
                    .map(StackWalker.StackFrame::getClassName)
                    .orElse("Unknown")
            }

    fun w(ex: Throwable? = null, message: (() -> String?)? = null) {
        if (game.logger.level <= Level.WARN) {
            if (ex != null) {
                getLogger().warn(message?.invoke(), ex)
            } else {
                getLogger().warn(message?.invoke())
            }
        }
    }

    fun i(ex: Throwable? = null, message: (() -> String?)? = null) {
        if (game.logger.level <= Level.INFO) {
            if (ex != null) {
                getLogger().info(message?.invoke(), ex)
            } else {
                getLogger().info(message?.invoke())
            }
        }
    }

    fun d(ex: Throwable? = null, message: (() -> String?)? = null) {
        if (game.logger.level <= Level.DEBUG) {
            if (ex != null) {
                getLogger().debug(message?.invoke(), ex)
            } else {
                getLogger().debug(message?.invoke())
            }
        }
    }

    fun e(ex: Throwable? = null, message: (() -> String?)? = null) {
        if (game.logger.level <= Level.ERROR) {
            if (ex != null) {
                getLogger().error(message?.invoke(), ex)
            } else {
                getLogger().error(message?.invoke())
            }
        }
    }
}