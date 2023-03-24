/*
 * Copyright 2021 Confluent Inc.
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * http://www.confluent.io/confluent-community-license
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.engine.rewrite;

import io.confluent.ksql.parser.DefaultKsqlParser;
import io.confluent.ksql.parser.ParsingException;
import org.antlr.v4.runtime.ParserRuleContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class QueryAnonymizerTest {

  private QueryAnonymizer anon;

  @Before
  public void setUp() {
    anon = new QueryAnonymizer();
  }

  @Test
  public void shouldWorkAsExpectedWhenPassedAParseTreeInsteadOfString() {
    // Given:
    final ParserRuleContext tree =
        DefaultKsqlParser.getParseTree("DESCRIBE my_stream;");

    // Then:
    Assert.assertEquals("DESCRIBE stream1;",
        anon.anonymize(tree));
  }

  @Test
  public void shouldThrowWhenUnparseableStringProvided() {
    // Given:
    final String nonsenseUnparsedQuery = "cat";
    // Then:
    Assert.assertThrows(ParsingException.class, () -> anon.anonymize(nonsenseUnparsedQuery));
  }

  @Test
  public void shouldLeaveListAndPrintStatementsAsTheyAre() {
    Assert.assertEquals("LIST PROPERTIES;", anon.anonymize("LIST PROPERTIES;"));
    Assert.assertEquals("SHOW PROPERTIES;", anon.anonymize("SHOW PROPERTIES;"));
    Assert.assertEquals("LIST TOPICS;", anon.anonymize("LIST TOPICS;"));
    Assert.assertEquals("SHOW ALL TOPICS EXTENDED;",
        anon.anonymize("SHOW ALL TOPICS EXTENDED;"));
    Assert.assertEquals("LIST STREAMS EXTENDED;",
        anon.anonymize("LIST STREAMS EXTENDED;"));
    Assert.assertEquals("LIST FUNCTIONS;", anon.anonymize("LIST FUNCTIONS;"));
    Assert.assertEquals("LIST CONNECTORS;", anon.anonymize("LIST CONNECTORS;"));
    Assert.assertEquals("LIST SOURCE CONNECTORS;",
        anon.anonymize("LIST SOURCE CONNECTORS;"));
    Assert.assertEquals("LIST TYPES;", anon.anonymize("LIST TYPES;"));
    Assert.assertEquals("LIST QUERIES;", anon.anonymize("LIST QUERIES;"));
  }

  @Test
  public void describeStatementsShouldGetAnonymized() {
    Assert.assertEquals("DESCRIBE stream1;",
        anon.anonymize("DESCRIBE my_stream;"));
    Assert.assertEquals("DESCRIBE FUNCTION function;",
        anon.anonymize("DESCRIBE FUNCTION my_function;"));
    Assert.assertEquals("DESCRIBE CONNECTOR connector;",
        anon.anonymize("DESCRIBE CONNECTOR my_connector;"));
  }

  @Test
  public void printStatementsShouldGetAnonymized() {
    Assert.assertEquals("PRINT topic FROM BEGINNING;",
        anon.anonymize("PRINT my_topic FROM BEGINNING;"));
    Assert.assertEquals("PRINT topic INTERVAL '0';",
        anon.anonymize("PRINT my_topic INTERVAL 2;"));
    Assert.assertEquals("PRINT topic LIMIT '0';",
        anon.anonymize("PRINT my_topic LIMIT 3;"));
  }

  @Test
  public void terminateQueryShouldGetAnonymized() {
    Assert.assertEquals("TERMINATE query;",
        anon.anonymize("TERMINATE my_query;"));
    Assert.assertEquals("TERMINATE ALL;",
        anon.anonymize("TERMINATE ALL;"));
  }

  @Test
  public void shouldAnonymizeSetUnsetProperty() {
    Assert.assertEquals("SET 'auto.offset.reset'='[string]';",
        anon.anonymize("SET 'auto.offset.reset'='earliest';"));
    Assert.assertEquals("UNSET 'auto.offset.reset';",
        anon.anonymize("UNSET 'auto.offset.reset';"));
  }

  @Test
  public void shouldAnonymizeSelectStatementCorrectly() {
    Assert.assertEquals("SELECT * FROM source1;",
        anon.anonymize("SELECT * FROM S1;"));
  }

  @Test
  public void shouldAnonymizeExplainStatementCorrectly() {
    Assert.assertEquals("EXPLAIN query;", anon.anonymize("EXPLAIN my_query;"));
    Assert.assertEquals("EXPLAIN SELECT * FROM source1;",
        anon.anonymize("EXPLAIN SELECT * from S1;"));
  }

  @Test
  public void shouldAnonymizeJoinStatementsCorrectly() {
    final String output = anon.anonymize("INSERT INTO OUTPUT SELECT col1, col2, col3"
        + " FROM SOURCE1 S1 JOIN SOURCE2 S2 WITHIN 1 SECOND ON col1.k=col2.k;");

    Assert.assertEquals(
        "INSERT INTO stream1 SELECT column1, column2, column3 FROM column4=null;",
        output);
  }

  @Test
  public void shouldAnonymizeCreateStreamQueryCorrectly() {
    final String output = anon.anonymize(
        "CREATE STREAM my_stream (profileId VARCHAR, latitude DOUBLE, longitude DOUBLE)\n"
            + "WITH (kafka_topic='locations', value_format='json', partitions=1);");

    Assert.assertEquals(
        "CREATE STREAM stream1 (column1 VARCHAR, column2 DOUBLE, column3 DOUBLE) "
            + "WITH (KAFKA_TOPIC='[string]', VALUE_FORMAT='[string]', PARTITIONS='0');",
        output);
  }

  @Test
  public void shouldAnonymizeCreateStreamAsQueryCorrectly() {
    final String output = anon.anonymize(
        "CREATE STREAM my_stream AS SELECT user_id, browser_cookie, ip_address\n"
            + "FROM another_stream\n"
            + "WHERE user_id = 4214\n"
            + "AND browser_cookie = 'aefde34ec'\n"
            + "AND ip_address = '10.10.0.2';");

    Assert.assertEquals(
        "CREATE STREAM stream1 AS SELECT column1, column2, column3 "
            + "FROM source1 WHERE column1='0' AND column2='[string]' AND column3='[string]';",
        output
    );
  }

  @Test
  public void shouldAnonymizeCreateTableCorrectly() {
    final String output = anon.anonymize(
        "CREATE TABLE my_table (profileId VARCHAR, latitude DOUBLE, longitude DOUBLE)\n"
            + "WITH (kafka_topic='locations', value_format='json', partitions=1);");

    Assert.assertEquals(
        "CREATE TABLE table1 (column1 VARCHAR, column2 DOUBLE, column3 DOUBLE) "
            + "WITH (KAFKA_TOPIC='[string]', VALUE_FORMAT='[string]', PARTITIONS='0');",
        output
    );
  }

  @Test
  public void shouldAnonymizeCreateTableAsQueryCorrectly() {
    final String output = anon.anonymize(
        "CREATE TABLE my_table AS SELECT user_id, browser_cookie, ip_address\n"
            + "FROM another_table\n"
            + "WHERE user_id = 4214\n"
            + "AND browser_cookie = 'aefde34ec'\n"
            + "AND ip_address = '10.10.0.2';");

    Assert.assertEquals(
        "CREATE TABLE table1 AS SELECT column1, column2, column3 FROM source1 "
            + "WHERE column1='0' AND column2='[string]' AND column3='[string]';",
        output
    );
  }

  @Test
  public void shouldAnonymizeCreateConnectorCorrectly() {
    final String output = anon.anonymize(
        "CREATE SOURCE CONNECTOR `jdbc-connector` WITH(\n"
            + "\"connector.class\"='io.confluent.connect.jdbc.JdbcSourceConnector',\n"
            + "\"connection.url\"='jdbc:postgresql://localhost:5432/my.db',\n"
            + "\"mode\"='bulk',\n"
            + "\"topic.prefix\"='jdbc-',\n"
            + "\"table.whitelist\"='users',\n"
            + "\"key\"='username');");

    Assert.assertEquals(
        "CREATE SOURCE CONNECTOR connector WITH (connector.class='[string]', "
            + "connection.url='[string]', mode='[string]', topic.prefix='[string]', "
            + "table.whitelist='[string]', key='[string]');",
        output
    );
  }

  @Test
  public void shouldAnonymizeCreateTypeCorrectly() {
    // simple statement
    Assert.assertEquals("CREATE TYPE type AS INTEGER;",
        anon.anonymize("CREATE TYPE ADDRESS AS INTEGER;"));

    // more elaborate statement
    final String output = anon.anonymize(
        "CREATE TYPE ADDRESS AS STRUCT<number INTEGER, street VARCHAR, city VARCHAR>;");

    Assert.assertEquals(
        "CREATE TYPE type AS STRUCT<INTEGER, VARCHAR, VARCHAR>;",
        output
    );
  }

  @Test
  public void shouldAnonymizeInsertIntoCorrectly() {
    final String output = anon.anonymize(
        "INSERT INTO my_stream SELECT user_id, browser_cookie, ip_address\n"
            + "FROM another_stream\n"
            + "WHERE user_id = 4214\n"
            + "AND browser_cookie = 'aefde34ec'\n"
            + "AND ip_address = '10.10.0.2';");

    Assert.assertEquals(
        "INSERT INTO stream1 SELECT column1, column2, column3 FROM source1 "
            + "WHERE column1='0' AND column2='[string]' AND column3='[string]';",
        output
    );
  }

  @Test
  public void shouldAnonymizeInsertValuesCorrectly() {
    final String output = anon.anonymize(
        "INSERT INTO foo (ROWTIME, KEY_COL, COL_A) VALUES (1510923225000, 'key', 'A');");

    Assert.assertEquals(
        "INSERT INTO stream1 (column1, column2, column3) "
            + "VALUES ('0' ,'[string]' ,'[string]');",
        output
    );
  }

  @Test
  public void shouldAnonymizeDropStatementsCorrectly() {
    Assert.assertEquals("DROP STREAM IF EXISTS stream1 DELETE TOPIC;",
        anon.anonymize("DROP STREAM IF EXISTS my_stream DELETE TOPIC;"));
    Assert.assertEquals("DROP TABLE IF EXISTS table1 DELETE TOPIC;",
        anon.anonymize("DROP TABLE IF EXISTS my_table DELETE TOPIC;"));
    Assert.assertEquals("DROP CONNECTOR connector;",
        anon.anonymize("DROP CONNECTOR my_connector;"));
    Assert.assertEquals("DROP TYPE type;",
        anon.anonymize("DROP TYPE my_type;"));
  }

  @Test
  public void shouldAnonymizeUDFQueriesCorrectly() {
    final String output = anon.anonymize("CREATE STREAM OUTPUT AS SELECT ID, "
        + "MAX(numbers) AS reduce FROM test;");

    Assert.assertEquals(
        "CREATE STREAM stream1 AS SELECT column1, udf1 FROM source1;",
        output
    );
  }
}