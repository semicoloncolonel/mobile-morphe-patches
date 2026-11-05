package semicoloncolonel.mobilestream.patches.shared.misc

import app.morphe.patcher.patch.PatchException
import java.security.MessageDigest

// ─────────────────────────────────────────────────────────────────────────────
// A React Native app's Hermes bytecode bundle (assets/index.android.bundle),
// for patches that change what a named JavaScript function returns.
//
// Hermes keeps each function's source name in the bundle, so a function is
// found by name rather than by its position or bytes. Only bytecode version
// 96 is read; any other version fails the patch rather than guessing at a
// layout. After editing, the SHA-1 footer at the end of the file is
// recomputed.
//
// Layout (version 96): a 128-byte file header; one 16-byte header per
// function (a function whose fields do not fit is "overflowed", and its full
// header sits elsewhere in the file); then the string kinds, identifier
// hashes, the string table (4 bytes per string), the overflow string table
// (8 bytes per entry), and the string storage.
// ─────────────────────────────────────────────────────────────────────────────

internal class HermesBundle(private val bytes: ByteArray) {
    enum class Value(val opcode: Int) { UNDEFINED(118), NULL(119), TRUE(120), FALSE(121) }

    private class Function(val offset: Int, val size: Int, val name: String)

    private val functions: List<Function>

    init {
        if (bytes.size < 128 || u8(0) != 0xC6 || u8(1) != 0x1F) {
            throw PatchException("Not a Hermes bytecode bundle")
        }
        val version = u32(8)
        if (version != 96) throw PatchException("Unsupported Hermes bytecode version $version")

        val functionCount = u32(40)
        val stringKindCount = u32(44)
        val identifierCount = u32(48)
        val stringCount = u32(52)
        val overflowStringCount = u32(56)

        var pos = 128 + functionCount * 16
        pos = align(pos) + stringKindCount * 4
        pos = align(pos) + identifierCount * 4
        val stringTable = align(pos)
        val overflowTable = align(stringTable + stringCount * 4)
        val storage = align(overflowTable + overflowStringCount * 8)

        fun string(id: Int): String {
            val entry = u32(stringTable + id * 4)
            val utf16 = entry and 1 != 0
            var offset = (entry ushr 1) and 0x7FFFFF
            var length = entry ushr 24
            if (length == 0xFF) {
                val overflow = overflowTable + offset * 8
                offset = u32(overflow)
                length = u32(overflow + 4)
            }
            val start = storage + offset
            return if (utf16) String(bytes, start, length * 2, Charsets.UTF_16LE)
            else String(bytes, start, length, Charsets.ISO_8859_1)
        }

        functions = (0 until functionCount).map { id ->
            val header = 128 + id * 16
            val word0 = u32(header)
            val word1 = u32(header + 4)
            val overflowed = u8(header + 15) and 0x20 != 0
            if (overflowed) {
                val large = ((u32(header + 8) and 0x1FFFFFF) shl 16) or (word0 and 0x1FFFFFF)
                Function(u32(large), u32(large + 8), string(u32(large + 12)))
            } else {
                Function(word0 and 0x1FFFFFF, word1 and 0x7FFF, string(word1 ushr 15))
            }
        }
    }

    /**
     * Makes the one function named [name] return [value] at once: its first
     * four bytes become "LoadConst<value> r0; Ret r0". Every function has a
     * frame of at least one register, and the rest of its body is left
     * unreachable. Fails unless exactly one function has that name.
     */
    fun returnAtStart(name: String, value: Value) {
        val matches = functions.filter { it.name == name }
        if (matches.size != 1) {
            throw PatchException("Hermes function $name: expected 1, found ${matches.size}")
        }
        val function = matches.single()
        if (function.size < 4) throw PatchException("Hermes function $name is too short to patch")
        bytes[function.offset] = value.opcode.toByte()
        bytes[function.offset + 1] = 0
        bytes[function.offset + 2] = OP_RET.toByte()
        bytes[function.offset + 3] = 0
    }

    /** The bundle with its SHA-1 footer recomputed. */
    fun toByteArray(): ByteArray {
        val footer = MessageDigest.getInstance("SHA-1").digest(bytes.copyOfRange(0, bytes.size - 20))
        footer.copyInto(bytes, bytes.size - 20)
        return bytes
    }

    private fun u8(at: Int) = bytes[at].toInt() and 0xFF
    private fun u32(at: Int) = u8(at) or (u8(at + 1) shl 8) or (u8(at + 2) shl 16) or (u8(at + 3) shl 24)
    private fun align(at: Int) = (at + 3) and 3.inv()

    private companion object {
        const val OP_RET = 92
    }
}

internal const val HERMES_BUNDLE = "assets/index.android.bundle"
