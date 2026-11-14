package com.kiraya.diary.domain.usecase

object BillSplitter {
    /**
     * Splits [total] paisa into [parts] shares that add up exactly to the total.
     * Leftover paisa go to the first shares. 1000 / 3 -> 334, 333, 333.
     */
    fun splitEqual(total: Long, parts: Int): List<Long> {
        if (parts <= 0) return emptyList()
        val base = total / parts
        val extra = (total % parts).toInt()
        return List(parts) { i -> base + if (i < extra) 1 else 0 }
    }
}
