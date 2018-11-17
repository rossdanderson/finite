package com.github.rossdanderson.finite

import com.github.rossdanderson.finite.config.ActionScope
import com.github.rossdanderson.finite.config.StateConfiguration
import com.github.rossdanderson.finite.config.TransitionAction
import kotlin.reflect.KClass

internal class StateModel<State : Any, Trigger : Any, Target>(
    private val state: KClass<out State>,
    private val superstateModel: StateModel<State, Trigger, Target>?,
    val terminal: Boolean,
    private val triggerActions: MutableMap<KClass<out Trigger>, (State, Trigger) -> TransitionAction<State>>,
    private val entryActions: List<suspend ActionScope<State, State, Trigger, Target>.() -> Unit>,
    private val exitActions: List<suspend ActionScope<State, State, Trigger, Target>.() -> Unit>,
    substateConfigurations: Map<KClass<out State>, StateConfiguration<State, out State, Trigger, Target>>
) {
    private val substateModels: List<StateModel<State, Trigger, Target>> =
        substateConfigurations.map {
            val stateModel: StateModel<State, Trigger, Target> = it.value.build(this)
            stateModel
        }

    fun canHandle(trigger: KClass<out Trigger>) =
        findTransition(trigger) != null

    fun findTransition(
        trigger: KClass<out Trigger>
    ): ((State, Trigger) -> TransitionAction<State>)? =
        findLocalTransition(trigger) ?: superstateModel?.findTransition(trigger)

    private fun findLocalTransition(
        trigger: KClass<out Trigger>
    ): ((State, Trigger) -> TransitionAction<State>)? =
        triggerActions[trigger]

    suspend fun enter(actionScope: ActionScope<State, State, Trigger, Target>) {
        if (actionScope.oldState::class == actionScope.newState::class) executeEntryActions(actionScope)
        else if (!includes(actionScope.oldState::class)) {
            superstateModel?.enter(actionScope)
            executeEntryActions(actionScope)
        }
    }

    private suspend fun executeEntryActions(actionScope: ActionScope<State, State, Trigger, Target>) =
        entryActions.forEach { actionScope.it() }

    suspend fun exit(actionScope: ActionScope<State, State, Trigger, Target>) {
        if (actionScope.oldState::class == actionScope.newState::class) executeExitActions(actionScope)
        else if (!includes(actionScope.newState::class)) {
            executeExitActions(actionScope)
            superstateModel?.exit(actionScope)
        }
    }

    private suspend fun executeExitActions(actionScope: ActionScope<State, State, Trigger, Target>) =
        exitActions.forEach { actionScope.it() }

    /**
     * Checks whether the given [state] matches the current state representation, or any of its substate representations
     */
    private fun includes(state: KClass<out State>): Boolean =
        this.state == state || substateModels.any { it.includes(state) }

    /**
     * Checks whether the given [state] matches the current state representation, of any of its superstate representations
     */
    fun isIncludedIn(state: KClass<out State>): Boolean =
        this.state == state || superstateModel?.isIncludedIn(state) ?: false

    fun flatten(): Iterable<Pair<KClass<out State>, StateModel<State, Trigger, Target>>> =
        listOf(Pair(state, this)) + this.substateModels.flatMap { it.flatten() }
}
