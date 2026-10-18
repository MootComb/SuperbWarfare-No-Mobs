package com.atsuishio.superbwarfare.client.overlay.ammo

internal class AmmoHudFrames {
    data class Pose(val index: Int, val x: Double, val opacity: Double)

    private data class Tween(val from: Double, val to: Double, val start: Long) {
        fun value(now: Long): Double {
            val t = ((now - start) / 240.0).coerceIn(0.0, 1.0)
            return from + (to - from) * (1 - (1 - t) * (1 - t) * (1 - t))
        }
        fun finished(now: Long) = now - start >= 240
    }

    private class Slot {
        var offset = Tween(0.0, 0.0, 0)
        var alpha = Tween(1.0, 1.0, 0)
    }

    private val slots = mutableListOf<Slot>()
    private var count = 0

    fun select(count: Int, direction: Int, now: Long, reduced: Boolean): List<Pose> {
        frame(now)
        val target = count.coerceAtLeast(1)
        if (this.count == 0 || reduced) {
            slots.clear()
            repeat(target) { slots.add(Slot()) }
        } else if (this.count != target) {
            while (slots.size < target) {
                slots.add(Slot().apply {
                    offset = Tween(direction * 16.0, 0.0, now)
                    alpha = Tween(0.0, 1.0, now)
                })
            }
            for ((index, slot) in slots.withIndex()) {
                val leaving = index >= target
                val alpha = if (leaving) 0.0 else 1.0
                // Shared frames keep their current animation; a reversal samples its exact pose.
                if (slot.alpha.to != alpha) {
                    slot.offset = Tween(slot.offset.value(now), if (leaving) -direction * 16.0 else 0.0, now)
                    slot.alpha = Tween(slot.alpha.value(now), alpha, now)
                }
            }
        }
        this.count = target
        return frame(now)
    }

    fun frame(now: Long): List<Pose> {
        while (slots.size > count && slots.last().alpha.finished(now)) slots.removeAt(slots.lastIndex)
        return slots.mapIndexed { index, slot -> Pose(index, index * STRIDE + slot.offset.value(now), slot.alpha.value(now)) }
    }

    fun clear() {
        slots.clear()
        count = 0
    }

    companion object {
        const val SIZE = 12.0
        const val STRIDE = 19.2
        fun width(count: Int) = SIZE + (count.coerceAtLeast(1) - 1) * STRIDE
    }
}
