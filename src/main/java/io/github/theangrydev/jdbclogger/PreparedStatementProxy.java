package io.github.theangrydev.jdbclogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PreparedStatementProxy implements InvocationHandler {

    private final PreparedStatement target;

    private PreparedStatementProxy(PreparedStatement target) {
        this.target = target;
    }

    public static PreparedStatement proxy(PreparedStatement target) {
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(), new Class<?>[]{PreparedStatement.class}, new PreparedStatementProxy(target));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = method.invoke(target, args);
        if (result instanceof ResultSet) {
            return ResultSetProxy.proxy((ResultSet) result);
        }
        return result;
    }
}
