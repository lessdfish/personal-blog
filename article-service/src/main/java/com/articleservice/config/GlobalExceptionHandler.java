package com.articleservice.config;

/**
 * ClassName:GlobalExceptionHandler
 * Package:com.articleservice.config
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/28 - 23:04
 * @Version: v1.0
 *
 */
import com.blogcommon.exception.BusinessException;
import com.blogcommon.result.Result;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常：把代码里主动抛出的 BusinessException 转成统一的接口返回。
     */
    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常：例如标题为空时，把校验失败的提示返回给前端。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return Result.fail(400, message);
    }

    /**
     * 兜底处理未知异常：防止没有被单独处理的错误直接暴露给用户。
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        e.printStackTrace();
        return Result.fail("系统异常");
    }
}
