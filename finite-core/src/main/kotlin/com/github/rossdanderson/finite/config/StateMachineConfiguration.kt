package com.github.rossdanderson.finite.config

import com.github.rossdanderson.finite.StateMachineModel
import com.github.rossdanderson.finite.StateModel
import kotlin.reflect.KClass

/**
 * The state machine configuration
 */
class StateMachineConfiguration<State : Any, Trigger : Any, Target> internal constructor() {

    private val stateConfigurationFactory = StateConfigurationFactory<State, Trigger, Target>()

    private val substateConfigurations =
        mutableMapOf<KClass<out State>, StateConfiguration<State, out State, Trigger, Target>>()

    fun <SubState : State> state(
        state: KClass<SubState>,
        init: StateConfiguration<State, SubState, Trigger, Target>.() -> Unit
    ) {
        substateConfigurations.computeIfAbsent(state) {
            stateConfigurationFactory.createConfiguration(state).apply(init)
        }
    }

    inline fun <reified SubState : State> state(
        noinline init: StateConfiguration<State, SubState, Trigger, Target>.() -> Unit
    ) {
        state(SubState::class, init)
    }

    internal fun build(): StateMachineModel<State, Trigger, Target> =
        StateMachineModel(
            substateConfigurations
                .map { it.value.build(null) }
                .flatMap(StateModel<State, Trigger, Target>::flatten)
                .toMap()
        )
}
