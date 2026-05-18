package com.sky.study.controller.user;

import com.sky.study.dto.ResourceListQueryDTO;
import com.sky.study.service.ResourceService;
import com.sky.study.vo.ResourceCategoryVO;
import com.sky.study.vo.ResourceVO;
import com.sky.study.vo.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController("userResourceController")
@Slf4j
@RequestMapping("/api/resource")
public class ResourceController {
    @Resource
    private ResourceService resourceService;

    @GetMapping("/category")
    public Result<List<ResourceCategoryVO>> category() {
        log.info("resource category summary request");
        return Result.success(resourceService.category());
    }

    @GetMapping("/list")
    public Result<List<ResourceVO>> list(@RequestParam(required = false) String resourceType,
                                         @RequestParam LocalDate reserveDate,
                                         @RequestParam LocalTime startTime,
                                         @RequestParam LocalTime endTime) {
        ResourceListQueryDTO resourceListQueryDTO = new ResourceListQueryDTO();
        resourceListQueryDTO.setResourceType(resourceType);
        resourceListQueryDTO.setReserveDate(reserveDate);
        resourceListQueryDTO.setStartTime(startTime);
        resourceListQueryDTO.setEndTime(endTime);
        log.info("resource list request: resourceType={}, reserveDate={}, startTime={}, endTime={}",
                resourceType, reserveDate, startTime, endTime);
        return Result.success(resourceService.list(resourceListQueryDTO));
    }

    @GetMapping("/{id}")
    public Result<ResourceVO> get(@PathVariable Long id) {
        log.info("resource query request: id={}", id);
        ResourceVO resourceVO = resourceService.get(id);
        log.info("{}", resourceVO);
        return Result.success(resourceVO);
    }
}
