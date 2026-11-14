package com.kiraya.diary.core.phone

object PhoneNumbers {
    /**
     * International digits for WhatsApp, assuming Pakistan for local numbers.
     * "0300 1234567" -> "923001234567", "+92 300..." -> "92300...". Null if too short.
     */
    fun toInternational(phone: String): String? {
        var d = phone.filter(Char::isDigit)
        if (phone.trim().startsWith("00")) d = d.drop(2)
        d = when {
            d.startsWith("92") -> d
            d.startsWith("0") -> "92" + d.drop(1)
            d.length == 10 && d.startsWith("3") -> "92$d"
            else -> d
        }
        return d.takeIf { it.length >= 10 }
    }
}
