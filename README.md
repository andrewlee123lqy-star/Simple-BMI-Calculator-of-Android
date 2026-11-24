这份代码很适合拿来入门：它把「Android 四大组件 → 视图 → 交互 → 业务计算」全压在一个文件里，没有 XML、没有 Compose、没有第三方库，每一行的来龙去脉都能看清。下面按 8 个模块拆。

模块 0：先看全局骨架（建立地图）

├── ① 声明区      package / import / 类 / 3 个成员变量
├── ② 生命周期    onCreate()          ← 入口，只干一件事
├── ③ 工具层      dp() / addFull()    ← 给布局搭把手
├── ④ 视图层      buildUi()           ← 造出整棵控件树
└── ⑤ 业务层      calculate()         ← 纯计算，不碰 UI


一句话记住它的设计取向：用代码动态搭界面，而不是用 XML。所以 setContentView() 传的是一个 View 对象，不是 R.layout.xxx。

顺带一个设计细节：只有 2 个输入框 + 1 个结果框被存成成员变量。标题、按钮创建完就扔进容器，不需要再拿回来 —— 判断"要不要存成字段"的标准就一条：后面还会不会读它。

模块 1：声明区

package com.example.bmi   // ← 必须和 build.gradle.kts 里的 namespace 一致

import android.app.Activity
import android.widget.*    // 实际写的是逐个 import

class MainActivity : Activity() {

    private lateinit var heightInput: EditText
    private lateinit var weightInput: EditText
    private lateinit var resultText: TextView


点 解释

继承 Activity 而不是 AppCompatActivity 后者来自 appcompat 库。这里只用系统原生控件，不引任何依赖，所以编译不受 AGP / AndroidX 版本影响

lateinit Kotlin 规定「非空类型的属性声明时必须初始化」，但 View 得等 onCreate 里才有 this 上下文能 new 出来。lateinit 就是跟编译器说：我保证用之前一定赋值

lateinit 的限制 只能修饰 var、不能是 Int/Double 这类原始类型、不能自定义 getter。这里三个都是 View 对象，正好合规

如果不用 lateinit 的写法：private var heightInput: EditText? = null，但后面每次都得写 heightInput?.text，会平白多出一堆问号。

模块 2：生命周期入口（只有 3 行）

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(buildUi())
}


• onCreate 是 Activity 的第一个回调，一个 Activity 只走一次（转屏重建除外）。

• savedInstanceState 是上次被销毁时存下来的数据，这里没用到，先忽略。

• super.onCreate(...) 必须第一行，否则会抛 SuperNotCalledException。

• setContentView(buildUi()) 是整份代码最关键的一行：buildUi() 现场造一棵 View 树，直接交给系统当界面。没有 R.layout，这就是「零 XML」的实现方式。

对照记忆：传统写法是 setContentView(R.layout.activity_main) → 系统去 inflate XML；这里是我们自己 inflate。

模块 3：工具层（两个小函数，解决"写布局太啰嗦"）

3.1 dp() —— 像素换算

private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()


Android 布局里写 24 指的是 dp（密度无关像素），但 setPadding() / LayoutParams 收的是 px（物理像素）。在 3 倍屏手机上直接写 24 会小得看不见。

density 就是倍率（1.0 / 2.0 / 3.0 …），乘一下再取整。这是单表达式函数，所以省了 {} 和 return。

3.2 addFull() —— 扩展函数 + 默认参数

private fun LinearLayout.addFull(view: View, bottomDp: Int = 12) {
    addView(view, LinearLayout.LayoutParams(
        MATCH_PARENT, WRAP_CONTENT
    ).apply { bottomMargin = dp(bottomDp) })
}


三个语法点叠在一起：

语法 体现

扩展函数 LinearLayout.addFull(...) —— 没改系统源码，却像给 LinearLayout 加了个方法。函数体内 this 就是那个 LinearLayout，所以能直接调 addView

默认参数 bottomDp: Int = 12，不传就按 12dp 留白；标题传 24、按钮传 16，省掉重载

apply 作用域函数，apply { } 里 this 是刚 new 出来的 LayoutParams，写完整体返回这个对象，正好塞进 addView 第二个参数

为什么值得抽出来：不抽的话，每加一个控件都要重复 4 行 LayoutParams 模板代码。抽完之后视图层就只剩"这个控件长什么样"。

模块 4：视图层 buildUi() —— 整棵控件树

4.1 根容器

val root = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL     // 纵向排队
    setPadding(dp(24), dp(24), dp(24), dp(24))
}


LinearLayout(this) 里的 this 是 Activity 作为 Context —— 所有 View 构造都要一个 Context 才能拿资源、主题、系统服务。

这里有个易混点：外层 apply 里 this 是 LinearLayout；但 LinearLayout(this) 括号里的 this 是外面的 MainActivity。同名不同对象，靠位置区分。

4.2 五个控件，按职责分成三组

控件 作用 关键配置

TextView 标题 纯展示 textSize = 24f（单位是 sp）、gravity = CENTER

EditText ×2 输入 hint 占位提示；inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL

Button 触发 setOnClickListener { ... }

TextView 结果 输出 只存引用，等被写入

inputType 那行的 or 是 Int 的中缀位或函数，等价于 Java 的 |。效果：弹数字键盘 + 允许小数点，从源头挡住乱输字母。

gravity vs layout_gravity：gravity 管自己内容往哪对齐（文字居中），layout_gravity 管自己在父容器里往哪站。这里用的是前者。

4.3 唯一的一根线：监听器

setOnClickListener { resultText.text = calculate() }


• Java 里要 new View.OnClickListener(){ ... }，Kotlin 对单方法接口（SAM）可以直接传 lambda。

• 这行是整份代码里唯一把「视图层」和「业务层」接起来的地方。

• calculate() 返回 String，直接赋给 TextView.text —— 连 toString() 都省了。

4.4 组装顺序 = 屏幕从上到下的顺序


标题(24dp) → 身高框(12) → 体重框(12) → 按钮(16) → 结果框(24) → return root


LinearLayout.VERTICAL 严格按 addView 先后排，所以代码顺序就是视觉顺序，读代码就能脑补出界面。

模块 5：业务层 calculate() —— 最该单独练的一块

它有个很好的性质：不碰任何 View，只进字符串、出字符串。所以可以单独搬到 JVM 里跑单元测试。

5.1 安全解析

val hCm = heightInput.text.toString().trim().toDoubleOrNull()


toDouble() 遇到空串/abc 会抛 NumberFormatException 直接崩 App；toDoubleOrNull() 失败返回 null，把"异常"变成"可判断的值"。这是 Kotlin 处理输入的惯用手法。

5.2 输入校验（防御式）

if (hCm == null || wKg == null || hCm <= 0.0 || wKg <= 0.0) {
    return "请输入大于 0 的身高和体重"
}


四个条件一个都不能少：null 挡住格式错，<= 0 挡住除零和负数（身高 0 会让 hM*hM 变 0，BMI 直接 Infinity）。

Kotlin 的 || 会短路：hCm == null 成立时，后面的 hCm <= 0.0 根本不会执行，编译器也知道此时 hCm 已智能转型为非空 Double。所以 if 之后可以直接拿它做算术，不用 !!。

5.3 公式与分级

val hM = hCm / 100.0          // cm → m
val bmi = wKg / (hM * hM)     // kg / m²

val category = when {
    bmi < 18.5 -> "偏瘦"
    bmi < 24.0 -> "正常"
    bmi < 28.0 -> "超重"
    else       -> "肥胖"
}


• 无参数 when：每个分支自己写布尔条件，比 if-else if 链清爽。

• 顺序有讲究：从上往下匹配，所以写的是 < 24 而不是 >= 18.5 && < 24 —— 能走到第二行，说明第一行已经否掉了，天然自带下界。

• 注释里的 24 / 28 是中国成人标准；换 WHO 标准只需改两个数字（25 / 30），业务逻辑完全不动。

5.4 格式化输出

return "BMI = %.2f\n%s".format(bmi, category)


"%.2f" = 保留两位小数，"\n" 换行，"%s" 填字符串。等价于 Java String.format()，Kotlin 把它做成了字符串扩展函数。

模块 6：把调用链串起来（一眼看懂运行时）


用户点「计算 BMI」
   ↓  OnClickListener 被回调
   ↓  calculate()
        ├─ 读 heightInput.text / weightInput.text   ← 视图层 → 业务层
        ├─ toDoubleOrNull + 校验（不合法 → 提前 return 提示语）
        ├─ cm→m → BMI 公式 → when 分级 → format
        └─ return String
   ↓  resultText.text = ...                        ← 业务层 → 视图层
屏幕刷新出「BMI = 22.49 / 正常」


整条链是单向的：UI 只在首尾各出现一次，中间是纯数据流动。这就是最朴素的「UI / 逻辑分离」，也是后面学 MVVM、Compose 状态提升时的雏形。

模块 7：这份代码里藏的 Kotlin 语法清单

建议对着代码逐个圈出来，比单独背语法快：

语法 出现位置

lateinit var 三个字段

扩展函数 LinearLayout.addFull()、"…".format()

默认参数 bottomDp: Int = 12

单表达式函数 dp() 的 =

apply 作用域函数 根布局、每个控件、LayoutParams

SAM 转换 / 尾随 lambda setOnClickListener { }

or 中缀位运算 inputType

空安全 + 智能转型 toDoubleOrNull() + \|\| 短路

无参数 when 分级判断

字符串模板 / 格式化 "%.2f\n%s".format(...)

建议的动手顺序（比只读一遍有效）

1. 先只改文案：把标题改成「我的 BMI」、按钮改成「算一下」→ 熟悉 buildUi() 里控件和屏幕位置的对应。
2. 改阈值：把 24/28 换成 25/30，输入 170 / 70 看分类怎么变 → 熟悉 when 的顺序依赖。
3. 故意输错：留空、填 abc、填 0 → 看校验分支，理解 toDoubleOrNull 为什么比 toDouble 好。
4. 加一个「清空」按钮：照着现有 Button 复制一份，监听里写 heightInput.text.clear() → 练 addFull 和 lambda。
5. 转屏试一次：输入完横屏，内容没了 → 这就是下一课 onSaveInstanceState / ViewModel 要解决的问题。

走到第 5 步，你自然会明白为什么真实项目要用 Compose 的 remember 或 ViewModel 管状态 —— 到时候我再给你对应的改写版，对照着看会非常直观。

想先深入哪一块？「apply / lateinit 这些 Kotlin 语法」还是「转屏丢数据怎么修」，我按你选的那条往下展开。
