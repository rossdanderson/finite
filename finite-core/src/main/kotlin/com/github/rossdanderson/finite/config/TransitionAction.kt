package com.github.rossdanderson.finite.config

@Suppress("unused")
sealed class TransitionAction<State : Any> {
    class DoNothing<State : Any> internal constructor() : TransitionAction<State>()
    data class Transition<State : Any> internal constructor(val toState: State) : TransitionAction<State>()
}
