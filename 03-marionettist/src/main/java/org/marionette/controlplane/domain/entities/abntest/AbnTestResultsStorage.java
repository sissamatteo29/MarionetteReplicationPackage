package org.marionette.controlplane.domain.entities.abntest;

public class AbnTestResultsStorage {    // Single result storage -> for every new test, remove preceding one!

    private SingleAbnTestResult testResults;

    public AbnTestResultsStorage() {

    }

    public AbnTestResultsStorage(SingleAbnTestResult testResults) {
        this.testResults = testResults;
    }

    public SingleAbnTestResult getResults() {
        return testResults;
    }

    public void putResults(SingleAbnTestResult result) {
        this.testResults = result;     
    }

}
