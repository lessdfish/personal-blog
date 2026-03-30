package com.blogcommon.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ClassName:Result
 * Package:com.blogcommon.result
 * Description:
 *
 * @Author:lyp
 * @Create:2026/3/26 - 21:51
 * @Version: v1.0
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T>{
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data){
        return new Result<>(200,"success",data);
    }

    public static <T> Result<T> success(){
        return new Result<>(200,"success",null);
    }
    public static <T> Result<T> fail(String message){
        return new Result<>(500,message,null);
    }
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
