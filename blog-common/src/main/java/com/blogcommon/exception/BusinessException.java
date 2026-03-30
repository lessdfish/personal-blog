package com.blogcommon.exception;

import com.blogcommon.enums.ResultCode;

/**
 * ClassName:BusinessException
 * Package:com.blogcommon.exception
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/26 - 21:54
 * @Version: v1.0
 *
 */
public class BusinessException extends RuntimeException{
    private final Integer code;
    public BusinessException(ResultCode resultCode){
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code,String message){
        super(message);
        this.code = code;
    }

    public Integer getCode(){
        return code;
    }
}
