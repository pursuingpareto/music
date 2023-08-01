package org.pareto.music.feature_requests.functions

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.pareto.music.feature_requests.FeatureRequest


@Disabled
class Overcalling : FeatureRequest() {

    @Test
    fun `creates an uncurried function`() = feature {}

    @Nested
    inner class UncurriedFunctions {

        @Test
        fun `pass overcalled parameters into calling function when called`() = feature {}
    }
}