package com.github.rossdanderson.finite

import com.github.rossdanderson.finite.StateMachineModel.Companion.create
import com.github.rossdanderson.finite.StateSingleton.*
import com.github.rossdanderson.finite.Trigger.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestCoroutineContext
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

sealed class StateSingleton {
    object First : StateSingleton()
    object Second : StateSingleton()
    object Third : StateSingleton()
    object Fourth : StateSingleton()

    override fun toString(): String {
        return "${this::class.java.simpleName}()"
    }
}

sealed class Trigger {
    object One : Trigger()
    object Two : Trigger()
    object Three : Trigger()

    override fun toString(): String {
        return "${this::class.java.simpleName}()"
    }
}

object Target {
    override fun toString(): String {
        return "${this::class.java.simpleName}()"
    }
}

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
object StateMachineTest : Spek({
    val testCoroutineContext by memoized { TestCoroutineContext("Test context") }

    describe("A state machine") {

        describe("with linear state transitions") {

            val stateMachineModel =
                create<StateSingleton, Trigger, Target> {
                    state<First> {
                        on<One> { transitionTo(Second) }
                    }
                    state<Second> {
                        on<Two> { transitionTo(Third) }
                        on<Three> { doNothing() }
                    }
                    state<Third> {}
                }

            val unhandledTriggers by memoized { mutableListOf<Trigger>() }
            val stateMachine by memoized {
                stateMachineModel.start(Target, First, GlobalScope + testCoroutineContext,
                    { _, _, trigger -> unhandledTriggers += trigger })
            }

            it("should transition through the states") {
                assertEquals(First, stateMachine.state)

                stateMachine.fire(One)
                testCoroutineContext.triggerActions()
                assertEquals(Second, stateMachine.state)

                stateMachine.fire(Two)
                testCoroutineContext.triggerActions()
                assertEquals(Third, stateMachine.state)
            }

            it("should ignore any triggers configured to do nothing") {
                assertEquals(First, stateMachine.state)

                stateMachine.fire(One)
                testCoroutineContext.triggerActions()
                assertEquals(Second, stateMachine.state)

                stateMachine.fire(Three)
                testCoroutineContext.triggerActions()
                assertEquals(Second, stateMachine.state)
            }

            it("should pass any unhandled triggers to the unhandled trigger handler") {
                assertEquals(First, stateMachine.state)

                stateMachine.fire(Two)
                testCoroutineContext.triggerActions()
                assertEquals(First, stateMachine.state)

                stateMachine.fire(Three)
                testCoroutineContext.triggerActions()
                assertEquals(First, stateMachine.state)

                assertEquals(listOf(Two, Three), unhandledTriggers)
            }

            it("should continue as normal after dealing with an unhandled trigger") {
                assertEquals(First, stateMachine.state)

                stateMachine.fire(Two)
                testCoroutineContext.triggerActions()
                assertEquals(First, stateMachine.state)

                stateMachine.fire(One)
                testCoroutineContext.triggerActions()
                assertEquals(Second, stateMachine.state)
            }
        }

        describe("with looping state transitions") {

            val stateMachineModel =
                create<StateSingleton, Trigger, Target> {
                    state<First> {
                        on<One> { transitionTo(Second) }
                    }
                    state<Second> {
                        on<Two> { transitionTo(Third) }
                    }
                    state<Third> {
                        on<Three> { transitionTo(First) }
                    }
                }

            val stateMachine by memoized { stateMachineModel.start(Target, First, GlobalScope + testCoroutineContext) }

            it("should transition through the states") {
                assertEquals(First, stateMachine.state)

                stateMachine.fire(One)
                testCoroutineContext.triggerActions()
                assertEquals(Second, stateMachine.state)

                stateMachine.fire(Two)
                testCoroutineContext.triggerActions()
                assertEquals(Third, stateMachine.state)

                stateMachine.fire(Three)
                testCoroutineContext.triggerActions()
                assertEquals(First, stateMachine.state)
            }
        }

        describe("with an implicitly terminal Second state") {

            val stateMachineModel = create<StateSingleton, Trigger, Target> {
                state<First> {
                    on<One> { transitionTo(Second) }
                }
                state<Second> {
                    // No further transitions, so considered terminal
                }
            }

            it("should be terminated when transitioning into it") {
                val stateMachine = stateMachineModel.start(Target, First, GlobalScope + testCoroutineContext)
                assertFalse(stateMachine.terminated)
                stateMachine.fire(One)
                testCoroutineContext.triggerActions()
                assertTrue(stateMachine.terminated)
            }

            it("should be terminated when started in it") {
                val stateMachine = stateMachineModel.start(Target, Second, GlobalScope + testCoroutineContext)
                assertTrue(stateMachine.terminated)
            }
        }

        describe("with first level sub-states") {

            val stateMachineModel = create<StateSingleton, Trigger, Target> {
                state<First> {
                    on<One> { transitionTo(Second) }
                    on<Two> { transitionTo(Third) }
                }
                state<Second> {
                    on<Three> { transitionTo(Third) }
                    state<Third> {}
                }
            }

            val stateMachine by memoized { stateMachineModel.start(Target, First, GlobalScope + testCoroutineContext) }

            it("can enter the sub-state from a root-level state") {
                assertEquals(First, stateMachine.state)

                stateMachine.fire(One)
                testCoroutineContext.triggerActions()
                assertEquals(Second, stateMachine.state)

                stateMachine.fire(Three)
                testCoroutineContext.triggerActions()
                assertEquals(Third, stateMachine.state)
            }

            it("can enter the sub-state from its parent state") {
                assertEquals(First, stateMachine.state)

                stateMachine.fire(Two)
                testCoroutineContext.triggerActions()
                assertEquals(Third, stateMachine.state)
                assertFalse(stateMachine.isInState<First>())
                assertTrue(stateMachine.isInState<Second>())
                assertTrue(stateMachine.isInState<Third>())
            }
        }

        describe("with first and second level sub states") {

            val stateMachineModel = create<StateSingleton, Trigger, Target> {
                state<First> {
                    on<One> { transitionTo(Third) }
                    on<Two> { transitionTo(Fourth) }
                }
                state<Second> {
                    state<Third> {
                        on<Three> { transitionTo(Fourth) }
                        state<Fourth> {}
                    }
                }
            }

            val stateMachine by memoized { stateMachineModel.start(Target, First, GlobalScope + testCoroutineContext) }

            it("can enter the nested sub-state from a root-level state") {
                assertEquals(First, stateMachine.state)

                stateMachine.fire(Two)
                testCoroutineContext.triggerActions()
                assertEquals(Fourth, stateMachine.state)
                assertFalse(stateMachine.isInState<First>())
                assertTrue(stateMachine.isInState<Second>())
                assertTrue(stateMachine.isInState<Third>())
                assertTrue(stateMachine.isInState<Fourth>())
            }

            it("can enter the nested sub-state from its parent state") {
                assertEquals(First, stateMachine.state)

                stateMachine.fire(One)
                testCoroutineContext.triggerActions()
                assertEquals(Third, stateMachine.state)
                assertFalse(stateMachine.isInState<First>())
                assertTrue(stateMachine.isInState<Second>())
                assertTrue(stateMachine.isInState<Third>())
                assertFalse(stateMachine.isInState<Fourth>())

                stateMachine.fire(Three)
                testCoroutineContext.triggerActions()
                assertEquals(Fourth, stateMachine.state)
                assertFalse(stateMachine.isInState<First>())
                assertTrue(stateMachine.isInState<Second>())
                assertTrue(stateMachine.isInState<Third>())
                assertTrue(stateMachine.isInState<Fourth>())
            }
        }

        xdescribe("with entry and exit actions on the root states") {

        }

        xdescribe("with entry and exit actions on the root and sub-states") {

        }

        xdescribe("with an entry action that immediately fires the next trigger") {
            xit("should wait until all entry and exit actions have been executed before processing the new trigger") {

            }
        }
    }
})
