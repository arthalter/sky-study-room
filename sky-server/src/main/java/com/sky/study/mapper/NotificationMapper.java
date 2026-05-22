package com.sky.study.mapper;

import com.sky.study.entity.Notification;
import com.sky.study.vo.NotificationVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {

    @Insert("insert into notification (user_id, title, content, read_status) " +
            "values (#{userId}, #{title}, #{content}, #{readStatus})")
    void insert(Notification notification);

    List<NotificationVO> pageQueryByUserId(@Param("userId") Long userId,
                                           @Param("readStatus") Integer readStatus);
}
