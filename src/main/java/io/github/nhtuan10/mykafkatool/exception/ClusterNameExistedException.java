package io.github.nhtuan10.mykafkatool.exception;

public class ClusterNameExistedException extends Exception {

    private String clusterName;

    public ClusterNameExistedException(String clusterName, String message) {
        super(message);
        this.clusterName = clusterName;
    }
}
