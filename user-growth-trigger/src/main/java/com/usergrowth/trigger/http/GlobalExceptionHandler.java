package com.usergrowth.trigger.http;

import com.usergrowth.api.common.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.alibaba.csp.sentinel.slots.block.BlockException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：积分不足、库存不足等
     */
    @ExceptionHandler(RuntimeException.class)
    public R<Void> handleRuntimeException(RuntimeException e) {
        log.warn("业务异常：{}", e.getMessage());
        return R.fail(400, e.getMessage());
    }

    /**
     * @Valid 校验失败：DTO 对象字段校验不通过
     * 适用于 @RequestBody 或 @ModelAttribute
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败：{}", message);
        return R.fail(400, message);
    }

    /**
     * @Validated 单个参数校验失败
     * 适用于 @RequestParam 上直接加注解的场景
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数约束违反：{}", message);
        return R.fail(400, message);
    }

    /**
     * 缺少必填参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数：{}", e.getParameterName());
        return R.fail(400, "缺少必填参数：" + e.getParameterName());
    }

    /**
     * 参数类型不匹配
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
     * 兜底异常
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return R.fail(500, "系统繁忙，请稍后再试");
    }

    /**
     * Sentinel 限流/熔断异常兜底
     * 当 blockHandler 方法签名不匹配时会走这里
     */
    @ExceptionHandler(BlockException.class)
    public R<Void> handleBlockException(BlockException e) {
        log.warn("Sentinel 触发限流或熔断：{}", e.getMessage());
        return R.fail(429, "系统繁忙，请稍后再试～");
    }
}