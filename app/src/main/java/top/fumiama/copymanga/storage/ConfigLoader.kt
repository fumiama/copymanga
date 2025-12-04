package top.fumiama.copymanga.storage

import top.fumiama.copymanga.api.Config
import java.nio.ByteBuffer
import java.util.zip.CRC32

class ConfigLoader {
    data class Settings(
        var appVer: String,
        var platform: String,
        var generalEnableTransparentSystemBar: Boolean,
        var generalDisableKanbanAnimation: Boolean,
        var generalCardPerRow: Int,

        var mangaDlMaxBatch: Int,
        var mangaDlShow0mManga: Boolean,

        var reverseProxyUrl: String,
        var networkApiUrl: String,
        var proxyKey: String,

        var netUseGzip: Boolean,
        var netUseJson: Boolean,
        var netPlatform: Boolean,
        var netReferer: Boolean,
        var netVersion: Boolean,
        var netRegion: Boolean,
        var netNoWebp: Boolean,
        var netUseComandy: Boolean,
        var netUseForeign: Boolean,
        var netUseImgProxy: Boolean,
        var netUseApiProxy: Boolean,

        var netImgResolution: String,
        var netUmstring: String,
        var netSource: String,
        var netUa: String,

        var viewMangaInverseChapters: Boolean,
        var viewMangaAlwaysDarkBg: Boolean,
        var viewMangaVerticalMax: Int,
        var viewMangaQuality: Int,
        var viewMangaVolTurn: Boolean,
        var viewMangaUseCellar: Boolean,
        var viewMangaHideInfo: Boolean,
    ) {
        fun export() {
            Config.version.value = appVer
            Config.platform.value = platform
            Config.general_enable_transparent_system_bar.value = generalEnableTransparentSystemBar
            Config.general_disable_kanban_animation.value = generalDisableKanbanAnimation
            Config.general_card_per_row.value = generalCardPerRow

            Config.manga_dl_max_batch.value = mangaDlMaxBatch
            Config.manga_dl_show_0m_manga.value = mangaDlShow0mManga

            Config.reverseProxyUrl.value = reverseProxyUrl
            Config.networkApiUrl.value = networkApiUrl
            Config.proxy_key.value = proxyKey

            Config.net_use_gzip.value = netUseGzip
            Config.net_use_json.value = netUseJson
            Config.net_platform.value = netPlatform
            Config.net_referer.value = netReferer
            Config.net_version.value = netVersion
            Config.net_region.value = netRegion
            Config.net_no_webp.value = netNoWebp
            Config.net_use_comandy.value = netUseComandy
            Config.net_use_foreign.value = netUseForeign
            Config.net_use_img_proxy.value = netUseImgProxy
            Config.net_use_api_proxy.value = netUseApiProxy

            Config.net_img_resolution.value = netImgResolution
            Config.net_umstring.value = netUmstring
            Config.net_source.value = netSource
            Config.net_ua.value = netUa

            Config.view_manga_inverse_chapters.value = viewMangaInverseChapters
            Config.view_manga_always_dark_bg.value = viewMangaAlwaysDarkBg
            Config.view_manga_vertical_max.value = viewMangaVerticalMax
            Config.view_manga_quality.value = viewMangaQuality
            Config.view_manga_vol_turn.value = viewMangaVolTurn
            Config.view_manga_use_cellar.value = viewMangaUseCellar
            Config.view_manga_hide_info.value = viewMangaHideInfo
        }
    }

    val settings: Settings

    constructor(): this(Settings(
        appVer = Config.version.value,
        platform = Config.platform.value,
        generalEnableTransparentSystemBar = Config.general_enable_transparent_system_bar.value,
        generalDisableKanbanAnimation = Config.general_disable_kanban_animation.value,
        generalCardPerRow = Config.general_card_per_row.value,

        mangaDlMaxBatch = Config.manga_dl_max_batch.value,
        mangaDlShow0mManga = Config.manga_dl_show_0m_manga.value,

        reverseProxyUrl = Config.reverseProxyUrl.value,
        networkApiUrl = Config.networkApiUrl.value,
        proxyKey = Config.proxy_key.value,

        netUseGzip = Config.net_use_gzip.value,
        netUseJson = Config.net_use_json.value,
        netPlatform = Config.net_platform.value,
        netReferer = Config.net_referer.value,
        netVersion = Config.net_version.value,
        netRegion = Config.net_region.value,
        netNoWebp = Config.net_no_webp.value,
        netUseComandy = Config.net_use_comandy.value,
        netUseForeign = Config.net_use_foreign.value,
        netUseImgProxy = Config.net_use_img_proxy.value,
        netUseApiProxy = Config.net_use_api_proxy.value,

        netImgResolution = Config.net_img_resolution.value,
        netUmstring = Config.net_umstring.value,
        netSource = Config.net_source.value,
        netUa = Config.net_ua.value,

        viewMangaInverseChapters = Config.view_manga_inverse_chapters.value,
        viewMangaAlwaysDarkBg = Config.view_manga_always_dark_bg.value,
        viewMangaVerticalMax = Config.view_manga_vertical_max.value,
        viewMangaQuality = Config.view_manga_quality.value,
        viewMangaVolTurn = Config.view_manga_vol_turn.value,
        viewMangaUseCellar = Config.view_manga_use_cellar.value,
        viewMangaHideInfo = Config.view_manga_hide_info.value,
    ))

    constructor(settings: Settings) {
        this.settings = settings
    }

    constructor(data: ByteArray) {
        if (data.size < 4) throw IllegalArgumentException("Data too short for CRC")
        val payload = data.copyOfRange(0, data.size - 4)
        val expectedCrc = ByteBuffer.wrap(data, data.size - 4, 4).int
        val actualCrc = CRC32().apply { update(payload) }.value.toInt()
        if (expectedCrc != actualCrc) throw IllegalArgumentException("CRC32 check failed")

        val buffer = ByteBuffer.wrap(payload)

        val netFlags = buffer.short.toInt() and 0xFFFF
        val miscFlags = buffer.get().toInt() and 0xFF
        fun bit(value: Int, pos: Int) = ((value shr pos) and 1) != 0

        val generalCardPerRow = buffer.int
        val mangaDlMaxBatch = buffer.int
        val viewMangaVerticalMax = buffer.int
        val viewMangaQuality = buffer.int

        fun readString(): String {
            val len = buffer.short.toInt()
            val bytes = ByteArray(len)
            buffer.get(bytes)
            return String(bytes, Charsets.UTF_8)
        }

        settings = Settings(
            appVer = readString(),
            platform = readString(),
            generalEnableTransparentSystemBar = bit(miscFlags, 7),
            generalDisableKanbanAnimation = bit(miscFlags, 6),
            generalCardPerRow = generalCardPerRow,

            mangaDlMaxBatch = mangaDlMaxBatch,
            mangaDlShow0mManga = bit(miscFlags, 5),

            reverseProxyUrl = readString(),
            networkApiUrl = readString(),
            proxyKey = readString(),

            netUseGzip = bit(netFlags, 15),
            netUseJson = bit(netFlags, 14),
            netPlatform = bit(netFlags, 13),
            netReferer = bit(netFlags, 12),
            netVersion = bit(netFlags, 11),
            netRegion = bit(netFlags, 10),
            netNoWebp = bit(netFlags, 9),
            netUseComandy = bit(netFlags, 8),
            netUseForeign = bit(netFlags, 7),
            netUseImgProxy = bit(netFlags, 6),
            netUseApiProxy = bit(netFlags, 5),

            netImgResolution = readString(),
            netUmstring = readString(),
            netSource = readString(),
            netUa = readString(),

            viewMangaInverseChapters = bit(miscFlags, 4),
            viewMangaAlwaysDarkBg = bit(miscFlags, 3),
            viewMangaVerticalMax = viewMangaVerticalMax,
            viewMangaQuality = viewMangaQuality,
            viewMangaVolTurn = bit(miscFlags, 2),
            viewMangaUseCellar = bit(miscFlags, 1),
            viewMangaHideInfo = bit(miscFlags, 0),
        )
    }

    fun toByteArray(): ByteArray {
        val strFields = listOf(
            settings.appVer, settings.platform,
            settings.reverseProxyUrl, settings.networkApiUrl, settings.proxyKey,
            settings.netImgResolution, settings.netUmstring, settings.netSource, settings.netUa
        )
        val strBytes = strFields.map { it.toByteArray(Charsets.UTF_8) }

        val netFlags = wrapBooleans(
            settings.netUseGzip,
            settings.netUseJson,
            settings.netPlatform,
            settings.netReferer,
            settings.netVersion,
            settings.netRegion,
            settings.netNoWebp,
            settings.netUseComandy,
            settings.netUseForeign,
            settings.netUseImgProxy,
            settings.netUseApiProxy,
            false, false, false, false, false
        )
        val miscFlags = wrapBooleans(
            settings.generalEnableTransparentSystemBar,
            settings.generalDisableKanbanAnimation,
            settings.mangaDlShow0mManga,
            settings.viewMangaInverseChapters,
            settings.viewMangaAlwaysDarkBg,
            settings.viewMangaVolTurn,
            settings.viewMangaUseCellar,
            settings.viewMangaHideInfo
        )

        val strTotal = strBytes.sumOf { 2 + it.size }
        val buffer = ByteBuffer.allocate(
            2 +     // netFlags: UShort (16 bits)
                    1 +     // miscFlags: UByte (8 bits)
                    4 * 4 + // Ints: 4 fields x 4 bytes
                    strTotal + // all string fields with 2-byte length prefix
                    4       // CRC32
        )
        buffer.putShort(netFlags.toShort())
        buffer.put(miscFlags.toByte())
        buffer.putInt(settings.generalCardPerRow)
        buffer.putInt(settings.mangaDlMaxBatch)
        buffer.putInt(settings.viewMangaVerticalMax)
        buffer.putInt(settings.viewMangaQuality)
        for (b in strBytes) {
            buffer.putShort(b.size.toShort())
            buffer.put(b)
        }

        val payload = buffer.array().copyOfRange(0, buffer.position())
        val crc = CRC32().apply { update(payload) }.value
        val finalBuffer = ByteBuffer.allocate(payload.size + 4)
        finalBuffer.put(payload)
        finalBuffer.putInt(crc.toInt())

        return finalBuffer.array()
    }

    private fun wrapBooleans(
        b0: Boolean, b1: Boolean, b2: Boolean, b3: Boolean,
        b4: Boolean, b5: Boolean, b6: Boolean, b7: Boolean
    ): UByte {
        var result: UByte = 0u
        val flags = listOf(b0, b1, b2, b3, b4, b5, b6, b7)
        for ((i, flag) in flags.withIndex()) {
            if (flag) result = result or (1u shl (7 - i)).toUByte()
        }
        return result
    }

    private fun wrapBooleans(
        b0: Boolean, b1: Boolean, b2: Boolean, b3: Boolean,
        b4: Boolean, b5: Boolean, b6: Boolean, b7: Boolean,
        b8: Boolean, b9: Boolean, b10: Boolean, b11: Boolean,
        b12: Boolean, b13: Boolean, b14: Boolean, b15: Boolean
    ): UShort {
        var result: UShort = 0u
        val flags = listOf(
            b0, b1, b2, b3, b4, b5, b6, b7,
            b8, b9, b10, b11, b12, b13, b14, b15
        )
        for ((i, flag) in flags.withIndex()) {
            if (flag) result = result or (1u shl (15 - i)).toUShort()
        }
        return result
    }

}