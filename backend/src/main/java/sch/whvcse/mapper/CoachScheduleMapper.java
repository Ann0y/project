package sch.whvcse.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import sch.whvcse.entity.CoachSchedule;

/** coach_schedule 教练排课时段 */
@Mapper
public interface CoachScheduleMapper {

    String COLS = "id, coach_id, vehicle_id, schedule_date, start_time, end_time, capacity, booked_count, status, del_flag";

    @Select("SELECT " + COLS + " FROM coach_schedule WHERE del_flag = 0 ORDER BY schedule_date, start_time")
    List<CoachSchedule> findAll();

    @Select("SELECT " + COLS + " FROM coach_schedule WHERE id = #{id} AND del_flag = 0")
    CoachSchedule findById(Long id);

    @Select("""
            <script>
            SELECT id, coach_id, vehicle_id, schedule_date, start_time, end_time,
                   capacity, booked_count, status, del_flag
            FROM coach_schedule
            WHERE del_flag = 0
            <if test="coachId != null"> AND coach_id = #{coachId} </if>
            <if test="date != null"> AND schedule_date = #{date} </if>
            <if test="fromDate != null"> AND schedule_date &gt;= #{fromDate} </if>
            <if test="toDate != null"> AND schedule_date &lt;= #{toDate} </if>
            ORDER BY schedule_date, start_time
            </script>
            """)
    List<CoachSchedule> search(@Param("coachId") Long coachId,
                               @Param("date") java.time.LocalDate date,
                               @Param("fromDate") java.time.LocalDate fromDate,
                               @Param("toDate") java.time.LocalDate toDate);

    /** 教练某天某时段是否已有排课（防重复发布） */
    @Select("""
            SELECT COUNT(*) FROM coach_schedule
            WHERE coach_id = #{coachId} AND schedule_date = #{date}
              AND start_time = #{startTime} AND del_flag = 0
              AND (#{excludeId} IS NULL OR id &lt;&gt; #{excludeId})
            """)
    long countClash(@Param("coachId") Long coachId,
                    @Param("date") java.time.LocalDate date,
                    @Param("startTime") java.time.LocalTime startTime,
                    @Param("excludeId") Long excludeId);

    /** E1 冲突检测：同一教练 / 同一车辆在同一时段是否已被占用 */
    @Select("""
            SELECT COUNT(*) FROM coach_schedule
            WHERE schedule_date = #{date}
              AND start_time &lt; #{endTime} AND end_time &gt; #{startTime}
              AND booked_count &gt; 0
              AND del_flag = 0
              AND id &lt;&gt; #{excludeId}
              AND (coach_id = #{coachId} OR (#{vehicleId} IS NOT NULL AND vehicle_id = #{vehicleId}))
            """)
    long countOccupied(@Param("coachId") Long coachId,
                       @Param("vehicleId") Long vehicleId,
                       @Param("date") java.time.LocalDate date,
                       @Param("startTime") java.time.LocalTime startTime,
                       @Param("endTime") java.time.LocalTime endTime,
                       @Param("excludeId") Long excludeId);

    @Insert("""
            INSERT INTO coach_schedule(coach_id, vehicle_id, schedule_date, start_time, end_time,
                                       capacity, booked_count, status)
            VALUES(#{coachId}, #{vehicleId}, #{scheduleDate}, #{startTime}, #{endTime},
                   #{capacity}, #{bookedCount}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CoachSchedule schedule);

    @Update("""
            UPDATE coach_schedule SET vehicle_id = #{vehicleId}, schedule_date = #{scheduleDate},
                   start_time = #{startTime}, end_time = #{endTime},
                   capacity = #{capacity}, status = #{status}
            WHERE id = #{id}
            """)
    int update(CoachSchedule schedule);

    @Update("UPDATE coach_schedule SET booked_count = #{bookedCount}, status = #{status} WHERE id = #{id}")
    int updateBooked(@Param("id") Long id,
                     @Param("bookedCount") Integer bookedCount,
                     @Param("status") String status);

    @Update("UPDATE coach_schedule SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE coach_schedule SET del_flag = 1 WHERE id = #{id}")
    int softDelete(Long id);

    /** 统计：某教练某日期区间的已排课时长（小时） */
    @Select("""
            SELECT COALESCE(SUM(TIMESTAMPDIFF(MINUTE, start_time, end_time)), 0) / 60
            FROM coach_schedule
            WHERE coach_id = #{coachId} AND del_flag = 0
              AND schedule_date >= #{from} AND schedule_date <= #{to}
            """)
    BigDecimal sumHoursByCoach(@Param("coachId") Long coachId,
                               @Param("from") java.time.LocalDate from,
                               @Param("to") java.time.LocalDate to);
}
