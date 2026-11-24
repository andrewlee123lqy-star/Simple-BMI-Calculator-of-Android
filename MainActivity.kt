package com.lqy.practice   // 改成你自己项目的 namespace

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var heightInput: EditText
    private lateinit var weightInput: EditText
    private lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())   // 不引用任何 R.layout.*
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun LinearLayout.addFull(view: View, bottomDp: Int = 12) {
        addView(view, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(bottomDp) })
    }

    private fun buildUi(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            //LinearLayout.VERTICAL 严格按 addView 顺序排布；以及 lateinit 只是「声明」，不是「初始化」，更不是「加入界面」。
            setPadding(dp(24), dp(24), dp(24), dp(24)) 
            //把四个 dp(28) 改成 dp(0) 跑一次，界面立刻贴边；只改第一个数（左）为 dp(60)，会看到只有左边空出一大块——四个位置各管一边。
        }
            //root.addFull 控件依照代码顺序从上往下排
        root.addFull(TextView(this).apply {
            text = "BMI Calculator"
            textSize = 24f
            gravity = Gravity.CENTER
        }, 24)

        heightInput = EditText(this).apply {
            hint = "身高 (cm)，例如 170"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        weightInput = EditText(this).apply {
            hint = "体重 (kg)，例如 65"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        root.addFull(heightInput)
        root.addFull(weightInput, 16) //"16"是体重输入框与计算按钮的距离

        resultText = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
        }

        root.addFull(Button(this).apply {
            text = "Calculate BMI"
            setOnClickListener { resultText.text = calculate() }
        }, 24) 
        //刚打开页面时，resultText 还没有文字或背景，按钮下方看起来都是一片空白，很难看出差别。
        //输入身高和体重，再点击 Calculate BMI，对比按钮底部到第一行结果文字的距离。设为 24 时，结果会比设为 0 时低 24dp。
        root.addFull(resultText)
        return root
    }

    private fun calculate(): String {
        val hCm = heightInput.text.toString().trim().toDoubleOrNull()
        val wKg = weightInput.text.toString().trim().toDoubleOrNull()

        if (hCm == null || wKg == null || hCm <= 0.0 || wKg <= 0.0) {
            return "请输入大于 0 的身高和体重"
        }

        val hM = hCm / 100.0
        val bmi = wKg / (hM * hM)

        // 中国成人标准：<18.5 偏瘦 / 18.5~23.9 正常 / 24~27.9 超重 / ≥28 肥胖
        // 换 WHO 国际标准：把 24.0 改成 25.0、28.0 改成 30.0
        val category = when {
            bmi < 18.5 -> "偏瘦"
            bmi < 24.0 -> "正常"
            bmi < 28.0 -> "超重"
            else -> "肥胖"
        }

        return "BMI = %.2f\n%s".format(bmi, category)
    }
}
