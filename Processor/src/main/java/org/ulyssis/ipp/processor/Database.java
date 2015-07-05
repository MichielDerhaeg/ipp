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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

public final class Database {
    @SuppressWarnings("unused")
    private Database() {}

    public enum ConnectionFlags {
        READ_WRITE,
        READ_ONLY
    }

    public static Connection createConnection(EnumSet<ConnectionFlags> flags) throws SQLException {
        assert flags.contains(ConnectionFlags.READ_ONLY) != flags.contains(ConnectionFlags.READ_WRITE);
        boolean readOnly = flags.contains(ConnectionFlags.READ_ONLY);
//        String url = "jdbc:postgresql://ipptest.local/ipp"; // TODO: Make URL configurable
        String url = "jdbc:h2:mem:ipp";
        Properties props = new Properties();
        props.setProperty("user", "ipp");
        props.setProperty("password", "ipp");
        props.setProperty("readOnly", readOnly ? "true" : "false");
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url);
//            connection = DriverManager.getConnection(url, props);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            return connection;
        } catch (Exception e) {
            if (connection != null) connection.rollback();
            throw e;
        }
    }

    public static void initDb(Connection connection) throws SQLException {
        List<String> statements = Arrays.asList(
                "DROP TABLE IF EXISTS \"events\" CASCADE",
                "DROP TABLE IF EXISTS \"tagSeenEvents\" CASCADE",
                "DROP TABLE IF EXISTS \"snapshots\" CASCADE",
                "CREATE TABLE \"events\" (" +
                    "\"id\" bigserial PRIMARY KEY NOT NULL," +
                    "\"type\" VARCHAR(255) NOT NULL," +
                    "\"time\" timestamp NOT NULL," +
                    "\"data\" text NOT NULL," +
                    "\"removed\" boolean NOT NULL" +
                ")",
                "CREATE UNIQUE INDEX ON \"events\" (\"time\" DESC, \"id\" DESC)",
                "CREATE INDEX ON \"events\" (\"type\", \"removed\")",
                "CREATE TABLE \"tagSeenEvents\" (" +
                    "\"id\" bigint PRIMARY KEY NOT NULL," +
                    "\"readerId\" integer NOT NULL," +
                    "\"updateCount\" bigint NOT NULL," +
                    "FOREIGN KEY (\"id\") REFERENCES \"events\" (\"id\")" +
                ")",
                "CREATE UNIQUE INDEX ON \"tagSeenEvents\" (\"readerId\", \"updateCount\" DESC)",
                "CREATE TABLE \"snapshots\" (" +
                    "\"id\" bigserial PRIMARY KEY NOT NULL," +
                    "\"time\" timestamp NOT NULL," +
                    "\"data\" text NOT NULL," +
                    "\"event\" bigint NOT NULL," +
                    "FOREIGN KEY (\"event\") REFERENCES \"events\" (\"id\")" +
                ")",
                "CREATE INDEX ON \"snapshots\" (\"time\" DESC)"
        );
        for (String statement : statements) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(statement);
            }
        }
    }
}
