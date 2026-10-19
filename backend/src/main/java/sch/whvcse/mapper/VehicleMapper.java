package sch.whvcse.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import sch.whvcse.entity.Vehicle;

/** vehicle 教练车辆 */
@Mapper
public interface VehicleMapper {

    String COLS = "id, plate_no, model, coach_id, status, del_flag";

    @Select("SELECT " + COLS + " FROM vehicle WHERE del_flag = 0 ORDER BY id")
    List<Vehicle> findAll();

    @Select("SELECT " + COLS + " FROM vehicle WHERE id = #{id} AND del_flag = 0")
    Vehicle findById(Long id);

    @Select("SELECT " + COLS + " FROM vehicle WHERE coach_id = #{coachId} AND del_flag = 0 ORDER BY id")
    List<Vehicle> findByCoachId(Long coachId);

    @Select("""
            <script>
            SELECT id, plate_no, model, coach_id, status, del_flag
            FROM vehicle
            WHERE del_flag = 0
            <if test="keyword != null and keyword != ''">
              AND (plate_no LIKE CONCAT('%', #{keyword}, '%') OR model LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY id
            </script>
            """)
    List<Vehicle> searchByKeyword(@Param("keyword") String keyword);

    @Insert("""
            INSERT INTO vehicle(plate_no, model, coach_id, status)
            VALUES(#{plateNo}, #{model}, #{coachId}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Vehicle vehicle);

    @Update("""
            UPDATE vehicle SET plate_no = #{plateNo}, model = #{model},
                   coach_id = #{coachId}, status = #{status}
            WHERE id = #{id}
            """)
    int update(Vehicle vehicle);

    @Update("UPDATE vehicle SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE vehicle SET del_flag = 1 WHERE id = #{id}")
    int softDelete(Long id);
}
