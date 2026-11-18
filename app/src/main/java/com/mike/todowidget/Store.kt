package com.mike.todowidget

import android.content.Context

object Store {

    private const val PREF = "todo_prefs"
    private const val KEY = "tasks_json"

    fun load(ctx: Context): MutableList<Task> {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return Task.listFromJson(sp.getString(KEY, null))
    }

    fun save(ctx: Context, list: List<Task>) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, Task.listToJson(list))
            .apply()
    }

    fun find(ctx: Context, id: String?): Task? {
        if (id == null) return null
        return load(ctx).firstOrNull { it.id == id }
    }

    fun toggle(ctx: Context, id: String?) {
        val t = find(ctx, id) ?: return
        val list = load(ctx)
        val cur = list.firstOrNull { it.id == t.id } ?: return
        cur.done = !cur.done
        cur.doneAt = if (cur.done) System.currentTimeMillis() else 0L
        save(ctx, list)
    }

    fun upsert(ctx: Context, task: Task) {
        val list = load(ctx)
        val idx = list.indexOfFirst { it.id == task.id }
        if (idx >= 0) list[idx] = task else list.add(task)
        save(ctx, list)
    }

    fun remove(ctx: Context, id: String) {
        val list = load(ctx)
        list.removeAll { it.id == id }
        save(ctx, list)
    }
}
