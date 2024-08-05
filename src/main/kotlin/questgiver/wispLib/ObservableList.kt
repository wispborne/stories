package wisp.questgiver.wispLib

class ObservableList<T>(private val wrapped: MutableList<T>) : MutableList<T> by wrapped, java.util.Observable() {
    override fun add(element: T): Boolean {
        if (wrapped.add(element)) {
            setChanged()
            notifyObservers(wrapped)
            return true
        }

        return false
    }

    override fun add(index: Int, element: T) {
        wrapped.add(index, element)
        setChanged()
        notifyObservers(wrapped)
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        if (wrapped.addAll(index, elements)) {
            setChanged()
            notifyObservers(wrapped)
            return true
        }

        return false
    }

    override fun addAll(elements: Collection<T>): Boolean {
        if (wrapped.addAll(elements)) {
            setChanged()
            notifyObservers(wrapped)
            return true
        }

        return false
    }

    override fun clear() {
        wrapped.clear()
        setChanged()
        notifyObservers(wrapped)
    }

    override fun remove(element: T): Boolean {
        if (wrapped.remove(element)) {
            setChanged()
            notifyObservers(wrapped)
            return true
        }

        return false
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        if (wrapped.removeAll(elements)) {
            setChanged()
            notifyObservers(wrapped)
            return true
        }

        return false
    }

    override fun removeAt(index: Int): T {
        val item = wrapped.removeAt(index)
        setChanged()
        notifyObservers(wrapped)
        return item
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        if (wrapped.retainAll(elements)) {
            setChanged()
            notifyObservers(wrapped)
            return true
        }
        return false
    }

    override fun set(index: Int, element: T): T {
        val item = wrapped.set(index, element)
        setChanged()
        notifyObservers(wrapped)
        return item
    }
}