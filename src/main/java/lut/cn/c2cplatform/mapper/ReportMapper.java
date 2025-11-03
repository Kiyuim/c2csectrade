package lut.cn.c2cplatform.mapper;

import lut.cn.c2cplatform.entity.Report;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ReportMapper {
    void insert(Report report);
    Report selectById(Long id);
    List<Report> selectAll();
    void updateStatus(Report report);
    Report selectPendingReportByProductAndReporter(Long productId, Long reporterId);
}
