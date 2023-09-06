package com.siheng.metadataplatform.dto;

import com.siheng.metadataplatform.enums.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Dearest
 * @date 2023/9/6 11:04 上午
 * @Desc
 */
@Data
@Builder
@AllArgsConstructor
public class ResultDto<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String message;

    private T data;


    public ResultDto(Integer code) {
        this.code = code;
    }

    public static <T> ResultDto<T> success() {
        return new ResultDto<T>(ResultCode.SUCCESS.getCode());
    }


    public static <T> ResultDto success(T data){
        return ResultDto.builder()
                .code(ResultCode.SUCCESS.getCode())
                .data(data)
                .message(ResultCode.SUCCESS.getMessage())
                .build();
    }



    public static <T> ResultDto error(String message){
        return ResultDto.builder()
                .code(ResultCode.ERROR.getCode())
                .message(message)
                .data(null)
                .build();
    }


}
