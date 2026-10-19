package sch.whvcse.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import sch.whvcse.entity.TrainCourse;

/** train_course 培训课程 / 科目学时配置 */
@Mapper
public interface TrainCourseMapper {

    String COLS = "id, license_type, subject_code, subject_name, required_hours, del_flag";

    @Select("SELECT " + COLS + " FROM train_course WHERE del_flag = 0 ORDER BY license_type, subject_code")
    List<TrainCourse> findAll();

    @Select("SELECT " + COLS + " FROM train_course WHERE license_type = #{licenseType} AND del_flag = 0 ORDER BY subject_code")
    List<TrainCourse> findByLicenseType(String licenseType);

    @Select("""
            SELECT id, license_type, subject_code, subject_name, required_hours, del_flag
            FROM train_course
            WHERE license_type = #{licenseType} AND subject_code = #{subjectCode} AND del_flag = 0
            """)
    TrainCourse findByTypeAndSubject(@Param("licenseType") String licenseType,
                                     @Param("subjectCode") String subjectCode);

    @Update("UPDATE train_course SET required_hours = #{requiredHours} WHERE id = #{id}")
    int updateHours(@Param("id") Long id, @Param("requiredHours") java.math.BigDecimal requiredHours);

    @Insert("""
            INSERT INTO train_course(license_type, subject_code, subject_name, required_hours)
            VALUES(#{licenseType}, #{subjectCode}, #{subjectName}, #{requiredHours})
            """)
    int insert(TrainCourse course);
}
