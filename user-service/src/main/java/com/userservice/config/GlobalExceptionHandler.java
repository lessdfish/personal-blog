package com.userservice.config;

import com.blogcommon.exception.BusinessException;
import com.blogcommon.enums.ResultCode;
import com.blogcommon.result.Result;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * ClassName:GlobalExceptionHandler
 * Package:com.userservice.config
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/27 - 18:29
 * @Version: v1.0
 *
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 配置 handleBusinessException：为当前服务准备运行时需要的组件或参数。
     */
    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e){
        return Result.fail(e.getCode(),e.getMessage());
    }

    /**
     * 配置 handleMethodArgumentNotValidException：为当前服务准备运行时需要的组件或参数。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return Result.fail(400, message);
    }

    /**
     * 配置 handleMaxUploadSizeExceededException：为当前服务准备运行时需要的组件或参数。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<String> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "上传文件过大");
    }

    /**
     * 配置 handleException：为当前服务准备运行时需要的组件或参数。
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        e.printStackTrace();
        return Result.fail("系统异常，请稍后再试！");
    }
}
