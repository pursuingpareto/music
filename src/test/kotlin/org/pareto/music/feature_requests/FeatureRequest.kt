package org.pareto.music.feature_requests

import org.opentest4j.AssertionFailedError
import kotlin.test.currentStackTrace

open class FeatureRequest {

    fun feature(block: () -> Unit) {

        fun pWrap(word: String) = println("Would $word requirement ${currentStackTrace()[2]}")

        try { block().also { pWrap("PASS")} }
        catch(ex : AssertionFailedError) { pWrap("FAIL") }
        println("${currentStackTrace()[2]}")
    }
}