package com.iscm.iam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User statistics response for admin dashboard")
public class UserStatisticsResponse {

    @Schema(description = "Total number of users", example = "150")
    private long totalUsers;

    @Schema(description = "Number of active users", example = "145")
    private long activeUsers;

    @Schema(description = "Number of inactive users", example = "5")
    private long inactiveUsers;

    @Schema(description = "Number of locked users", example = "2")
    private long lockedUsers;

    @Schema(description = "Number of users with MFA enabled", example = "120")
    private long mfaEnabledUsers;

    @Schema(description = "Number of users registered today", example = "3")
    private long registeredToday;

    @Schema(description = "Number of users registered this week", example = "15")
    private long registeredThisWeek;

    @Schema(description = "Number of users registered this month", example = "45")
    private long registeredThisMonth;

    @Schema(description = "Number of super admin users", example = "2")
    private long superAdminCount;

    @Schema(description = "Number of admin users", example = "5")
    private long adminCount;

    @Schema(description = "Number of regular users", example = "143")
    private long regularUserCount;
}