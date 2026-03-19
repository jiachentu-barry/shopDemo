package com.example.demo4.task;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo4.entity.ExpiredOrderReport;
import com.example.demo4.entity.Order;
import com.example.demo4.entity.OrderItem;
import com.example.demo4.service.OrderService;

@Component
public class OrderTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTask.class);

    private final OrderService orderService;
    private final String reportExportDir;
    private final String salesExportDir;

    public OrderTask(OrderService orderService,
                     @Value("${report.export-dir:reports}") String reportExportDir,
                     @Value("${sales.export-dir:reports}") String salesExportDir) {
        this.orderService = orderService;
        this.reportExportDir = reportExportDir;
        this.salesExportDir = salesExportDir;
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
     * 每天凌晨1点生成当天的超时未支付订单报表
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void generateDailyReport() {
        var report = generateAndExportReportByDate(LocalDate.now());
        log.info("定时任务：生成 {} 日报表，取消订单 {} 个，总金额 {}",
                report.getReportDate(), report.getCancelledCount(), report.getTotalAmount());
    }

    /**
     * 每天凌晨1点10分生成前一天销售汇总（按订单明细）
     */
    @Scheduled(cron = "0 10 1 * * ?")
    public void generateDailySalesSummary() {
        LocalDate reportDate = LocalDate.now().minusDays(1);
        Path csvPath = exportDailySalesSummaryCsv(reportDate);
        log.info("定时任务：销售汇总CSV已导出 -> {}", csvPath.toAbsolutePath());
    }

    public ExpiredOrderReport generateTodayReportNow() {
        return generateAndExportReportByDate(LocalDate.now());
    }

    public ExpiredOrderReport generateAndExportReportByDate(LocalDate reportDate) {
        ExpiredOrderReport report = orderService.generateExpiredReportByDate(reportDate);
        exportReportCsv(report);
        return report;
    }

    public Path generateAndExportReportCsvByDate(LocalDate reportDate) {
        ExpiredOrderReport report = orderService.generateExpiredReportByDate(reportDate);
        return exportReportCsv(report);
    }

    private Path exportDailySalesSummaryCsv(LocalDate reportDate) {
        try {
            Path exportDir = Paths.get(salesExportDir);
            Files.createDirectories(exportDir);

            String fileName = "sales-orders-" + reportDate + ".csv";
            Path csvPath = exportDir.resolve(fileName);

            List<Order> paidOrders = orderService.getPaidOrdersByDate(reportDate);
            BigDecimal totalAmount = paidOrders.stream()
                    .map(Order::getTotalAmount)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            StringBuilder content = new StringBuilder();
            content.append("reportDate,orderNo,userId,status,payTime,totalQuantity,totalAmount,itemDetails\n");
            for (Order order : paidOrders) {
                content.append(csv(reportDate.toString())).append(",")
                        .append(csv(order.getOrderNo())).append(",")
                        .append(csv(order.getUserId() == null ? "" : order.getUserId().toString())).append(",")
                        .append(csv(order.getStatus())).append(",")
                        .append(csv(formatDateTime(order.getPayTime()))).append(",")
                        .append(csv(order.getTotalQuantity() == null ? "0" : order.getTotalQuantity().toString())).append(",")
                        .append(csv(toPlainString(order.getTotalAmount()))).append(",")
                        .append(csv(buildItemDetails(order.getItems())))
                        .append("\n");
            }

            content.append(csv(reportDate.toString())).append(",")
                    .append(csv("TOTAL")).append(",")
                    .append(csv("-")).append(",")
                    .append(csv("-")).append(",")
                    .append(csv("-")).append(",")
                    .append(csv(paidOrders.stream().map(Order::getTotalQuantity).filter(v -> v != null).reduce(0, Integer::sum).toString())).append(",")
                    .append(csv(toPlainString(totalAmount))).append(",")
                    .append(csv("orders=" + paidOrders.size()))
                    .append("\n");

            Files.writeString(csvPath, content.toString(), StandardCharsets.UTF_8);
            return csvPath;
        } catch (IOException e) {
            log.error("定时任务：导出销售汇总CSV失败", e);
            throw new RuntimeException("导出销售汇总CSV失败", e);
        }
    }

    private Path exportReportCsv(ExpiredOrderReport report) {
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
            return csvPath;
        } catch (IOException e) {
            log.error("定时任务：导出超时报表CSV失败", e);
            throw new RuntimeException("导出超时报表CSV失败", e);
        }
    }

    private String toPlainString(BigDecimal amount) {
        return amount == null ? "0" : amount.toPlainString();
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? "" : time.toString();
    }

    private String buildItemDetails(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return items.stream()
                .map(i -> (i.getProductName() == null ? "商品" : i.getProductName())
                        + " x" + (i.getQuantity() == null ? 0 : i.getQuantity())
                        + "(" + toPlainString(i.getSubtotal()) + ")")
                .collect(Collectors.joining(" | "));
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
