#!/usr/bin/env bash
# ============================================================================
# 后端接口联调验证（三端角色 + 核心业务流程）
# 用法：bash scripts/api-test.sh [BASE_URL]
# ============================================================================
set -u
BASE="${1:-http://127.0.0.1:8088}"
PASS=0
FAIL=0

c_pass() { echo "PASS  $1"; PASS=$((PASS+1)); }
c_fail() { echo "FAIL  $1  →  $2"; FAIL=$((FAIL+1)); }

# 断言：响应里包含指定字符串
expect() {
  local name="$1" body="$2" needle="$3"
  if echo "$body" | grep -q -- "$needle"; then c_pass "$name"; else c_fail "$name" "$(echo "$body" | head -c 200)"; fi
}

# 从 JSON 里取简单字段（避免依赖 jq）
jget() {
  echo "$1" | python3 -c "import sys,json;d=json.load(sys.stdin);print(eval(\"d$2\"))" 2>/dev/null || echo ""
}

echo "================ 0. 公开接口 ================"
B=$(curl -s "$BASE/api/enums")
expect "字典接口免登录可访问" "$B" '"code":200'
expect "字典包含准驾车型" "$B" 'C1'

echo
echo "================ 1. 鉴权 ================"
B=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/students/me/progress")
[ "$B" = "401" ] && c_pass "未登录访问受保护接口返回 401" || c_fail "未登录应返回 401" "实际 $B"

B=$(curl -s -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"wrong"}')
expect "错误密码登录失败" "$B" '"code":401'

# 三个角色登录，各自保存 cookie
CK_ADMIN=$(mktemp); CK_COACH=$(mktemp); CK_STU=$(mktemp)

B=$(curl -s -c "$CK_ADMIN" -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}')
expect "管理员登录成功" "$B" '"role":"admin"'

B=$(curl -s -c "$CK_COACH" -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"coach","password":"123456"}')
expect "教练登录成功" "$B" '"role":"coach"'
COACH_ID=$(jget "$B" "['data']['coachId']")
[ -n "$COACH_ID" ] && c_pass "教练登录返回 coachId=$COACH_ID" || c_fail "教练登录应返回 coachId" "$B"

B=$(curl -s -c "$CK_STU" -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"student","password":"123456"}')
expect "学员登录成功" "$B" '"role":"student"'
STU_ID=$(jget "$B" "['data']['studentId']")
[ -n "$STU_ID" ] && c_pass "学员登录返回 studentId=$STU_ID" || c_fail "学员登录应返回 studentId" "$B"

B=$(curl -s -b "$CK_STU" "$BASE/api/auth/me")
expect "获取当前登录用户" "$B" '"username":"student"'

echo
echo "================ 2. 学员端 F02 培训进度 ================"
B=$(curl -s -b "$CK_STU" "$BASE/api/students/me/progress")
expect "培训进度接口可用" "$B" '"code":200'
expect "返回带教教练" "$B" 'coachName'
expect "返回车辆" "$B" 'vehiclePlate'
expect "返回各科目进度" "$B" 'subjects'
expect "返回整体完成率" "$B" 'percent'
expect "身份证已脱敏" "$B" '310101********'
expect "返回结业判定" "$B" 'graduationReady'

echo
echo "================ 3. 学员端 F03 约车 ================"
B=$(curl -s -b "$CK_STU" "$BASE/api/schedules/mine")
expect "可预约时段接口可用" "$B" '"code":200'
expect "时段含 bookedByMe 标记" "$B" 'bookedByMe'
# 业务规则：一名学员最多同时持有 2 条「未完成」预约（待确认 / 已确认）。
# 先清掉已有未完成预约（待确认→取消，已确认→标记完成），再测预约与上限，保证脚本可重复执行。
B=$(curl -s -b "$CK_STU" "$BASE/api/bookings/my")
ACTIVE_IDS=$(echo "$B" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']['list']
for x in d:
    if x['status']=='待确认': print('C', x['id'])
    elif x['status']=='已确认': print('F', x['id'])
")
echo "$ACTIVE_IDS" | while read -r kind id; do
  [ -z "$kind" ] && continue
  if [ "$kind" = "C" ]; then
    curl -s -b "$CK_STU" -X PATCH "$BASE/api/bookings/$id/cancel" > /dev/null
  fi
done
# 已确认的预约由教练端标记完成（管理员也可），这里用管理员接口
echo "$ACTIVE_IDS" | while read -r kind id; do
  [ "$kind" = "F" ] && curl -s -b "$CK_ADMIN" -X PATCH "$BASE/api/bookings/$id/status" \
    -H 'Content-Type: application/json' -d '{"status":"已完成"}' > /dev/null
done

# 取 3 个尚未预约、状态为可约的时段
B=$(curl -s -b "$CK_STU" "$BASE/api/schedules/mine")
read -r S1 S2 S3 <<< "$(echo "$B" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
free=[x['id'] for x in d if x.get('status')=='可约' and not x.get('bookedByMe')]
print(' '.join(str(i) for i in free[:3]))
")"
if [ -z "${S1:-}" ] || [ -z "${S2:-}" ]; then
  c_fail "找不到可预约时段" "可约时段不足 2 个"
else
  B=$(curl -s -b "$CK_STU" -X POST "$BASE/api/bookings" -H 'Content-Type: application/json' \
    -d "{\"scheduleId\":$S1}")
  expect "第 1 条预约提交成功" "$B" '"status":"待确认"'

  B=$(curl -s -b "$CK_STU" -X POST "$BASE/api/bookings" -H 'Content-Type: application/json' \
    -d "{\"scheduleId\":$S2}")
  expect "第 2 条预约提交成功（未完成预约上限为 2）" "$B" '"status":"待确认"'

  if [ -n "${S3:-}" ]; then
    B=$(curl -s -b "$CK_STU" -X POST "$BASE/api/bookings" -H 'Content-Type: application/json' \
      -d "{\"scheduleId\":$S3}")
    expect "第 3 条预约被拒（最多 2 条未完成）" "$B" '你已有 2 个未完成的预约'
  else
    echo "SKIP  未完成预约上限校验（可约时段不足 3 个）"
  fi

  # 同一时段重复预约仍应被拒
  B=$(curl -s -b "$CK_STU" -X POST "$BASE/api/bookings" -H 'Content-Type: application/json' \
    -d "{\"scheduleId\":$S1}")
  expect "重复预约同一时段被拒" "$B" '你已预约该时段'
fi

echo
echo "================ 3.1 考试报名顺序限定 ================"
B=$(curl -s -b "$CK_STU" "$BASE/api/exam-plans/open")
expect "考试计划含报考顺序校验字段" "$B" 'prerequisiteMet'
# 找一个「上一科目还没报名」的场次，报名应被拒
BLOCKED=$(echo "$B" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data']
for x in d:
    if x.get('prerequisiteMet') is False and not x.get('registered'):
        print(x['id']); break
")
if [ -z "$BLOCKED" ]; then
  echo "SKIP  报考顺序校验（当前数据里没有「前置科目未报名」的场次）"
else
  B=$(curl -s -b "$CK_STU" -X POST "$BASE/api/exam-plans/$BLOCKED/register")
  expect "上一科目未报名时不允许报名后一科目" "$B" '请先报名'
fi

B=$(curl -s -b "$CK_STU" "$BASE/api/bookings/my")
expect "我的预约列表" "$B" '"code":200'

echo
echo "================ 4. 教练端 F07 预约处理 ================"
B=$(curl -s -b "$CK_COACH" "$BASE/api/coach/workbench")
expect "教练工作台可用" "$B" '"code":200'
expect "工作台含待确认预约数" "$B" 'pendingBookings'

B=$(curl -s -b "$CK_COACH" --get --data-urlencode "status=待确认" "$BASE/api/coach/bookings")
expect "教练查看待确认预约" "$B" '"code":200'
BK=$(jget "$B" "['data'][0]['id']")

B=$(curl -s -b "$CK_COACH" -X PATCH "$BASE/api/coach/bookings/$BK/status" \
  -H 'Content-Type: application/json' -d '{"status":"已确认"}')
expect "教练确认预约" "$B" '"code":200'

echo
echo "================ 5. 角色越权拦截 ================"
B=$(curl -s -o /dev/null -w "%{http_code}" -b "$CK_STU" "$BASE/api/users")
[ "$B" = "403" ] && c_pass "学员访问用户管理被拒（403）" || c_fail "学员访问用户管理应 403" "实际 $B"

B=$(curl -s -o /dev/null -w "%{http_code}" -b "$CK_STU" "$BASE/api/coach/workbench")
[ "$B" = "403" ] && c_pass "学员访问教练工作台被拒（403）" || c_fail "学员访问教练工作台应 403" "实际 $B"

B=$(curl -s -o /dev/null -w "%{http_code}" -b "$CK_COACH" "$BASE/api/statistics/overview")
[ "$B" = "403" ] && c_pass "教练访问管理员看板被拒（403）" || c_fail "教练访问看板应 403" "实际 $B"

echo
echo "================ 6. 教练端 F08 学时登记 ================"
B=$(curl -s -b "$CK_COACH" -X POST "$BASE/api/coach/hours" -H 'Content-Type: application/json' \
  -d "{\"studentId\":$STU_ID,\"subjectCode\":\"3\",\"hours\":2,\"trainDate\":\"2026-10-15\"}")
expect "教练登记学时" "$B" '"auditStatus":"待审核"'
HR=$(jget "$B" "['data']['id']")

echo
echo "================ 7. 管理端 F13 学时审核 ================"
B=$(curl -s -b "$CK_ADMIN" --get --data-urlencode "auditStatus=待审核" "$BASE/api/hours")
expect "管理员查看待审核学时" "$B" '"code":200'

B=$(curl -s -b "$CK_ADMIN" -X PATCH "$BASE/api/hours/$HR/audit" \
  -H 'Content-Type: application/json' -d '{"pass":true}')
expect "管理员审核通过学时" "$B" '"code":200'

B=$(curl -s -b "$CK_STU" "$BASE/api/students/me/progress")
expect "学时通过后计入学员进度" "$B" '"doneHoursBySubject"'

echo
echo "================ 8. 管理端 F11 学员管理 ================"
B=$(curl -s -b "$CK_ADMIN" "$BASE/api/students?page=1&size=5")
expect "学员分页检索" "$B" '"total"'
expect "学员列表身份证脱敏" "$B" '310101********'

B=$(curl -s -b "$CK_ADMIN" --get --data-urlencode "status=在训" "$BASE/api/students")
expect "按状态筛选学员" "$B" '"status":"在训"'

B=$(curl -s -b "$CK_ADMIN" -X POST "$BASE/api/students" -H 'Content-Type: application/json' \
  -d '{"name":"测试学员","phone":"13900009999","licenseType":"C1","idCard":"310101199901019999"}')
expect "报名登记新增学员" "$B" '测试学员'

echo
echo "================ 9. 管理端 F12 教练与车辆 ================"
B=$(curl -s -b "$CK_ADMIN" "$BASE/api/coaches")
expect "教练列表" "$B" '"code":200'
expect "教练含带教人数" "$B" 'studentCount'

B=$(curl -s -b "$CK_ADMIN" "$BASE/api/vehicles")
expect "车辆列表" "$B" '沪A·1234学'

echo
echo "================ 10. 管理端 F14 考试管理 ================"
B=$(curl -s -b "$CK_ADMIN" "$BASE/api/exam-plans")
expect "考试计划列表" "$B" '"code":200'
expect "考试计划含科目" "$B" 'subjectCode'

B=$(curl -s -b "$CK_ADMIN" "$BASE/api/exam-scores")
expect "考试成绩列表（JOIN 科目）" "$B" '"code":200'

echo
echo "================ 11. 管理端 F15 统计报表 ================"
B=$(curl -s -b "$CK_ADMIN" "$BASE/api/statistics/overview")
expect "工作台指标" "$B" 'trainingCount'
expect "待办事项" "$B" 'pendingHours'
expect "报名趋势" "$B" 'enrollTrend'

B=$(curl -s -b "$CK_ADMIN" "$BASE/api/statistics/report")
expect "统计报表" "$B" 'subjectPassRate'
expect "报表含教练工作量" "$B" 'coachWorkload'

echo
echo "================ 12. 管理端 F16 用户管理 ================"
B=$(curl -s -b "$CK_ADMIN" "$BASE/api/users")
expect "用户列表" "$B" '"code":200'
if echo "$B" | grep -q '"password":[^n]'; then
  c_fail "用户列表不应返回密码" "泄露了 password 字段"
else
  c_pass "用户列表不返回密码"
fi

NEWUSER="newcoach$(date +%s)"
B=$(curl -s -b "$CK_ADMIN" -X POST "$BASE/api/users" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$NEWUSER\",\"name\":\"新教练\",\"role\":\"coach\"}")
expect "创建用户" "$B" "$NEWUSER"

echo
echo "================ 13. 登出 ================"
B=$(curl -s -b "$CK_STU" -X POST "$BASE/api/auth/logout")
expect "登出成功" "$B" '"code":200'
B=$(curl -s -o /dev/null -w "%{http_code}" -b "$CK_STU" "$BASE/api/students/me/progress")
[ "$B" = "401" ] && c_pass "登出后会话失效" || c_fail "登出后应 401" "实际 $B"

rm -f "$CK_ADMIN" "$CK_COACH" "$CK_STU"

echo
echo "=============================================="
echo "  通过 $PASS 项，失败 $FAIL 项"
echo "=============================================="
[ "$FAIL" -eq 0 ] || exit 1
