package com.nexora.desktop.util

import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Post-MVP WhatsApp Business integration: Formats localized silk mandi messages
 * and launches WhatsApp Web / Desktop app using the standard `wa.me` protocol.
 */
object WhatsAppHelper {

    fun openChat(phone: String?, message: String) {
        val cleanPhone = phone?.filter(Char::isDigit)?.let {
            if (it.length == 10) "91$it" else it
        }.orEmpty()

        val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        val url = if (cleanPhone.isNotBlank()) {
            "https://wa.me/$cleanPhone?text=$encoded"
        } else {
            "https://wa.me/?text=$encoded"
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url))
            }
        } catch (ignored: Exception) {
        }
    }

    fun formatCustomerStatementMessage(
        customerName: String,
        outstanding: String,
        advance: String? = null,
        bankDetails: String? = null,
    ): String {
        val isTa = Strings.currentLocale() == "ta"
        return if (isTa) {
            """
                வணக்கம் $customerName,
                தங்களுடைய பட்டு நூல் கணக்கு நிலுவை விவரம்:
                • மொத்த நிலுவைத் தொகை: ₹$outstanding
                ${if (!advance.isNullOrBlank() && advance != "₹0.00") "• முன்பண இருப்பு: ₹$advance" else ""}
                
                தயவுசெய்து நிலுவைத் தொகையை வங்கி கணக்கு அல்லது UPI மூலம் செலுத்துமாறு கேட்டுக்கொள்கிறோம்.
                ${if (!bankDetails.isNullOrBlank()) "\nவங்கி விவரம்:\n$bankDetails" else ""}
                
                நன்றி!
                SmartSilk (Nexora)
            """.trimIndent()
        } else {
            """
                Dear $customerName,
                Here is your current silk account balance statement:
                • Total Outstanding: ₹$outstanding
                ${if (!advance.isNullOrBlank() && advance != "₹0.00") "• Advance Held: ₹$advance" else ""}
                
                Please remit the outstanding amount at your earliest convenience.
                ${if (!bankDetails.isNullOrBlank()) "\nBank / UPI Details:\n$bankDetails" else ""}
                
                Thank you!
                SmartSilk (Nexora)
            """.trimIndent()
        }
    }

    fun formatSalesBillMessage(
        customerName: String,
        orderNumber: String,
        product: String,
        weightKg: String,
        totalAmount: String,
    ): String {
        val isTa = Strings.currentLocale() == "ta"
        return if (isTa) {
            """
                வணக்கம் $customerName,
                தங்களுக்கான பட்டு நூல் ஆர்டர் பில் விவரம்:
                • ஆர்டர் எண்: $orderNumber
                • ரகம்: $product
                • அளவு: $weightKg kg
                • மொத்த தொகை: ₹$totalAmount
                
                பொருட்கள் அனுப்பப்பட்டுவிட்டன. நன்றி!
                SmartSilk (Nexora)
            """.trimIndent()
        } else {
            """
                Dear $customerName,
                Here are your sales bill details:
                • Order No: $orderNumber
                • Product: $product
                • Weight: $weightKg kg
                • Total Amount: ₹$totalAmount
                
                Goods dispatched. Thank you for your business!
                SmartSilk (Nexora)
            """.trimIndent()
        }
    }
}
