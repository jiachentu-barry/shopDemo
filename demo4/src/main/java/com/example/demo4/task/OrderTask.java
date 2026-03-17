package com.example.demo4.task;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo4.entity.ExpiredOrderReport;
import com.example.demo4.service.OrderService;

@Component
public class OrderTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTask.class);

    private final OrderService orderService;
    private final String reportExportDir;

    public OrderTask(OrderService orderService,
                     @Value("${report.export-dir:reports}") String reportExportDir) {
        this.orderService = orderService;
        this.reportExportDir = reportExportDir;
    }

    /**
     * 每5分钟检查一次，自动取消超过30分钟未支付的订单并恢复库存
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void cancelExpiredOrders() {
        int count = orderService.autoCancelExpiredOrders();
        if (count > 0) {
            log.info("定时任务：自动取消了 {} 个超时未支付订单", count);
        }
    }

    /**
     * 每天凌晨1点生成前一天的超时未支付订单报表
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void generateDailyReport() {
        var report = orderService.generateDailyExpiredReport();
        exportReportCsv(report);
        log.info("定时任务：生成 {} 日报表，取消订单 {} 个，总金额 {}",
                report.getReportDate(), report.getCancelledCount(), report.getTotalAmount());
    }

    private void exportReportCsv(ExpiredOrderReport report) {
        try {
            Path exportDir = Paths.get(reportExportDir);
            Files.createDirectories(exportDir);

            String fileName = "expired-orders-" + report.getReportDate() + ".csv";
            Path csvPath = exportDir.resolve(fileName);

            StringBuilder content = new StringBuilder();
            content.append("reportDate,cancelledCount,totalAmount,orderNos\n");
            content.append(csv(report.getReportDate() == null ? "" : report.getReportDate().toString())).append(",")
                    .append(csv(report.getCancelledCount() == null ? "0" : report.getCancelledCount().toString())).append(",")
                    .append(csv(toPlainString(report.getTotalAmount()))).append(",")
                    .append(csv(report.getOrderNos()))
                    .append("\n");

            Files.writeString(csvPath, content.toString(), StandardCharsets.UTF_8);
            log.info("定时任务：超时报表CSV已导出 -> {}", csvPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("定时任务：导出超时报表CSV失败", e);
        }
    }

    private String toPlainString(BigDecimal amount) {
        return amount == null ? "0" : amount.toPlainString();
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
