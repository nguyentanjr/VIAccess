package org.tensorflow.lite.examples.shravan.utils

import java.net.URI

/** Nội dung mã QR / mã vạch đã được phân loại để đọc cho người dùng (không phụ thuộc ML Kit để test được). */
sealed class BarcodeInfo {
    data class Link(val host: String) : BarcodeInfo()
    data class Wifi(val ssid: String, val password: String?) : BarcodeInfo()
    data class Phone(val number: String) : BarcodeInfo()
    data class VietQr(val name: String?, val account: String?, val amount: String?) : BarcodeInfo()
    data class Product(val digits: String) : BarcodeInfo()
    data class Text(val text: String) : BarcodeInfo()

    companion object {
        /**
         * @param raw nội dung thô của mã
         * @param isProductCode true với mã vạch 1D trên bao bì (EAN / UPC)
         */
        fun parse(raw: String, isProductCode: Boolean = false): BarcodeInfo {
            val v = raw.trim()
            if (isProductCode) return Product(v)
            if (v.startsWith("000201")) parseVietQr(v)?.let { return it }
            val lower = v.lowercase()
            if (lower.startsWith("http://") || lower.startsWith("https://")) {
                val host = runCatching { URI(v).host }.getOrNull()?.removePrefix("www.")
                return Link(host ?: v)
            }
            if (lower.startsWith("wifi:")) return parseWifi(v)
            if (lower.startsWith("tel:")) return Phone(v.substring(4))
            return Text(v)
        }

        /** WIFI:T:WPA;S:ten-mang;P:mat-khau;; */
        private fun parseWifi(v: String): BarcodeInfo {
            val fields = v.substring(5).split(';').mapNotNull {
                val i = it.indexOf(':')
                if (i <= 0) null else it.substring(0, i).uppercase() to it.substring(i + 1)
            }.toMap()
            return Wifi(fields["S"].orEmpty(), fields["P"]?.takeIf { it.isNotEmpty() })
        }

        /** Đọc chuỗi TLV kiểu EMVCo: 2 số ID + 2 số độ dài + giá trị. */
        fun parseTlv(s: String): Map<String, String>? {
            val out = linkedMapOf<String, String>()
            var i = 0
            while (i < s.length) {
                if (i + 4 > s.length) return null
                val id = s.substring(i, i + 2)
                val len = s.substring(i + 2, i + 4).toIntOrNull() ?: return null
                if (i + 4 + len > s.length) return null
                out[id] = s.substring(i + 4, i + 4 + len)
                i += 4 + len
            }
            return out
        }

        /** VietQR (NAPAS): tag 38 → 01 → 01 là số tài khoản; 54 số tiền; 59 tên người nhận (nếu có). */
        private fun parseVietQr(v: String): VietQr? {
            val top = parseTlv(v) ?: return null
            val consumer = top["38"]?.let { parseTlv(it) }?.get("01")?.let { parseTlv(it) }
            val account = consumer?.get("01")
            if (account == null && top["54"] == null && top["59"] == null) return null
            return VietQr(top["59"], account, top["54"])
        }
    }
}
