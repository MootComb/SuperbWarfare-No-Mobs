package com.atsuishio.superbwarfare.client.overlay.ammo

internal class AmmoHudCarousel {
    data class Pose(val key: String?, val width: Double, val x: Double, val opacity: Double) {
        val visible get() = key != null && opacity > 0.0
    }

    data class Frame(val width: Double, val current: Int, val slots: List<Pose>)

    private data class Tween(val from: Double, val to: Double, val start: Long, val duration: Long) {
        fun value(now: Long): Double {
            val t = ((now - start).toDouble() / duration).coerceIn(0.0, 1.0)
            val eased = 1.0 - (1.0 - t) * (1.0 - t) * (1.0 - t)
            return from + (to - from) * eased
        }
        fun finished(now: Long) = now - start >= duration
    }

    private class Slot {
        var key: String? = null
        var width = 12.0
        var x = Tween(0.0, 0.0, 0, 240)
        var opacity = Tween(1.0, 1.0, 0, 240)

        fun pose(now: Long) = Pose(key, width, x.value(now), opacity.value(now))
        fun move(xFrom: Double, xTo: Double, alphaFrom: Double, alphaTo: Double, now: Long) {
            x = Tween(xFrom, xTo, now, 240)
            opacity = Tween(alphaFrom, alphaTo, now, 240)
        }
    }

    private val slots = arrayOf(Slot(), Slot())
    private var current = 0
    private var seat = Tween(12.0, 12.0, 0, 160)

    fun select(key: String, width: Double, direction: Int, now: Long, reduced: Boolean): Frame {
        val before = frame(now)
        if (slots[current].key == null || reduced) {
            val slot = slots[current]
            slot.key = key
            slot.width = width
            slot.move(0.0, 0.0, 1.0, 1.0, now)
            slots[1 - current].key = null
            seat = Tween(width, width, now, 160)
        } else if (slots[current].key != key) {
            val next = 1 - current
            val entering = slots[next]
            val leaving = slots[current]
            // A short slide inside the seat, independent of the number of simultaneous sources.
            // Growing a composite group must not fling all its icons in from a full group-width away.
            val travel = 12.0
            // A -> B -> A reverses the two existing nodes without recreating either item.
            val reuse = entering.key == key && before.slots[next].visible
            val enteringX = if (reuse) before.slots[next].x else direction * travel
            val enteringAlpha = if (reuse) before.slots[next].opacity else 0.28
            entering.key = key
            entering.width = width
            entering.move(enteringX, 0.0, enteringAlpha, 1.0, now)
            leaving.move(before.slots[current].x, -direction * travel, before.slots[current].opacity, 0.0, now)
            seat = Tween(before.width, width, now, 160)
            current = next
        } else if (slots[current].width != width) {
            slots[current].width = width
            seat = Tween(before.width, width, now, 160)
        }
        return frame(now)
    }

    fun frame(now: Long): Frame {
        val outgoing = slots[1 - current]
        if (outgoing.key != null && outgoing.opacity.to == 0.0 && outgoing.opacity.finished(now)) {
            outgoing.key = null
        }
        return Frame(seat.value(now), current, slots.map { it.pose(now) })
    }

    fun clear() {
        slots.forEach { it.key = null }
        current = 0
        seat = Tween(12.0, 12.0, 0, 160)
    }
}
