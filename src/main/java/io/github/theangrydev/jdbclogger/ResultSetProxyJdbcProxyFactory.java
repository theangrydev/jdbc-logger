package io.github.theangrydev.jdbclogger;

import net.ttddyy.dsproxy.proxy.InterceptorHolder;
import net.ttddyy.dsproxy.proxy.ProxyJdbcObject;
import net.ttddyy.dsproxy.proxy.jdk.JdkJdbcProxyFactory;
import net.ttddyy.dsproxy.proxy.jdk.StatementInvocationHandler;

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static io.github.theangrydev.jdbclogger.ResultSetResultProxy.resultSetResultProxy;

class ResultSetProxyJdbcProxyFactory extends JdkJdbcProxyFactory {

    @Override
    public Statement createStatement(Statement statement, InterceptorHolder interceptorHolder) {
        return super.createStatement(resultSetResultProxy(statement, Statement.class), interceptorHolder);
    }

    @Override
    public Statement createStatement(Statement statement, InterceptorHolder interceptorHolder, String dataSourceName) {
        return super.createStatement(resultSetResultProxy(statement, Statement.class), interceptorHolder, dataSourceName);
    }

    @Override
    public PreparedStatement createPreparedStatement(PreparedStatement preparedStatement, String query, InterceptorHolder interceptorHolder) {
        return super.createPreparedStatement(resultSetResultProxy(preparedStatement, PreparedStatement.class), query, interceptorHolder);
    }

    @Override
    public PreparedStatement createPreparedStatement(PreparedStatement preparedStatement, String query, InterceptorHolder interceptorHolder, String dataSourceName) {
        return super.createPreparedStatement(resultSetResultProxy(preparedStatement, PreparedStatement.class), query, interceptorHolder, dataSourceName);
    }

    @Override
    public CallableStatement createCallableStatement(CallableStatement callableStatement, String query, InterceptorHolder interceptorHolder, String dataSourceName) {
        return super.createCallableStatement(resultSetResultProxy(callableStatement, CallableStatement.class), query, interceptorHolder, dataSourceName);
    }
}
