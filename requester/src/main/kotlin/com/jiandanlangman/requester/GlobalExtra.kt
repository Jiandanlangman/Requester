package com.jiandanlangman.requester

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

private val alc = object : Application.ActivityLifecycleCallbacks {

    override fun onActivityDestroyed(activity: Activity) {
        Requester.cancelAll(this)
        if (activity is FragmentActivity)
            activity.supportFragmentManager.fragments.forEach { Requester.cancelAll(it) }
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

}

private val oascl = object : View.OnAttachStateChangeListener {

    override fun onViewDetachedFromWindow(v: View) {
        Requester.cancelAll(v)
        v.removeOnAttachStateChangeListener(this)
        addOASCLViews.remove(v)
    }

    override fun onViewAttachedToWindow(v: View?) = Unit

}

private val addOASCLViews = HashSet<View>()

private var isAddAlc = false

fun Activity.post(url: String): Request {
    addAlc(this)
    return Requester.post(url, this)
}

fun Activity.get(url: String): Request {
    addAlc(this)
    return Requester.get(url, this)
}

fun Fragment.post(url: String) = view?.post(url) ?: Requester.post(url, this)

fun Fragment.get(url: String) = view?.get(url) ?: Requester.get(url, this)

fun View.post(url: String): Request {
    addOASCL(this)
    return Requester.post(url, this)
}

fun View.get(url: String): Request {
    addOASCL(this)
    return Requester.get(url, this)
}

fun Dialog.post(url: String) = window!!.decorView.post(url)

fun Dialog.get(url: String) = window!!.decorView.get(url)

private fun addAlc(context: Context) {
    if (isAddAlc)
        return
    (context.applicationContext as Application).registerActivityLifecycleCallbacks(alc)
    isAddAlc = true
}

private fun addOASCL(view: View) {
    view.addOnAttachStateChangeListener(oascl)
    addOASCLViews.add(view)
}