package com.simplemobiletools.smsmessenger.models

import android.content.Context
import com.simplemobiletools.commons.extensions.normalizePhoneNumber
import com.simplemobiletools.smsmessenger.R
import com.simplemobiletools.smsmessenger.extensions.config
import com.simplemobiletools.smsmessenger.extensions.format
import ezvcard.VCard
import ezvcard.property.*

private val displayedPropertyClasses = arrayOf(
    Telephone::class.java, Email::class.java, Organization::class.java, Birthday::class.java, Anniversary::class.java, Note::class.java
)

data class VCardWrapper(val vCard: VCard, var expanded: Boolean = false) {

    fun getFullName(): String? {
        var formattedName = vCard.formattedName?.value
        if (formattedName.isNullOrEmpty()) {
            val structured = vCard.structuredName
            val given = structured?.given
            val family = structured.family
            formattedName = if (family != null) {
                given?.plus(" ")?.plus(family)
            } else {
                given
            }
        }
        return formattedName
    }

    fun getVCardProperties(context: Context): List<VCardPropertyWrapper> {
        return vCard.properties
            .filter { displayedPropertyClasses.contains(it::class.java) }
            .map { VCardPropertyWrapper.from(context, it) }
            .distinctBy { it.value }
    }
}

data class VCardPropertyWrapper(val value: String, val type: String, val property: VCardProperty) {

    companion object {
        private const val CELL = "CELL"
        private const val HOME = "HOME"
        private const val WORK = "WORK"

        private fun VCardProperty.getPropertyTypeString(context: Context): String {
            return when (parameters.type) {
                CELL -> context.getString(R.string.mobile)
                HOME -> context.getString(R.string.home)
                WORK -> context.getString(R.string.work)
                else -> ""
            }
        }

        fun from(context: Context, property: VCardProperty): VCardPropertyWrapper {
            return property.run {
                when (this) {
                    is Telephone -> VCardPropertyWrapper(text.normalizePhoneNumber(), getPropertyTypeString(context), property)
                    is Email -> VCardPropertyWrapper(value, getPropertyTypeString(context), property)
                    is Organization -> VCardPropertyWrapper(values.joinToString(), context.getString(R.string.work), property)
                    is Birthday -> VCardPropertyWrapper(date.format(context.config.dateFormat), context.getString(R.string.birthday), property)
                    is Anniversary -> VCardPropertyWrapper(date.format(context.config.dateFormat), context.getString(R.string.anniversary), property)
                    is Note -> VCardPropertyWrapper(value, context.getString(R.string.notes), property)
                    else -> VCardPropertyWrapper("", "", property)
                }
            }
        }
    }
}
