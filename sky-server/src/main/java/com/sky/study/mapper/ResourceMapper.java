package com.sky.study.mapper;

import com.sky.study.annotation.AutoFill;
import com.sky.study.dto.ResourceListQueryDTO;
import com.sky.study.dto.ResourcePageQueryDTO;
import com.sky.study.entity.Resource;
import com.sky.study.enumeration.OperationType;
import com.sky.study.vo.ResourceCategoryVO;
import com.sky.study.vo.ResourceVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ResourceMapper {
    @AutoFill(OperationType.INSERT)
    @Insert("INSERT INTO resource (resource_code, resource_name, resource_type, status, floor, open_time, description) " +
            "VALUES (#{resourceCode}, #{resourceName}, #{resourceType}, #{status}, #{floor}, #{openTime}, #{description})")
    void insert(Resource resource);

    @Select("SELECT * FROM resource WHERE id = #{id}")
    ResourceVO getById(Long id);

    @AutoFill(OperationType.UPDATE)
    @Update("UPDATE resource set status=#{status} where id=#{id}")
    void updateStatus(Long id, Integer status);

    @AutoFill(OperationType.UPDATE)
    void update(Resource resource);

    List<ResourceVO> pageQuery(ResourcePageQueryDTO resourcePageQueryDTO);

    List<ResourceCategoryVO> category();

    List<ResourceVO> list(ResourceListQueryDTO resourceListQueryDTO);
}
