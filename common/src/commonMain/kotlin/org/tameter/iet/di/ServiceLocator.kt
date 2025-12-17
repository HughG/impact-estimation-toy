package org.tameter.iet.di

/**
 * Simple service locator for early stages (Stage 0).
 *
 * Decision: Use a tiny manual locator for v0–v1. Avoid heavy DI.
 * Rationale: Keeps commonMain portable (future Web), minimises dependencies.
 */
object ServiceLocator {
    private val registry = mutableMapOf<Class<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(clazz: Class<T>): T =
        (registry[clazz]
            ?: error("Service not registered: ${clazz.name}")) as T

    fun <T : Any> register(clazz: Class<T>, instance: T) {
        registry[clazz] = instance
    }

    fun clear() {
        registry.clear()
    }
}
