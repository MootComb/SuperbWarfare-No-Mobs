function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    const maxEnergy = pmc.get("MaxEnergy")
    if (maxEnergy > 0) {
        pmc.add("EnergyRegeneration", Math.floor((1 + level * 0.4) * maxEnergy / 4000))
    }
}
