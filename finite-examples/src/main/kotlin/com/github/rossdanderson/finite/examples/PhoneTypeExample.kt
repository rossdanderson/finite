package com.github.rossdanderson.finite.examples

import com.github.rossdanderson.finite.StateMachineModel.Companion.create
import com.github.rossdanderson.finite.examples.State.OffHook
import com.github.rossdanderson.finite.examples.State.OffHook.AwaitingNumber
import com.github.rossdanderson.finite.examples.State.OffHook.HasNumber.*
import com.github.rossdanderson.finite.examples.State.OnHook
import com.github.rossdanderson.finite.examples.Trigger.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() {
    val stateMachineModel = create<State, Trigger, Target> {

        state<OnHook> {
            on<PickedUp> { transitionTo(AwaitingNumber()) }

            on<NumberValidationResult.Success> { doNothing() }
            on<NumberValidationResult.Failure> { doNothing() }

            on<ConnectionResult.Success> { doNothing() }
            on<ConnectionResult.Failure> { doNothing() }
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
    val stateMachine = stateMachineModel.start(Target("Target 1"), OnHook)

    stateMachine.fire(PickedUp)
    stateMachine.fire(NumberDialled("01234567890"))

    // Wait for connection, have a bit of a chat
    runBlocking { delay(10000L) }

    stateMachine.fire(HungUp)

    // Sit idle for a bit
    runBlocking { delay(5000L) }
}
