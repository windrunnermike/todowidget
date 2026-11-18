package com.mike.todowidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 点击组件里的某一行：切换完成状态，然后立刻重绘所有组件 */
class ToggleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE) return
        val id = intent.getStringExtra(EXTRA_ID)
        Store.toggle(context, id)
        WidgetRenderer.refreshAll(context)
    }

    companion object {
        const val ACTION_TOGGLE = "com.mike.todowidget.action.TOGGLE"
        const val EXTRA_ID = "extra_id"
    }
}

/** 开机 / 解锁后重排闹钟并重绘，否则重启后提醒会丢 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            WidgetRenderer.refreshAll(context)
            Reminder.schedule(context)
        }
    }
}
