package com.example.mytool.ui;

import lombok.Data;

@Data
public class UIProperty {
    public static final String START_OFFSET = "Start Offset";
    public static final String END_OFFSET = "End Offset";
    public static final String NO_MESSAGES = "No Messages";
    public static final String LEADER = "Leader";
    public static final String REPLICA_IN_SYNC = "Replica [In-Sync]";
    public static final String REPLICA_NOT_IN_SYNC = "Replica [Not-In-Sync]";
    private final String name;
    private final String value;
}
