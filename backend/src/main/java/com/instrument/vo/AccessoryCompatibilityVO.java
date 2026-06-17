package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class AccessoryCompatibilityVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean compatible;

    private List<String> warnings = new ArrayList<>();

    private List<String> errors = new ArrayList<>();

    private String summary;

    private String suggestion;

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public boolean hasIssues() {
        return !warnings.isEmpty() || !errors.isEmpty();
    }
}
