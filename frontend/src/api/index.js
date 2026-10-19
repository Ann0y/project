import request from '@/utils/request'

/**
 * 后端接口封装（对应《需求文档》第 7 章）。
 * /api 前缀由 request 统一处理，这里只写相对路径。
 */

/* ------------------------------ 认证 / 字典 ------------------------------ */

export const loginApi = (data) => request.post('/auth/login', data)
export const registerApi = (data) => request.post('/auth/register', data)
export const logoutApi = () => request.post('/auth/logout')
export const getMeApi = () => request.get('/auth/me')
export const changePasswordApi = (data) => request.put('/auth/password', data)

/* -------------------------------- 学员端 -------------------------------- */

export const myProgressApi = () => request.get('/students/me/progress')
export const mySchedulesApi = (date) => request.get('/schedules/mine', { params: { date } })
export const myBookingsApi = (params) => request.get('/bookings/my', { params })
export const createBookingApi = (scheduleId) => request.post('/bookings', { scheduleId })
export const cancelBookingApi = (id) => request.patch(`/bookings/${id}/cancel`)
export const myScoresApi = (params) => request.get('/exam-scores/my', { params })
export const openExamPlansApi = () => request.get('/exam-plans/open')
export const registerExamApi = (planId) => request.post(`/exam-plans/${planId}/register`)
export const myPaymentsApi = () => request.get('/payments/my')

/* -------------------------------- 教练端 -------------------------------- */

export const coachWorkbenchApi = () => request.get('/coach/workbench')
export const coachSchedulesApi = (date) => request.get('/coach/schedules', { params: { date } })
export const createScheduleApi = (data) => request.post('/coach/schedules', data)
export const updateScheduleApi = (id, data) => request.put(`/coach/schedules/${id}`, data)
export const deleteScheduleApi = (id) => request.delete(`/coach/schedules/${id}`)
export const coachBookingsApi = (params) => request.get('/coach/bookings', { params })
export const changeCoachBookingStatusApi = (id, status) =>
  request.patch(`/coach/bookings/${id}/status`, { status })
export const coachHoursApi = () => request.get('/coach/hours')
export const createHoursApi = (data) => request.post('/coach/hours', data)
export const coachStudentsApi = () => request.get('/coach/students')

/* -------------------------------- 管理端 -------------------------------- */

export const dashboardApi = () => request.get('/statistics/overview')
export const reportApi = (params) => request.get('/statistics/report', { params })

export const studentsApi = (params) => request.get('/students', { params })
export const createStudentApi = (data) => request.post('/students', data)
export const updateStudentApi = (id, data) => request.put(`/students/${id}`, data)
export const deleteStudentApi = (id) => request.delete(`/students/${id}`)
export const auditStudentApi = (id, data) => request.post(`/students/${id}/audit`, data)
export const assignCoachApi = (id, data) => request.put(`/students/${id}/coach`, data)
export const changeStudentStatusApi = (id, status) =>
  request.patch(`/students/${id}/status`, { status })

export const coachesApi = (keyword) => request.get('/coaches', { params: { keyword } })
export const createCoachApi = (data) => request.post('/coaches', data)
export const updateCoachApi = (id, data) => request.put(`/coaches/${id}`, data)

export const vehiclesApi = (keyword) => request.get('/vehicles', { params: { keyword } })
export const createVehicleApi = (data) => request.post('/vehicles', data)
export const updateVehicleApi = (id, data) => request.put(`/vehicles/${id}`, data)
export const changeVehicleStatusApi = (id, status) =>
  request.patch(`/vehicles/${id}/status`, { status })

export const adminBookingsApi = (params) => request.get('/bookings', { params })
export const changeAdminBookingStatusApi = (id, status) =>
  request.patch(`/bookings/${id}/status`, { status })

export const adminHoursApi = (params) => request.get('/hours', { params })
export const auditHoursApi = (id, data) => request.patch(`/hours/${id}/audit`, data)
export const coursesApi = () => request.get('/courses')
export const updateCourseApi = (id, data) => request.put(`/courses/${id}`, data)

export const examPlansApi = () => request.get('/exam-plans')
export const createExamPlanApi = (data) => request.post('/exam-plans', data)
export const updateExamPlanApi = (id, data) => request.put(`/exam-plans/${id}`, data)
export const deleteExamPlanApi = (id) => request.delete(`/exam-plans/${id}`)
export const changeExamPlanStatusApi = (id, status) =>
  request.patch(`/exam-plans/${id}/status`, { status })
export const confirmRosterApi = (id) => request.post(`/exam-plans/${id}/confirm`)
export const examScoresApi = (params) => request.get('/exam-scores', { params })
export const saveScoreApi = (data) => request.post('/exam-scores', data)

export const usersApi = (params) => request.get('/users', { params })
export const createUserApi = (data) => request.post('/users', data)
export const updateUserApi = (id, data) => request.put(`/users/${id}`, data)
export const deleteUserApi = (id) => request.delete(`/users/${id}`)
export const changeUserStatusApi = (id, status) =>
  request.patch(`/users/${id}/status`, { status })
export const resetPasswordApi = (id, password) =>
  request.patch(`/users/${id}/password`, { password })

export const paymentsApi = (studentId) => request.get('/payments', { params: { studentId } })
export const registerPaymentApi = (data) => request.post('/payments/register', data)
