# Finite

![Build status](https://travis-ci.org/rossdanderson/finite.svg?branch=master "Build status")

Define and create state machines using a simple Kotlin DSL.

# Features
 * Clean DSL helping you to reason about your state.
 * Both Triggers and State may carry and transfer data.
 * Ability to inspect triggers and the previous state in order to choose the next state.
 * Provide a target object for applying side effects to for each state model instance - keep your code loosely coupled.
 * Re-use the same model to create many state machines.
 * Neatly handle unexpected triggers to mitigate issues such as crossing on the wire.
 * State transitions are handled on a (configurable) coroutine scope, helping to avoid common threading pitfalls.

# Usage
An example state machine, and usage of it might be:
```kotlin
val stateMachineModel = create<State, Trigger, Target> {

    state<OnHook> {
        on<PickedUp> { transitionTo(AwaitingNumber()) }
    }

    state<OffHook> {
        onEntry { target.turnOnLight() }
        onExit { target.turnOffLight() }

        state<AwaitingNumber> {
            on<NumberDialled> { transitionTo(Validating(state.timeRemovedFromHook, trigger.phoneNumber)) }
            on<HungUp> { transitionTo(OnHook) }
        }

        state<Validating> {

            onEntry {
                if (target.validate(newState.phoneNumber)) {
                    stateMachine.fire(NumberValidationResult.Success)
                } else {
                    stateMachine.fire(NumberValidationResult.Failure)
                }
            }

            on<NumberValidationResult.Success> {
                transitionTo(Connecting(state.timeRemovedFromHook, state.phoneNumber))
            }
            on<NumberValidationResult.Failure> {
                transitionTo(Failed(state.timeRemovedFromHook, state.phoneNumber))
            }
            on<HungUp> { transitionTo(OnHook) }
        }

        state<Connecting> {
            on<HungUp> { transitionTo(OnHook) }
            on<ConnectionResult.Success> {
                transitionTo(Connected(state.timeRemovedFromHook, state.phoneNumber))
            }
            on<ConnectionResult.Failure> {
                transitionTo(Failed(state.timeRemovedFromHook, state.phoneNumber))
            }
        }

        state<Connected> {
            on<HungUp> { transitionTo(OnHook) }
        }

        state<Failed> {
            on<HungUp> { transitionTo(OnHook) }
        }
    }
}

// Start a state machine with a target object and initial state
val stateMachine = stateMachineModel.start(Target("Phone 1"), OnHook)

stateMachine.fire(PickedUp)
stateMachine.fire(NumberDialled("01234567890"))

// Wait for connection, have a bit of a chat
runBlocking { delay(10000L) }

stateMachine.fire(HungUp)
```

Please note that all of your Triggers and State objects should be immutable. 
Using Kotlin's data classes or singleton objects to represent these is strongly recommended.

The State and Trigger objects used for the above example are:
```kotlin
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
```

License
===
Apache 2.0 License

Credit
===
Credit goes to [stateless4j](https://github.com/oxo42/stateless4j), and its project author [John Oxley](https://github.com/oxo42), from which this code is heavily inspired.
