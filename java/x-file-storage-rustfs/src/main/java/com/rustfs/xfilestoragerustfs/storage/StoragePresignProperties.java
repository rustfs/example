package com.rustfs.xfilestoragerustfs.storage;

public class StoragePresignProperties {

    private long expireSeconds = 900;

    private long maxExpireSeconds = 604800;

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public long getMaxExpireSeconds() {
        return maxExpireSeconds;
    }

    public void setMaxExpireSeconds(long maxExpireSeconds) {
        this.maxExpireSeconds = maxExpireSeconds;
    }
}

