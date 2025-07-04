package top.fumiama.copymanga.strings

import android.util.Base64
import android.util.Log

object Base16384 {
    private const val BASE = 16384
    private const val FIRST_CHAR = 0x4E00
    private const val PAD_START = 0x3D00

    fun encode(input: ByteArray): String {
        val sb = StringBuilder()
        var buffer = 0
        var bLen = 0

        for (b in input) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bLen += 8
            while (bLen >= 14) {
                bLen -= 14
                val index = (buffer shr bLen) and (BASE - 1)
                sb.append(Char(FIRST_CHAR + index))
                buffer = buffer and ((1 shl bLen) - 1)
            }
        }

        if (bLen > 0) {
            buffer = (buffer shl (14 - bLen))
            val index = buffer and (BASE - 1)
            sb.append(Char(FIRST_CHAR + index))
            val padIndex = PAD_START + (input.size%7)
            sb.append(Char(padIndex))
        }

        Log.d("MyB14", "encode offset ${input.size%7}, len ${input.size}, data: ${Base64.encode(input, Base64.DEFAULT).decodeToString()}")

        return sb.toString()
    }

    fun decode(input: String): ByteArray {
        val bits = mutableListOf<Boolean>()
        var offset = 0
        for (c in input) {
            when (val code = c.code) {
                in FIRST_CHAR until FIRST_CHAR + BASE -> {
                    val idx = code - FIRST_CHAR
                    for (i in 13 downTo 0) {
                        bits.add(((idx shr i) and 1) == 1)
                    }
                }
                in (PAD_START + 1)..(PAD_START + 6) -> {
                    offset = code - PAD_START
                    break
                }
                else -> throw IllegalArgumentException("Invalid base16384 char: $c")
            }
        }

        val dLen = input.toByteArray(Charsets.UTF_16BE).size
        val outLen = when (offset) {
            0 -> dLen
            1 -> dLen - 4
            2, 3 -> dLen - 6
            4, 5 -> dLen - 8
            6 -> dLen - 10
            else -> dLen
        }/8*7 + offset

        val out = ByteArray(outLen)
        for (i in 0 until outLen) {
            var byteVal = 0
            for (j in 0 until 8) {
                if (bits[i * 8 + j]) byteVal = byteVal or (1 shl (7 - j))
            }
            out[i] = byteVal.toByte()
        }

        Log.d("MyB14", "decode offset $offset, len ${out.size}, data: ${Base64.encode(out, Base64.DEFAULT).decodeToString()}")
        return out
    }
}
