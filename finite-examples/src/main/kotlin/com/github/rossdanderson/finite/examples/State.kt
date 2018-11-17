package com.github.rossdanderson.finite.examples

import java.time.Instant

sealed class State {

    object OnHook : State()

    sealed class OffHook : State() {
        abstract val timeRemovedFromHook: Instant

        data class AwaitingNumber(
            override val timeRemovedFromHook: Instant = Instant.now()
        ) : OffHook()

        sealed class HasNumber : OffHook() {
            abstract val phoneNumber: String

            data class Validating(
                override val timeRemovedFromHook: Instant,
                override val phoneNumber: String
            ) : HasNumber()

            data class Connecting(
                override val timeRemovedFromHook: Instant,
                override val phoneNumber: String
            ) : HasNumber()

            data class Failed(
                override val timeRemovedFromHook: Instant,
                override val phoneNumber: String
            ) : HasNumber()

            data class Connected(
                override val timeRemovedFromHook: Instant,
                override val phoneNumber: String
            ) : HasNumber()
        }
    }

    override fun toString(): String {
        return "${this::class.java.simpleName}()"
    }
}
