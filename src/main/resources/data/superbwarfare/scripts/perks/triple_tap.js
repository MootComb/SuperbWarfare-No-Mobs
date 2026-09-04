function tick(tag, level, gunData, entity) {
    tag.reduceCooldown("TripleTapTick")
    const count = tag.getInt("TripleTapCount")

    if (count >= 3) {
        tag.remove("TripleTapTick")
        tag.remove("TripleTapCount")

        const mag = gunData.getMagazine()
        if (mag > 0) {
            gunData.setAmmo(Math.min(mag, gunData.getAmmo() + 1))
        } else {
            gunData.addVirtualAmmo(1)
        }
    }
}

function onHurtEntity(damage, tag, level, gunData, target, source) {
    const directEntity = source.getDirectEntity()
    if (!directEntity.isProjectile()) return

    const bypassArmorRate = directEntity.getBypassArmorRate()
    if ((bypassArmorRate >= 1 && source.isAbsoluteHeadshot()) || source.isHeadshot()) {
        const tick = tag.getInt("TripleTapTick")
        if (tick <= 0) {
            tag.putInt("TripleTapTick", 60 + 10 * level)
            tag.putInt("TripleTapCount", 1)
        } else {
            const count = tag.getInt("TripleTapCount")
            if (count < 3) {
                tag.putInt("TripleTapCount", Math.min(3, count + 1))
            }
        }
    }
}
