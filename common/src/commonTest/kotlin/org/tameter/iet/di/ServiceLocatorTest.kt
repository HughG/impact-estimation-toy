package org.tameter.iet.di

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Requirements refs: IET/Model (namespaces), IET/Model/Storage (namespaces),
 * IET/UI/Desktop/Framework (Compose chosen), and Stage 0 plan items.
 */
class ServiceLocatorTest {
    @Test
    fun serviceLocator_register_and_get_returns_instance() {
        data class Foo(val x: Int)
        ServiceLocator.clear()
        ServiceLocator.register(Foo::class.java, Foo(42))
        val got = ServiceLocator.get(Foo::class.java)
        assertEquals(42, got.x)
    }

    @Test
    fun serviceLocator_get_unregistered_throws() {
        data class Bar(val y: Int)
        ServiceLocator.clear()
        assertFailsWith<IllegalStateException> {
            ServiceLocator.get(Bar::class.java)
        }
    }
}