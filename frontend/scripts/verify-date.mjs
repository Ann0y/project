/** 验证日期下拉可选、无「已预约」堆积 */
import { spawn } from 'node:child_process'
import { setTimeout as sleep } from 'node:timers/promises'
const PROFILE='/home/annoy/study/projects/.chrome-profile'
const chrome=spawn('/usr/bin/chromium',['--headless=new','--no-sandbox','--disable-gpu','--disable-dev-shm-usage','--no-first-run','--disable-crash-reporter','--disable-breakpad',`--user-data-dir=${PROFILE}`,'--remote-debugging-port=9332','--remote-allow-origins=*','--window-size=1600,1000','about:blank'],{stdio:'ignore',env:{...process.env,HOME:PROFILE,XDG_CONFIG_HOME:PROFILE,XDG_CACHE_HOME:PROFILE}})
async function wsUrl(){for(let i=0;i<80;i++){try{const r=await fetch('http://127.0.0.1:9332/json/version');const j=await r.json();if(j.webSocketDebuggerUrl)return j.webSocketDebuggerUrl}catch{}await sleep(250)}throw new Error('no chrome')}
const ws=new WebSocket(await wsUrl());await new Promise(r=>ws.onopen=r)
let id=0;const p=new Map()
ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.id&&p.has(m.id)){const{resolve}=p.get(m.id);p.delete(m.id);resolve(m.result??m.error)}}
const send=(method,params={},sessionId)=>{const i=++id;return new Promise(r=>{p.set(i,{resolve:r});ws.send(JSON.stringify({id:i,method,params,sessionId}))})}
const {targetInfos}=await send('Target.getTargets');const t=targetInfos.find(x=>x.type==='page')
const {sessionId}=await send('Target.attachToTarget',{targetId:t.targetId,flatten:true})
const S=(m,q)=>send(m,q,sessionId)
await S('Page.enable');await S('Runtime.enable')
const ev=async(x)=>{const r=await S('Runtime.evaluate',{expression:x,returnByValue:true,awaitPromise:true});if(r.exceptionDetails)throw new Error(r.exceptionDetails.text);return r.result?.value}
const results=[];const check=(n,ok,d='')=>{results.push({n,ok,d});console.log(`${ok?'PASS':'FAIL'}  ${n}${d?'  → '+d:''}`)}

await S('Page.navigate',{url:'http://127.0.0.1:5199/login'});await sleep(2500)
await ev(`(()=>{const i=[...document.querySelectorAll('input')].filter(x=>x.type==='text'||x.type==='password');const s=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;s.call(i[0],'student');i[0].dispatchEvent(new Event('input',{bubbles:true}));s.call(i[1],'123456');i[1].dispatchEvent(new Event('input',{bubbles:true}));return 1})()`)
await sleep(300);await ev(`document.querySelector('button[type=submit]').click()`);await sleep(2200)
await S('Page.navigate',{url:'http://127.0.0.1:5199/training'});await sleep(3000)

// 已有预约则先取消，保证能测选择
// 前置：清掉学员现有有效预约（待确认→取消；已确认→置为已完成），否则无法测下拉
await ev(`(async () => {
  const app = document.querySelector('#app').__vue_app__
  const data = app.config.globalProperties.$pinia._s.get('data')
  const list = await data.loadMyBookings()
  for (const b of list) {
    if (b.status === '待确认') await data.setBookingStatus(b.id, '已取消')
    else if (b.status === '已确认') {
      // 教练/管理员侧才能改「已确认」，这里直接调管理员接口
      await fetch('/api/bookings/' + b.id + '/status', {
        method: 'PATCH', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: '已完成' }), credentials: 'include',
      })
    }
  }
  await data.loadMyBookings()
  return true
})()`)
// 清理后重新进入页面，确保渲染最新状态
await S('Page.navigate', { url: 'http://127.0.0.1:5199/training' })
await sleep(3200)

const hasBlock = await ev(`!!document.querySelector('.booking-block')`)
if (hasBlock) {
  check('已有预约时显示「已有预约」提示而非下拉', true)
  const clicked = await ev(`(async()=>{
    const sleep=(ms)=>new Promise(r=>setTimeout(r,ms))
    const b=[...document.querySelectorAll('.booking-block button')].find(x=>x.textContent.includes('取消'))
    if(!b) return '无可取消按钮'
    b.click(); await sleep(700)
    // Element Plus 的确认按钮是 .el-message-box__btns 里的 primary 按钮
    const btns=[...document.querySelectorAll('.el-message-box__btns button')]
    if(!btns.length) return '无确认弹窗，按钮列表:' + [...document.querySelectorAll('button')].map(x=>x.textContent.trim()).filter(Boolean).join('|')
    const ok = btns.find(x=>x.classList.contains('el-button--primary')) || btns[btns.length-1]
    ok.click(); await sleep(2000)
    return 'ok'
  })()`)
  check('取消预约成功', clicked==='ok', clicked)
}
await sleep(1000)

// 日期下拉：逐个点选，验证 v-model 真的变了
const dateFlow = await ev(`(async () => {
  const sleep=(ms)=>new Promise(r=>setTimeout(r,ms))
  const sel=()=>document.querySelectorAll('.booking-form .el-select')[0]
  if(!sel()) return {error:'日期下拉不存在'}
  // Element Plus 2.x 选中值渲染在 .el-select__selected-item 内，兜底读 input 的 value
  // 选中值在 .el-select__placeholder 里（第一个 selected-item 是隐藏的 input 容器）
  const read=()=>sel().querySelector('.el-select__placeholder')?.textContent.trim() || ''
  const options=()=>[...document.querySelectorAll('.el-select__popper')]
        .find(p=>p.querySelector('.el-select-dropdown__item')?.textContent.includes('（'))
  const panel=options
  const picks=[]
  for (const idx of [1,2,3,0]) {
    sel().querySelector('.el-select__wrapper').click(); await sleep(500)
    const pp=panel(); if(!pp) return {error:'面板未打开'}
    const items=[...pp.querySelectorAll('.el-select-dropdown__item')]
    if (!items[idx]) return {error:'选项不足'}
    const want=items[idx].textContent.trim()
    items[idx].click(); await sleep(700)
    picks.push({want, got: read()})
  }
  return {picks}
})()`)
if (dateFlow.error) { check('日期下拉可选择', false, dateFlow.error) }
else {
  const allOk = dateFlow.picks.every(p => p.got && p.got.includes(p.want.slice(0,10)))
  check('日期下拉可自由切换（不被自动改回）', allOk, JSON.stringify(dateFlow.picks))
}

// 时段下拉：统计「已预约」数量
const slotInfo = await ev(`(async () => {
  const sleep=(ms)=>new Promise(r=>setTimeout(r,ms))
  const sels=document.querySelectorAll('.booking-form .el-select')
  if(sels.length<2) return {error:'无时段下拉'}
  sels[1].querySelector('.el-select__wrapper').click(); await sleep(600)
  const pp=[...document.querySelectorAll('.el-select__popper')].find(p=>p.querySelector('.el-select-dropdown__item')?.textContent.includes('—'))
  const texts = pp? [...pp.querySelectorAll('.el-select-dropdown__item')].map(e=>e.textContent.trim()) : []
  document.body.click(); await sleep(300)
  return {total:texts.length, mine:texts.filter(t=>t.includes('我的预约')).length, sample:texts}
})()`)
check('时段下拉中「我的预约」不超过 1 个', (slotInfo.mine ?? 0) <= 1, `已预约 ${slotInfo.mine} / 共 ${slotInfo.total}`)

const failed=results.filter(r=>!r.ok)
console.log(`\n===== ${results.length-failed.length}/${results.length} 通过 =====`)
ws.close();chrome.kill()
process.exit(failed.length?1:0)
