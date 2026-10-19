package sch.whvcse.mapper;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import sch.whvcse.entity.ExamPlan;

/** exam_plan 考试计划 */
@Mapper
public interface ExamPlanMapper {

    String COLS = "id, subject_code, exam_date, exam_time, venue, capacity, enrolled_count, status, del_flag";

    @Select("SELECT " + COLS + " FROM exam_plan WHERE del_flag = 0 ORDER BY exam_date, exam_time")
    List<ExamPlan> findAll();

    @Select("SELECT " + COLS + " FROM exam_plan WHERE id = #{id} AND del_flag = 0")
    ExamPlan findById(Long id);

    @Select("SELECT " + COLS + " FROM exam_plan WHERE status = '报名中' AND del_flag = 0 ORDER BY exam_date")
    List<ExamPlan> findOpen();

    @Insert("""
            INSERT INTO exam_plan(subject_code, exam_date, exam_time, venue, capacity, enrolled_count, status)
            VALUES(#{subjectCode}, #{examDate}, #{examTime}, #{venue}, #{capacity}, #{enrolledCount}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExamPlan plan);

    @Update("""
            UPDATE exam_plan SET subject_code = #{subjectCode}, exam_date = #{examDate},
                   exam_time = #{examTime}, venue = #{venue}, capacity = #{capacity}, status = #{status}
            WHERE id = #{id}
            """)
    int update(ExamPlan plan);

    /** 报名人数 +N（冗余缓存，需与明细同步） */
    @Update("UPDATE exam_plan SET enrolled_count = enrolled_count + #{delta} WHERE id = #{id}")
    int addEnrolled(@Param("id") Long id, @Param("delta") int delta);

    @Update("UPDATE exam_plan SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE exam_plan SET del_flag = 1 WHERE id = #{id}")
    int softDelete(Long id);

    @Select("SELECT COUNT(*) FROM exam_plan WHERE del_flag = 0 AND exam_date >= #{today}")
    long countUpcoming(@Param("today") LocalDate today);
}
