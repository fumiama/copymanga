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
        var bitCount = 0

        for (b in input) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitCount += 8
            while (bitCount >= 14) {
                bitCount -= 14
                val index = (buffer shr bitCount) and (BASE - 1)
                sb.append(Char(FIRST_CHAR + index))
                buffer = buffer and ((1 shl bitCount) - 1)
            }
        }

        if (bitCount in 1..6) {
            buffer = (buffer shl (14 - bitCount))
            val index = buffer and (BASE - 1)
            sb.append(Char(FIRST_CHAR + index))
            val padIndex = PAD_START + bitCount
            sb.append(Char(padIndex))
        }

        Log.d("MyB14", "encode bitCount $bitCount, len ${input.size}, data: ${Base64.encode(input, Base64.DEFAULT).decodeToString()}")

        return sb.toString()
    }

    fun decode(input: String): ByteArray {
        val bits = mutableListOf<Boolean>()
        var tailBits = 0
        for (c in input) {
            when (val code = c.code) {
                in FIRST_CHAR until FIRST_CHAR + BASE -> {
                    val idx = code - FIRST_CHAR
                    for (i in 13 downTo 0) {
                        bits.add(((idx shr i) and 1) == 1)
                    }
                }
                in (PAD_START + 1)..(PAD_START + 6) -> {
                    tailBits = code - PAD_START
                    break
                }
                else -> throw IllegalArgumentException("Invalid base16384 char: $c")
            }
        }

        val totalBits = bits.size - (14 - tailBits)
        val byteCount = totalBits / 8
        val out = ByteArray(byteCount)
        for (i in 0 until byteCount) {
            var byteVal = 0
            for (j in 0 until 8) {
                if (bits[i * 8 + j]) byteVal = byteVal or (1 shl (7 - j))
            }
            out[i] = byteVal.toByte()
        }

        Log.d("MyB14", "decode totalBits $totalBits, tailBits $tailBits, len ${out.size}, data: ${Base64.encode(out, Base64.DEFAULT).decodeToString()}")
        return out
    }
}
