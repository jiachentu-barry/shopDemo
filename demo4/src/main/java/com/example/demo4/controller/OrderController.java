package com.example.demo4.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo4.entity.ExpiredOrderReport;
import com.example.demo4.entity.Order;
import com.example.demo4.entity.Users;
import com.example.demo4.service.OrderService;
import com.example.demo4.service.UsersService;
import com.example.demo4.task.OrderTask;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;
    private final UsersService usersService;
    private final OrderTask orderTask;

    public OrderController(OrderService orderService, UsersService usersService, OrderTask orderTask) {
        this.orderService = orderService;
        this.usersService = usersService;
        this.orderTask = orderTask;
    }

    private Users getLoginUser(HttpSession session) {
        return (Users) session.getAttribute("loginUser");
    }

    @PostMapping("/create")
    public Map<String, Object> createOrder(@RequestBody Map<String, List<Long>> body,
                                            HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        List<Long> productIds = body.get("productIds");
        if (productIds == null || productIds.isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择商品");
            return result;
        }
        try {
            Order order = orderService.createOrder(user.getId(), productIds);
            result.put("success", true);
            result.put("message", "下单成功");
            result.put("orderNo", order.getOrderNo());
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/list")
    public Map<String, Object> listOrders(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        List<Order> orders = orderService.getUserOrders(user.getId());
        result.put("success", true);
        result.put("orders", orders);
        return result;
    }

    @GetMapping("/detail")
    public Map<String, Object> orderDetail(@RequestParam String orderNo, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        Order order = orderService.getOrderByNo(orderNo);
        if (order == null || !order.getUserId().equals(user.getId())) {
            result.put("success", false);
            result.put("message", "订单不存在");
            return result;
        }
        result.put("success", true);
        result.put("order", order);
        return result;
    }

    @PostMapping("/pay")
    public Map<String, Object> payOrder(@RequestParam String orderNo, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        try {
            Order order = orderService.payOrder(orderNo);
            if (!order.getUserId().equals(user.getId())) {
                result.put("success", false);
                result.put("message", "无权操作");
                return result;
            }
            result.put("success", true);
            result.put("message", "支付成功");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/all")
    public Map<String, Object> listAllOrders(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            result.put("success", false);
            result.put("message", "无权限操作");
            return result;
        }
        List<Order> orders = orderService.getAllOrders();
        // 附加用户名信息
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> o = new HashMap<>();
            o.put("id", order.getId());
            o.put("orderNo", order.getOrderNo());
            o.put("userId", order.getUserId());
            Users u = usersService.getUserById(order.getUserId());
            o.put("username", u != null ? u.getUsername() : "未知用户");
            o.put("totalAmount", order.getTotalAmount());
            o.put("totalQuantity", order.getTotalQuantity());
            o.put("status", order.getStatus());
            o.put("createTime", order.getCreateTime());
            o.put("payTime", order.getPayTime());
            o.put("items", order.getItems());
            list.add(o);
        }
        result.put("success", true);
        result.put("orders", list);
        return result;
    }

    @PostMapping("/cancel")
    public Map<String, Object> cancelOrder(@RequestParam String orderNo, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        try {
            orderService.cancelOrder(orderNo, user.getId());
            result.put("success", true);
            result.put("message", "订单已取消");
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 查询超时未支付订单报表
    @GetMapping("/expiredReports")
    public Map<String, Object> getExpiredReports(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            result.put("success", false);
            result.put("message", "无权限操作");
            return result;
        }
        result.put("success", true);
        result.put("reports", orderService.getAllExpiredReports());
        return result;
    }

    // 手动生成今日报表（统计昨天的数据）
    @PostMapping("/generateReport")
    public Map<String, Object> generateReport(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            result.put("success", false);
            result.put("message", "无权限操作");
            return result;
        }
        ExpiredOrderReport report = orderService.generateDailyExpiredReport();
        result.put("success", true);
        result.put("message", "报表生成成功");
        result.put("report", report);
        return result;
    }

    // 立即生成今天的超时报表并导出CSV
    @PostMapping("/generateTodayReport")
    public Map<String, Object> generateTodayReport(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = getLoginUser(session);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            result.put("success", false);
            result.put("message", "无权限操作");
            return result;
        }

        ExpiredOrderReport report = orderTask.generateTodayReportNow();
        result.put("success", true);
        result.put("message", "今日超时报表生成成功");
        result.put("report", report);
        return result;
    }

    @GetMapping("/exportCsv")
    public ResponseEntity<?> exportCsv(@RequestParam(required = false) String date, HttpSession session) {
        Users user = getLoginUser(session);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "无权限操作");
            return ResponseEntity.status(403).body(result);
        }

        LocalDate reportDate;
        try {
            reportDate = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "日期格式错误，应为 yyyy-MM-dd");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            Path csvPath = orderTask.generateAndExportReportCsvByDate(reportDate);
            byte[] bytes = Files.readAllBytes(csvPath);
            String fileName = csvPath.getFileName().toString();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                    .body(new ByteArrayResource(bytes));
        } catch (IOException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "读取CSV文件失败");
            return ResponseEntity.internalServerError().body(result);
        } catch (RuntimeException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}
