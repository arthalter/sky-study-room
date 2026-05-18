package com.sky.study.service;

import com.sky.study.dto.ResourceListQueryDTO;
import com.sky.study.dto.ResourcePageQueryDTO;
import com.sky.study.dto.ResourceSaveDTO;
import com.sky.study.vo.PageResult;
import com.sky.study.vo.ResourceCategoryVO;
import com.sky.study.vo.ResourceVO;

import java.util.List;

public interface ResourceService {

    void save(ResourceSaveDTO resourceSaveDTO);

    ResourceVO get(Long id);

    void update(ResourceSaveDTO resourceSaveDTO);

    void updateStatus(Long id, Integer status);

    PageResult pageQuery(ResourcePageQueryDTO resourcePageQueryDTO);

    List<ResourceCategoryVO> category();

    List<ResourceVO> list(ResourceListQueryDTO resourceListQueryDTO);
}
