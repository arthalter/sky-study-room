package com.sky.study.controller.admin;

import com.sky.study.dto.ResourcePageQueryDTO;
import com.sky.study.dto.ResourceSaveDTO;
import com.sky.study.dto.ResourceStatusDTO;
import com.sky.study.service.ResourceService;
import com.sky.study.vo.PageResult;
import com.sky.study.vo.ResourceVO;
import com.sky.study.vo.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("adminResourceController")
@Slf4j
@RequestMapping("/api/admin/resource")
public class ResourceController {
    @Resource
    private ResourceService resourceService;

    @PostMapping
    public Result save(@RequestBody ResourceSaveDTO resourceSaveDTO) {
        log.info("资源保存请求: {}", resourceSaveDTO);
        resourceService.save(resourceSaveDTO);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<ResourceVO> get(@PathVariable Long id) {
        log.info("资源查询请求: id={}", id);
        ResourceVO resourceVO = resourceService.get(id);
        log.info("{}", resourceVO);
        return Result.success(resourceVO);
    }

    @PutMapping
    public Result update(@RequestBody ResourceSaveDTO resourceSaveDTO) {
        log.info("资源更新请求: {}", resourceSaveDTO);
        resourceService.update(resourceSaveDTO);
        return Result.success();
    }

    @PostMapping("/status")
    public Result updateStatus(@RequestBody ResourceStatusDTO resourceStatusDTO) {
        log.info("状态更新请求: {}", resourceStatusDTO);
        resourceService.updateStatus(resourceStatusDTO.getId(), resourceStatusDTO.getStatus());
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageResult> pageQuery(
            @RequestParam Integer page,
            @RequestParam Integer pageSize,
            @RequestParam(required = false) String resourceName,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Integer status
    ) {
        log.info("page={}, pageSize={}, resourceName={}, resourceType={}, status={}",
                page, pageSize, resourceName, resourceType, status);

        ResourcePageQueryDTO dto = new ResourcePageQueryDTO();
        dto.setPage(page);
        dto.setPageSize(pageSize);
        dto.setResourceName(resourceName);
        dto.setResourceType(resourceType);
        dto.setStatus(status);

        PageResult pageResult = resourceService.pageQuery(dto);
        return Result.success(pageResult);
    }
}
