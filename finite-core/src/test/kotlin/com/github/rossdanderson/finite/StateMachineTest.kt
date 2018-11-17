package com.github.rossdanderson.finite

import com.github.rossdanderson.finite.StateMachineModel.Companion.create
import com.github.rossdanderson.finite.StateSingleton.*
import com.github.rossdanderson.finite.Trigger.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
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
                    }
                    state<Third> {}
                }

            it("should transition through the states") {
                runBlocking(testCoroutineContext) {
                    val stateMachine = stateMachineModel.start(Target, First, this)
                    assertEquals(First, stateMachine.state)

                    stateMachine.fire(One)
                    testCoroutineContext.triggerActions()
                    assertEquals(Second, stateMachine.state)

                    stateMachine.fire(Two)
                    testCoroutineContext.triggerActions()
                    assertEquals(Third, stateMachine.state)
                }
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


            it("should transition through the states") {

                runBlocking(testCoroutineContext) {
                    val stateMachine = stateMachineModel.start(Target, First, this)
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

                    stateMachine.close()
                }
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
                runBlocking(testCoroutineContext) {
                    val stateMachine = stateMachineModel.start(Target, First, this)
                    stateMachine.fire(One)
                    testCoroutineContext.triggerActions()
                }
            }

            it("should be terminated when started in it") {
                runBlocking(testCoroutineContext) {
                    stateMachineModel.start(Target, Second, this)
                }
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

            it("can enter the sub-state from a root-level state") {
                runBlocking(testCoroutineContext) {

                    val stateMachine = stateMachineModel.start(Target, First, this)
                    assertEquals(First, stateMachine.state)

                    stateMachine.fire(One)
                    testCoroutineContext.triggerActions()
                    assertEquals(Second, stateMachine.state)

                    stateMachine.fire(Three)
                    testCoroutineContext.triggerActions()
                    assertEquals(Third, stateMachine.state)
                }
            }

            it("can enter the sub-state from its parent state") {
                runBlocking(testCoroutineContext) {
                    val stateMachine = stateMachineModel.start(Target, First, this)
                    assertEquals(First, stateMachine.state)

                    stateMachine.fire(Two)
                    testCoroutineContext.triggerActions()
                    assertEquals(Third, stateMachine.state)
                    assertFalse(stateMachine.isInState<First>())
                    assertTrue(stateMachine.isInState<Second>())
                    assertTrue(stateMachine.isInState<Third>())
                }
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

            it("can enter the nested sub-state from a root-level state") {
                runBlocking(testCoroutineContext) {
                    val stateMachine = stateMachineModel.start(Target, First, this)
                    assertEquals(First, stateMachine.state)

                    stateMachine.fire(Two)
                    testCoroutineContext.triggerActions()
                    assertEquals(Fourth, stateMachine.state)
                    assertFalse(stateMachine.isInState<First>())
                    assertTrue(stateMachine.isInState<Second>())
                    assertTrue(stateMachine.isInState<Third>())
                    assertTrue(stateMachine.isInState<Fourth>())
                }
            }

            it("can enter the nested sub-state from its parent state") {
                runBlocking(testCoroutineContext) {
                    val stateMachine = stateMachineModel.start(Target, First, this)
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
        }
    }
//
//        group("Simple actions") {
//
//        }
//
//        group("Sub-state actions") {
//
//        }
//    }
})
