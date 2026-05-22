package com.sky.study.mapper;

import com.sky.study.entity.ReservationAuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReservationAuditLogMapper {

    @Insert("insert into reservation_audit_log (reservation_id, admin_id, old_status, new_status, review_remark) " +
            "values (#{reservationId}, #{adminId}, #{oldStatus}, #{newStatus}, #{reviewRemark})")
    void insert(ReservationAuditLog reservationAuditLog);

    @Select("select * from reservation_audit_log where reservation_id = #{reservationId} order by create_time desc")
    List<ReservationAuditLog> listByReservationId(Long reservationId);
}
