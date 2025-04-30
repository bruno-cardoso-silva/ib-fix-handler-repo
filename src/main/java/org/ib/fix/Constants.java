package org.ib.fix;

public enum Constants {
    ALL_MESSAGES_FILE_NAME("AllMsgs.csv"),
    FULL_FILL_REPORT_FILE_NAME("FullFill.txt"),
    EXECUTION_REPORT_FILE_NAME("FinalReport.csv"),
    FIX_MESSAGES_FILE_PATH("fix_messages.txt");

    private final String name;

    Constants(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
