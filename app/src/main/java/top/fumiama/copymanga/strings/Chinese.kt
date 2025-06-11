package top.fumiama.copymanga.strings

object Chinese {
    /**
     * 如果输入字符串中已包含汉字，直接返回。
     * 否则尝试以 UTF8 重新解码。
     */
    fun fixEncodingIfNeeded(input: String): String {
        // 如果本身包含汉字，直接返回
        if (containsChinese(input)) {
            return input
        }

        return try {
            val decoded = input.toByteArray(Charsets.ISO_8859_1).decodeToString()

            // 检测解码后的是否包含汉字
            if (containsChinese(decoded)) {
                decoded
            } else {
                input // 解码后也没有汉字，说明可能不是编码问题
            }
        } catch (e: Exception) {
            input
        }
    }

    /**
     * 简单检测字符串是否包含常见 CJK（中日韩）汉字。
     */
    private fun containsChinese(text: String): Boolean {
        val regex = Regex("[\u4E00-\u9FFF]")
        return regex.containsMatchIn(text)
    }
}