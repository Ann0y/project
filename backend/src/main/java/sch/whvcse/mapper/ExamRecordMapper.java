package sch.whvcse.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import sch.whvcse.entity.ExamRecord;
import sch.whvcse.vo.ExamRecordView;

/**
 * exam_record 考试报名与成绩。
 * 注意：exam_record 表不含 subject_code，科目需 JOIN exam_plan 取得，
 * 因此列表类查询统一走 findView* 系列方法。
 */
@Mapper
public interface ExamRecordMapper {

    String COLS = "id, plan_id, student_id, register_time, attempt_no, score, status, exam_date";

    /** 联合查询的列（exam_record + exam_plan + student） */
    String VIEW_COLS = "r.id, r.plan_id, r.student_id, r.register_time, r.attempt_no, r.score, "
            + "r.status, r.exam_date, "
            + "p.subject_code AS subjectCode, p.exam_date AS planDate, p.exam_time AS examTime, p.venue, "
            + "u.name AS studentName, s.license_type AS licenseType";

    String VIEW_JOIN = " FROM exam_record r"
            + " JOIN exam_plan p ON p.id = r.plan_id"
            + " JOIN student s ON s.id = r.student_id"
            + " JOIN sys_user u ON u.id = s.user_id";

    @Select("SELECT " + COLS + " FROM exam_record ORDER BY register_time DESC, id DESC")
    List<ExamRecord> findAll();

    @Select("SELECT " + COLS + " FROM exam_record WHERE id = #{id}")
    ExamRecord findById(Long id);

    @Select("SELECT " + COLS + " FROM exam_record WHERE student_id = #{studentId} ORDER BY register_time DESC, id DESC")
    List<ExamRecord> findByStudentId(Long studentId);

    @Select("SELECT " + COLS + " FROM exam_record WHERE plan_id = #{planId} ORDER BY id")
    List<ExamRecord> findByPlanId(Long planId);

    /* ----------------------- 带科目的联合视图 ----------------------- */

    @Select("SELECT " + VIEW_COLS + VIEW_JOIN + " WHERE r.student_id = #{studentId} ORDER BY r.register_time DESC, r.id DESC")
    List<ExamRecordView> findViewByStudent(Long studentId);

    @Select("<script>"
            + "SELECT " + VIEW_COLS + VIEW_JOIN
            + " WHERE 1 = 1"
            + "<if test=\"studentId != null\"> AND r.student_id = #{studentId} </if>"
            + "<if test=\"subjectCode != null and subjectCode != ''\"> AND p.subject_code = #{subjectCode} </if>"
            + "<if test=\"status != null and status != ''\"> AND r.status = #{status} </if>"
            + " ORDER BY r.register_time DESC, r.id DESC"
            + "</script>")
    List<ExamRecordView> findView(@Param("studentId") Long studentId,
                                  @Param("subjectCode") String subjectCode,
                                  @Param("status") String status);

    @Select("SELECT " + VIEW_COLS + VIEW_JOIN + " WHERE r.plan_id = #{planId} ORDER BY r.id")
    List<ExamRecordView> findViewByPlan(Long planId);

    @Select("SELECT " + VIEW_COLS + VIEW_JOIN + " WHERE r.id = #{id}")
    ExamRecordView findViewByRecordId(Long id);

    /* ----------------------- 业务校验查询 ----------------------- */

    /** 某学员某科目已合格次数（科目经 exam_plan 取得） */
    @Select("""
            SELECT COUNT(*) FROM exam_record r
            JOIN exam_plan p ON p.id = r.plan_id
            WHERE r.student_id = #{studentId} AND p.subject_code = #{subjectCode} AND r.status = '合格'
            """)
    long countPassedBySubject(@Param("studentId") Long studentId,
                              @Param("subjectCode") String subjectCode);

    /** 某学员已合格的不同科目数（用于结业判定，需 ≥ 4） */
    @Select("""
            SELECT COUNT(DISTINCT p.subject_code) FROM exam_record r
            JOIN exam_plan p ON p.id = r.plan_id
            WHERE r.student_id = #{studentId} AND r.status = '合格'
            """)
    long countPassedSubjects(Long studentId);

    /**
     * 某学员某科目是否已有报名记录（任意状态）。
     * 用于「按科目顺序报名」校验：后一科目只有在上一科目已报名后才允许报名。
     */
    @Select("""
            SELECT COUNT(*) FROM exam_record r
            JOIN exam_plan p ON p.id = r.plan_id
            WHERE r.student_id = #{studentId} AND p.subject_code = #{subjectCode}
            """)
    long countRegisteredBySubject(@Param("studentId") Long studentId,
                                  @Param("subjectCode") String subjectCode);

    /** 某学员某科目是否已报名过该计划之外的同科目考试（用于避免重复报考同科目） */
    @Select("""
            SELECT COUNT(*) FROM exam_record r
            JOIN exam_plan p ON p.id = r.plan_id
            WHERE r.student_id = #{studentId} AND p.subject_code = #{subjectCode}
              AND r.status IN ('待考', '补考中')
            """)
    long countPendingBySubject(@Param("studentId") Long studentId,
                               @Param("subjectCode") String subjectCode);

    @Select("SELECT COUNT(*) FROM exam_record WHERE status = '待考'")
    long countPending();

    /** 按状态分组计数（管理端看板 / 考试状态分布） */
    @Select("SELECT status, COUNT(*) AS cnt FROM exam_record GROUP BY status")
    List<java.util.Map<String, Object>> countGroupByStatus();

    @Insert("""
            INSERT INTO exam_record(plan_id, student_id, attempt_no, score, status, exam_date)
            VALUES(#{planId}, #{studentId}, #{attemptNo}, #{score}, #{status}, #{examDate})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExamRecord record);

    @Update("UPDATE exam_record SET score = #{score}, status = #{status}, exam_date = #{examDate} WHERE id = #{id}")
    int updateScore(ExamRecord record);

    /* ----------------------- 统计（报表用） ----------------------- */

    @Select("SELECT COUNT(*) FROM exam_record WHERE score IS NOT NULL")
    long countScored();

    @Select("SELECT COUNT(*) FROM exam_record WHERE score IS NOT NULL AND status = '合格'")
    long countScoredPassed();

    /** 各科目通过率（科目来自 exam_plan） */
    @Select("""
            SELECT p.subject_code AS subjectCode,
                   COUNT(*) AS total,
                   SUM(CASE WHEN r.status = '合格' THEN 1 ELSE 0 END) AS passed,
                   SUM(CASE WHEN r.attempt_no > 1 THEN 1 ELSE 0 END) AS retake
            FROM exam_record r
            JOIN exam_plan p ON p.id = r.plan_id
            GROUP BY p.subject_code
            ORDER BY p.subject_code
            """)
    List<java.util.Map<String, Object>> statsBySubject();
}
