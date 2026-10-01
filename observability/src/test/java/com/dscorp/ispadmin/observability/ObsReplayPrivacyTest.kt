package com.dscorp.ispadmin.observability

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

class ObsReplayPrivacyTest {

    @After
    fun tearDown() = ObsReplayPrivacy.reset()

    @Test
    fun `replay stays suppressed until every sensitive screen releases`() {
        val first = ObsReplayPrivacy.hold()
        val second = ObsReplayPrivacy.hold()

        first.release()
        assertThat(ObsReplayPrivacy.isSuppressed()).isTrue()

        second.release()
        second.release()
        assertThat(ObsReplayPrivacy.isSuppressed()).isFalse()
    }
}
