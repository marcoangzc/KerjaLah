# 🚀 KerjaLah 新队友 Onboarding 指南

> 写给即将加入 KerjaLah 的你。
> 这份文档假设你**没写过 Kotlin、没用过 Jetpack Compose、没碰过 SQL / Supabase、也没调过 AI API** —— 没关系，全部从零讲起。
> 所有代码片段都来自本仓库的真实文件（标注了文件路径），学到的每一个概念都能立刻在项目里找到原型。

---

## 目录

0. 这份文档怎么用（学习路线图）
1. KerjaLah 是什么？（10 分钟了解全局）
2. Day 0 · 环境搭建（今天就把 App 跑起来）
3. Kotlin 速成（只讲项目里真正用到的语法）
4. Jetpack Compose 入门（声明式 UI）
5. 架构：UDF 单向数据流
6. SQL 与 Supabase 基础
7. AI Prompt 基础（读懂并写好 AI 提示词）
8. 代码地图：该按什么顺序读这 10 个文件
9. 上手任务（3 个由易到难的实战练习）
10. 常见坑与自救指南
11. 术语表（中英对照）

---

## 0. 这份文档怎么用

### 学习路线图（建议 5–7 天）

| 时间 | 内容 | 章节 | 目标 |
|------|------|------|------|
| Day 0 | 环境搭建，App 跑起来 | 第 2 章 | 手机/模拟器上能看到 KerjaLah |
| Day 1 | Kotlin 语法速成 | 第 3 章 | 能看懂项目里的 .kt 文件 |
| Day 2 | Jetpack Compose 入门 | 第 4 章 | 能看懂一个 Screen 文件的结构 |
| Day 3 | 架构 + 代码阅读 | 第 5、8 章 | 说清楚"点一下按钮之后发生了什么" |
| Day 4 | SQL 与 Supabase | 第 6 章 | 能在 SQL Editor 里查询三张表 |
| Day 5 | AI Prompt | 第 7 章 | 看懂 AiClient.kt，会写结构化 Prompt |
| Day 5–7 | 实战任务 | 第 9 章 | 完成任务 1→2→3，正式开工 |

### 三条使用规则

1. **先跑起来，再学理论。** 不要从第 3 章开始死磕语法 —— 先照第 2 章把 App 编译运行成功，成就感是最好的燃料。
2. **每个概念都回到真实代码。** 文档里的示例全部来自本项目，读完一节就去打开对应文件对照一遍。
3. **卡住超过 30 分钟就求助。** 问队友，或者用第 7.5 节的模板去问 AI。自己硬耗一晚上是最亏的学习方式。

---

## 1. KerjaLah 是什么？（10 分钟了解全局）

**一句话介绍：** KerjaLah 是一个原生 Android App（Kotlin 编写），帮马来西亚大学生找到**薪资合规**的兼职工作。

### 三个角色如何互动

```
 学生 (Student)                雇主 (Employer)
      │                             │
      │ ① 浏览兼职、一键投递          │ ② 发布兼职
      ▼                             ▼
┌─────────────────────────────────────────┐
│              Supabase 云端数据库           │
│   applications 表        jobs 表          │
└─────────────────────────────────────────┘
      │                             │
      │ ③ AI Advisor 后台打分         │ ④ 看到 AI 建议（匹配度% + 理由）
      │   （只是建议！）               │    但【决定权永远在人】
      ▼                             ▼
 ⑤ 雇主点 Accept 的瞬间，学生端界面实时变色（Realtime 推送）
```

### 三大功能模块

| 模块 | 内容 | 主要代码目录 |
|------|------|--------------|
| Module 1 · 用户与认证 | 启动页、注册、登录、选择身份（学生/雇主）、编辑个人资料 | `ui/user/` |
| Module 2 · 兼职职位 | 雇主发职位（受 Fair-Wage Check 保护）；学生浏览职位详情 | `ui/job/` + `ui/employer/` |
| Module 3 · 申请与 AI | 一键投递/撤回、接受/拒绝、实时状态同步、AI 顾问打分 | `ui/application/` |

### 两个必须理解的"灵魂设定"

1. **⚖️ Fair-Wage Check（公平薪资检查）**
   马来西亚《2024 最低工资令》：月薪 RM 1,700 = **时薪不得低于 RM 8.72**。
   任何低于这个数的职位**根本发不出去** —— 这是 App 的核心卖点（呼应联合国 SDG 8 体面工作）。
   规则只写在一个地方：`data/data/FairWage.kt`（第 3.5 节会精读它）。

2. **🤖 AI 只建议，人做决定（Human-in-the-loop）**
   学生投递时，AI（Groq 的 Llama 3.3 模型）会读"职位 + 学生资料"，给出匹配度百分比和
   建议状态（限时约 12 秒，结果随申请一起写入数据库）。但**只有雇主本人**能点 Accept /
   Reject。而且 AI 失败绝不影响投递本身 —— 最多这条申请暂时没有 AI 建议。

### 技术栈一览

| 层 | 技术 | 一句话解释 |
|----|------|-----------|
| 语言 | Kotlin (100%) | Google 官方推荐的 Android 语言 |
| UI | Jetpack Compose (Material 3) | 用代码直接"描述"界面，不用写 XML |
| 架构 | UDF 单向数据流：ViewModel + StateFlow | 数据只朝一个方向流，好排查问题 |
| 后端 | Supabase（Auth · 数据库 · Realtime） | 开源的"Firebase 替代品"，底层是 PostgreSQL |
| 网络 | Ktor client | 发 HTTP 请求用的库 |
| 序列化 | kotlinx.serialization | JSON ↔ Kotlin 对象互转 |
| AI | Groq API（llama-3.3-70b-versatile） | 兼容 OpenAI 格式的免费/低价大模型接口 |
| 导航 | Navigation Compose | 所有页面路径集中在 `Routes.kt` 一个文件里 |

---

## 2. Day 0 · 环境搭建（今天就把 App 跑起来）

> 目标：本章结束前，你在自己的电脑上看到 KerjaLah 运行起来。预计 1–2 小时（大部分时间在等下载）。

### 步骤 1 · 安装 Android Studio

- 下载地址：https://developer.android.com/studio （选 Ladybug 或更新版本）
- 安装时一路默认即可，它会自带 JDK 和 Android SDK。
- 首次启动向导选 **Standard** 安装类型。

### 步骤 2 · 克隆项目

```bash
git clone https://github.com/marcoangzc/KerjaLah.git
cd KerjaLah
```

然后用 Android Studio：**File → Open** → 选择 KerjaLah 文件夹。

### 步骤 3 · 配置 Supabase（数据库 + 登录系统）

1. 去 https://supabase.com 用 GitHub 账号注册（免费）。
2. 点 **New Project** 创建一个项目（名字随意，区域选离马来西亚近的 Singapore）。
   - 创建时会让你设置**数据库密码** —— 记下来，但本 App 其实用不到它，别弄丢就行。
3. 进入项目后：左侧菜单 **SQL Editor** → **New query**。
4. 打开仓库根目录的 `supabase_schema.sql`，**全选复制**进去，点 **Run**。
   ✅ 成功标志：左侧 **Table Editor** 里出现 `profiles`、`jobs`、`applications` 三张表。
5. 拿两样东西（稍后要用）：
   - 左侧 **Project Settings → Data API** → `Project URL`（形如 `https://xxxx.supabase.co`）
   - 同一页面的 `anon public` key（一长串）

### 步骤 4 · 申请 Groq API Key（AI 顾问用）

1. 去 https://console.groq.com/keys 注册并创建一个 API Key（免费额度够开发用）。
2. 复制保存，形如 `gsk_xxxxxxxx`。

### 步骤 5 · 写入密钥（⚠️ 最容易出错的一步）

在**项目根目录**（和 `settings.gradle.kts` 同级的那一层）创建/编辑 `local.properties` 文件：

```properties
SUPABASE_URL=https://你的项目ID.supabase.co
SUPABASE_ANON_KEY=你的anon开头的长字符串
```

还要加一行（服务端地址，不是密钥）：

```properties
ADVISOR_BASE_URL=http://10.0.2.2:8080
```

> ⚠️ **本文档下面几章讲的"`AiClient.kt` 直接调 Groq"已经过时。**
> Groq key **不再**写进 `local.properties`：编译进 APK 的东西都能被反编译出来。
> 现在 AI 顾问是一个独立的 Kotlin 服务（`advisor/` 模块，Ktor 写的），
> key 作为环境变量设在那台服务器上，见 `advisor/README.md`。
> `AiClient.kt` 还在，但它现在只负责"带着登录 token 去请求 advisor"，
> Prompt 组装、JSON 解析、超时兜底都搬到了 `advisor/GroqAdvisor.kt`——
> 那些概念完全适用，而且照样是 Kotlin，只是跑在服务端。

三个要点：

- 这个文件已经被 `.gitignore` 忽略，**永远不会提交到 GitHub** —— 所以密钥放这里是安全的。
- 反过来也成立：**克隆别人的仓库后必须自己手动创建这个文件**，否则编译出的 App 连不上数据库。
- 位置必须是项目根目录。构建脚本（`app/build.gradle.kts`）会读它，并通过 `BuildConfig.SUPABASE_URL` 这样的常量注入到代码里。

### 步骤 6 · 运行！

1. 等 Android Studio 右下角的 **Gradle sync** 转完（第一次要下载很多依赖，可能 10 分钟）。
2. 顶部设备下拉框 → **Device Manager** → 创建一个虚拟手机（Pixel 系列 + 最新系统镜像都行），或者用数据线连真机（手机上开启"开发者选项 → USB 调试"）。
3. 按绿色 **Run ▶** 按钮。

### ✅ 环境验收清单

- [ ] App 成功安装并显示启动页（Splash）
- [ ] 能注册一个 **STUDENT** 账号，也能注册一个 **EMPLOYER** 账号（用不同邮箱）
- [ ] 用雇主账号发职位：填时薪 **8.00** → 应被拦截报错；填 **9.00** → 发布成功
- [ ] 用学生账号能看到刚才发布的职位

四条全过，环境就绪。接下来开始学语言。

---

## 3. Kotlin 速成（只讲项目里真正用到的语法）

> 不求全面，只求你看懂本项目每一行代码。建议边读边在 https://play.kotlinlang.org （在线 Kotlin 编辑器，无需安装任何东西）里敲一遍。

### 3.1 变量：val 与 var

```kotlin
val name = "Ahmad"     // val = 只能赋值一次（推荐默认用它）
var age = 21           // var = 可以重新赋值
age = 22               // OK
// name = "Ali"        // ❌ 编译错误：val 不能重新赋值
```

Kotlin 会自动推断类型（上面 name 自动是 String）。也可以显式写：

```kotlin
val pay: Double = 9.50
```

**团队习惯：能用 val 就用 val**，减少意外的状态变化。

### 3.2 空安全（Null Safety）—— Kotlin 的招牌特性

```kotlin
var nickname: String? = null    // 类型后面加 ? 表示"可以为 null"
// nickname.length              // ❌ 编译不过！编译器逼你处理 null
println(nickname?.length)       // ?. 安全调用：null 时返回 null，不崩溃
println(nickname ?: "无名")      // ?: Elvis 操作符：左边为 null 就用右边的默认值
```

项目里的真实例子 —— `AiClient.kt`：

```kotlin
val percent = percentStr.toIntOrNull()?.coerceIn(0, 100) ?: 0
//             ↑ 转整数，转不动返回 null
//                                  ↑ 夹在 0~100 之间（AI 有时会输出越界的数字）
//                                                      ↑ 整条链路任何一环是 null，最终兜底为 0
```

这一行同时用了 `toIntOrNull`、`?.`、`?:` 三个武器 —— 这就是 Kotlin 防 AI 输出乱码的防御式写法。

### 3.3 函数：默认参数 + 具名参数

来自 `navigation/Routes.kt`（真实的导航工具函数）：

```kotlin
fun employerPost(jobId: String? = null) =
    if (jobId == null) "employer/post" else "employer/post?jobId=$jobId"
```

三个知识点：

- `jobId: String? = null`：参数可空且**有默认值**，调用时可以不传。
- 单表达式函数：函数体只有一句时可以直接 `=` 接表达式，省略 return。
- `"$jobId"`：**字符串模板**，双引号里用 `$变量` 直接拼接值；复杂表达式用 `"${a + b}"`。到处都会见到。

调用时还能用**具名参数**提高可读性（Compose 代码里铺天盖地）：

```kotlin
employerPost(jobId = "abc123")
```

### 3.4 data class：一行顶十行的数据容器

Kotlin 表示"一条数据"的标准姿势（本项目里 `Job`、`User`、各种 `UiState` 都是 data class）：

```kotlin
data class Job(
    val id: String,
    val title: String,
    val payPerHour: Double,
)
```

编译器自动帮你生成 `equals()`、`hashCode()`、`toString()`、`copy()`。
`copy()` 特别常用：想"改"一个不可变对象的一个字段时——

```kotlin
val raised = job.copy(payPerHour = 12.0)
```

### 3.5 object：全 App 只有一份的单例

精读第一个真实文件 —— `data/data/FairWage.kt`（全文就这么短，但它承载核心业务规则）：

```kotlin
object FairWage {
    const val MIN_HOURLY_RM = 8.72

    // Why: keep the wage rule in ONE place,
    // so UI and ViewModel never hardcode numbers.
    fun isFair(payPerHour: Double): Boolean = payPerHour >= MIN_HOURLY_RM
}
```

- `object` = 单例：不需要 `new`，直接 `FairWage.isFair(9.0)` 调用，整个 App 共享同一份。
- 项目里所有"全局唯一"的东西都用它：`FairWage`（工资规则）、`Routes`（页面路由）、`SupabaseClientProvider`（数据库连接）、`AiClient`(AI 客户端)。
- 注意注释里的设计哲学：**规则只写在一处**（single source of truth），界面层绝不允许自己硬编码 8.72。

### 3.6 集合操作：map 与常见链式调用

来自 `ui/job/JobListViewModel.kt`：

```kotlin
jobs.map { it.toUi() }   // 把 List<Job> 一对一转换成 List<JobUi>
```

- `map {}` 对列表每一项做转换，返回新列表（不改原列表）。
- `{ it.toUi() }` 是 **lambda（匿名函数）**，`it` 是约定俗成的单个参数名。
- `.toUi()` 是**扩展函数**（extension function）：在不修改原类的前提下给它加方法，定义在 `JobUi.kt` 里。这是 Kotlin 让代码整洁的利器。

其他高频集合函数：`filter {}`（筛选）、`firstOrNull {}`（找第一个符合条件的，找不到返回 null）、`forEach {}`（遍历）。

### 3.7 协程与 Flow：异步不头痛

App 里所有网络请求都不能卡住界面，Kotlin 用**协程（coroutine）**处理，语法上就是给函数加 `suspend` 标记，然后在协程作用域里调用：

```kotlin
// JobListViewModel.kt —— 真实代码
init {
    viewModelScope.launch {                 // 启动一个跟 ViewModel 共存亡的协程
        JobRepository.jobs.collect { jobs -> // 持续监听数据库数据流
            _uiState.value = JobListUiState(
                jobs = jobs.map { it.toUi() },
                isLoading = false,
            )
        }
    }
}
```

理解三件事就够了：

| 概念 | 一句话解释 | 生活比喻 |
|------|-----------|---------|
| `suspend fun` | 可以"暂停再恢复"的函数，里面能慢慢等网络 | 点了外卖可以先干别的，外卖到了再吃 |
| `viewModelScope.launch {}` | 在 ViewModel 里启动后台任务 | 派一个助手去办事 |
| `Flow` / `collect` | 一股持续更新的数据流，collect 是"持续接收" | 订阅微信公众号，每次推文自动送达 |

`MutableStateFlow` / `StateFlow`（状态流）是 Compose 界面和逻辑层之间的桥梁，第 5 章详述。
还有个小语法 `by lazy`：第一次被用到时才初始化，之后复用（见 `SupabaseClientProvider.client`）。

### 3.8 练习 ✍️

在 play.kotlinlang.org 里写一个函数：

```kotlin
fun wageLabel(payPerHour: Double): String =
    if (payPerHour >= 8.72) "RM $payPerHour/hr ✔ 合规"
    else "RM $payPerHour/hr ✘ 低于最低工资"
```

跑通后试着改造它：用 `when` 或三元风格的 `if` 表达式、处理 `payPerHour <= 0` 的非法输入（返回 "Invalid"）。写不出来没关系，把报错贴给 AI（用 7.5 节模板）。

---

## 4. Jetpack Compose 入门（声明式 UI）

### 4.1 传统 UI vs Compose：一次思维转变

```
传统方式（命令式）：                    Compose 方式（声明式）：
                                      ┌────────────────────────┐
拿到 textView 引用                     │ 界面 = f(state) 状态的函数 │
textView.setText("Hi")  手动改控件      │                        │
                                      │ state 变了 → f 重新执行   │
界面散落在几十处手动更新语句里             │ → 界面自动刷新            │
                                      │ （称为 Recomposition 重组）│
                                      └────────────────────────┘
```

你**从不**说"把这个文字改成 X"，你只说"**界面长什么样**"，剩下的交给框架。

### 4.2 第一个 @Composable：精读 FairWageBadge

来自 `ui/job/JobListScreen.kt`（真实代码，职位卡片右下角的绿色徽章）：

```kotlin
@Composable
fun FairWageBadge() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Fair Wage",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}
```

拆解：

- `@Composable` 注解 = "这是一个 UI 函数"。普通 Kotlin 函数只在调用时执行一次；Composable 函数会被框架反复调用（重组）来刷新界面。
- `Row` / `Column` / `Surface` 是**布局容器**：Row 横排、Column 竖排、Surface 是一块带背景色/圆角的画布。
- `Modifier` 是每个组件的"装修清单"，**从上到下依次生效**：`Modifier.padding(8.dp).size(14.dp)` = 先留边距再定大小。
- `MaterialTheme.colorScheme.xxx` 从主题取颜色，`typography.xxx` 取字体样式 —— **永远别写死十六进制色值**，统一走主题（主题定义在 `ui/theme/Color.kt` 和 `Theme.kt`）。
- `Icon` 的 `contentDescription = null`：给视障用户的读屏描述，纯装饰图标才允许设 null。

### 4.3 高频组件速查表

| 组件 | 用途 | 项目中的例子 |
|------|------|--------------|
| `Scaffold` | 页面骨架（顶部栏+底部栏+内容区） | 每个 Screen 的外壳 |
| `CenterAlignedTopAppBar` | 居中标题的顶栏 | "Jobs Near You" |
| `Card(onClick = …)` | 可点击卡片 | 职位卡片 `JobCard` |
| `LazyVerticalGrid` / `LazyColumn` | 长列表/网格（只渲染屏幕内的项） | 职位瀑布网格 |
| `CircularProgressIndicator` | 加载转圈 | `uiState.isLoading` 时 |
| `Button` / `OutlinedButton` / `TextButton` | 按钮 | 登录/投递按钮 |
| `TextField` / `OutlinedTextField` | 输入框 | 注册、发布职位表单 |
| `Chip` / `StatusChip` | 小标签 | 申请状态标签 |

### 4.4 状态驱动界面：collectAsStateWithLifecycle

`JobListScreen.kt` 的开头（真实代码，节选）：

```kotlin
@Composable
fun JobListScreen(
    viewModel: JobListViewModel = viewModel(),
    onJobClick: (String) -> Unit,   // 导航事件向上交给 NavGraph（见第 5 章）
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Jobs Near You") }) },
    ) { innerPadding ->
        if (uiState.isLoading) {
            CircularProgressIndicator()          // 状态 A：加载中 → 转圈
        } else {
            LazyVerticalGrid(...) {             // 状态 B：加载完 → 网格
                items(uiState.jobs, key = { it.id }) { job ->
                    JobCard(job = job, onClick = { onJobClick(job.id) })
                }
            }
        }
    }
}
```

这一段浓缩了 Compose 的全部精髓：

1. `viewModel.uiState` 是第 3.7 节讲的 StateFlow；`collectAsStateWithLifecycle()` 把它变成 **Compose 状态**。
2. `by` 委托关键字让你直接写 `uiState.isLoading` 而不用 `.value`。
3. **数据库里多了个职位 → Repository 流出新数据 → uiState 变化 → 这个函数自动重组 → 网格自动多一张卡片。** 你没有写任何"刷新界面"的代码。
4. `if / else` 直接当界面的"分支逻辑"用 —— 界面就是状态的函数。
5. 注意 `onJobClick: (String) -> Unit`：Screen 自己**不做**跳转，只把事件抛给上级（lambda 参数）。这是本项目的铁律，第 5 章解释为什么。

### 4.5 页面导航：一切尽在 Routes.kt

所有页面路径集中定义在一个文件里（真实代码节选）：

```kotlin
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val STUDENT_JOBS = "student/jobs"
    const val STUDENT_JOB_DETAIL = "student/job/{jobId}"   // {jobId} 是占位符
    ...
    // 工具函数：拼出真实路径，避免各处手写字符串打错字
    fun studentJobDetail(jobId: String) = "student/job/$jobId"
}
```

- `NavGraph.kt` 是唯一的"地铁调度中心"：它把每条 route 映射到一个 Screen，并把"点击事件"接过去。
- 你要新增页面时：在 `Routes.kt` 加常量和工具函数 → 在 `NavGraph.kt` 注册 composable → 完毕。**禁止**在其他地方手写跳转字符串。

### 4.6 练习 ✍️

在 `ui/job/JobListScreen.kt` 里找到 `FairWageBadge`，把文字 `"Fair Wage"` 临时改成 `"✅ Fair Wage"`，按 Run，在职位卡片上亲眼确认变化。（做完记得改回来，或者留着 —— 你开心就好，但要在提交信息里说明。）

---

## 5. 架构：UDF 单向数据流

> 📌 **名词澄清**：本项目架构的正式名称是 **UDF（Unidirectional Data Flow，单向数据流）** —— 这也是 README 里写的唯一说法，代码注释里也反复出现 "unidirectional data flow"。
> 以后看教程你可能遇到另一个词 **MVVM**（Model-View-ViewModel）：它和 UDF 不是竞争关系 —— **MVVM 描述"有哪几层"**（Screen / ViewModel / Repository），**UDF 描述"数据往哪边流"**。本项目「Screen + ViewModel + StateFlow + Repository」的结构同时符合两者，但对外统一只说 **UDF**。

### 5.1 全景图

```
        事件(Event) 向上流动                数据(State) 向下流动
  ───────────────────────────▶◀───────────────────────────────
┌────────┐                  ┌───────────┐              ┌─────────────┐
│ Screen │  用户点击"投递"     │ ViewModel │  调用挂起函数  │ Repository  │
│(Compose)│ ───────────────▶ │(StateFlow)│ ───────────▶ │ (Supabase/AI)│
│        │ ◀─────────────── │           │ ◀─────────── │             │
└────────┘  新 UiState →    └───────────┘   数据/实时推送   └─────────────┘
            界面自动重组
```

三条铁律（项目代码注释里反复出现的 "unidirectional data flow / UDF" 就是这个意思）：

1. **Screen 不含业务逻辑**：只负责把 UiState 画出来，把用户点击包装成事件往上传。
2. **ViewModel 持有状态**：把 Repository 来的原始数据加工成 `XxxUiState`，通过 StateFlow 广播。
3. **Repository 是唯一碰网络的层**：封装 Supabase 查询和 AI 调用，其他人不直接摸数据库客户端。

### 5.2 跟着一次"投递"走完全程

场景：学生小明在职位详情页点了 **Apply**。

```
1. JobDetailScreen → viewModel.onApplyClick()（事件向上）
2. JobDetailViewModel → 调用 ApplicationRepository.apply(jobId, studentId)
3. ApplicationRepository → 先请 AiClient.assessApplication(job, student) 打分
   （限时约 12 秒；任何失败都返回 null 并跳过 —— AI 挂了也不挡投递）
4. 向 applications 表 INSERT 一行：status=PENDING，
   ai_match_percent / ai_suggested_status / ai_reason 随行写入
   （为什么随行？RLS 只允许学生 INSERT 自己的行、不允许 UPDATE，见第 6.5 节）
5. 雇主打开 ApplicantsScreen → 看到小明的申请 + AI 匹配度卡片
6. 雇主点 Accept → UPDATE applications SET status='ACCEPTED'
7. Supabase Realtime 把这次变更推送给所有订阅者
8. 小明手机上的 MyApplicationsScreen 的 StateFlow 收到推送
   → uiState 更新 → 界面上那条申请自动变成绿色 ACCEPTED 🎉
```

注意第 8 步：**没有任何"轮询"或手动刷新代码** —— 这就是 StateFlow + Realtime 组合的威力。

### 5.3 目录职责速查

```
app/src/main/java/com/kerjalah/app/
├── MainActivity.kt          # 入口：设置主题 + 挂载 NavGraph（20 行，最先读）
├── data/data/               # 数据层（是的，有两层 data 文件夹，历史原因，别慌）
│   ├── SupabaseClientProvider.kt  # 全局唯一数据库连接
│   ├── *Repository.kt       # 每张表一个仓库：查询 + StateFlow 缓存 + Realtime 订阅
│   ├── Job.kt / User.kt ... # 数据模型（对应数据库表的行）
│   ├── SupabaseDtos.kt      # 网络传输用的 DTO（@Serializable）
│   ├── FairWage.kt          # ⚖️ 工资合规规则（单一事实来源）
│   └── AiClient.kt          # 🤖 Groq AI 顾问
├── navigation/              # Routes.kt（所有路径）+ NavGraph（唯一调度员）
└── ui/
    ├── user/                # 登录、注册、选身份、个人资料（每个 Screen 配一个 ViewModel）
    ├── job/                 # 学生端：职位列表 + 详情
    ├── application/         # 学生端：我的申请
    ├── employer/            # 雇主端：我的职位、发布职位、查看申请人
    └── theme/               # Material 3 主题（颜色、字体）
```

**规律**：每个功能 = `XxxScreen.kt`（界面）+ `XxxViewModel.kt`（逻辑）+ `XxxUiState.kt`（状态数据类）。找一个模块模仿它的结构就能写新功能。

---

## 6. SQL 与 Supabase 基础

### 6.1 关系型数据库 60 秒入门

把它想象成一个**多工作表的 Excel**：

- **Table（表）** = 一张工作表，比如 `jobs`
- **Row（行）** = 一条记录，比如"星巴克咖啡师兼职"
- **Column（列）** = 一个字段，比如 `title`、`pay_per_hour`
- **Primary Key 主键** = 每行的身份证号。本项目用 `uuid`（随机生成的全球唯一字符串），由数据库自动生成：`id uuid primary key default gen_random_uuid()`
- **Foreign Key 外键** = 指向另一张表主键的"箭头"，用来表达关系。比如 `applications.job_id` 指向 `jobs.id`

### 6.2 本项目只有三张表

来自 `supabase_schema.sql`：

```
┌──────────────┐        ┌──────────────┐        ┌────────────────┐
│   profiles   │ 1    N │     jobs     │ 1    N │  applications  │
│──────────────│───────▶│──────────────│───────▶│────────────────│
│ id (PK, uuid)│        │ id (PK)      │        │ id (PK)        │
│ role         │        │ employer_id ─┼────────┤ job_id (FK)    │
│  ('STUDENT'  │        │ title        │        │ student_id ────┼──▶ profiles.id
│   /'EMPLOYER')│       │ pay_per_hour │        │ status         │
│ name / email │        │ hours_per_wk │        │  (PENDING/     │
│ organization │        │ description  │        │   ACCEPTED/    │
│ bio          │        └──────────────┘        │   REJECTED)    │
└──────────────┘                                │ applied_at     │
   ↑ 登录账号本体存在 auth.users（Supabase 内置）  │ ai_* 三列       │ ← AI 顾问回填
   └ profiles 通过外键关联它                       └────────────────┘
```

值得注意的设计细节（面试聊起来都是加分项）：

- `check (role in ('STUDENT','EMPLOYER'))`：数据库层面就禁止乱七八糟的角色值。
- `unique (job_id, student_id)`：同一学生对同一职位只能投一次，数据库替你把关。
- `applied_at bigint`：存的是毫秒时间戳，为了和 Kotlin 端的类型直接对齐。

### 6.3 SQL 四个基本动词（今天只需掌握这些）

打开 Supabase Dashboard → **SQL Editor**，直接练习（SELECT 是只读的，随便跑）：

```sql
-- ① SELECT 查询：所有时薪 >= 10 令吉的职位，按时薪从高到低
select title, company_name, pay_per_hour
from jobs
where pay_per_hour >= 10
order by pay_per_hour desc;

-- ② INSERT 插入（一般由 App 做，这里了解即可）
insert into jobs (employer_id, title, company_name, location, pay_per_hour, hours_per_week, description)
values ('某个profile的uuid', 'Test', 'Test Cafe', 'KL', 9.5, 10, 'testing');

-- ③ UPDATE 更新（⚠️ 没有 WHERE 会改全表！Supabase 里没有撤销）
update jobs set pay_per_hour = 9.99 where title = 'Test';

-- ④ DELETE 删除（同样，务必带 WHERE）
delete from jobs where title = 'Test';
```

进阶一点 —— **JOIN**（把两张表拼起来查）：

```sql
-- 查每份申请对应的学生姓名和职位名
select a.status, p.name as student_name, j.title as job_title
from applications a
join profiles p on p.id = a.student_id
join jobs j on j.id = a.job_id;
```

日常开发其实更多用 Supabase 网页上的 **Table Editor**（可视化表格，像 Excel 一样直接点开看数据、改数据）。

### 6.4 Supabase 到底是什么？

= **PostgreSQL 数据库** + 一堆开箱即用的云服务：

| 服务 | 作用 | 本项目怎么用 |
|------|------|--------------|
| PostgREST | 自动的 HTTPS API：每张表天生就有 REST 接口 | App **从不写 SQL 字符串**，supabase-kt 库把 Kotlin 调用翻译成 HTTP 请求 |
| Auth | 注册/登录/会话管理 | RegisterScreen / LoginScreen 背后就是它；密码哈希等安全问题全托管 |
| Realtime | 数据库变更实时推送到 App | 雇主点 Accept → 学生端秒变绿 |
| Row Level Security (RLS) | 行级权限防火墙 | 见下节，本项目安全的基石 |

### 6.5 RLS（Row Level Security）：为什么密钥公开也不怕

关键矛盾：`SUPABASE_ANON_KEY` 会被打包进 APK，理论上任何人都能解包拿出来。那岂不是谁都能删库？

答案：**anon key 只是"入场券"，能做什么由 RLS 决定**。RLS 是数据库里的规则，规定"每一行，谁能读、谁能改"。

用白话翻译 `supabase_schema.sql` 里的几条策略：

```sql
-- 「职位人人可看」（只要登录了）
create policy "jobs are readable by signed-in users"
  on public.jobs for select to authenticated using (true);

-- 「你只能发属于自己的职位」—— 插入时数据库校验 employer_id 必须是你本人
create policy "employer inserts own jobs"
  on public.jobs for insert to authenticated with check (employer_id = auth.uid());

-- 「学生只能撤回自己处于 PENDING 状态的申请」
create policy "student withdraws own pending application"
  on public.applications for delete to authenticated
  using (student_id = auth.uid() and status = 'PENDING');
```

`auth.uid()` 是 Supabase 提供的函数：当前登录用户的 id。也就是说**权限判断发生在数据库内部**，客户端伪造请求也没用 —— 这是 Supabase 安全模型的精髓。

### 6.6 Realtime 的两行魔法

```sql
alter publication supabase_realtime add table public.jobs;
alter publication supabase_realtime add table public.applications;
```

这两行把两张表加入了"变更广播频道"。之后 Kotlin 端订阅（`JobRepository` 里有对应代码），任何 INSERT/UPDATE 都会通过 WebSocket 即时推送到所有在线设备。

### 6.7 练习 ✍️（在 SQL Editor 完成，全部只读安全）

1. 查出所有职位，按时薪降序排列。
2. 统计每种状态的申请数量（提示：`group by status`）。
3. 用 JOIN 查出每份申请的学生姓名 + 职位名 + 当前状态。
4. ⭐ 查出**还没有收到任何申请**的职位（提示：`left join ... where ... is null`）。
5. ⭐ 数一数每位雇主各自发布了多少职位（提示：`group by employer_id`，配合 JOIN 显示雇主名）。

卡住了？这正是练习 AI 提问的好机会 —— 带着"表结构 + 你的 SQL + 报错"去问（见 7.5）。

---

## 7. AI Prompt 基础

### 7.1 大模型 API 的信件模型

调 AI 就像**寄一封格式严格的信**。项目里的 `AiClient.kt` 拆开来看：

```
收件地址 (URL)：  POST https://api.groq.com/openai/v1/chat/completions
信封 (Header)：   Authorization: Bearer gsk_xxxx     ← 证明你是付费用户
信纸 (Body)：     {
                    "model": "llama-3.3-70b-versatile",     ← 用哪个模型
                    "messages": [                            ← 对话历史
                      { "role": "user", "content": "<你的提示词>" }
                    ],
                    "response_format": { "type": "json_object" }  ← 强制返回 JSON
                  }
回信 (Response)： choices[0].message.content → 一段文本（我们让它必须是 JSON）
```

`messages` 里 role 可以是 `system`（设定人设）/ `user`（用户输入）/ `assistant`（模型回复）。本项目只用了一条 user 消息，够用了。

### 7.2 精读项目里的真实 Prompt

`AiClient.kt` 中，每次学生投递都会组装这段话发给 Llama 3.3：

```kotlin
val prompt = """
    You are a hiring assistant for part-time student jobs in Malaysia.
    Rate how well this student matches this job.
    Reply ONLY with valid JSON in exactly this shape:
    {"matchPercent": <integer 0-100>, "suggestedStatus": "ACCEPTED" or "REJECTED", "reason": "<one short sentence>"}

    Job: ${job.title} at ${job.companyName}, ${job.location}.
    Pay: RM ${job.payPerHour} per hour, ${job.hoursPerWeek} hours per week.
    Description: ${job.description}

    Student: ${student.name}, ${student.organization.ifBlank { "university not stated" }}.
    Bio: ${student.bio.ifBlank { "(no bio provided)" }}
""".trimIndent()
```

这段 Prompt 有 5 个值得抄走作业的设计点：

1. **先派角色**："You are a hiring assistant..." —— 给模型一个明确身份，回答质量立刻提升。
2. **任务动词精确**："Rate how well this student matches" —— 不是"看看这两个合不合适"这种模糊表述。
3. **输出格式写成合同**：直接给出 JSON 模板，字段名、取值范围（0-100）、枚举值（ACCEPTED/REJECTED）全部锁死。配合请求体里的 `response_format: json_object` 双保险。
4. **喂的是结构化的事实**，不是一大段自由文本：职位的每个属性单独一行，模型不需要自己从段落里"捞"信息。
5. **兜底处理缺失数据**：`bio.ifBlank { "(no bio provided)" }` —— 宁可明说"没有"，也不能留空让模型瞎猜。

### 7.3 为什么非要 JSON？

因为返回值要**存进数据库**（`ai_match_percent`、`ai_suggested_status`、`ai_reason` 三列）并在界面上渲染。自由文本没法可靠地拆解，JSON 可以：

```kotlin
// 从回信中层层剥洋葱：choices[0].message.content
val text = json.parseToJsonElement(raw)...jsonPrimitive.content
// 再解析里面的 JSON
val obj = json.parseToJsonElement(text).jsonObject
```

### 7.4 防御式设计：AI 是实习生，不是老板

再看一层工程化思维（这段设计思想比代码本身更值钱）：

```kotlin
suspend fun assessApplication(...): AiAssessment? = runCatching { ... }
    .onFailure { Log.e(TAG, "Groq call failed", it) }
    .getOrNull()     // 任何失败 → 返回 null
```

- `runCatching` 把整段包起来：网络挂了、key 失效、模型抽风返回怪 JSON……统统不会让 App 崩溃。
- 失败返回 `null`，调用方拿到 null 就跳过 AI 字段、申请照常入库。**学生的投递永远不会因为 AI 挂掉而失败** —— 顶多这张卡片不显示。
- 就算 AI 给了建议，`suggestedStatus` 也只是界面上的一个"参考意见"，Accept/Reject 按钮永远握在雇主手里（human-in-the-loop）。

### 7.5 用 AI 帮你学习的三个万能模板

开发期间你会大量求助 ChatGPT/Claude/Gemini，模板质量决定回答质量：

**模板 ① 解释代码**
```
我是 Kotlin 零基础新手，正在参与一个 Jetpack Compose + Supabase 的 Android 项目。
请逐行解释下面这段代码，每遇到一个新概念，用一个生活中的比喻帮助我理解，
最后用三句话总结它在整个数据流（Screen → ViewModel → Repository）中的位置：
<粘贴代码>
```

**模板 ② 修报错**
```
我在 Android Studio 遇到这个报错，请先告诉我最可能的 1-3 个原因（按概率排序），
再给出改动最小的修复方案：
完整报错：<从 Logcat 或 Build 输出原样粘贴>
相关代码：<粘贴出错的文件片段，注明文件名>
环境：Kotlin + Jetpack Compose + Supabase，minSdk 24
```

**模板 ③ 写新功能**
```
我要在这个项目中实现：<用一两句话描述需求>
项目风格参考（现有类似文件）：<粘贴一个相近的 Screen + ViewModel>
请给出需要新增/修改的每个文件的完整代码，中文注释解释关键行，
并遵守项目约定：路由只写在 Routes.kt、业务规则不放 Screen、
状态用 StateFlow 从 ViewModel 流向界面。
```

### 7.6 练习 ✍️

用模板 ① 让 AI 解释 `supabase_schema.sql` 里 "student applies as self" 那条 policy，然后对照第 6.5 节自查：你能用自己的话说清 `with check` 和 `using` 的区别吗？

---

## 8. 代码地图：按这个顺序读 10 个文件

> 总共约 1500 行有效代码，全部读完一个下午足够。顺序经过设计，前面的文件是后面的地基。

| # | 文件 | 行数量级 | 为什么读它 |
|---|------|---------|-----------|
| 1 | `MainActivity.kt` | ~20 | 入口。看懂"App 启动后第一件事是什么" |
| 2 | `ui/theme/Theme.kt` + `Color.kt` | ~80 | 主题从哪来，颜色在哪定义 |
| 3 | `navigation/Routes.kt` | ~36 | 全部页面路径一览，相当于地图图例 |
| 4 | `navigation/NavGraph.kt` | — | 地铁调度中心：路径 ↔ 页面如何绑定 |
| 5 | `data/data/FairWage.kt` | ~13 | 全项目最短的文件，却是核心业务规则的"单一事实来源" |
| 6 | `data/data/SupabaseClientProvider.kt` | ~27 | 数据库连接怎么建、密钥怎么进来 |
| 7 | `ui/job/JobListViewModel.kt` | ~31 | UDF 的教科书样本：Repository → StateFlow → Screen |
| 8 | `ui/job/JobListScreen.kt` | ~165 | 最典型的 Compose 界面：加载态、网格、卡片、事件上抛 |
| 9 | `data/data/JobRepository.kt` | — | 看 Supabase 查询怎么写、Realtime 怎么订阅 |
| 10 | `data/data/AiClient.kt` | ~97 | AI 顾问全貌：Prompt 组装 → HTTP → 解析 → 防御 |

读的时候开着纸/笔记，给每个文件写一句话总结。说不清的文件，就是你要重点补的章节。

---

## 9. 上手任务（按顺序完成，才算正式 onboarded）

### 任务 1 · 热身：改主题（30 分钟）

- 打开 `ui/theme/Color.kt`，把品牌主色换成另一个颜色（比如紫色系）。
- 同时把 `JobListScreen` 顶栏标题 "Jobs Near You" 改成你想要的名字。
- **验收**：Run 之后启动页/按钮/顶栏全部变色。
- **目的**：走通"改代码 → 编译 → 看到效果"的完整循环，顺便熟悉 Color/Theme 的关系。

### 任务 2 · SQL 实战（1 小时）

完成 6.7 节的 5 道 SQL 题，截图或保存你的查询语句，交给队友 review。

### 任务 3 · 毕业挑战：给职位加"类别"标签（1–2 天）⭐

需求：雇主发布职位时可以选择一个类别（如 Food Service / Retail / Tutoring / Events / General），学生在职位卡片上能看到彩色标签。

这是一条**纵贯全部五层**的完整改动，做完你就摸遍了整个架构：

```
数据库 → DTO → 领域模型 → Repository/ViewModel → UI
```

Checklist（强烈建议严格自上而下按序做，每步做完先让 App 能编译）：

- [ ] **1. 数据库**：SQL Editor 执行
  ```sql
  alter table public.jobs
  add column category text not null default 'General';
  ```
  （已有数据不受影响，老职位自动归入 General —— 这就是 default 的好处）
- [ ] **2. DTO**：`data/data/SupabaseDtos.kt` 里 jobs 相关的 DTO 加 `category` 字段
- [ ] **3. 领域模型**：`data/data/Job.kt` 加字段；`ui/job/JobUi.kt` 的 UI 模型和 `toUi()` 映射同步加
- [ ] **4. 表单**：`ui/employer/PostJobScreen.kt` 加类别选择（可以用一排 `FilterChip`，参考项目里已有的 Chip 用法）
- [ ] **5. 展示**：`ui/job/JobListScreen.kt` 的 `JobCard` 里加一个类别小标签（模仿 `FairWageBadge` 的写法）
- [ ] **验收**：发一个 "Tutoring" 类别的新职位 → 学生端列表卡片上能看到标签；老职位显示 General。

提示：

- 每一步都可以让 AI 代劳，但**你必须看懂每一行 diff** —— 面试/汇报时会被问到。
- 卡在某一步编译不过，用模板 ② 求助；别忘了贴上完整报错。
- 做完后 commit，commit message 用英文一行说清做了什么，例如 `feat: add job category field across db/dto/ui layers`。

---

## 10. 常见坑与自救指南

| 症状 | 最可能的原因 | 解法 |
|------|-------------|------|
| Gradle Sync 一直失败 | 网络问题 / JDK 版本不对 | File → Settings 检查 Gradle JDK ≥ 11；关代理重试；Invalidate Caches & Restart |
| App 秒崩，日志提到 `BuildConfig` 或空白 URL | `local.properties` 缺失/位置错/key 拼错 | 对照 2.5 节检查；改完**必须重新 Sync + Run** |
| 登录成功但列表永远转圈/为空 | schema 没跑 / RLS 正常拦截了未登录请求 / anon key 不是这个项目的 | 先去 Supabase Table Editor 确认表里有数据；再核对 URL 和 key 是否同一个项目 |
| 发布职位被拒绝 | 触发了 Fair-Wage Check | 这不是 bug！时薪必须 ≥ RM 8.72（规则在 `FairWage.kt`） |
| AI 匹配度一直是空的 | Groq key 无效 / 余额用完 / 网络不通 | Logcat 过滤框输入 `AiClient` 看红色错误；注意设计上 AI 失败**静默跳过**，不影响申请 |
| 改了界面没生效 | 改错文件 / 没 rebuild / 模拟器没重装 | 确认改的是 `main` source set；Clean Project 后重新 Run |
| `Unresolved reference: xxx` | import 丢了 / 依赖没加 | 光标放红字上按 `Alt+Enter` 让 IDE 自动 import |

**Logcat 是你最好的朋友**：Android Studio 底部 Logcat 面板，用 `package:com.kerjalah.app level:error` 过滤，只看你 App 的报错。

**求助时的黄金模板**（发给队友或 AI 都适用）：

```
1. 我想做什么：
2. 我做了什么（操作步骤）：
3. 我期望发生： / 实际发生：
4. 完整报错文本（原样粘贴，不要截断）：
5. 相关文件名：
```

---

## 11. 术语表（中英对照）

| 英文 | 中文 | 一句话解释 |
|------|------|-----------|
| Kotlin | — | JetBrains 出品的编程语言，Android 官方首选 |
| Jetpack Compose | — | Google 的声明式 Android UI 框架 |
| Composable | 可组合函数 | 带 `@Composable` 注解的 UI 函数 |
| Recomposition | 重组 | 状态变化后框架重新执行 Composable 来刷新界面 |
| State / StateFlow | 状态 / 状态流 | 可观察的数据容器，变了就通知界面 |
| UDF (Unidirectional Data Flow) | 单向数据流 | 数据向下、事件向上的架构纪律 |
| MVVM | Model-View-ViewModel | 教程里对「View + ViewModel + Model」分层的常见称呼；本项目即此结构配 UDF 数据流向（见第 5 章名词澄清） |
| ViewModel | 视图模型 | 持有界面状态与逻辑的层，屏幕旋转也不丢数据 |
| Repository | 仓库/仓储层 | 唯一负责网络与数据来源的层 |
| Coroutine | 协程 | Kotlin 的轻量异步方案 |
| `suspend fun` | 挂起函数 | 可暂停等待（如网络响应）而不卡界面的函数 |
| Null Safety | 空安全 | 编译器强制你处理"值可能不存在"的情况 |
| data class | 数据类 | 自动生成 equals/copy 等方法的纯数据载体 |
| object (singleton) | 单例 | 全局唯一实例，如 `FairWage`、`Routes` |
| DSL / Modifier | 修饰符 | Compose 里给组件"装修"的链式配置器 |
| Material 3 | — | Google 官方设计系统（颜色/字体/组件规范） |
| Navigation Compose | — | 页面路由框架，本项目集中于 `Routes.kt` |
| SQL | 结构化查询语言 | 操作关系型数据库的语言 |
| PostgreSQL | — | Supabase 底层的开源关系型数据库 |
| Table / Row / Column | 表 / 行 / 列 | 数据库的基本组织单位 |
| Primary Key (PK) | 主键 | 行的唯一标识，本项目用 UUID |
| Foreign Key (FK) | 外键 | 指向另一张表主键的字段，表达关系 |
| RLS (Row Level Security) | 行级安全 | 数据库里按行控制"谁能读写"的策略 |
| Supabase | — | 开源 BaaS：数据库 + 认证 + 实时推送 + 自动 API |
| PostgREST | — | 把数据库表自动变成 REST API 的服务 |
| Realtime | 实时通道 | 基于 WebSocket 的数据库变更推送 |
| DTO | 数据传输对象 | 专门用于网络传输的对象（`@Serializable`） |
| Serialization | 序列化 | 对象 ↔ JSON 文本的互相转换 |
| LLM | 大语言模型 | 如 Llama 3.3、GPT、Claude |
| Prompt | 提示词 | 发给大模型的指令文本 |
| Token | 词元 | 模型计量文本的单位（计费按 token） |
| Human-in-the-loop | 人机协同决策 | AI 只建议，最终由人拍板 |
| Fire-and-forget | 发射后不管 | 异步任务发出后不等结果、失败也不影响主流程 |

---

## 结语

KerjaLah 的代码量不大、结构极其规整，是一个非常适合第一次接触真实项目的练手场。
按路线图走完两周内你就能独立承担功能。记住两条心法：

1. **跑起来 > 看懂 > 改动 > 新增**，永远按这个顺序推进，不要跳级。
2. **AI 是加速器不是代驾** —— 让它写代码之前，先确保你看得懂它写了什么。

祝顺利上车 —— *KerjaLah!* 💪

> 有任何问题随时找你的队友（本文档作者的同组伙伴）。发现文档里有讲错或过时的地方，请直接指出，我们一起维护它。
