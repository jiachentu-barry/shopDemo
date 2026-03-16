package com.example.demo4.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo4.entity.ExpiredOrderReport;

public interface ExpiredOrderReportRepository extends JpaRepository<ExpiredOrderReport, Long> {
    List<ExpiredOrderReport> findAllByOrderByReportDateDesc();

    Optional<ExpiredOrderReport> findByReportDate(LocalDate reportDate);
}
