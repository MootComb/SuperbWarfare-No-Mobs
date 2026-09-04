function scaleDurationBySpeed(pmc, key, speedFactor) {
    const current = pmc.get(key)
    if (current > 0) {
        // The gun data stores duration. +X% speed means the duration is divided by 1+X%.
        pmc.set(key, Math.max(1, Math.round(current / speedFactor)))
    }
}

function scaleValue(pmc, key, factor) {
    const current = pmc.get(key)
    if (current > 0) {
        pmc.set(key, current / factor)
    }
}

function modifyProperty(pmc, level, perkTag, gunData) {
    if (!pmc) return

    const zoomRate = 0.45 + 0.05 * level
    scaleDurationBySpeed(pmc, "ZoomTime", 1 + zoomRate)

    const stabilityRate = 0.05 * level
    const stabilityFactor = 1 + stabilityRate
    scaleValue(pmc, "RecoilX", stabilityFactor)
    scaleValue(pmc, "RecoilY", stabilityFactor)
}
