package com.example.mytool.exception;

public class ClusterNameExistedException extends Exception {

    String clusterName;

    public ClusterNameExistedException(String clusterName, String message) {
        super(message);
        this.clusterName = clusterName;
    }
}
