package sch.whvcse.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import sch.whvcse.entity.Booking;

/** booking 培训预约 */
@Mapper
public interface BookingMapper {

    String COLS = "id, student_id, schedule_id, status, create_time";

    @Select("SELECT " + COLS + " FROM booking ORDER BY create_time DESC, id DESC")
    List<Booking> findAll();

    @Select("SELECT " + COLS + " FROM booking WHERE id = #{id}")
    Booking findById(Long id);

    @Select("SELECT " + COLS + " FROM booking WHERE student_id = #{studentId} ORDER BY create_time DESC, id DESC")
    List<Booking> findByStudentId(Long studentId);

    /** 教练派单：本人排课时段产生的预约 */
    @Select("""
            SELECT b.id, b.student_id, b.schedule_id, b.status, b.create_time
            FROM booking b
            JOIN coach_schedule s ON s.id = b.schedule_id
            WHERE s.coach_id = #{coachId}
            ORDER BY b.create_time DESC, b.id DESC
            """)
    List<Booking> findByCoachId(Long coachId);

    @Select("""
            <script>
            SELECT id, student_id, schedule_id, status, create_time
            FROM booking
            WHERE 1 = 1
            <if test="status != null and status != ''"> AND status = #{status} </if>
            ORDER BY create_time DESC, id DESC
            </script>
            """)
    List<Booking> searchByStatus(@Param("status") String status);

    /**
     * 防重复预约：同一学员同一时段是否已有「未完成」预约。
     * 已完成 / 已取消 / 爽约 都属于已结束的预约，不再阻止重新预约该时段。
     */
    @Select("""
            SELECT COUNT(*) FROM booking
            WHERE student_id = #{studentId} AND schedule_id = #{scheduleId}
              AND status IN ('待确认', '已确认')
            """)
    long countActive(@Param("studentId") Long studentId, @Param("scheduleId") Long scheduleId);

    /**
     * 该学员当前「未完成」预约数（待确认 / 已确认）。
     * 业务规则：一名学员最多同时持有 MAX_ACTIVE_BOOKINGS 条未完成预约。
     */
    @Select("""
            SELECT COUNT(*) FROM booking
            WHERE student_id = #{studentId} AND status IN ('待确认', '已确认')
            """)
    long countActiveByStudent(@Param("studentId") Long studentId);

    /**
     * 时段当前占用的容量。
     * 只有「未完成」预约（待确认 / 已确认）占用容量，
     * 已完成 / 已取消 / 爽约 都不再占用，时段可继续被预约。
     */
    @Select("""
            SELECT COUNT(*) FROM booking
            WHERE schedule_id = #{scheduleId} AND status IN ('待确认', '已确认')
            """)
    long countActiveBySchedule(@Param("scheduleId") Long scheduleId);

    @Insert("""
            INSERT INTO booking(student_id, schedule_id, status)
            VALUES(#{studentId}, #{scheduleId}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Booking booking);

    @Update("UPDATE booking SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 重新激活历史预约（同一学员再次预约曾被取消/爽约的同一时段）。
     * booking 表有 uk_student_schedule 唯一约束，无法直接再插入一条，
     * 因此复用原记录：改状态并刷新提交时间。
     */
    @Update("""
            UPDATE booking SET status = #{status}, create_time = NOW()
            WHERE id = #{id}
            """)
    int reactivate(@Param("id") Long id, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM booking WHERE status = #{status}")
    long countByStatus(String status);
}
