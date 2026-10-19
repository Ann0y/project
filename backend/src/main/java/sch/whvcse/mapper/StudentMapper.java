package sch.whvcse.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import sch.whvcse.entity.Student;

/** student 学员档案 */
@Mapper
public interface StudentMapper {

    String COLS = "id, user_id, id_card, phone, license_type, enroll_date, status, coach_id, vehicle_id, del_flag";

    @Select("SELECT " + COLS + " FROM student WHERE del_flag = 0 ORDER BY id")
    List<Student> findAll();

    @Select("SELECT " + COLS + " FROM student WHERE id = #{id} AND del_flag = 0")
    Student findById(Long id);

    @Select("SELECT " + COLS + " FROM student WHERE user_id = #{userId} AND del_flag = 0")
    Student findByUserId(Long userId);

    @Select("SELECT " + COLS + " FROM student WHERE coach_id = #{coachId} AND del_flag = 0 ORDER BY id")
    List<Student> findByCoachId(Long coachId);

    /** F11 多条件组合查询 */
    @Select("""
            <script>
            SELECT s.id, s.user_id, s.id_card, s.phone, s.license_type, s.enroll_date,
                   s.status, s.coach_id, s.vehicle_id, s.del_flag
            FROM student s
            JOIN sys_user u ON u.id = s.user_id
            WHERE s.del_flag = 0
            <if test="name != null and name != ''"> AND u.name LIKE CONCAT('%', #{name}, '%') </if>
            <if test="idCard != null and idCard != ''"> AND s.id_card LIKE CONCAT('%', #{idCard}, '%') </if>
            <if test="status != null and status != ''"> AND s.status = #{status} </if>
            <if test="licenseType != null and licenseType != ''"> AND s.license_type = #{licenseType} </if>
            <if test="coachId != null"> AND s.coach_id = #{coachId} </if>
            ORDER BY s.id
            </script>
            """)
    List<Student> search(@Param("name") String name,
                         @Param("idCard") String idCard,
                         @Param("status") String status,
                         @Param("licenseType") String licenseType,
                         @Param("coachId") Long coachId);

    @Insert("""
            INSERT INTO student(user_id, id_card, phone, license_type, enroll_date, status, coach_id, vehicle_id)
            VALUES(#{userId}, #{idCard}, #{phone}, #{licenseType}, #{enrollDate}, #{status}, #{coachId}, #{vehicleId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Student student);

    @Update("""
            UPDATE student SET id_card = #{idCard}, phone = #{phone}, license_type = #{licenseType},
                   enroll_date = #{enrollDate}, status = #{status},
                   coach_id = #{coachId}, vehicle_id = #{vehicleId}
            WHERE id = #{id}
            """)
    int update(Student student);

    @Update("UPDATE student SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE student SET coach_id = #{coachId}, vehicle_id = #{vehicleId} WHERE id = #{id}")
    int assignCoach(@Param("id") Long id, @Param("coachId") Long coachId, @Param("vehicleId") Long vehicleId);

    @Update("UPDATE student SET del_flag = 1 WHERE id = #{id}")
    int softDelete(Long id);

    /** 统计：某状态学员数 */
    @Select("SELECT COUNT(*) FROM student WHERE status = #{status} AND del_flag = 0")
    long countByStatus(String status);

    /** 统计：某月报名人数 */
    @Select("""
            SELECT COUNT(*) FROM student
            WHERE del_flag = 0
              AND enroll_date >= #{start} AND enroll_date < #{end}
            """)
    long countByEnrollRange(@Param("start") java.time.LocalDate start,
                            @Param("end") java.time.LocalDate end);
}
