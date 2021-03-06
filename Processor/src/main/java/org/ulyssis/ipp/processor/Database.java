/*
 * Copyright (C) 2014-2015 ULYSSIS VZW
 *
 * This file is part of i++.
 *
 * i++ is free software: you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Affero General Public License
 * as published by the Free Software Foundation. No other versions apply.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */
package org.ulyssis.ipp.processor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.sql.*;
import java.util.*;

public final class Database {
    private static final Logger LOG = LogManager.getLogger(Database.class);

    @SuppressWarnings("unused")
    private Database() {}

    public enum ConnectionFlags {
        READ_WRITE,
        READ_ONLY
    }

    private static URI databaseURI = null;

    public static void setDatabaseURI(URI uri) {
        databaseURI = uri;
    }

    public static Connection createConnection(EnumSet<ConnectionFlags> flags) throws SQLException {
        assert flags.contains(ConnectionFlags.READ_ONLY) != /* XOR */ flags.contains(ConnectionFlags.READ_WRITE);
        assert databaseURI != null;
        boolean readOnly = flags.contains(ConnectionFlags.READ_ONLY);
        Properties props = new Properties();
        props.setProperty("readOnly", readOnly ? "true" : "false");
        Connection connection = null;
        try {
            if (databaseURI.toString().startsWith("jdbc:h2")) {
                Class.forName("org.h2.Driver");
                connection = DriverManager.getConnection(databaseURI.toString());
            } else {
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(databaseURI.toString(), props);
            }
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            return connection;
        } catch (ClassNotFoundException e) {
            LOG.fatal("Couldn't load driver!", e);
            throw new IllegalStateException(e);
        } catch (SQLException e) {
            if (connection != null) connection.rollback();
            throw e;
        }
    }

    public static void clearDb(Connection connection) throws SQLException {
        List<String> statements = Arrays.asList(
                "DROP TABLE IF EXISTS \"events\" CASCADE",
                "DROP TABLE IF EXISTS \"tagSeenEvents\" CASCADE",
                "DROP TABLE IF EXISTS \"snapshots\" CASCADE"
        );
        for (String statement : statements) {
            try (Statement stmt = connection.createStatement()) {
                LOG.debug("Executing statement: {}", statement);
                stmt.execute(statement);
            }
        }
    }

    private static class TableDescription {
        String tableName;
        List<String> createStrings;

        TableDescription(String tableName, List<String> createStrings) {
            this.tableName = tableName;
            this.createStrings = createStrings;
        }
    }

    public static void initDb(Connection connection) throws SQLException {
        List<TableDescription> descriptions = Arrays.asList(
                new TableDescription(
                        "events",
                        Arrays.asList(
                            "CREATE TABLE \"events\" (" +
                                "\"id\" bigserial PRIMARY KEY NOT NULL," +
                                "\"type\" VARCHAR(255) NOT NULL," +
                                "\"time\" timestamp NOT NULL," +
                                "\"data\" text NOT NULL," +
                                "\"removed\" boolean NOT NULL" +
                            ")",
                            "CREATE UNIQUE INDEX ON \"events\" (\"time\" DESC, \"id\" DESC)",
                            "CREATE INDEX ON \"events\" (\"type\", \"removed\")"
                        )
                ),
                new TableDescription(
                        "tagSeenEvents",
                        Arrays.asList(
                            "CREATE TABLE \"tagSeenEvents\" (" +
                                "\"id\" bigint PRIMARY KEY NOT NULL," +
                                "\"readerId\" integer NOT NULL," +
                                "\"updateCount\" bigint NOT NULL," +
                                "FOREIGN KEY (\"id\") REFERENCES \"events\" (\"id\")" +
                            ")",
                            "CREATE UNIQUE INDEX ON \"tagSeenEvents\" (\"readerId\", \"updateCount\" DESC)"
                        )
                ),
                new TableDescription(
                        "snapshots",
                        Arrays.asList(
                            "CREATE TABLE \"snapshots\" (" +
                                "\"id\" bigserial PRIMARY KEY NOT NULL," +
                                "\"time\" timestamp NOT NULL," +
                                "\"data\" text NOT NULL," +
                                "\"event\" bigint NOT NULL," +
                                "FOREIGN KEY (\"event\") REFERENCES \"events\" (\"id\")" +
                            ")",
                            "CREATE INDEX ON \"snapshots\" (\"time\" DESC)"
                        )
                )
        );
        for (TableDescription desc : descriptions) {
            String existsCheck = "SELECT 1 FROM information_schema.tables WHERE table_name = ?";
            try (PreparedStatement stmt = connection.prepareStatement(existsCheck)) {
                stmt.setString(1, desc.tableName);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    // Exists check failed
                    for (String createString : desc.createStrings) {
                        try (Statement createStmt = connection.createStatement()) {
                            LOG.debug("Executing statement: {}", createString);
                            createStmt.execute(createString);
                        }
                    }
                }
            }
        }
    }
}
