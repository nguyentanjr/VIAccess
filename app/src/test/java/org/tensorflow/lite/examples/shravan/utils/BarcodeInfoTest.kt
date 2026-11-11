package org.tensorflow.lite.examples.shravan.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class BarcodeInfoTest {

    private fun tlv(id: String, value: String) = id + "%02d".format(value.length) + value

    @Test
    fun linkWifiPhoneTextAndProduct() {
        assertEquals(BarcodeInfo.Link("shopee.vn"), BarcodeInfo.parse("https://www.shopee.vn/abc?x=1"))
        assertEquals(BarcodeInfo.Wifi("Nha-Tro", "12345678"), BarcodeInfo.parse("WIFI:T:WPA;S:Nha-Tro;P:12345678;;"))
        assertEquals(BarcodeInfo.Wifi("Cafe", null), BarcodeInfo.parse("WIFI:T:nopass;S:Cafe;;"))
        assertEquals(BarcodeInfo.Phone("0912345678"), BarcodeInfo.parse("tel:0912345678"))
        assertEquals(BarcodeInfo.Text("Bàn số 5"), BarcodeInfo.parse("Bàn số 5"))
        assertEquals(BarcodeInfo.Product("8934563138165"), BarcodeInfo.parse("8934563138165", isProductCode = true))
    }

    @Test
    fun vietQrExtractsAccountAmountAndName() {
        val consumer = tlv("00", "970436") + tlv("01", "1234567890")
        val merchant = tlv("00", "A000000727") + tlv("01", consumer) + tlv("02", "QRIBFTTA")
        val raw = tlv("00", "01") + tlv("01", "12") + tlv("38", merchant) + tlv("53", "704") +
            tlv("54", "50000") + tlv("58", "VN") + tlv("59", "NGUYEN VAN A") + tlv("63", "ABCD")
        assertEquals(BarcodeInfo.VietQr("NGUYEN VAN A", "1234567890", "50000"), BarcodeInfo.parse(raw))
    }

    @Test
    fun malformedVietQrFallsBackToText() {
        assertEquals(BarcodeInfo.Text("000201xx"), BarcodeInfo.parse("000201xx"))
    }
}
