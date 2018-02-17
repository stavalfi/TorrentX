package com.utils;

public enum ErrorSignalType {
    IOException(java.io.IOException.class),
    UnknownHostException(java.net.UnknownHostException.class),
    SecurityException(java.lang.SecurityException.class),
    BadResponseException(main.tracker.BadResponseException.class),
    SocketException(java.net.SocketException.class),
    EOFException(java.io.EOFException.class);

    final Class<? extends Throwable> errorSignal;

    ErrorSignalType(Class<? extends Throwable> errorSignal) {
        this.errorSignal = errorSignal;
    }

    public Class<? extends Throwable> getErrorSignal() {
        return this.errorSignal;
    }
}
