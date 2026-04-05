package com.articleservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BoardCreateDTO {
    @NotBlank(message = "boardName不能为空")
    private String boardName;
    @NotBlank(message = "boardCode不能为空")
    private String boardCode;
    private String description;
    private Integer sortOrder;
}
