package com.teamyg.parfait.feature.groups.canvas.impl.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextEditSessionHolderTest {
    private val holder = TextEditSessionHolder()

    @Test
    fun finish_secondCallWithoutSession_isIgnored() {
        holder.start(session(textId = 1L))

        assertTrue(holder.finish(TextEditResult.Deleted))
        assertFalse(holder.finish(TextEditResult.Deleted))
    }

    @Test
    fun clear_dropsTheResultOfThatSession() {
        val session = session(textId = 1L)
        holder.start(session)
        holder.finish(TextEditResult.Deleted)

        holder.clear(session)

        assertNull(holder.result.value)
    }

    @Test
    fun clear_dropsThatSessionWhileStillOpen() {
        val session = session(textId = 1L)
        holder.start(session)

        holder.clear(session)

        assertNull(holder.session())
    }

    @Test
    fun clear_leavesALaterSessionAndItsResult() {
        val earlier = session(textId = 1L)
        val later = session(textId = 2L)
        holder.start(earlier)
        holder.start(later)

        holder.clear(earlier)
        assertEquals(later, holder.session())

        holder.finish(TextEditResult.Deleted)
        holder.clear(earlier)
        assertEquals(TextEditOutcome(later, TextEditResult.Deleted), holder.result.value)
    }

    @Test
    fun clear_leavesTheResultOfAnotherSession() {
        val finished = session(textId = 1L)
        val other = session(textId = 2L)
        holder.start(finished)
        holder.finish(TextEditResult.Deleted)

        holder.clear(other)

        assertEquals(TextEditOutcome(finished, TextEditResult.Deleted), holder.result.value)
        assertEquals(1L, holder.result.value?.parfaitTextId)
    }

    private fun session(textId: Long) = TextEditSession(
        groupId = 1L,
        parfaitId = 2L,
        original = EditableText(
            parfaitTextId = textId,
            isMine = true,
            text = "안녕",
            color = TextToppingColor.Black,
            type = TextToppingType.DEFAULT,
            designFontSize = 18f,
            positionX = 0.5f,
            positionY = 0.5f,
            positionZ = 1,
        ),
    )
}
