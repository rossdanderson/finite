package com.github.rossdanderson.finite.config

import java.util.*
import kotlin.reflect.KClass

internal class StateConfigurationFactory<State : Any, Trigger : Any, Target> {

    private val stateConfigurations =
        HashMap<KClass<out State>, StateConfiguration<State, out State, Trigger, Target>>()

    fun <NewState : State> createConfiguration(
        state: KClass<NewState>,
        superstateConfiguration: StateConfiguration<State, out State, Trigger, Target>? = null
    ): StateConfiguration<State, NewState, Trigger, Target> {
        val existingStateConfiguration = (stateConfigurations[state] as StateConfiguration<*, *, *, *>?)
        val superstate = existingStateConfiguration?.superstateConfiguration?.state
        if (existingStateConfiguration != null) throw IllegalStateException(
            "State $state has already been defined as a ${if (superstate == null) "top level state" else "substate of $superstate"}"
        )

        val newStateConfiguration = StateConfiguration(superstateConfiguration, state, this)
        stateConfigurations[state] = newStateConfiguration
        return newStateConfiguration
    }
}
