package com.meta.brain.module.data

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.meta.brain.module.base.MetaBrainApp
import com.meta.brain.module.utils.PrefUtil
import java.time.LocalDateTime


class DataManager {
    companion object {
        lateinit var user: UserData

        fun init(context: Context){
            val userString = PrefUtil.getUserData(context)
            if(userString == null || userString.isEmpty()){
                if(MetaBrainApp.debug) {
                    Log.d("[DataManager]", "Init new data success");
                }
                user = UserData()
            }
            else{
                user = Gson().fromJson(userString, UserData::class.java)
                if(MetaBrainApp.debug) {
                    Log.d("[DataManager]", "Get user data success");
                }
            }
        }

        fun saveData(context: Context){
            PrefUtil.setUserData(context,user)
        }
    }
}

class UserData {
    var userName = ""
    var showRate = false
    var chooseLang = false
    var language = ""
    var chooseAge = false
    var age = 0
    var dayLogin = 0
    var firstOpen = true;
    var organic =false
    var country = ""
    var recoverCount = 0

    var removeAds = false
    var expireRemoveAds: LocalDateTime? = null
    @RequiresApi(Build.VERSION_CODES.O)
    var lastLogin: LocalDateTime? = null
}