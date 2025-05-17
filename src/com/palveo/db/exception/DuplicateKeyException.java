package com.palveo.db.exception;

import java.sql.SQLException;

public class DuplicateKeyException extends SQLException {

    public DuplicateKeyException(String reason, String sqlState, int vendorCode) {
        super(reason, sqlState, vendorCode);
    }

    public DuplicateKeyException(String reason, String sqlState) {
        super(reason, sqlState);
    }

    public DuplicateKeyException(String reason) {
        super(reason);
    }

    public DuplicateKeyException() {
        super();
    }

    public DuplicateKeyException(Throwable cause) {
        super(cause);
    }

    public DuplicateKeyException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public DuplicateKeyException(String reason, String sqlState, Throwable cause) {
        super(reason, sqlState, cause);
    }

    public DuplicateKeyException(String reason, String sqlState, int vendorCode, Throwable cause) {
        super(reason, sqlState, vendorCode, cause);
    }
}