package com.ichi2.upgrade

import com.ichi2.libanki.Collection

import org.json.JSONException
import org.json.JSONObject

object Upgrade {

    fun upgradeJSONIfNecessary(col: Collection, conf: JSONObject, name: String, defaultValue: Boolean): Boolean {
        var `val` = defaultValue
        try {
            `val` = conf.getBoolean(name)
        } catch (e: JSONException) {
            // workaround to repair wrong values from older libanki versions
            try {
                conf.put(name, `val`)
            } catch (e1: JSONException) {
                // do nothing
            }

            col.save()
        }

        return `val`
    }
}
