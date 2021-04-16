package com.example.myapplication.app

import android.content.Context
import androidx.multidex.MultiDex
import com.alibaba.android.arouter.launcher.ARouter
import com.example.myapplication.R
import com.example.myapplication.ui.login.LoginActivity
import me.goldze.mvvmhabit.BuildConfig
import me.goldze.mvvmhabit.base.BaseApplication
import me.goldze.mvvmhabit.crash.CaocConfig
import me.goldze.mvvmhabit.utils.KLog

/**
 * Created by goldze on 2017/7/16.
 */
class AppApplication : BaseApplication() {

    companion object {
        /**
         * 获得上下文对象
         *
         * @return
         */
        var appContext: Context? = null
        private var apps: Apps? = null
    }


    override fun onCreate() {
        super.onCreate()
        // 解决方法大于65535问题
        MultiDex.install(this)
        //是否开启打印日志
        KLog.init(BuildConfig.DEBUG)
        //初始化全局异常崩溃
        initCrash()
        appContext = applicationContext
        ARouter.init(this)
    }

    private fun initCrash() {
        CaocConfig.Builder.create()
            .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT) //背景模式,开启沉浸式
            .enabled(true) //是否启动全局异常捕获
            .showErrorDetails(true) //是否显示错误详细信息
            .showRestartButton(true) //是否显示重启按钮
            .trackActivities(true) //是否跟踪Activity
            .minTimeBetweenCrashesMs(2000) //崩溃的间隔时间(毫秒)
            .errorDrawable(R.mipmap.ic_launcher) //错误图标
            .restartActivity(LoginActivity::class.java) //重新启动后的activity
            //                .errorActivity(YourCustomErrorActivity.class) //崩溃后的错误activity
            //                .eventListener(new YourCustomEventListener()) //崩溃后的错误监听
            .apply()
    }

}