package com.luckyzyx.luckytool.hook.scopes.systemui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSwipeSessionTest {
    @Test
    fun `blank upward gesture blocks panel movement through release coordinates`() {
        val session = session()

        assertTrace(
            session,
            Sample("DOWN", 0f, 0f, false),
            Sample("MOVE", 0.25f, -0.5f, false),
            Sample("MOVE", 0.5f, -1.5f, true),
            Sample("MOVE", 0.75f, -3f, true),
            Sample("UP", 0.75f, -4f, true),
        )

        assertTrue(session.decided)
    }

    @Test
    fun `jitter and motion at the slop boundary remain undecided`() {
        val session = session()

        assertTrace(
            session,
            Sample("DOWN", 0f, 0f, false),
            Sample("MOVE", 0.25f, -0.5f, false),
            Sample("MOVE", -0.5f, 0.25f, false),
            Sample("MOVE", 1f, -1f, false),
            Sample("UP", 0.5f, -0.25f, false),
        )

        assertFalse(session.decided)
    }

    @Test
    fun `downward and horizontal starts keep native handling after reversing upward`() {
        for (firstMove in listOf(0f to 2f, -2f to 0.5f, 2f to -0.5f)) {
            val session = session()

            assertTrace(
                session,
                Sample("DOWN", 0f, 0f, false),
                Sample("MOVE", firstMove.first, firstMove.second, false),
            )
            assertTrue(session.decided)

            assertTrace(
                session,
                Sample("MOVE", 0.25f, -4f, false),
                Sample("UP", 0f, -6f, false),
            )
        }
    }

    @Test
    fun `an upward decision survives reversal until release coordinates`() {
        val session = session()

        assertTrace(
            session,
            Sample("DOWN", 0f, 0f, false),
            Sample("MOVE", 0.25f, -2f, true),
            Sample("MOVE", 4f, 0f, true),
            Sample("MOVE", 0f, 2f, true),
            Sample("UP", 0f, 4f, true),
        )
    }

    @Test
    fun `a pointer change before direction is decided keeps native handling`() {
        val session = session()

        assertTrace(
            session,
            Sample("DOWN", 0f, 0f, false),
            Sample("MOVE", 0.25f, -0.5f, false),
            Sample("MOVE", 0f, -2f, false, pointerId = POINTER_ID + 1),
            Sample("MOVE", 0f, -4f, false),
            Sample("UP", 0f, -6f, false),
        )

        assertTrue(session.decided)
    }

    @Test
    fun `multiple pointers before direction is decided keep native handling`() {
        for (pointerCount in listOf(2, 3)) {
            val session = session()

            assertTrace(
                session,
                Sample("DOWN", 0f, 0f, false),
                Sample("MOVE", 0.25f, -0.5f, false, pointerCount = pointerCount),
                Sample("MOVE", 0f, -2f, false),
                Sample("UP", 0f, -4f, false),
            )

            assertTrue(session.decided)
        }
    }

    @Test
    fun `a fresh down starts undecided regardless of a decided previous session`() {
        val previous = session()
        assertTrace(previous, Sample("MOVE", 0f, -2f, true))

        val current = session()

        assertFalse(current.decided)
        assertFalse(current.blocksPanel)
        assertTrace(
            current,
            Sample("DOWN", 0f, 0f, false),
            Sample("MOVE", 0f, 2f, false),
            Sample("UP", 0f, 4f, false),
        )
        assertTrue(previous.blocksPanel)
    }

    @Test
    fun `equivalent gestures use the configured slop at different coordinate scales`() {
        for (slop in listOf(2f, 8f, 32f)) {
            val session = session(slop = slop)

            assertTrace(
                session,
                Sample("DOWN", 0f, 0f, false),
                Sample("MOVE", 0.5f, -0.75f, false),
                Sample("MOVE", 0.75f, -1.25f, true),
                Sample("UP", 1f, -3f, true),
                slop = slop,
            )
        }
    }

    private fun session(
        slop: Float = TOUCH_SLOP,
    ) = NotificationSwipeSession(
        pointerId = POINTER_ID,
        downX = 12f * slop,
        downY = 24f * slop,
        slop = slop,
    )

    private fun assertTrace(
        session: NotificationSwipeSession,
        vararg samples: Sample,
        slop: Float = TOUCH_SLOP,
    ) {
        for ((index, sample) in samples.withIndex()) {
            val message = "sample $index ${sample.phase}, slop=$slop"
            assertEquals(
                message,
                sample.blocksPanel,
                session.shouldBlock(
                    x = (12f + sample.dx) * slop,
                    y = (24f + sample.dy) * slop,
                    pointerId = sample.pointerId,
                    pointerCount = sample.pointerCount,
                ),
            )
            assertEquals(message, sample.blocksPanel, session.blocksPanel)
        }
    }

    private data class Sample(
        val phase: String,
        val dx: Float,
        val dy: Float,
        val blocksPanel: Boolean,
        val pointerId: Int = POINTER_ID,
        val pointerCount: Int = 1,
    )

    private companion object {
        const val POINTER_ID = 7
        const val TOUCH_SLOP = 8f
    }
}
