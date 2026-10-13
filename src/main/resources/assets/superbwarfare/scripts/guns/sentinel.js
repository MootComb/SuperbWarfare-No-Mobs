const CHARGE_BONES = ["charge_illuminated", "charge2_illuminated", "charge3_illuminated", "charge4_illuminated"]

function transformCustomModelPart(stack, model, transformType, partialTick, renderer) {
    // 电量不够打一发充能射击就整组不画
    const lit = renderer.scriptEnergy(stack) >= renderer.scriptChargeCost(stack)

    for (let i = 0; i < CHARGE_BONES.length; i++) {
        let bone = model.getBone(CHARGE_BONES[i])
        if (bone == null) {
            continue
        }

        bone.visible = lit
    }
}
