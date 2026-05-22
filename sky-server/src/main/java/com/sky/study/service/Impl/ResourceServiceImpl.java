package com.sky.study.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.sky.study.utils.ReservationValidationUtil;
import com.sky.study.vo.PageResult;
import com.sky.study.vo.ResourceCategoryVO;
import com.sky.study.vo.ResourceVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.sky.study.constant.MessageConstant.RESOURCE_NOT_FOUND;

@Service
@Slf4j
public class ResourceServiceImpl implements ResourceService {
    private static final String RESOURCE_CATEGORY_CACHE_KEY = "resource:category";
    private static final long RESOURCE_CATEGORY_CACHE_TTL_MINUTES = 10L;
    private static final TypeReference<List<ResourceCategoryVO>> RESOURCE_CATEGORY_LIST_TYPE = new TypeReference<>() {
    };

    @Autowired
    private ResourceMapper resourceMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    public void save(ResourceSaveDTO resourceSaveDTO) {
        Resource resource = new Resource();
        BeanUtils.copyProperties(resourceSaveDTO, resource);
        resource.setStatus(ResourceStatusConstant.ENABLED);
        log.info("resource:{}", resource);
        resourceMapper.insert(resource);
        evictResourceCategoryCache();
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
        evictResourceCategoryCache();
    }

    public void updateStatus(Long id, Integer status) {
        if (id == null || id == 0) {
            throw new RuntimeException(RESOURCE_NOT_FOUND);
        }
        resourceMapper.updateStatus(id, status);
        evictResourceCategoryCache();
    }

    public PageResult pageQuery(ResourcePageQueryDTO resourcePageQueryDTO) {
        PageHelper.startPage(resourcePageQueryDTO.getPage(), resourcePageQueryDTO.getPageSize());
        Page<ResourceVO> page = (Page<ResourceVO>) resourceMapper.pageQuery(resourcePageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    public List<ResourceCategoryVO> category() {
        String cachedCategories = stringRedisTemplate.opsForValue().get(RESOURCE_CATEGORY_CACHE_KEY);
        if (StringUtils.hasText(cachedCategories)) {
            try {
                return objectMapper.readValue(cachedCategories, RESOURCE_CATEGORY_LIST_TYPE);
            } catch (JsonProcessingException e) {
                log.warn("resource category cache parse failed, key={}", RESOURCE_CATEGORY_CACHE_KEY, e);
            }
        }

        List<ResourceCategoryVO> categories = resourceMapper.category();
        for (ResourceCategoryVO category : categories) {
            category.setResourceTypeName(getResourceTypeName(category.getResourceType()));
        }
        try {
            String categoryJson = objectMapper.writeValueAsString(categories);
            stringRedisTemplate.opsForValue().set(
                    RESOURCE_CATEGORY_CACHE_KEY,
                    categoryJson,
                    RESOURCE_CATEGORY_CACHE_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (JsonProcessingException e) {
            log.warn("resource category cache write failed, key={}", RESOURCE_CATEGORY_CACHE_KEY, e);
        }
        return categories;
    }

    public List<ResourceVO> list(ResourceListQueryDTO resourceListQueryDTO) {
        ReservationValidationUtil.validateTimeRange(
                resourceListQueryDTO.getStartTime(),
                resourceListQueryDTO.getEndTime()
        );
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

    private void evictResourceCategoryCache() {
        stringRedisTemplate.delete(RESOURCE_CATEGORY_CACHE_KEY);
        log.info("evict resource category cache: key={}", RESOURCE_CATEGORY_CACHE_KEY);
    }
}
