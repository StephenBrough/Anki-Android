package com.ichi2.anki.widgets

import java.lang.reflect.Field
import java.lang.reflect.Method
import android.content.Context
import android.support.v7.widget.PopupMenu
import android.view.View

/**
 * A simple little hack to force the icons to display in the PopupMenu
 */
class PopupMenuWithIcons(context: Context, anchor: View, showIcons: Boolean) : PopupMenu(context, anchor) {

    init {
        if (showIcons) {
            try {
                val fields = PopupMenu::class.java.declaredFields
                for (field in fields) {
                    if ("mPopup" == field.name) {
                        field.isAccessible = true
                        val menuPopupHelper = field.get(this)
                        val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                        val setForceIcons = classPopupHelper.getMethod(
                                "setForceShowIcon", Boolean::class.javaPrimitiveType)
                        setForceIcons.invoke(menuPopupHelper, true)
                        break
                    }
                }
            } catch (e: Exception) {
            }

        }
    }
}