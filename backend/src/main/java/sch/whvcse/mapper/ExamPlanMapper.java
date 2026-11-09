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

    /**
     * 同步考试场次（来自交管系统或考试记录）。
     * <p>考试在交管软件中报考，本系统不再对外提供“发布 / 修改 / 删除考试计划”的接口，
     * 此方法仅供进度同步与演示数据初始化内部使用。</p>
     */
    @Insert("""
            INSERT INTO exam_plan(subject_code, exam_date, exam_time, venue, capacity, enrolled_count, status)
            VALUES(#{subjectCode}, #{examDate}, #{examTime}, #{venue}, #{capacity}, #{enrolledCount}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExamPlan plan);

    /** 只同步场次信息（科目 / 日期 / 时间 / 考场），不改容量与报名状态 */
    @Update("""
            UPDATE exam_plan SET subject_code = #{subjectCode}, exam_date = #{examDate},
                   exam_time = #{examTime}, venue = #{venue}
            WHERE id = #{id}
            """)
    int syncMeta(ExamPlan plan);

    /** 报名人数 +N（冗余缓存，需与明细同步） */
    @Update("UPDATE exam_plan SET enrolled_count = enrolled_count + #{delta} WHERE id = #{id}")
    int addEnrolled(@Param("id") Long id, @Param("delta") int delta);

    @Update("UPDATE exam_plan SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM exam_plan WHERE del_flag = 0 AND exam_date >= #{today}")
    long countUpcoming(@Param("today") LocalDate today);
}
