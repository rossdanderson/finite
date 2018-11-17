package com.github.rossdanderson.finite.examples

import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

/**
 * The object we're going to update the state of (the target)
 */
class Target(private val id: String) {

    private var lightOn = false

    fun turnOnLight() {
        logger.info("Turning light on")
        lightOn = true
    }

    fun turnOffLight() {
        logger.info("Turning light off")
        lightOn = false
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun validate(number: String): Boolean {
        delay(1000L)
        return true
    }

    override fun toString(): String {
        return "Target(id='$id', lightOn=$lightOn)"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Target::class.java)
    }
}
