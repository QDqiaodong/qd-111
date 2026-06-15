package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class GroupHealthScoreVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long groupId;
    private String groupName;
    private int score;
    private String level;
    private String color;
    private int totalCount;
    private int severeCount;
    private int brokenCount;
    private int overdueCount;
    private int recentReplacementCount;
    private List<ScoreDetailItem> details;

    @Data
    public static class ScoreDetailItem implements Serializable {

        private static final long serialVersionUID = 1L;

        private String factor;
        private String label;
        private int count;
        private int deduction;
        private int maxDeduction;
    }
}
