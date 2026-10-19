package sch.whvcse.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import sch.whvcse.entity.HoursRecord;

/** hours_record 学时记录 */
@Mapper
public interface HoursRecordMapper {

    String COLS = "id, student_id, coach_id, booking_id, subject_code, hours, train_date, "
            + "audit_status, auditor_id, audit_opinion, audit_time";

    @Select("SELECT " + COLS + " FROM hours_record ORDER BY train_date DESC, id DESC")
    List<HoursRecord> findAll();

    @Select("SELECT " + COLS + " FROM hours_record WHERE id = #{id}")
    HoursRecord findById(Long id);

    @Select("SELECT " + COLS + " FROM hours_record WHERE coach_id = #{coachId} ORDER BY train_date DESC, id DESC")
    List<HoursRecord> findByCoachId(Long coachId);

    @Select("SELECT " + COLS + " FROM hours_record WHERE student_id = #{studentId} ORDER BY train_date DESC, id DESC")
    List<HoursRecord> findByStudentId(Long studentId);

    @Select("""
            <script>
            SELECT id, student_id, coach_id, booking_id, subject_code, hours, train_date,
                   audit_status, auditor_id, audit_opinion, audit_time
            FROM hours_record
            WHERE 1 = 1
            <if test="studentId != null"> AND student_id = #{studentId} </if>
            <if test="subjectCode != null and subjectCode != ''"> AND subject_code = #{subjectCode} </if>
            <if test="auditStatus != null and auditStatus != ''"> AND audit_status = #{auditStatus} </if>
            <if test="coachId != null"> AND coach_id = #{coachId} </if>
            ORDER BY train_date DESC, id DESC
            </script>
            """)
    List<HoursRecord> search(@Param("studentId") Long studentId,
                             @Param("subjectCode") String subjectCode,
                             @Param("auditStatus") String auditStatus,
                             @Param("coachId") Long coachId);

    /** 某学员各科目「已通过」学时汇总（F02 培训进度 / 结业判定） */
    @Select("""
            SELECT subject_code, COALESCE(SUM(hours), 0) AS hours
            FROM hours_record
            WHERE student_id = #{studentId} AND audit_status = '已通过'
            GROUP BY subject_code
            """)
    List<java.util.Map<String, Object>> sumApprovedBySubject(Long studentId);

    /** 某学员总已通过学时 */
    @Select("""
            SELECT COALESCE(SUM(hours), 0) FROM hours_record
            WHERE student_id = #{studentId} AND audit_status = '已通过'
            """)
    Double sumApprovedHours(@Param("studentId") Long studentId);

    @Insert("""
            INSERT INTO hours_record(student_id, coach_id, booking_id, subject_code, hours,
                                     train_date, audit_status)
            VALUES(#{studentId}, #{coachId}, #{bookingId}, #{subjectCode}, #{hours},
                   #{trainDate}, #{auditStatus})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(HoursRecord record);

    /** 审核结果写入 */
    @Update("""
            UPDATE hours_record
            SET audit_status = #{auditStatus}, auditor_id = #{auditorId},
                audit_opinion = #{auditOpinion}, audit_time = NOW()
            WHERE id = #{id}
            """)
    int audit(@Param("id") Long id,
              @Param("auditStatus") String auditStatus,
              @Param("auditorId") Long auditorId,
              @Param("auditOpinion") String auditOpinion);

    @Select("SELECT COUNT(*) FROM hours_record WHERE audit_status = #{status}")
    long countByAuditStatus(String status);

    /** 统计：时间区间内已通过的总学时 */
    @Select("""
            SELECT COALESCE(SUM(hours), 0) FROM hours_record
            WHERE audit_status = '已通过' AND train_date >= #{from} AND train_date <= #{to}
            """)
    Double sumApprovedInRange(@Param("from") java.time.LocalDate from,
                              @Param("to") java.time.LocalDate to);
}
