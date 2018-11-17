package com.github.rossdanderson.finite.config

data class TransitionActionScope<State : Any, CurrentState : State, Trigger : Any>(
    val state: CurrentState,
    val trigger: Trigger
) {
    fun doNothing(): TransitionAction.DoNothing<State> = TransitionAction.DoNothing()

    fun transitionTo(state: State): TransitionAction.Transition<State> = TransitionAction.Transition(state)
}
