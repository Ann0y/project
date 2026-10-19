package sch.whvcse.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import sch.whvcse.entity.Payment;

/** payment 缴费记录 */
@Mapper
public interface PaymentMapper {

    String COLS = "id, student_id, item, amount, pay_time, pay_method, status, operator_id";

    @Select("SELECT " + COLS + " FROM payment ORDER BY id DESC")
    List<Payment> findAll();

    @Select("SELECT " + COLS + " FROM payment WHERE id = #{id}")
    Payment findById(Long id);

    @Select("SELECT " + COLS + " FROM payment WHERE student_id = #{studentId} ORDER BY id DESC")
    List<Payment> findByStudentId(Long studentId);

    @Insert("""
            INSERT INTO payment(student_id, item, amount, pay_time, pay_method, status, operator_id)
            VALUES(#{studentId}, #{item}, #{amount}, #{payTime}, #{payMethod}, #{status}, #{operatorId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Payment payment);

    @Update("""
            UPDATE payment SET item = #{item}, amount = #{amount}, pay_time = #{payTime},
                   pay_method = #{payMethod}, status = #{status}
            WHERE id = #{id}
            """)
    int update(Payment payment);

    @Delete("DELETE FROM payment WHERE id = #{id}")
    int delete(Long id);

    /** 登记缴费：置为已缴并记录时间/方式 */
    @Update("""
            UPDATE payment SET status = '已缴', pay_time = NOW(),
                   pay_method = #{payMethod}, operator_id = #{operatorId}
            WHERE id = #{id}
            """)
    int markPaid(@Param("id") Long id,
                 @Param("payMethod") String payMethod,
                 @Param("operatorId") Long operatorId);

    @Select("SELECT COUNT(*) FROM payment WHERE student_id = #{studentId} AND status = '未缴'")
    long countUnpaid(Long studentId);
}
