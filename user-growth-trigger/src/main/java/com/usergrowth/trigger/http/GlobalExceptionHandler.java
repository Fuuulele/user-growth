package com.usergrowth.trigger.http;

import com.usergrowth.api.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：积分不足、库存不足、任务不存在等
     */
    @ExceptionHandler(RuntimeException.class)
    public R<Void> handleRuntimeException(RuntimeException e) {
        log.warn("业务异常：{}", e.getMessage());
        return R.fail(400, e.getMessage());
    }

    /**
     * 缺少必填参数：userId 未传等
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数：{}", e.getParameterName());
        return R.fail(400, "缺少必填参数：" + e.getParameterName());
    }

    /**
     * 参数类型不匹配：userId 传了 "abc" 等
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型错误，参数名：{}，传入值：{}", e.getName(), e.getValue());
        return R.fail(400, "参数 [" + e.getName() + "] 类型错误，请检查传入值");
    }

    /**
     * 参数非法
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数非法：{}", e.getMessage());
        return R.fail(400, "参数非法：" + e.getMessage());
    }

    /**
     * 兜底异常：未知系统错误，不暴露细节
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return R.fail(500, "系统繁忙，请稍后再试");
    }
}