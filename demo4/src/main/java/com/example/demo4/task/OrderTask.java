package com.example.demo4.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo4.service.OrderService;

@Component
public class OrderTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTask.class);

    private final OrderService orderService;

    public OrderTask(OrderService orderService) {
        this.orderService = orderService;
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
        log.info("定时任务：生成 {} 日报表，取消订单 {} 个，总金额 {}",
                report.getReportDate(), report.getCancelledCount(), report.getTotalAmount());
    }
}
