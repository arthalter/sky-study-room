package com.sky.study.service.Impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.study.constant.ResourceStatusConstant;
import com.sky.study.constant.ResourceTypeConstant;
import com.sky.study.dto.ResourceListQueryDTO;
import com.sky.study.dto.ResourcePageQueryDTO;
import com.sky.study.dto.ResourceSaveDTO;
import com.sky.study.entity.Resource;
import com.sky.study.mapper.ResourceMapper;
import com.sky.study.service.ResourceService;
import com.sky.study.vo.PageResult;
import com.sky.study.vo.ResourceCategoryVO;
import com.sky.study.vo.ResourceVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.sky.study.constant.MessageConstant.RESOURCE_NOT_FOUND;

@Service
@Slf4j
public class ResourceServiceImpl implements ResourceService {
    @Autowired
    private ResourceMapper resourceMapper;

    public void save(ResourceSaveDTO resourceSaveDTO) {
        Resource resource = new Resource();
        BeanUtils.copyProperties(resourceSaveDTO, resource);
        resource.setStatus(ResourceStatusConstant.ENABLED);
        log.info("resource:{}", resource);
        resourceMapper.insert(resource);
    }

    public ResourceVO get(Long id) {
        if (id == null || id == 0) {
            throw new RuntimeException(RESOURCE_NOT_FOUND);
        }
        ResourceVO resourceVO = resourceMapper.getById(id);

        if (resourceVO == null) {
            throw new RuntimeException(RESOURCE_NOT_FOUND);
        }
        log.info("resource:{}", resourceVO);
        return resourceVO;
    }

    public void update(ResourceSaveDTO resourceSaveDTO) {
        Long id = resourceSaveDTO.getId();
        if (id == null || id == 0) {
            throw new RuntimeException(RESOURCE_NOT_FOUND);
        }
        Resource resource = new Resource();
        BeanUtils.copyProperties(resourceSaveDTO, resource);
        resource.setId(id);
        log.info("resource:{}", resource);
        resourceMapper.update(resource);
    }

    public void updateStatus(Long id, Integer status) {
        if (id == null || id == 0) {
            throw new RuntimeException(RESOURCE_NOT_FOUND);
        }
        resourceMapper.updateStatus(id, status);
    }

    public PageResult pageQuery(ResourcePageQueryDTO resourcePageQueryDTO) {
        PageHelper.startPage(resourcePageQueryDTO.getPage(), resourcePageQueryDTO.getPageSize());
        Page<ResourceVO> page = (Page<ResourceVO>) resourceMapper.pageQuery(resourcePageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    public List<ResourceCategoryVO> category() {
        List<ResourceCategoryVO> categories = resourceMapper.category();
        for (ResourceCategoryVO category : categories) {
            category.setResourceTypeName(getResourceTypeName(category.getResourceType()));
        }
        return categories;
    }

    public List<ResourceVO> list(ResourceListQueryDTO resourceListQueryDTO) {
        return resourceMapper.list(resourceListQueryDTO);
    }

    private String getResourceTypeName(String resourceType) {
        if (ResourceTypeConstant.PUBLIC_SEAT.equals(resourceType)) {
            return "公共自习位";
        }
        if (ResourceTypeConstant.PRIVATE_ROOM.equals(resourceType)) {
            return "独立自习室";
        }
        if (ResourceTypeConstant.MEETING_ROOM.equals(resourceType)) {
            return "讨论室";
        }
        return resourceType;
    }
}
