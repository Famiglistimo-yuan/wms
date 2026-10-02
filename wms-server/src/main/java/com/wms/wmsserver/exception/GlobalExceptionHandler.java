package com.wms.wmsserver.exception;

import com.wms.common.BusinessException;
import com.wms.common.ErrorCode;
import com.wms.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：统一转 Result（code/message），客户端按 code 分类弹窗（CONTRIBUTING §4.2）。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：code 与 message 原样透传 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验失败：400 + 字段级错误信息 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("参数校验失败");
        return Result.fail(ErrorCode.BAD_REQUEST, message);
    }

    /** 请求体不可读（JSON 畸形/类型不匹配）：400，属客户端请求问题，不进 500 兜底 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleUnreadable(HttpMessageNotReadableException e) {
        return Result.fail(ErrorCode.BAD_REQUEST, "请求体格式错误");
    }

    /** 静态资源不存在（含升级包）：直接 HTTP 404 空 body，不进 500 兜底、不刷 error 日志。
     *  这是传输层资源语义（Result 业务码体系之外），Updater 等下载方按 HTTP 状态码处理 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /** 兜底：500，不向前端泄露堆栈 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpected(Exception e) {
        log.error("服务端异常", e);
        return Result.fail(ErrorCode.SERVER_ERROR, "服务端异常，请稍后重试");
    }
}
