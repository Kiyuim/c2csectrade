package lut.cn.c2cplatform.service;

import lut.cn.c2cplatform.entity.Report;
import lut.cn.c2cplatform.mapper.ReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private ReportMapper reportMapper;

    public void createReport(Report report) {
        // 检查是否已存在待处理的举报
        Report existingReport = reportMapper.selectPendingReportByProductAndReporter(
            report.getProductId(),
            report.getReporterId()
        );

        if (existingReport != null) {
            throw new RuntimeException("您已经举报过该商品，请等待管理员处理");
        }

        report.setStatus("PENDING");
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        reportMapper.insert(report);
    }

    public List<Report> getAllReports() {
        return reportMapper.selectAll();
    }

    public Report getReportById(Long id) {
        return reportMapper.selectById(id);
    }

    public void updateReportStatus(Long reportId, String status) {
        Report report = reportMapper.selectById(reportId);
        if (report != null) {
            report.setStatus(status);
            reportMapper.updateStatus(report);
        }
    }
}
