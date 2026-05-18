package com.sky.study.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceCategoryVO implements Serializable {

    private String resourceType;

    private String resourceTypeName;

    private Integer totalCount;

    private Integer availableCount;
}
