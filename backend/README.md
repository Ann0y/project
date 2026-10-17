# 驾校学员培训与考试管理系统 · 后端

Spring Boot 4.1 + MyBatis + MySQL/MariaDB，实现《需求文档》第 6 章数据库设计与第 7 章接口。

## 技术栈

| 层次 | 选型 |
| --- | --- |
| Web | Spring Boot 4.1（内嵌 Tomcat 11） |
| 持久层 | MyBatis 4.1（注解 SQL） |
| 数据库 | MySQL 8 / MariaDB 10.6+，库名 `driving_school` |
| 认证 | HttpSession（`JSESSIONID`），30 分钟滑动过期 |
| 加密 | BCrypt（密码）/ AES-GCM（身份证号） |

## 快速开始

```bash
# 1. 建库建表（含科目学时初始数据）
mysql -uroot -p123456 < src/main/resources/sql/schema.sql

# 2. 启动（默认 8080）
mvn spring-boot:run
#   若 8080 被占用：
java -jar target/backend-1.0-SNAPSHOT.jar --server.port=8088

# 3. 接口联调验证
bash scripts/api-test.sh http://127.0.0.1:8088
```

首次启动时 `DataInitializer` 会自动写入演示数据（`sys_user` 为空时才执行）。

## 演示账户（密码统一 `123456`）

| 用户名 | 角色 | 说明 |
| --- | --- | --- |
| `admin` | 管理员 | 全站权限 |
| `coach` | 教练 | 王建国，工号 C001，带教 8 名学员 |
| `student` | 学员 | 李小明，C1 在训，科目一已满、科目二 77% |

另有 `coach02` / `coach03` 两名教练、14 个学员账号，覆盖
在训 / 待审核 / 结业 / 退学 各种状态，密码同样为 `123456`。

### 初始化数据规模

首次启动由 `DataInitializer` 生成，便于演示与答辩：

| 数据 | 数量 | 说明 |
| --- | --- | --- |
| 教练 | 3 人 | 名下共 5 台教练车（含 1 台维修中） |
| 学员 | 16 人 | 覆盖四种状态、多种准驾车型 |
| 排课时段 | 122 个 | 未来 14 天、跳过周日、每天 4 个时段 |
| 预约记录 | 18 条 | 覆盖待确认/已确认/已完成/已取消/爽约 |
| 学时记录 | 218 条 | 按各科目逐条登记，含待审核记录 |
| 考试计划 | 6 场 | 含历史场次与未来报名中场次 |
| 考试记录 | 18 条 | 覆盖合格/不合格/补考中/待考 |
| 缴费记录 | 16 条 | 已缴 / 未缴 / 退费 |

## 目录结构

```
src/main/java/sch/whvcse/
├── common/          Result / PageResult / BizException / 全局异常 / 枚举常量
├── config/          DataInitializer（种子数据）、WebConfig（CORS+拦截器）、密码器
├── controller/      Auth / Student / Coach / Admin 四个控制器
├── entity/          11 张表的实体
├── interceptor/     LoginInterceptor（只校验是否登录）
├── mapper/          9 个 Mapper（注解 SQL）
├── service/         业务接口 + impl
├── util/            IdCardCrypto（AES 加解密+脱敏）、SessionUser（身份解析）
└── vo/              返回给前端的视图对象
src/main/resources/
├── sql/schema.sql   建库建表脚本
└── application.properties
```

## 接口清单（对齐需求文档第 7 章）

认证与字典免登录或按角色限制，其余接口均需登录。

### 认证 / 字典

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 注册（学员），同时创建待审核学员档案 |
| POST | `/api/auth/login` | 登录，返回 `studentId` / `coachId` |
| GET | `/api/auth/me` | 当前登录用户 |
| POST | `/api/auth/logout` | 登出 |
| PUT | `/api/auth/password` | 修改本人密码 |
| GET | `/api/enums` | 字典枚举（**免登录**） |

### 学员端 F02–F04

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/students/me/progress` | 我的培训进度（学时/科目/结业判定/时间轴） |
| GET | `/api/schedules/mine?date=` | 本人带教教练的可约时段 |
| GET | `/api/bookings/my?status=&page=&size=` | 我的预约 |
| POST | `/api/bookings` | 提交预约（E1 冲突检测） |
| PATCH | `/api/bookings/{id}/cancel` | 取消预约 |
| GET | `/api/exam-scores/my?subjectCode=&status=` | 我的成绩 |
| GET | `/api/exam-plans/open` | 可报名考试计划（含学时达标判定） |
| POST | `/api/exam-plans/{id}/register` | 考试报名（校验学时/容量） |
| GET | `/api/payments/my` | 我的缴费记录 |

### 教练端 F05–F09

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/coach/workbench` | 工作台（今日排课/待确认/带教数/7 天趋势） |
| GET/POST/PUT/DELETE | `/api/coach/schedules[/{id}]` | 我的排课 CRUD |
| GET | `/api/coach/bookings?status=&studentName=` | 派给我的预约 |
| PATCH | `/api/coach/bookings/{id}/status` | 预约状态流转 |
| GET/POST | `/api/coach/hours` | 学时登记 |
| GET | `/api/coach/students` | 我的学员（含进度） |

### 管理端 F10–F16

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/statistics/overview` | 工作台指标 + 待办 |
| GET/POST/PUT/DELETE | `/api/students[/{id}]` | 学员档案（多条件分页查询） |
| POST | `/api/students/{id}/audit` | 报名审核 |
| PUT | `/api/students/{id}/coach` | 分配带教教练与车辆 |
| PATCH | `/api/students/{id}/status` | 状态流转（在训/结业/退学） |
| GET/POST/PUT/DELETE | `/api/coaches[/{id}]` | 教练管理 |
| GET/POST/PUT/DELETE | `/api/vehicles[/{id}]` | 车辆管理 |
| GET | `/api/bookings`、PATCH `/api/bookings/{id}/status` | 预约管理 |
| GET | `/api/hours`、PATCH `/api/hours/{id}/audit` | 学时审核 |
| GET | `/api/courses`、PUT `/api/courses/{id}` | 科目学时配置 |
| GET/POST/PUT/DELETE | `/api/exam-plans[/{id}]` | 考试计划 |
| POST | `/api/exam-plans/{id}/confirm` | 确认名单 |
| GET/POST | `/api/exam-scores` | 成绩查询 / 录入 |
| GET | `/api/statistics/report` | 统计报表（E2 图表数据） |
| GET/POST/PUT/DELETE | `/api/users[/{id}]` | 用户管理与角色分配 |
| PATCH | `/api/users/{id}/status`、`/password` | 启停用 / 重置密码 |
| GET/POST/DELETE | `/api/payments[/{id}]`、`POST /register` | 缴费登记 |

## 关键设计说明

**统一响应体**：`{code, message, data}`，`code=200` 为成功。异常同时映射真实 HTTP 状态码（401/403/404/400/500），便于前端 axios 拦截器识别。

**身份不进前端参数**：学员/教练接口一律从 Session 解析 `studentId`/`coachId`，不接受前端传参，杜绝越权查他人数据。管理端写操作额外校验 `requireAdmin`。

**身份证加解密**：`IdCardCrypto` 用 AES/GCM + 随机 IV，密文形如 `v1:Base64(iv+ct)`，落库长度 67 字符（`student.id_card` 设为 `VARCHAR(128)`）。列表展示统一脱敏为 `前6********后4`。

**固定带教（需求 2）**：学员只能预约 `student.coach_id` 对应教练发布的时段，服务端在 `book()` 中强校验。

**E1 冲突检测**：`booking` 表 `uk_student_schedule` + 容量校验 + 状态校验三重保障；`coach_schedule.booked_count` 在预约/取消/爽约时同步更新。

**一人一预约**：同一学员同一时间只能存在 1 个有效预约（待确认 / 已确认）。
已存在有效预约时再次预约会被拒：`"你已有一个未完成的预约，请先完成或取消后再预约"`。
已取消 / 爽约 / 已完成的记录不占用名额；重新预约曾被取消的同一时段会复用原记录
（`booking` 表有 `uk_student_schedule` 唯一约束，不能重复插入）。

**可预约时间窗口**：学员端只能预约**今天起 5 天内**的时段（今天 ~ 第 4 天）。
该限制在 `StudentPortalServiceImpl` 中双重生效：
`/api/schedules/mine` 只返回窗口内且未过期的时段，`book()` 也会拒绝越界时段，
防止绕过前端直接调接口。前端约车页用「日期下拉 + 时段下拉」两步选择。

**整体完成率**：按**各科目完成率的平均值**计算（4 科等权），而非「总学时 / 总要求学时」。
单科完成率封顶 100%，超额学时不计入，避免某一科超额拉高整体。
例：科目一 100%、科目二 77%、科目三 42%、科目四 0% → 整体 **55%**
（若按学时总和口径则会是 58%）。学员端首页与教练端「我的学员」使用同一口径。

**结业判定**：系统只计算「学时全部达标 + 4 科目全部合格」并提示，最终由管理员 `PATCH /api/students/{id}/status` 手动置为结业。

## 配置项

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/driving_school?...
spring.datasource.username=root
spring.datasource.password=123456
server.servlet.session.timeout=30m
app.crypto.aes-key=driving-school-2026-secret-key   # 生产请用环境变量注入
```

## IDE 使用说明

IDEA 中「代码爆红但能正常运行」的排查请见 [docs/IDEA-配置说明.md](docs/IDEA-配置说明.md)，
已修复项目 JDK 指向错误与 Lombok 处理器路径写死两个问题。

## IDE 使用说明

IDEA 中「代码爆红但能正常运行」的排查见 [docs/IDEA-配置说明.md](docs/IDEA-配置说明.md)。
已修复两个根因：项目 JDK 指向了不存在的 `openjdk-26`、Lombok 注解处理器路径被写死。

## 已知限制

- 本期不做接口级细粒度 RBAC（需求附录 A），仅以「角色 + 本人口径」做数据过滤。
- 身份证号加密后无法用 `LIKE` 直接检索，管理员按证件号查询时先按其它条件缩小范围再在内存中匹配。
- 缴费记录为流水表，不做逻辑删除（需求 6.1），删除接口为物理删除，仅管理员可用。
