package sch.whvcse.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理（需求文档 7 章：统一封装为 {code, message, data}）。
 *
 * 同时把业务码映射到真实 HTTP 状态码：
 *   - 前端 axios 拦截器能据此识别 401 自动跳登录页；
 *   - 也便于用 curl / Postman 直观判断结果。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：code 与 HTTP status 保持一致 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException e) {
        int code = e.getCode();
        HttpStatus status = switch (code) {
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            default -> code >= 400 && code < 500
                    ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(Result.fail(code, e.getMessage()));
    }

    /** @Valid 参数校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return ResponseEntity.badRequest().body(Result.fail(400, msg));
    }

    /** 请求体格式错误（如 JSON 解析失败） */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleBadBody(Exception e) {
        return ResponseEntity.badRequest().body(Result.fail(400, "请求参数格式不正确"));
    }

    /** 路径参数类型不匹配（如 /students/abc） */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(Exception e) {
        return ResponseEntity.badRequest().body(Result.fail(400, "请求参数类型不正确"));
    }

    /** 访问不存在的接口 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.fail(404, "接口不存在：" + e.getResourcePath()));
    }

    /** 数据库约束冲突（唯一键 / 外键等） */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrity(
            org.springframework.dao.DataIntegrityViolationException e) {
        String msg = String.valueOf(e.getMostSpecificCause().getMessage());
        String friendly = msg.contains("uk_") || msg.contains("Duplicate")
                ? "数据已存在，请勿重复提交"
                : msg.contains("foreign key") || msg.contains("FK")
                    ? "该数据仍被其它记录引用，无法删除"
                    : "数据不合法或超出长度限制";
        return ResponseEntity.badRequest().body(Result.fail(400, friendly));
    }

    /** 兜底 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        // 打印堆栈便于排查，但只把概要返回给前端
        e.printStackTrace();
        String hint = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(500, "服务器内部错误：" + hint));
    }
}
