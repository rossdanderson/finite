package com.github.rossdanderson.finite

import com.github.rossdanderson.finite.config.StateMachineConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import java.util.*
import kotlin.reflect.KClass

@Suppress("MemberVisibilityCanBePrivate")
class StateMachineModel<State : Any, Trigger : Any, Target> internal constructor(
    private val allStateRepresentations: Map<KClass<out State>, StateModel<State, Trigger, Target>>
) {
    companion object {
        fun <State : Any, Trigger : Any, Target> create(
            init: StateMachineConfiguration<State, Trigger, Target>.() -> Unit = {}
        ): StateMachineModel<State, Trigger, Target> =
            StateMachineConfiguration<State, Trigger, Target>().apply(init).build()
    }

    fun start(
        target: Target,
        initialState: State,
        scope: CoroutineScope = GlobalScope,
        @Suppress("UNCHECKED_CAST") unhandledTriggerHandler: ((Target, State, Trigger) -> Unit)? = null,
        id: String = UUID.randomUUID().toString()
    ): StateMachine<State, Trigger, Target> {
        var state: State = initialState
        return start(target, { state }, { state = it }, scope, unhandledTriggerHandler, id)
    }

    fun start(
        target: Target,
        stateAccessor: () -> State,
        stateMutator: (State) -> Unit,
        scope: CoroutineScope = GlobalScope,
        @Suppress("UNCHECKED_CAST") unhandledTriggerHandler: ((Target, State, Trigger) -> Unit)?,
        id: String = UUID.randomUUID().toString()
    ): StateMachine<State, Trigger, Target> =
        StateMachine(
            id,
            target,
            allStateRepresentations,
            stateAccessor,
            stateMutator,
            unhandledTriggerHandler
        ).apply { start(scope) }
}
