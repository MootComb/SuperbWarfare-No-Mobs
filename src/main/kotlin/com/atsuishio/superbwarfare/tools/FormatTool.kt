package com.atsuishio.superbwarfare.tools

import java.text.DecimalFormat

/**
 * Extension function to convert camelCase string into snake_case.
 */
fun String.camelToSnake() = FormatTool.camelToSnake(this)

/**
 * Utility class for decimal formatting and string modifications.
 *
 * @author atsuishio
 * @since 0.8.9.1
 */
object FormatTool {
    @JvmField
    val DECIMAL_FORMAT_0 = DecimalFormat("##")

    @JvmField
    val DECIMAL_FORMAT_1 = DecimalFormat("##.#")

    @JvmField
    val DECIMAL_FORMAT_2 = DecimalFormat("##.##")

    @JvmField
    val DECIMAL_FORMAT_1Z = DecimalFormat("##.0")

    @JvmField
    val DECIMAL_FORMAT_1ZZ = DecimalFormat("#0.0")

    @JvmField
    val DECIMAL_FORMAT_2ZZZ = DecimalFormat("#0.00")

    @JvmStatic
    @JvmOverloads
    fun format0D(num: Double, str: String = "") = DECIMAL_FORMAT_0.format(num) + str

    @JvmStatic
    @JvmOverloads
    fun format1D(num: Double, str: String = "") = DECIMAL_FORMAT_1.format(num) + str

    @JvmStatic
    @JvmOverloads
    fun format2D(num: Double, str: String = "") = DECIMAL_FORMAT_2.format(num) + str

    @JvmStatic
    @JvmOverloads
    fun format1DZ(num: Double, str: String = "") = DECIMAL_FORMAT_1Z.format(num) + str

    @JvmStatic
    @JvmOverloads
    fun format1DZZ(num: Double, str: String = "") = DECIMAL_FORMAT_1ZZ.format(num) + str

    /**
     * Converts camelCase string to snake_case format safely.
     * Supports acronyms (DPSGenerator -> dps_generator) and digit transitions (Tm62 -> tm_62, M18Smoke -> m18_smoke).
     * Existing underscores are preserved so already snake_cased input stays unchanged.
     */
    @JvmStatic
    fun camelToSnake(camel: String): String {
        if (camel.isEmpty()) return camel
        val result = StringBuilder()
        for (i in camel.indices) {
            val ch = camel[i]
            if (i > 0) {
                val prev = camel[i - 1]
                val next = if (i + 1 < camel.length) camel[i + 1] else null
                val boundary =
                    // 大写跟在字母/数字后（普通驼峰边界）
                    (ch.isUpperCase() && (prev.isLowerCase() || prev.isDigit())) ||
                            // 连续大写简称的最后一个字母后接小写（如 DPSGenerator -> dps_generator）
                            (ch.isUpperCase() && prev.isUpperCase() && next != null && next.isLowerCase()) ||
                            // 数字跟在字母后（如 Tm62 -> tm_62）
                            (ch.isDigit() && prev.isLowerCase()) ||
                            // 字母跟在数字后（如 M18Smoke -> m18_smoke）
                            (ch.isLetter() && prev.isDigit())
                if (boundary && prev != '_' && next != '_') {
                    result.append('_')
                }
            }
            result.append(ch.lowercaseChar())
        }
        return result.toString()
    }
}
