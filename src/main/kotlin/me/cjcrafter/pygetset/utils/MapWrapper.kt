package me.cjcrafter.pygetset.utils

class MapWrapper<K, V>(public var map: MutableMap<K, V>, private var callback: Runnable) {
    operator fun get(key: K): V? {
        return map[key]
    }

    operator fun set(key: K, value: V) {
        callback.run()
        map[key] = value
    }
}