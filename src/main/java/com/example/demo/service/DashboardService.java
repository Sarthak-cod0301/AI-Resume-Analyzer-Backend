// service/DashboardService.java
package com.example.demo.service;

import com.example.demo.dto.DashboardSummaryDTO;

public interface DashboardService {
    DashboardSummaryDTO getDashboard(String userId);
}