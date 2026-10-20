const CHARGE_BAR_PREFIX = "charge_bar_"
const CHARGE_BAR_SEGMENTS = 40

function transformCustomModelPart(stack, model, transformType, partialTick, renderer) {
    const chargeProgress = renderer.scriptChargeProgress(stack)
    const litChargeSegments = Math.ceil(chargeProgress * CHARGE_BAR_SEGMENTS)

    for (let i = 1; i <= CHARGE_BAR_SEGMENTS; i++) {
        let chargeBone = model.getBone(CHARGE_BAR_PREFIX + i)
        if (chargeBone == null) {
            continue
        }

        chargeBone.visible = i <= litChargeSegments
    }

    const progress = renderer.scriptBipodProgress(stack)
    if (progress <= 0) {
        return
    }
}
