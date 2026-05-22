package com.sky.study.mapper;

import com.sky.study.annotation.AutoFill;
import com.sky.study.entity.Reservation;
import com.sky.study.enumeration.OperationType;
import com.sky.study.vo.ReservationVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Mapper
public interface ReservationMapper {

    @AutoFill(OperationType.INSERT)
    @Insert("insert into reservation (user_id, resource_id, reserve_date, start_time, end_time, purpose, status, review_remark, create_time, update_time) " +
            "values (#{userId}, #{resourceId}, #{reserveDate}, #{startTime}, #{endTime}, #{purpose}, #{status}, #{reviewRemark}, #{createTime}, #{updateTime})")
    void insert(Reservation reservation);

    @Select("select * from reservation where id = #{id}")
    Reservation getById(Long id);

    @Select("select * from reservation where id = #{id} for update")
    Reservation getByIdForUpdate(Long id);

    List<ReservationVO> pageQueryByUserId(@Param("userId") Long userId, @Param("status") Integer status);

    List<ReservationVO> pageQuery(@Param("status") Integer status);

    Integer countApprovedConflict(@Param("resourceId") Long resourceId,
                                  @Param("reserveDate") LocalDate reserveDate,
                                  @Param("startTime") LocalTime startTime,
                                  @Param("endTime") LocalTime endTime);

    List<Reservation> lockApprovedByResourceAndDate(@Param("resourceId") Long resourceId,
                                                    @Param("reserveDate") LocalDate reserveDate);

    @AutoFill(OperationType.UPDATE)
    void update(Reservation reservation);
}
