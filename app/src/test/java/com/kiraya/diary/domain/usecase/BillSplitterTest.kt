package com.kiraya.diary.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class BillSplitterTest {
    @Test
    fun sharesAddUpToTotal() {
        val shares = BillSplitter.splitEqual(1000, 3)
        assertEquals(listOf(334L, 333L, 333L), shares)
        assertEquals(1000L, shares.sum())
    }

    @Test
    fun noPartsGivesEmpty() {
        assertEquals(emptyList<Long>(), BillSplitter.splitEqual(1000, 0))
    }
}
