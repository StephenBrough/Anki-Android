package com.ichi2.utils

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import kotlinx.android.synthetic.main.my_account.*
import java.io.Serializable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Dismisses DialogFragments that have been added using their simple name
 */
inline fun <reified Dialog: DialogFragment>FragmentManager.dismissExisting() {
    (findFragmentByTag(Dialog::class.simpleName) as? Dialog)?.dismiss()
}

fun Fragment.nonNullArgs(): Bundle =
        arguments ?: Bundle().apply{ this@nonNullArgs.arguments = this }

class StringArg(val default: String = "") : ReadWriteProperty<Fragment, String> {
    override fun getValue(thisRef: Fragment, property: KProperty<*>): String = thisRef.nonNullArgs().getString(property.name, default)
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: String) = thisRef.nonNullArgs().putString(property.name, value)
}

class BooleanArg(val default: Boolean = false) : ReadWriteProperty<Fragment, Boolean> {
    override fun getValue(thisRef: Fragment, property: KProperty<*>): Boolean = thisRef.nonNullArgs().getBoolean(property.name, default)
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: Boolean) = thisRef.nonNullArgs().putBoolean(property.name, value)
}

class IntArg(val default: Int = -1) : ReadWriteProperty<Fragment, Int> {
    override fun getValue(thisRef: Fragment, property: KProperty<*>): Int = thisRef.nonNullArgs().getInt(property.name, default)
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: Int) = thisRef.nonNullArgs().putInt(property.name, value)
}

class LongArg(val default: Long = -1L) : ReadWriteProperty<Fragment, Long> {
    override fun getValue(thisRef: Fragment, property: KProperty<*>): Long = thisRef.nonNullArgs().getLong(property.name, default)
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: Long) = thisRef.nonNullArgs().putLong(property.name, value)
}

@Suppress("UNCHECKED_CAST")
class SerializableArg<T: Serializable>(val default: T) : ReadWriteProperty<Fragment, T> {
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T = thisRef.nonNullArgs().getSerializable(property.name) as? T ?: default
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) = thisRef.nonNullArgs().putSerializable(property.name, value)
}

class StringArrayListArg(val default: ArrayList<String> = ArrayList()) : ReadWriteProperty<Fragment, ArrayList<String>> {
    override fun getValue(thisRef: Fragment, property: KProperty<*>): ArrayList<String> = thisRef.nonNullArgs().getStringArrayList(property.name) ?: ArrayList()
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: ArrayList<String>) = thisRef.nonNullArgs().putStringArrayList(property.name, value)
}

fun Context.toast(msg: CharSequence) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Context.toastLong(msg: CharSequence) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}

fun Activity.hideSoftKeyboard() {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
}