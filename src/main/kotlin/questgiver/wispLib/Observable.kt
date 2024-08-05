package wisp.questgiver.wispLib

import kotlin.properties.Delegates

open class Observable<T>(
    initialValue: T
) {
    open var value: T by Delegates.observable(initialValue) { _, _, newValue ->
        notifyObservers(newValue)
    }

    val observers = mutableMapOf<Any, Action<T>>()

    protected fun notifyObservers(newValue: T) = observers.forEach { it.value(newValue) }

    override fun toString() = value.toString()
}

typealias Action<T> = (newValue: T) -> Unit