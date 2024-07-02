package com.android.server.location.gnss.gnssSelfRecovery;

/* loaded from: classes.dex */
public class DiagnoticResult {
    private String diagnosticName;
    private double performance;
    private boolean result;

    public DiagnoticResult() {
    }

    public DiagnoticResult(String diagnosticName, boolean result) {
        this.diagnosticName = diagnosticName;
        this.result = result;
    }

    public DiagnoticResult(String diagnosticName, boolean result, double performance) {
        this.diagnosticName = diagnosticName;
        this.result = result;
        this.performance = performance;
    }

    public String getDiagnosticName() {
        return this.diagnosticName;
    }

    public void setDiagnosticName(String diagnosticName) {
        this.diagnosticName = diagnosticName;
    }

    public boolean getResult() {
        return this.result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public double getPerformance() {
        return this.performance;
    }

    public void setPerformance(double performance) {
        this.performance = performance;
    }

    public String toString() {
        return "DiagnoticResult{diagnosticName='" + this.diagnosticName + "', result=" + this.result + ", performance=" + this.performance + '}';
    }
}
