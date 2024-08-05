object Easing {
    object Quadratic {
        /**
         * Quadratic easing in - accelerating from zero velocity.
         */
        fun easeIn(time: Float, valueAtStart: Float, valueAtEnd: Float, duration: Float): Float {
            return flipIfNeeded(time, valueAtStart, valueAtEnd, duration) { thyme, start, end, dur ->
                val t = thyme / dur
                return@flipIfNeeded (end - start) * t * t + start
            }
        }

        /**
         * Quadratic easing out - decelerating to zero velocity.
         */
        fun easeOut(time: Float, valueAtStart: Float, valueAtEnd: Float, duration: Float): Float {
            val t = time / duration
            return -1 * (valueAtEnd - valueAtStart) * t * (t - 2) + valueAtStart
        }

        /**
         * Quadratic easing in/out - acceleration until halfway, then deceleration.
         */
        fun easeInThenOut(time: Float, valueAtStart: Float, valueAtEnd: Float, duration: Float): Float {
            return flipIfNeeded(time, valueAtStart, valueAtEnd, duration) { thyme, start, end, dur ->
                val t = thyme / (dur / 2)
                return@flipIfNeeded if (t < 1)
                    (end - start) / 2 * t * t + start
                else
                    -(end - start) / 2 * ((t - 1) * (t - 3) - 1) + start
            }
        }
    }

    object Linear {
        /**
         * Simple linear tweening - no easing.
         */
        fun tween(time: Float, valueAtStart: Float, valueAtEnd: Float, duration: Float): Float {
            return flipIfNeeded(time, valueAtStart, valueAtEnd, duration) { thyme, start, end, dur ->
                return@flipIfNeeded (end - start) * thyme / dur + start
            }
        }
    }

    object Cubic {
        /**
         * Cubic easing in - accelerating from zero velocity.
         */
        fun easeIn(time: Float, valueAtStart: Float, valueAtEnd: Float, duration: Float): Float {
            return flipIfNeeded(time, valueAtStart, valueAtEnd, duration) { thyme, start, end, dur ->
                val t = thyme / dur
                return@flipIfNeeded (end - start) * t * t * t + start
            }
        }

        /**
         * Cubic easing out - decelerating to zero velocity.
         */
        fun easeOut(time: Float, valueAtStart: Float, valueAtEnd: Float, duration: Float): Float {
            return flipIfNeeded(time, valueAtStart, valueAtEnd, duration) { thyme, start, end, dur ->
                val t = thyme / dur - 1
                return@flipIfNeeded (end - start) * (t * t * t + 1) + start
            }
        }

        /**
         * Cubic easing in/out - acceleration until halfway, then deceleration.
         */
        fun easeInThenOut(time: Float, valueAtStart: Float, valueAtEnd: Float, duration: Float): Float {
            return flipIfNeeded(time, valueAtStart, valueAtEnd, duration) { thyme, start, end, dur ->
                val t = thyme / (dur / 2)
                return@flipIfNeeded if (t < 1)
                    (end - start) / 2 * t * t * t + start
                else
                    (end - start) / 2 * ((t - 2) * t * t + 2) + start
            }
        }
    }

    private fun flipIfNeeded(
        time: Float,
        valueAtStart: Float,
        valueAtEnd: Float,
        duration: Float,
        easeFunction: (time: Float, valueAtStart: Float, valueAtEnd: Float, duration: Float) -> Float
    ): Float {
        return if (valueAtStart > valueAtEnd) {
            valueAtEnd - easeFunction(time, valueAtEnd, valueAtStart, duration)
        } else {
            easeFunction(time, valueAtStart, valueAtEnd, duration)
        }
    }
}
