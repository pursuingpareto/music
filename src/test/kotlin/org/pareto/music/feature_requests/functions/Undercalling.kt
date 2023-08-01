package org.pareto.music.feature_requests.functions

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.pareto.music.feature_requests.FeatureRequest

class Undercalling : FeatureRequest() {

    @Test
    fun `occurs when a function is passed fewer params than it requires`() = feature {  }

    @Test
    fun `creates curried function`() = feature {}

    @Nested
    inner class CreatesCurriedFunction {

        @Test
        fun `can be called with reduced number of required args`() = feature {}
    }
}