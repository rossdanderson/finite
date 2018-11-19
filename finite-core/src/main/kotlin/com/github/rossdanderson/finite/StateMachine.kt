package com.github.rossdanderson.finite

import com.github.rossdanderson.finite.config.ActionScope
import com.github.rossdanderson.finite.config.TransitionAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import mu.KLogging
import kotlin.reflect.KClass

/**
 * Models behaviour as transitions between a finite set of states
 *
 * [State] The type used to represent the states
 * [Trigger] The type used to represent the triggers that cause state transitions
 * [Target] the type to apply entry/exit actions to.
 */
class StateMachine<State : Any, Trigger : Any, Target> internal constructor(
    val id: String,
    private val target: Target,
    private val allStateRepresentations: Map<KClass<out State>, StateModel<State, Trigger, Target>>,
    private val stateAccessor: () -> State,
    private val stateMutator: (State) -> Unit,
    unhandledTriggerHandler: ((Target, State, Trigger) -> Unit)?
) {
    companion object : KLogging()

    private val _unhandledTriggerHandler: (Target, State, Trigger) -> Unit =
        unhandledTriggerHandler ?: { _, state, trigger ->
            logger.warn {
                "$id - No valid transitions are configured from state <$state> with trigger <$trigger>. " +
                        "Ignore this trigger to hide this warning."
            }
        }

    private val channel = Channel<Trigger>(capacity = UNLIMITED)

    val terminated
        get() = channel.isClosedForSend

    /**
     * The current state
     */
    var state: State
        get() = stateAccessor()
        private set(value) = stateMutator(value)

    @Suppress("EXPERIMENTAL_API_USAGE")
    internal fun start(scope: CoroutineScope) {
        val initialRepresentation = findRepresentation(state::class)
        if (initialRepresentation.terminal) channel.close()
        else scope.launch {
            channel.consumeEach { trigger ->
                val currentRepresentation = findRepresentation(state::class)
                val transition: ((State, Trigger) -> TransitionAction<State>)? =
                    currentRepresentation.findTransition(trigger::class)
                if (transition == null) {
                    _unhandledTriggerHandler(target, state, trigger)
                } else {
                    when (val transitionAction = transition(state, trigger)) {
                        is TransitionAction.DoNothing -> {
                            logger.debug { "$id - Ignoring trigger <$trigger>" }
                        }
                        is TransitionAction.Transition -> {
                            val newState = transitionAction.toState
                            val newRepresentation = findRepresentation(newState::class)

                            if (newRepresentation.terminal) channel.close()

                            logger.info { "$id - <$trigger> trigger causing state transition <$state> -> <$transitionAction>" }
                            val oldState = state
                            val actionScope = ActionScope(this@StateMachine, target, trigger, oldState, newState)
                            currentRepresentation.exit(actionScope)
                            state = newState
                            newRepresentation.enter(actionScope)
                        }
                    }
                }
            }
            logger.info { "$id - Terminated" }
        }
    }

    private fun findRepresentation(state: KClass<out State>): StateModel<State, Trigger, Target> =
        allStateRepresentations[state]
            ?: throw IllegalStateException("$id - State <$state> is not valid - Please ensure it has been configured.")

    fun fire(trigger: Trigger) {
        channel.offer(trigger)
    }

    fun close() {
        channel.close()
    }

    /**
     * Determine if the state machine is in the supplied state
     * @param testState The state to test for
     * @return `true` if the current state is the same as or a substate of the supplied state
     */
    fun isInState(testState: KClass<out State>): Boolean = findRepresentation(this.state::class).isIncludedIn(testState)

    /**
     * Determine if the state machine is in the supplied state
     * @param TestState The state to test for
     * @return `true` if the current state is the same as or a substate of the supplied state
     */
    inline fun <reified TestState : State> isInState(): Boolean = isInState(TestState::class)

    /**
     * Returns `true` if the trigger can be fired in the current state
     * @param testTrigger Trigger to test
     * @return `true` if the trigger can be fired
     */
    fun canFire(testTrigger: KClass<out Trigger>): Boolean = findRepresentation(state::class).canHandle(testTrigger)

    /**
     * Returns `true` if the trigger can be fired in the current state
     * @param TestTrigger Trigger to test
     * @return `true` if the trigger can be fired
     */
    inline fun <reified TestTrigger : Trigger> canFire() = canFire(TestTrigger::class)
}
