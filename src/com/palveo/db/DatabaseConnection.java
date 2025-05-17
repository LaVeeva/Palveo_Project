package com.palveo.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import com.palveo.config.AppConfig;

public class DatabaseConnection {

    private static Connection connection = null;
    private static boolean isAttemptingInitialize = false;
    private static ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();
    private static ThreadLocal<Integer> transactionDepth = ThreadLocal.withInitial(() -> 0);
    private static ThreadLocal<Boolean> rollbackOnly = ThreadLocal.withInitial(() -> false);


    static {
        initializeBaseConnection();
    }

    private static synchronized void initializeBaseConnection() {
        if (isAttemptingInitialize) {
            return;
        }
        isAttemptingInitialize = true;

        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    isAttemptingInitialize = false;
                    return;
                } else {
                    System.out.println("DB Connection: Base connection was found closed, setting to null for re-initialization.");
                    connection = null;
                }
            } catch (SQLException e) {
                System.err.println("DB Connection: SQLException checking existing base connection: " + e.getMessage());
                connection = null;
            }
        }

        System.out.println("DB Connection: Attempting to initialize new base connection.");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(AppConfig.DB_URL, AppConfig.DB_USER, AppConfig.DB_PASSWORD);

            if (connection != null && !connection.isClosed()) {
                System.out.println("DB Connection: Base connection initialization successful!");
            } else {
                System.err.println("DB Connection: Base connection initialization FAILED (connection is null or closed).");
                connection = null;
            }
        } catch (ClassNotFoundException e) {
            System.err.println("DB Connection: MySQL JDBC Driver not found during base initialize!");
            connection = null;
        } catch (SQLException e) {
            System.err.println("DB Connection: SQLException during new base connection attempt: " + e.getMessage());
            connection = null;
        } finally {
            isAttemptingInitialize = false;
        }
    }

    public static Connection getConnection() {
        Connection localTransConn = transactionConnection.get();
        if (localTransConn != null) {
            try {
                if (!localTransConn.isClosed()) {
                    return localTransConn;
                } else {
                    System.err.println("DB Connection: Found closed transactional connection in thread local. Removing it.");
                    transactionConnection.remove();
                    transactionDepth.set(0);
                    rollbackOnly.set(false);
                }
            } catch (SQLException e) {
                 System.err.println("DB Connection: Error checking transactional connection state. Removing. Error: " + e.getMessage());
                 transactionConnection.remove();
                 transactionDepth.set(0);
                 rollbackOnly.set(false);
            }
        }

        boolean needsInitialize = false;
        if (connection == null) {
            System.out.println("DB Connection: Base connection is null. Needs initialization.");
            needsInitialize = true;
        } else {
            try {
                if (connection.isClosed()) {
                    System.out.println("DB Connection: Base connection is closed. Needs re-initialization.");
                    needsInitialize = true;
                }
            } catch (SQLException e) {
                System.err.println("DB Connection: SQLException checking if base connection is closed. Assuming it needs re-initialization. Error: " + e.getMessage());
                needsInitialize = true;
            }
        }

        if (needsInitialize) {
            initializeBaseConnection();
        }
        return connection;
    }

    public static boolean isConnected() {
        Connection connToCheck = transactionConnection.get();
        if (connToCheck == null) {
            connToCheck = connection;
        }
        if (connToCheck == null) {
            return false;
        }
        try {
            return !connToCheck.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static void closeConnection() {
        System.out.println("DB Connection: closeConnection() called.");
        Connection localTransConn = transactionConnection.get();
        if (localTransConn != null) {
            try {
                if (!localTransConn.isClosed()) {
                     System.out.println("DB Connection: Rolling back and closing active transactional connection from thread local during app shutdown.");
                     localTransConn.rollback();
                     localTransConn.close();
                }
            } catch (SQLException e) {
                 System.err.println("DB Connection: SQLException during app shutdown (transactional conn rollback/close): " + e.getMessage());
            }
            transactionConnection.remove();
            transactionDepth.set(0);
            rollbackOnly.set(false);
        }

        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("DB Connection: Base connection explicitly closed.");
                }
            } catch (SQLException e) {
                System.err.println("DB Connection: SQLException during app shutdown (base conn close): " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    public static void beginTransaction() throws SQLException {
        Connection existingTransaction = transactionConnection.get();
        int depth = transactionDepth.get();

        if (depth == 0 || existingTransaction == null || existingTransaction.isClosed()) {
            System.out.println("DB Connection: Beginning new (outer) transaction for thread " + Thread.currentThread().getName());
            if(existingTransaction != null && !existingTransaction.isClosed()){
                try { existingTransaction.close(); } catch (SQLException e) { System.err.println("DB Connection: Error closing potentially stale connection before new transaction: " + e.getMessage());}
            }
            Connection conn = DriverManager.getConnection(AppConfig.DB_URL, AppConfig.DB_USER, AppConfig.DB_PASSWORD);
            conn.setAutoCommit(false);
            transactionConnection.set(conn);
            transactionDepth.set(1);
            rollbackOnly.set(false);
        } else {
            System.out.println("DB Connection: Incrementing transaction depth for thread " + Thread.currentThread().getName() + ". New depth: " + (depth + 1));
            transactionDepth.set(depth + 1);
        }
    }

    public static void commitTransaction() throws SQLException {
        Connection conn = transactionConnection.get();
        int depth = transactionDepth.get();

        if (conn == null || depth == 0) {
            System.err.println("DB Connection: Attempt to commit transaction when none is active or depth is zero on this thread.");
            transactionDepth.set(0); 
            rollbackOnly.set(false);
            throw new SQLException("No active transaction to commit or depth is zero.");
        }

        transactionDepth.set(depth - 1);

        if (transactionDepth.get() == 0) {
            System.out.println("DB Connection: Committing outer transaction for thread " + Thread.currentThread().getName());
            try {
                if (conn.isClosed()) {
                    throw new SQLException("Cannot commit: Transactional connection is closed.");
                }
                if (conn.getAutoCommit()){
                    throw new SQLException("Cannot commit: Connection is in auto-commit mode.");
                }
                if (rollbackOnly.get()) {
                    System.err.println("DB Connection: Transaction marked for rollback. Rolling back instead of committing.");
                    conn.rollback();
                } else {
                    conn.commit();
                }
            } finally {
                try { if (!conn.isClosed()) conn.close(); } catch (SQLException e) { System.err.println("DB Connection: Error closing connection after commit/outer rollback: " + e.getMessage()); }
                transactionConnection.remove();
                rollbackOnly.set(false);
            }
        } else {
             System.out.println("DB Connection: Decremented transaction depth for thread " + Thread.currentThread().getName() + ". New depth: " + transactionDepth.get() + ". Not committing yet.");
        }
    }

    public static void rollbackTransaction() throws SQLException {
        Connection conn = transactionConnection.get();
        int depth = transactionDepth.get();

        if (conn == null || depth == 0) {
            System.err.println("DB Connection: Attempt to rollback transaction when none is active or depth is zero. Ignoring (or resetting if inconsistent).");
            transactionDepth.set(0);
            rollbackOnly.set(false);
            if (conn != null) {
                 try { if (!conn.isClosed()) conn.close(); } catch (SQLException e) {System.err.println("DB Connection: Error closing potentially open conn on shallow rollback: " + e.getMessage());}
                 transactionConnection.remove();
            }
            return;
        }
        
        System.out.println("DB Connection: Rollback requested for thread " + Thread.currentThread().getName() + ". Marking for rollback.");
        rollbackOnly.set(true); 
        transactionDepth.set(depth - 1);

        if (transactionDepth.get() == 0) {
            System.out.println("DB Connection: Performing outer transaction rollback for thread " + Thread.currentThread().getName());
            try {
                 if (conn.isClosed()) {
                    System.err.println("DB Connection: Attempt to rollback on a closed connection.");
                    return; 
                }
                 if (conn.getAutoCommit()){
                    System.err.println("DB Connection: Attempt to rollback on a connection in auto-commit mode.");
                    return; 
                }
                conn.rollback();
            } finally {
                try { if (!conn.isClosed()) conn.close(); } catch (SQLException e) { System.err.println("DB Connection: Error closing connection after outer rollback: " + e.getMessage());}
                transactionConnection.remove();
                rollbackOnly.set(false);
            }
        } else {
            System.out.println("DB Connection: Decremented transaction depth for thread " + Thread.currentThread().getName() + ". New depth: " + transactionDepth.get() + ". Actual DB rollback deferred to outermost call.");
        }
    }

    public static Connection getTransactionalConnection() {
        return transactionConnection.get();
    }

    public static boolean isTransactionalConnection(Connection conn) {
        return conn != null && conn == transactionConnection.get();
    }
}