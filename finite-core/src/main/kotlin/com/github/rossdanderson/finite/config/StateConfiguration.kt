package com.github.rossdanderson.finite.config

import com.github.rossdanderson.finite.StateModel
import kotlin.reflect.KClass

@Suppress("unused", "UNCHECKED_CAST")
class StateConfiguration<State : Any, CurrentState : State, Trigger : Any, Target>
internal constructor(
    internal val superstateConfiguration: StateConfiguration<State, out State, Trigger, Target>? = null,
    internal val state: KClass<CurrentState>,
    private val stateConfigurationFactory: StateConfigurationFactory<State, Trigger, Target>
) {
    private val triggerActions =
        mutableMapOf<KClass<out Trigger>, (State, Trigger) -> TransitionAction<State>>()
    private val substateConfigurations =
        mutableMapOf<KClass<out State>, StateConfiguration<State, out State, Trigger, Target>>()
    private val entryActions = mutableListOf<suspend ActionScope<State, State, Trigger, Target>.() -> Unit>()
    private val exitActions = mutableListOf<suspend ActionScope<State, State, Trigger, Target>.() -> Unit>()

    /**
     * Begin configuration of the entry/exit actions and allowed transitions
     * when the state machine is in a particular state
     * @param state The state to configure
     * @return A configuration object through which the state can be configured
     */
    fun <NewState : State> state(
        state: KClass<NewState>,
        init: StateConfiguration<State, NewState, Trigger, Target>.() -> Unit
    ): StateConfiguration<State, NewState, Trigger, Target> = substateConfigurations.computeIfAbsent(state) {
        stateConfigurationFactory.createConfiguration(state, this).apply(init)
    } as (StateConfiguration<State, NewState, Trigger, Target>)

    inline fun <reified NewState : State> state(
        noinline init: StateConfiguration<State, NewState, Trigger, Target>.() -> Unit
    ): StateConfiguration<State, NewState, Trigger, Target> =
        state(NewState::class, init)

    fun <FromTrigger : Trigger> on(
        triggerType: KClass<FromTrigger>,
        block: TransitionActionScope<State, CurrentState, FromTrigger>.() -> TransitionAction<State>
    ) {
        val innerAction =
            { state: CurrentState, trigger: FromTrigger ->
                TransitionActionScope<State, CurrentState, FromTrigger>(state, trigger).block()
            }
        triggerActions[triggerType] = (innerAction as (State, Trigger) -> TransitionAction<State>)
    }

    inline fun <reified FromTrigger : Trigger> on(
        noinline block: TransitionActionScope<State, CurrentState, FromTrigger>.() -> TransitionAction<State>
    ) {
        on(FromTrigger::class, block)
    }

    fun onEntry(action: suspend ActionScope<State, CurrentState, Trigger, Target>.() -> Unit) {
        entryActions.add(action as suspend ActionScope<State, State, Trigger, Target>.() -> Unit)
    }

    fun onExit(action: suspend ActionScope<State, CurrentState, Trigger, Target>.() -> Unit) {
        exitActions.add(action as suspend ActionScope<State, State, Trigger, Target>.() -> Unit)
    }

    internal fun build(superstateModel: StateModel<State, Trigger, Target>?): StateModel<State, Trigger, Target> {
        return StateModel(
            state,
            superstateModel,
            triggerActions.isEmpty(),
            triggerActions,
            entryActions.toList(),
            exitActions.toList(),
            substateConfigurations
        )
    }
}
