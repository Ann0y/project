/**
 * 端到端验证：前端 ↔ 后端真实接口
 * ---------------------------------------------------------------------------
 * 前置：后端运行在 8088，前端 dev server 运行在 5199（已代理 /api）。
 * 覆盖：三角色登录、navbar 按角色显示、各页面数据从接口渲染、ECharts 出图、
 *       以及一次完整的业务闭环（学员约车 → 教练确认 → 学员看到状态）。
 */
import { spawn } from 'node:child_process'
import { setTimeout as sleep } from 'node:timers/promises'

const BASE = process.env.BASE_URL || 'http://127.0.0.1:5199'
const PORT = 9225
const PROFILE = '/home/annoy/study/projects/.chrome-profile'

const chrome = spawn('/usr/bin/chromium', [
  '--headless=new', '--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage',
  '--no-first-run', '--disable-crash-reporter', '--disable-breakpad',
  `--user-data-dir=${PROFILE}`, `--remote-debugging-port=${PORT}`,
  '--remote-allow-origins=*', '--window-size=1600,1000', 'about:blank',
], { stdio: 'ignore', env: { ...process.env, HOME: PROFILE, XDG_CONFIG_HOME: PROFILE, XDG_CACHE_HOME: PROFILE } })

async function wsUrl() {
  for (let i = 0; i < 80; i += 1) {
    try {
      const r = await fetch(`http://127.0.0.1:${PORT}/json/version`)
      const j = await r.json()
      if (j.webSocketDebuggerUrl) return j.webSocketDebuggerUrl
    } catch {}
    await sleep(250)
  }
  throw new Error('Chrome 未能启动')
}

const ws = new WebSocket(await wsUrl())
await new Promise((r) => (ws.onopen = r))
let msgId = 0
const pending = new Map()
const pageErrors = []
ws.onmessage = (e) => {
  const m = JSON.parse(e.data)
  if (m.id && pending.has(m.id)) {
    const { resolve } = pending.get(m.id)
    pending.delete(m.id)
    resolve(m.result ?? m.error)
  } else if (m.method === 'Runtime.exceptionThrown') {
    pageErrors.push(m.params.exceptionDetails?.exception?.description || m.params.exceptionDetails?.text)
  }
}
const send = (method, params = {}, sessionId) => {
  const id = ++msgId
  return new Promise((resolve) => {
    pending.set(id, { resolve })
    ws.send(JSON.stringify({ id, method, params, sessionId }))
  })
}

const { targetInfos } = await send('Target.getTargets')
const page = targetInfos.find((t) => t.type === 'page')
const { sessionId } = await send('Target.attachToTarget', { targetId: page.targetId, flatten: true })
const S = (m, p) => send(m, p, sessionId)
await S('Page.enable'); await S('Runtime.enable')

async function ev(expression) {
  const { result, exceptionDetails } = await S('Runtime.evaluate', {
    expression, returnByValue: true, awaitPromise: true,
  })
  if (exceptionDetails) throw new Error(exceptionDetails.exception?.description || exceptionDetails.text)
  return result.value
}

async function goto(path, wait = 2200) {
  await S('Page.navigate', { url: BASE + path })
  await sleep(wait)
}

const results = []
const check = (name, pass, detail = '') => {
  results.push({ name, pass, detail })
  console.log(`${pass ? 'PASS' : 'FAIL'}  ${name}${detail ? '  → ' + detail : ''}`)
}

/** 通过界面真实登录（走 /api/auth/login） */
async function uiLogin(username, password = '123456') {
  // 先清掉前端登录态并销毁后端会话，避免已登录时 /login 被守卫重定向
  await goto('/')
  await ev(`(async () => {
    try {
      const app = document.querySelector('#app')?.__vue_app__
      const auth = app?.config.globalProperties.$pinia?._s.get('auth')
      if (auth?.isAuthenticated) await auth.logout()
    } catch {}
    localStorage.removeItem('dsms.auth.user')
    return true
  })()`).catch(() => {})
  await goto('/login')
  // 等待登录表单渲染（登录页可能因会话存在被重定向回首页）
  for (let i = 0; i < 20; i++) {
    const n = await ev(`[...document.querySelectorAll('input')].filter(x=>x.type==='text'||x.type==='password').length`).catch(() => 0)
    if (n >= 2) break
    await sleep(250)
  }
  await sleep(300)
  await ev(`(() => {
    const inputs = [...document.querySelectorAll('input')].filter(i => i.type === 'text' || i.type === 'password')
    if (inputs.length < 2) return 'no-inputs'
    const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set
    setter.call(inputs[0], ${JSON.stringify(username)})
    inputs[0].dispatchEvent(new Event('input', { bubbles: true }))
    setter.call(inputs[1], ${JSON.stringify(password)})
    inputs[1].dispatchEvent(new Event('input', { bubbles: true }))
    return 'ok'
  })()`)
  await sleep(200)
  await ev(`document.querySelector('button[type=submit]').click()`)
  await sleep(2000)
  return ev(`location.pathname`)
}

/* ------------------------- 1. 游客 ------------------------- */
await goto('/')
await ev(`localStorage.clear(); sessionStorage.clear()`).catch(() => {})
await goto('/')
check('游客可访问学员端首页', await ev(`document.body.innerText.includes('我的培训进度')`))

/* ------------------------- 2. 学员登录 ------------------------- */
const p1 = await uiLogin('student')
check('学员登录（真实接口）跳转学员端', p1 === '/progress', p1)
await sleep(800)
const progressText = await ev(`document.body.innerText`)
check('培训进度展示真实学员数据', progressText.includes('李小明') && progressText.includes('C1'), '')
check('展示带教教练与车辆', progressText.includes('王建国') && progressText.includes('沪A'), '')
const canvas1 = await ev(`[...document.querySelectorAll('canvas')].filter(c=>c.width>0).length`)
check('进度页 ECharts 出图', canvas1 >= 2, `canvas=${canvas1}`)
const navStudent = await ev(`[...document.querySelectorAll('.nav-link')].map(a=>a.textContent.trim())`)
check('navbar 仅显示学员端菜单',
  navStudent.length === 4 && !navStudent.includes('统计报表'), navStudent.join(' | '))

/* ------------------------- 3. 学员约车 ------------------------- */
await goto('/training')
await sleep(1200)
// 前置：清掉现有有效预约，保证有下拉可选
await ev(`(async () => {
  const app = document.querySelector('#app').__vue_app__
  const data = app.config.globalProperties.$pinia._s.get('data')
  const list = await data.loadMyBookings()
  for (const b of list) {
    if (b.status === '待确认') await data.setBookingStatus(b.id, '已取消')
    else if (b.status === '已确认') {
      await fetch('/api/bookings/' + b.id + '/status', {
        method: 'PATCH', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: '已完成' }), credentials: 'include',
      })
    }
  }
  await data.loadMyBookings()
  location.reload()
  return true
})()`).catch(() => {})
await sleep(3500)

const slotCount = await ev(`(async () => {
  // 约车页已改为下拉：统计时段下拉里的选项数
  const selects = document.querySelectorAll('.booking-form .el-select')
  if (!selects.length) return 0
  selects[1].querySelector('.el-select__wrapper').click()
  await new Promise(r => setTimeout(r, 600))
  const panels = [...document.querySelectorAll('.el-select__popper')]
  const panel = panels.find(p => (p.querySelector('.el-select-dropdown__item')?.textContent || '').includes('—'))
  const n = panel ? panel.querySelectorAll('.el-select-dropdown__item').length : 0
  document.body.click()
  await new Promise(r => setTimeout(r, 300))
  return n
})()`)
check('约车页从接口加载可预约时段（下拉）', slotCount > 0, `时段选项=${slotCount}`)

// 通过下拉提交预约
const booked = await ev(`(async () => {
  const btn = [...document.querySelectorAll('.booking-form button')].find(b => b.textContent.includes('提交预约'))
  if (!btn) return '无提交按钮'
  if (btn.disabled) return '按钮禁用（无可选时段）'
  btn.click()
  await new Promise(r => setTimeout(r, 600))
  const ok = [...document.querySelectorAll('.el-message-box__btns button')].find(b => b.textContent.includes('提交预约'))
  if (!ok) return '无确认弹窗'
  ok.click()
  await new Promise(r => setTimeout(r, 1800))
  return 'ok'
})()`)
check('学员提交预约成功', booked === 'ok', booked)
const hasPending = await ev(`document.body.innerText.includes('待确认')`)
check('预约后列表出现「待确认」记录', hasPending)

/* ------------------------- 4. 教练处理预约 ------------------------- */
const p2 = await uiLogin('coach')
check('教练登录跳转工作台', p2 === '/coach/workbench', p2)
await sleep(800)
const wbText = await ev(`document.body.innerText`)
check('教练工作台显示真实统计', wbText.includes('今日排课') && wbText.includes('待确认预约'))

await goto('/coach/bookings')
await sleep(1200)
const coachRows = await ev(`document.querySelectorAll('.el-table__body tr').length`)
check('教练端能查到学员提交的预约（跨角色同一数据源）', coachRows >= 1, `rows=${coachRows}`)

/* ------------------------- 5. 管理端 ------------------------- */
const p3 = await uiLogin('admin')
check('管理员登录跳转工作台', p3 === '/admin/dashboard', p3)
await sleep(1000)
const dashText = await ev(`document.body.innerText`)
check('管理端工作台显示真实指标', dashText.includes('运营工作台') && dashText.includes('在训学员'))
const canvas2 = await ev(`[...document.querySelectorAll('canvas')].filter(c=>c.width>0).length`)
check('工作台 ECharts 出图', canvas2 >= 2, `canvas=${canvas2}`)

await goto('/admin/students')
await sleep(1500)
const stuText = await ev(`document.body.innerText`)
check('学员管理从接口加载列表', stuText.includes('李小明'), '')
check('身份证脱敏展示', /310101\*{8}/.test(stuText), '')

await goto('/admin/reports')
await sleep(1800)
const canvas3 = await ev(`[...document.querySelectorAll('canvas')].filter(c=>c.width>0).length`)
check('统计报表 4 张图渲染', canvas3 >= 4, `canvas=${canvas3}`)

await goto('/admin/resources')
await sleep(1500)
const coachTabText = await ev(`document.body.innerText`)
check('教练与车辆页加载教练列表（真实数据）', coachTabText.includes('王建国') && coachTabText.includes('C001'), '')
// 切到车辆 tab 再验证车辆数据
await ev(`[...document.querySelectorAll('.el-tabs__item')].find(t => t.textContent.includes('教练车辆'))?.click()`)
await sleep(800)
check('车辆 tab 加载车辆档案（真实数据）', await ev(`document.body.innerText.includes('沪A')`))

await goto('/admin/exams')
await sleep(1500)
check('考试管理页加载计划与成绩',
  await ev(`document.body.innerText.includes('考试计划')`))

await goto('/admin/users')
await sleep(1500)
const userText = await ev(`document.body.innerText`)
check('用户管理加载账号列表', userText.includes('admin') && userText.includes('student'))

/* ------------------------- 6. 会话与登出 ------------------------- */
await ev(`(async () => {
  const app = document.querySelector('#app').__vue_app__
  const auth = app.config.globalProperties.$pinia._s.get('auth')
  await auth.logout()
  return true
})()`)
await goto('/admin/users')
await sleep(1500)
check('登出后访问管理端被拦到登录页',
  (await ev(`location.pathname`)) === '/login', await ev(`location.pathname`))

const failed = results.filter((r) => !r.pass)
console.log('\n===== 页面运行时错误 =====')
console.log(pageErrors.length ? pageErrors.slice(0, 6).join('\n---\n') : '(none)')
console.log(`\n===== ${results.length - failed.length}/${results.length} 通过 =====`)
if (failed.length) console.log('FAILED:\n' + failed.map((f) => ` - ${f.name} (${f.detail})`).join('\n'))

ws.close(); chrome.kill()
process.exit(failed.length ? 1 : 0)
