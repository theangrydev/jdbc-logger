package io.github.theangrydev.jdbclogger;

import net.ttddyy.dsproxy.proxy.InterceptorHolder;
import net.ttddyy.dsproxy.proxy.jdk.JdkJdbcProxyFactory;

import java.sql.PreparedStatement;

class ResultSetProxyJdbcProxyFactory extends JdkJdbcProxyFactory {

    @Override
    public PreparedStatement createPreparedStatement(PreparedStatement preparedStatement, String query, InterceptorHolder interceptorHolder) {
        return super.createPreparedStatement(PreparedStatementProxy.proxy(preparedStatement), query, interceptorHolder);
    }

    @Override
    public PreparedStatement createPreparedStatement(PreparedStatement preparedStatement, String query, InterceptorHolder interceptorHolder, String dataSourceName) {
        return super.createPreparedStatement(PreparedStatementProxy.proxy(preparedStatement), query, interceptorHolder, dataSourceName);
    }
}
