package io.github.theangrydev.jdbclogger;

import io.github.tjheslin1.tl.ColumnWidthCalculator;
import io.github.tjheslin1.tl.TableFormatter;
import io.github.tjheslin1.tl.TableLogger;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.h2.jdbcx.JdbcDataSource;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;

public class Main {

    public static void main(String[] args) throws SQLException, InterruptedException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test");
        ProxyDataSource proxyDataSource = ProxyDataSourceBuilder.create(dataSource)
                .listener(new LoggingExecutionListener(dataSource))
                .build();
        exampleUsage(proxyDataSource);
    }

    private static void exampleUsage(ProxyDataSource proxyDataSource) throws SQLException {
        Connection connection = proxyDataSource.getConnection();
        connection.createStatement().execute("CREATE TABLE test(a INT, b INT)");
        connection.prepareStatement("INSERT INTO test(a, b) VALUES(1,2)").executeUpdate();
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * from test WHERE a = ? AND a = ?");
        preparedStatement.setInt(1, 1);
        preparedStatement.setInt(2, 1);
        ResultSet resultSet = preparedStatement.executeQuery();

        resultSet.next();
        int anInt = resultSet.getInt(1);
        int bnInt = resultSet.getInt(2);
        resultSet.close();
        System.out.println("anInt = " + anInt);
        System.out.println("bnInt = " + bnInt);
    }

    private static class LoggingExecutionListener implements QueryExecutionListener {
        private final JdbcDataSource dataSource;

        public LoggingExecutionListener(JdbcDataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
            System.out.println(format("Request from application to database:%n%s%n", reconstructQuery(queryInfoList)));
        }

        @Override
        public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
            if (execInfo.getMethod().getName().equals("executeQuery")) {
                String query = reconstructQuery(queryInfoList);
                String result = executeQueryAgain(query);
                System.out.println(format("Response from database to application:%n%s%n", result));
            } else {
                String result = execInfo.isSuccess() ? "OK" : "FAIL";
                System.out.println(format("Response from database to application:%n%s%n", result));
            }
        }

        private String executeQueryAgain(String query) {
            try {
                Connection connection = dataSource.getConnection();
                ResultSet resultSet = connection.prepareStatement(query).executeQuery();
                return extractTable(resultSet);
            } catch (SQLException e) {
                throw new IllegalStateException("Problem with query: " + query, e);
            }
        }

        private String extractTable(ResultSet resultSet) throws SQLException {
            AtomicReference<String> table = new AtomicReference<>();
            TableLogger tableLogger = new TableLogger(new TableFormatter(new ColumnWidthCalculator()), table::set);

            extractColumns(resultSet, tableLogger);
            while (resultSet.next()) {
                extractRow(resultSet, tableLogger);
            }
            tableLogger.print();
            return table.get();
        }

        private ResultSetMetaData extractColumns(ResultSet resultSet, TableLogger tableLogger) throws SQLException {
            ResultSetMetaData metaData = resultSet.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                tableLogger.withColumn(metaData.getColumnName(i));
            }
            return metaData;
        }

        private void extractRow(ResultSet resultSet, TableLogger tableLogger) throws SQLException {
            ResultSetMetaData metaData = resultSet.getMetaData();
            String[] values = new String[metaData.getColumnCount()];
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                values[i - 1] = String.valueOf(resultSet.getObject(i));
            }
            tableLogger.addRow(values);
        }

        private static String reconstructQuery(List<QueryInfo> queryInfoList) {
            QueryInfo queryInfo = queryInfoList.get(0);
            String query = queryInfo.getQuery();
            if (queryInfo.getQueryArgsList().isEmpty()) {
                return query;
            }
            Map<String, Object> queryArgsList = queryInfo.getQueryArgsList().get(0);
            for (int i = 1; i <= queryArgsList.size(); i++) {
                query = query.replaceFirst("\\?", queryArgsList.get(String.valueOf(i)).toString());
            }
            return query;
        }
    }
}
