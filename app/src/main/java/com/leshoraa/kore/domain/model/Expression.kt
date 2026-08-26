package com.leshoraa.kore.domain.model

/**
 * Discrete 2D facial emotional expressions rendered on the companion OLED display.
 *
 * Conforms to the hardware affective system specification and Web UI control indices.
 *
 * @property code Integer opcode sent over the communication transport layer.
 * @property displayName Human-readable label matching the Web dashboard buttons.
 * @property description Semantic explanation of the affective state.
 */
enum class Expression(
    val code: Int,
    val displayName: String,
    val description: String
) {
    IDLE(0, "IDLE", "Neutral baseline expression"),
    JOY(1, "JOY", "Happy, joyful state"),
    ANGRY(2, "ANGRY", "Aggressive, irritated state"),
    SMIRK(3, "SMIRK", "Playful, mischievous state"),
    SHOCK(4, "SHOCK", "Surprised, startled state"),
    OVERLOAD(5, "OVERLOAD", "Cognitive overload / dizzy state"),
    SAD(6, "SAD", "Melancholic, sad state"),
    DEADPAN(7, "DEADPAN", "Blank, unreadable poker face");

    companion object {
        /**
         * Resolves an expression enum from its integer opcode, returning null if unmapped.
         *
         * @param code Integer opcode to resolve.
         * @return Matching [Expression] instance, or null if the code is unrecognized or Auto Mood (-1).
         */
        fun fromCode(code: Int): Expression? = entries.find { it.code == code }
    }
}
