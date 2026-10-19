package sch.whvcse.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import sch.whvcse.entity.Coach;

/** coach 教练档案 */
@Mapper
public interface CoachMapper {

    String COLS = "id, user_id, code, phone, license_types, status, del_flag";

    @Select("SELECT " + COLS + " FROM coach WHERE del_flag = 0 ORDER BY id")
    List<Coach> findAll();

    @Select("SELECT " + COLS + " FROM coach WHERE id = #{id} AND del_flag = 0")
    Coach findById(Long id);

    @Select("SELECT " + COLS + " FROM coach WHERE user_id = #{userId} AND del_flag = 0")
    Coach findByUserId(Long userId);

    @Select("SELECT " + COLS + " FROM coach WHERE status = '在职' AND del_flag = 0 ORDER BY id")
    List<Coach> findActive();

    @Select("""
            <script>
            SELECT id, user_id, code, phone, license_types, status, del_flag
            FROM coach
            WHERE del_flag = 0
            <if test="keyword != null and keyword != ''">
              AND (code LIKE CONCAT('%', #{keyword}, '%') OR phone LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY id
            </script>
            """)
    List<Coach> searchByKeyword(@Param("keyword") String keyword);

    @Insert("""
            INSERT INTO coach(user_id, code, phone, license_types, status)
            VALUES(#{userId}, #{code}, #{phone}, #{licenseTypes}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Coach coach);

    @Update("""
            UPDATE coach SET phone = #{phone}, license_types = #{licenseTypes}, status = #{status}
            WHERE id = #{id}
            """)
    int update(Coach coach);

    @Update("UPDATE coach SET del_flag = 1 WHERE id = #{id}")
    int softDelete(Long id);
}
