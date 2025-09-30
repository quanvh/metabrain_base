package com.meta.brain.module.utils

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.google.gson.Gson
import com.meta.brain.module.data.UserData

class PrefUtil {
    companion object {
        const val NAME_PREFERENCES: String = "MB01_FileRecovery"

        fun getUserData(context: Context): String?{
            val pre = context.getSharedPreferences(NAME_PREFERENCES, Context.MODE_PRIVATE)
            val decodedBytes = Base64.decode(pre.getString(NAME_PREFERENCES + "_data",""), Base64.DEFAULT)
            return String(decodedBytes, Charsets.UTF_8);
        }

        fun setUserData(context: Context, userData: UserData){
            val stringData = Gson().toJson(userData)
            val encoded  = Base64.encodeToString(stringData.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
            val pre = context.getSharedPreferences(NAME_PREFERENCES, Context.MODE_PRIVATE)
            pre.edit {
                putString(NAME_PREFERENCES + "_data", encoded)
            }
        }
    }
}