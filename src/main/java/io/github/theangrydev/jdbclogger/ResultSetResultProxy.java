package io.github.theangrydev.jdbclogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ResultSetResultProxy<T> implements InvocationHandler {

    private final T target;

    private ResultSetResultProxy(T target) {
        this.target = target;
    }

    public static <T> T resultSetResultProxy(T target, Class<T> interfaceToProxy) {
        return interfaceToProxy.cast(Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(), new Class<?>[]{PreparedStatement.class}, new ResultSetResultProxy<>(target)));
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
