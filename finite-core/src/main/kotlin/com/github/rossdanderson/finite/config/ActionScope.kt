package com.github.rossdanderson.finite.config

import com.github.rossdanderson.finite.StateMachine

data class ActionScope<State : Any, CurrentState : State, Trigger : Any, Target>(
    val stateMachine: StateMachine<State, Trigger, Target>,
    val target: Target,
    val trigger: Trigger,
    val oldState: State,
    val newState: CurrentState
)
