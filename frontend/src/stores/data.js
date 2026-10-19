import { ref } from 'vue'
import { defineStore } from 'pinia'
import * as api from '@/api'
import { SUBJECTS, subjectShort } from '@/data/demo'

/**
 * 业务数据 Store ： 对接后端真实接口
 * ---------------------------------------------------------------------------
 * 设计：保持原先的字段与函数名不变（各页面几乎不用改），
 *       内部实现从"本地数组"换成"调用 /api/** 并缓存结果"。
 *
 * 数据加载约定：
 *   每个页面在 onMounted 时调用对应的 load* 方法拉数据，
 *   例如学员端进度页调用 loadProgress()，管理端学员页调用 loadStudents()。
 */
export const useDataStore = defineStore('data', () => {
  /* ============================== 状态 ============================== */
  const loading = ref(false)

  // 学员端
  const progress = ref(null)
  const schedules = ref([])          // 本人带教教练的可约时段 / 教练本人排课
  const bookings = ref([])           // 预约列表（三端共用）
  const examPlans = ref([])          // 考试计划
  const examRecords = ref([])        // 考试记录 / 成绩
  const payments = ref([])

  // 教练端
  const workbench = ref(null)
  const hoursRecords = ref([])
  const students = ref([])           // 带教学员（教练端）/ 学员档案（管理端）

  // 管理端
  const dashboard = ref(null)
  const report = ref(null)
  const coaches = ref([])
  const vehicles = ref([])
  const users = ref([])
  const courses = ref([])
  const studentPage = ref({ list: [], total: 0, page: 1, size: 10 })
  const hoursPage = ref({ list: [], total: 0, page: 1, size: 10 })
  const bookingPage = ref({ list: [], total: 0, page: 1, size: 10 })
  const examScorePage = ref({ list: [], total: 0, page: 1, size: 10 })
  const userPage = ref({ list: [], total: 0, page: 1, size: 10 })

  /* ============================== 学员端加载 ============================== */

  async function loadProgress() {
    loading.value = true
    try {
      progress.value = await api.myProgressApi()
      return progress.value
    } finally {
      loading.value = false
    }
  }

  async function loadMySchedules(date) {
    schedules.value = await api.mySchedulesApi(date)
    return schedules.value
  }

  async function loadMyBookings(params = {}) {
    const page = await api.myBookingsApi({ page: 1, size: 100, ...params })
    bookings.value = page.list ?? []
    return bookings.value
  }

  async function loadMyExamRecords(params = {}) {
    const page = await api.myScoresApi({ page: 1, size: 100, ...params })
    examRecords.value = page.list ?? []
    return examRecords.value
  }

  async function loadOpenPlans() {
    examPlans.value = await api.openExamPlansApi()
    return examPlans.value
  }

  async function loadMyPayments() {
    payments.value = await api.myPaymentsApi()
    return payments.value
  }

  /* ============================== 学员端写操作 ============================== */

  async function createBooking({ scheduleId }) {
    try {
      await api.createBookingApi(scheduleId)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function setBookingStatus(bookingId, status) {
    try {
      if (status === '已取消') await api.cancelBookingApi(bookingId)
      else await api.changeAdminBookingStatusApi(bookingId, status)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function registerExam(planId) {
    try {
      await api.registerExamApi(planId)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  /* ============================== 教练端 ============================== */

  async function loadWorkbench() {
    workbench.value = await api.coachWorkbenchApi()
    return workbench.value
  }

  async function loadCoachSchedules(date) {
    schedules.value = await api.coachSchedulesApi(date)
    return schedules.value
  }

  async function loadCoachBookings(params = {}) {
    bookings.value = await api.coachBookingsApi(params)
    return bookings.value
  }

  async function loadCoachHours() {
    hoursRecords.value = await api.coachHoursApi()
    return hoursRecords.value
  }

  async function loadCoachStudents() {
    students.value = await api.coachStudentsApi()
    return students.value
  }

  async function saveSchedule(payload) {
    try {
      const body = {
        scheduleDate: payload.scheduleDate,
        startTime: payload.startTime,
        endTime: payload.endTime,
        capacity: payload.capacity,
        vehicleId: payload.vehicleId,
        status: payload.status,
      }
      if (payload.id) await api.updateScheduleApi(payload.id, body)
      else await api.createScheduleApi(body)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function removeSchedule(id) {
    try {
      await api.deleteScheduleApi(id)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function changeCoachBookingStatus(id, status) {
    try {
      await api.changeCoachBookingStatusApi(id, status)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function createHoursRecord(payload) {
    try {
      await api.createHoursApi(payload)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  /* ============================== 管理端加载 ============================== */

  async function loadDashboard() {
    dashboard.value = await api.dashboardApi()
    return dashboard.value
  }

  async function loadReport(params = {}) {
    report.value = await api.reportApi(params)
    return report.value
  }

  async function loadStudents(params = {}) {
    studentPage.value = await api.studentsApi(params)
    students.value = studentPage.value.list ?? []
    return studentPage.value
  }

  async function loadCoaches(keyword) {
    coaches.value = await api.coachesApi(keyword)
    return coaches.value
  }

  async function loadVehicles(keyword) {
    vehicles.value = await api.vehiclesApi(keyword)
    return vehicles.value
  }

  async function loadAdminBookings(params = {}) {
    bookingPage.value = await api.adminBookingsApi(params)
    bookings.value = bookingPage.value.list ?? []
    return bookingPage.value
  }

  async function loadAdminHours(params = {}) {
    hoursPage.value = await api.adminHoursApi(params)
    hoursRecords.value = hoursPage.value.list ?? []
    return hoursPage.value
  }

  async function loadCourses() {
    courses.value = await api.coursesApi()
    return courses.value
  }

  async function loadExamPlans() {
    examPlans.value = await api.examPlansApi()
    return examPlans.value
  }

  async function loadExamScores(params = {}) {
    examScorePage.value = await api.examScoresApi(params)
    examRecords.value = examScorePage.value.list ?? []
    return examScorePage.value
  }

  async function loadUsers(params = {}) {
    userPage.value = await api.usersApi(params)
    users.value = userPage.value.list ?? []
    return userPage.value
  }

  async function loadPayments(studentId) {
    payments.value = await api.paymentsApi(studentId)
    return payments.value
  }

  /* ============================== 管理端写操作 ============================== */

  async function saveStudent(payload) {
    try {
      if (payload.id) await api.updateStudentApi(payload.id, payload)
      else await api.createStudentApi(payload)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function auditStudent(id, pass, opinion) {
    try {
      await api.auditStudentApi(id, { pass, opinion })
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function assignCoachAndVehicle(id, coachId, vehicleId) {
    try {
      await api.assignCoachApi(id, { coachId, vehicleId })
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function changeStudentStatus(id, status) {
    try {
      await api.changeStudentStatusApi(id, status)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function deleteStudent(id) {
    try {
      await api.deleteStudentApi(id)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function saveCoach(payload) {
    try {
      if (payload.id) await api.updateCoachApi(payload.id, payload)
      else await api.createCoachApi(payload)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function saveVehicle(payload) {
    try {
      if (payload.id) await api.updateVehicleApi(payload.id, payload)
      else await api.createVehicleApi(payload)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function changeVehicleStatus(id, status) {
    try {
      await api.changeVehicleStatusApi(id, status)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function changeAdminBookingStatus(id, status) {
    try {
      await api.changeAdminBookingStatusApi(id, status)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function auditHoursRecord(id, pass, opinion = '') {
    try {
      await api.auditHoursApi(id, { pass, opinion })
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function updateCourseHours(id, requiredHours) {
    try {
      await api.updateCourseApi(id, { requiredHours })
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function saveExamPlan(payload) {
    try {
      if (payload.id) await api.updateExamPlanApi(payload.id, payload)
      else await api.createExamPlanApi(payload)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function changeExamPlanStatus(id, status) {
    try {
      await api.changeExamPlanStatusApi(id, status)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function deleteExamPlan(id) {
    try {
      await api.deleteExamPlanApi(id)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function saveExamScore(recordId, score) {
    try {
      await api.saveScoreApi({ recordId, score })
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function confirmRoster(planId) {
    try {
      await api.confirmRosterApi(planId)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function saveUser(payload) {
    try {
      if (payload.id) await api.updateUserApi(payload.id, payload)
      else await api.createUserApi(payload)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function setUserStatus(id, status) {
    try {
      await api.changeUserStatusApi(id, status)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function resetUserPassword(id, password = '123456') {
    try {
      await api.resetPasswordApi(id, password)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function removeUser(id) {
    try {
      await api.deleteUserApi(id)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  async function registerPayment(payload) {
    try {
      await api.registerPaymentApi(payload)
      return { ok: true }
    } catch (e) {
      return { ok: false, reason: e.message }
    }
  }

  return {
    loading,
    progress, schedules, bookings, examPlans, examRecords, payments,
    workbench, hoursRecords, students,
    dashboard, report, coaches, vehicles, users, courses,
    studentPage, hoursPage, bookingPage, examScorePage, userPage,
    // 学员端
    loadProgress, loadMySchedules, loadMyBookings, loadMyExamRecords,
    loadOpenPlans, loadMyPayments,
    createBooking, setBookingStatus, registerExam,
    // 教练端
    loadWorkbench, loadCoachSchedules, loadCoachBookings, loadCoachHours, loadCoachStudents,
    saveSchedule, removeSchedule, changeCoachBookingStatus, createHoursRecord,
    // 管理端
    loadDashboard, loadReport, loadStudents, loadCoaches, loadVehicles,
    loadAdminBookings, loadAdminHours, loadCourses, loadExamPlans, loadExamScores,
    loadUsers, loadPayments,
    saveStudent, auditStudent, assignCoachAndVehicle, changeStudentStatus, deleteStudent,
    saveCoach, saveVehicle, changeVehicleStatus,
    changeAdminBookingStatus, auditHoursRecord, updateCourseHours,
    saveExamPlan, changeExamPlanStatus, deleteExamPlan, saveExamScore, confirmRoster,
    saveUser, setUserStatus, resetUserPassword, removeUser, registerPayment,
  }
})

export { SUBJECTS, subjectShort }
