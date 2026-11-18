package com.mike.todowidget

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class MainActivity : Activity() {

    private lateinit var listBox: LinearLayout
    private lateinit var subTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Reminder.ensureChannel(this)
        askNotificationPermission()
        setContentView(buildUi())
        if (intent?.getStringExtra(EXTRA_MODE) == MODE_NEW) {
            intent?.removeExtra(EXTRA_MODE)
            showEditor(null)
        }
    }

    override fun onResume() {
        super.onResume()
        rebuild()
        WidgetRenderer.refreshAll(this)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
    }

    // ---------------------------------------------------------------- 界面骨架
    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.page_bg))
            setPadding(dp(18), dp(30), dp(18), dp(12))
        }

        val title = TextView(this).apply {
            text = "我的待办"
            textSize = 24f
            setTextColor(getColor(R.color.text_primary))
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(title)

        subTitle = TextView(this).apply {
            textSize = 12f
            setTextColor(getColor(R.color.text_secondary))
        }
        root.addView(subTitle, lp(matchWidth = true, height = -2, top = dp(4)))

        val scroll = ScrollView(this)
        listBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(12))
        }
        scroll.addView(listBox)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val addBtn = TextView(this).apply {
            text = "＋ 新建待办"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.white))
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.pill)
            setOnClickListener { showEditor(null) }
        }
        root.addView(addBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)))

        return root
    }

    // ---------------------------------------------------------------- 列表
    private fun rebuild() {
        val all = Store.load(this)
        val now = System.currentTimeMillis()
        listBox.removeAllViews()
        subTitle.text = Fmt.summary(all, now) + " · " + Fmt.doneCount(all, now)

        if (all.isEmpty()) {
            listBox.addView(hint("还没有待办，点下面「＋ 新建待办」"))
            return
        }

        val today = Fmt.todayRows(all, now, 100)
        val longs = Fmt.longRows(all, now, 100)

        listBox.addView(sectionHeader(getString(R.string.today_label), getColor(R.color.accent)))
        today.forEach { listBox.addView(rowView(it, now)) }

        listBox.addView(sectionHeader(getString(R.string.long_label), getColor(R.color.orange)))
        longs.forEach { listBox.addView(rowView(it, now)) }
    }

    private fun sectionHeader(text: String, color: Int): View = TextView(this).apply {
        this.text = text
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(color)
        setPadding(0, dp(14), 0, dp(6))
    }

    private fun hint(text: String): View = TextView(this).apply {
        this.text = text
        textSize = 13f
        gravity = Gravity.CENTER
        setTextColor(getColor(R.color.text_tertiary))
        setPadding(0, dp(40), 0, dp(40))
    }

    private fun rowView(t: Task, now: Long): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundResource(R.drawable.row_bg)
        }

        val check = TextView(this).apply {
            text = if (t.done) "✓" else "○"
            textSize = 16f
            setTextColor(if (t.done) getColor(R.color.accent) else getColor(R.color.text_tertiary))
        }
        row.addView(check, LinearLayout.LayoutParams(dp(24), ViewGroup.LayoutParams.WRAP_CONTENT))

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val title = TextView(this).apply {
            text = t.title
            textSize = 15f
            setTextColor(if (t.done) getColor(R.color.text_tertiary) else getColor(R.color.text_primary))
            if (t.done) paintFlags = Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG
        }
        val sub = TextView(this).apply {
            text = Fmt.subOf(t, now)
            textSize = 11f
            setTextColor(getColor(R.color.text_tertiary))
        }
        box.addView(title)
        box.addView(sub)
        row.addView(box, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val c = Fmt.chipOf(this, t, now)
        val chip = TextView(this).apply {
            text = if (t.done) "" else c.text
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(7), dp(3), dp(7), dp(3))
            setTextColor(c.fgColor)
            setBackgroundResource(c.bgRes)
        }
        row.addView(chip)

        row.setOnClickListener {
            Store.toggle(this, t.id)
            rebuild()
            WidgetRenderer.refreshAll(this)
        }
        row.setOnLongClickListener {
            showActions(t)
            true
        }
        listBoxRowMargin(row)
        return row
    }

    private fun listBoxRowMargin(row: View) {
        (row.layoutParams as? LinearLayout.LayoutParams)?.let {
            it.bottomMargin = dp(8)
        } ?: run {
            row.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }
    }

    // ---------------------------------------------------------------- 操作
    private fun showActions(t: Task) {
        val items = arrayOf(
            if (t.done) "标记为未完成" else "标记为完成",
            "编辑",
            if (t.deadline > 0L) "清除截止时间" else "设置截止时间",
            "删除"
        )
        AlertDialog.Builder(this)
            .setTitle(t.title)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        Store.toggle(this, t.id)
                        rebuild()
                        WidgetRenderer.refreshAll(this)
                    }
                    1 -> showEditor(t)
                    2 -> {
                        val list = Store.load(this)
                        list.firstOrNull { it.id == t.id }?.let {
                            it.deadline = if (t.deadline > 0L) 0L else nextHour()
                            Store.save(this, list)
                        }
                        rebuild()
                        WidgetRenderer.refreshAll(this)
                    }
                    else -> {
                        Store.remove(this, t.id)
                        rebuild()
                        WidgetRenderer.refreshAll(this)
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditor(existing: Task?) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }

        val titleEt = EditText(this).apply {
            hint = "要做什么？"
            setText(existing?.title ?: "")
        }
        box.addView(titleEt)

        val group = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val rToday = RadioButton(this).apply { text = "每日待办"; id = View.generateViewId() }
        val rLong = RadioButton(this).apply { text = "长期待办"; id = View.generateViewId() }
        group.addView(rToday)
        group.addView(rLong)
        if (existing?.isLong() == true) rLong.isChecked = true else rToday.isChecked = true
        box.addView(group)

        val counterEt = EditText(this).apply {
            hint = "进度，例如 6/12 章（可留空）"
            setText(existing?.counter ?: "")
        }
        box.addView(counterEt)

        var pendingDeadline: Long = existing?.deadline ?: 0L

        val deadlineText = TextView(this).apply {
            textSize = 13f
            setTextColor(getColor(R.color.text_secondary))
            text = if (pendingDeadline > 0L) {
                "截止：${Fmt.fullDate(pendingDeadline)} ${Fmt.time(pendingDeadline)}"
            } else {
                "截止：未设置"
            }
        }
        box.addView(deadlineText, lp(matchWidth = true, height = -2, top = dp(12)))

        val pickBtn = Button(this).apply {
            text = "选择截止时间"
            setOnClickListener {
                val base = Calendar.getInstance()
                if (pendingDeadline > 0L) base.timeInMillis = pendingDeadline
                DatePickerDialog(
                    this@MainActivity,
                    { _, y, m, d ->
                        val c = Calendar.getInstance()
                        c.set(Calendar.YEAR, y)
                        c.set(Calendar.MONTH, m)
                        c.set(Calendar.DAY_OF_MONTH, d)
                        c.set(Calendar.SECOND, 0)
                        c.set(Calendar.MILLISECOND, 0)
                        TimePickerDialog(
                            this@MainActivity,
                            { _, h, min ->
                                c.set(Calendar.HOUR_OF_DAY, h)
                                c.set(Calendar.MINUTE, min)
                                pendingDeadline = c.timeInMillis
                                deadlineText.text =
                                    "截止：${Fmt.fullDate(pendingDeadline)} ${Fmt.time(pendingDeadline)}"
                            },
                            if (pendingDeadline > 0L) base.get(Calendar.HOUR_OF_DAY) else 18,
                            if (pendingDeadline > 0L) base.get(Calendar.MINUTE) else 0,
                            true
                        ).show()
                    },
                    base.get(Calendar.YEAR),
                    base.get(Calendar.MONTH),
                    base.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }
        box.addView(pickBtn)

        val scroll = ScrollView(this)
        scroll.addView(box)

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "新建待办" else "编辑待办")
            .setView(scroll)
            .setPositiveButton("保存") { _, _ ->
                val name = titleEt.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "先写点什么吧", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val task = existing ?: Task()
                task.title = name
                task.type = if (rLong.isChecked) Task.TYPE_LONG else Task.TYPE_TODAY
                task.counter = counterEt.text.toString().trim()
                task.deadline = pendingDeadline
                Store.upsert(this, task)
                rebuild()
                WidgetRenderer.refreshAll(this)
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("清除截止") { _, _ ->
                pendingDeadline = 0L
                existing?.let {
                    it.deadline = 0L
                    Store.upsert(this, it)
                    rebuild()
                    WidgetRenderer.refreshAll(this)
                }
            }
            .show()
    }

    private fun nextHour(): Long {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, 1)
        c.set(Calendar.HOUR_OF_DAY, 18)
        c.set(Calendar.MINUTE, 0)
        return c.timeInMillis
    }

    // ---------------------------------------------------------------- 小工具
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun lp(matchWidth: Boolean, height: Int, top: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            if (matchWidth) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
            height
        ).apply { topMargin = top }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val MODE_NEW = "new"
    }
}
