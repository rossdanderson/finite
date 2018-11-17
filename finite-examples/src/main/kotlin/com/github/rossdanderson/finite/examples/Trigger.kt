package com.github.rossdanderson.finite.examples

sealed class Trigger {
    object PickedUp : Trigger()

    data class NumberDialled(val phoneNumber: String) : Trigger()

    sealed class NumberValidationResult : Trigger() {
        object Success : NumberValidationResult()
        object Failure : NumberValidationResult()
    }

    sealed class ConnectionResult : Trigger() {
        object Success : ConnectionResult()
        object Failure : ConnectionResult()
    }

    object HungUp : Trigger()

    override fun toString(): String {
        return "${this::class.java.simpleName}()"
    }
}
