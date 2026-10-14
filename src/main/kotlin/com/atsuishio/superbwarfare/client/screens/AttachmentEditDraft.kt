package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.network.message.send.AttachmentAdjustMessage

/** 未确认的草稿不计时、不提交；确认后的预览只保留到服务端应答或超时。 */
internal class AttachmentEditDraft {
    var preview: AttachmentAdjustMessage? = null
        private set
    var awaitingServer = false
        private set
    private var submittedTicks = 0

    fun update(value: AttachmentAdjustMessage) {
        preview = value
        awaitingServer = false
        submittedTicks = 0
    }

    fun submit(): AttachmentAdjustMessage? {
        if (awaitingServer) return null
        return preview?.also {
            awaitingServer = true
            submittedTicks = 0
        }
    }

    fun acknowledge(matches: Boolean) {
        if (awaitingServer && matches) discard()
    }

    fun tick() {
        if (awaitingServer && ++submittedTicks >= 40) discard()
    }

    fun discard() {
        preview = null
        awaitingServer = false
        submittedTicks = 0
    }
}
