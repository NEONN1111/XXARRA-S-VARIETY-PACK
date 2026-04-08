package neon.nsp.data.scripts.util

data class Flag(var isEnabled: Boolean = true) {
    var isFiltered: Boolean
        get() = !isEnabled
        set(filtered) { isEnabled = !filtered }
}