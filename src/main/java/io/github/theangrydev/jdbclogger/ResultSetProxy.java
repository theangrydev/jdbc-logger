package io.github.theangrydev.jdbclogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

import static java.lang.String.format;
import static java.util.Arrays.stream;

public class ResultSetProxy implements InvocationHandler {

    private final Map<String, Integer> columnNameToIndex;
    private final ResultSet target;
    private final int columnCount;

    private int resultPointer;
    private boolean closed;
    private Object[] currentResult;
    private final List<Object[]> allResults = new ArrayList<>();

    private ResultSetProxy(Map<String, Integer> columnNameToIndex, ResultSet target, int columnCount) throws SQLException {
        this.columnNameToIndex = columnNameToIndex;
        this.target = target;
        this.columnCount = columnCount;
    }

    public static ResultSet proxy(ResultSet target) throws SQLException {
        ResultSetMetaData metaData = target.getMetaData();
        int columnCount = metaData.getColumnCount();
        ResultSetProxy resultSetProxy = new ResultSetProxy(columnNameToIndex(metaData), target, columnCount);
        return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class}, resultSetProxy);
    }

    private static Map<String, Integer> columnNameToIndex(ResultSetMetaData metaData) throws SQLException {
        Map<String, Integer> columnNameToIndex = new HashMap<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            columnNameToIndex.put(metaData.getColumnLabel(i).toUpperCase(), i);
        }
        return columnNameToIndex;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (isGetMetaDataMethod(method) || isCloseMethod(method)) {
            return method.invoke(target, args);
        }
        if (closed) {
            if (isGetMethod(method) && resultPointer >= allResults.size()) {
                throw new SQLException(format("Result set exhausted. There were %d result(s) only", allResults.size()));
            }
            if (isGetMethod(method) && resultPointer < allResults.size()) {
                int columnIndex = determineColumnIndex(method, args);
                return currentResult[columnIndex];
            }
            if (isNextMethod(method) && resultPointer < allResults.size()) {
                currentResult = allResults.get(resultPointer);
                resultPointer++;
                return true;
            }
            if (isNextMethod(method) && resultPointer == allResults.size()) {
                return false;
            }
        } else {
            if (isGetMethod(method)) {
                int columnIndex = determineColumnIndex(method, args);
                Object result = method.invoke(target, args);
                currentResult[columnIndex] = result;
                return result;
            }
            if (isNextMethod(method)) {
                Object result = method.invoke(target, args);
                currentResult = new Object[columnCount + 1];
                allResults.add(currentResult);
                return result;
            }
            if (isBeforeFirstMethod(method)) {
                resultPointer = 0;
                closed = true;
                return null;
            }
        }
        throw new UnsupportedOperationException(format("Method '%s' is not supported by this proxy", method));
    }

    private boolean isCloseMethod(Method method) {
        return methodNameIs(method);
    }

    private boolean methodNameIs(Method method) {
        return method.getName().equals("close");
    }

    private boolean isGetMetaDataMethod(Method method) {
        return method.getName().equals("getMetaData");
    }

    private boolean isGetMethod(Method method) {
        return method.getName().startsWith("get") && !isGetMetaDataMethod(method);
    }

    private boolean isNextMethod(Method method) {
        return method.getName().equals("next");
    }

    private boolean isBeforeFirstMethod(Method method) {
        return method.getName().equals("beforeFirst");
    }

    private int determineColumnIndex(Method method, Object[] args) {
        Optional<Integer> columnIndex = columnIndexParameter(args);
        if (columnIndex.isPresent()) {
            return columnIndex.get();
        }
        Optional<String> columnName = columnNameParameter(args);
        if (columnName.isPresent()) {
            return columnNameToIndex(columnName.get());
        }
        throw new IllegalStateException(format("Could not determine column index for method '%s'", method));
    }

    private Integer columnNameToIndex(String s) {
        return columnNameToIndex.get(s.toUpperCase());
    }

    // assumed to be the first int parameter
    private Optional<Integer> columnIndexParameter(Object[] args) {
        return stream(args)
                .filter(arg -> arg.getClass() == Integer.class)
                .map(Integer.class::cast)
                .findFirst();
    }

    // assumed to be the first String parameter
    private Optional<String> columnNameParameter(Object[] args) {
        return stream(args)
                .filter(arg -> arg.getClass() == String.class)
                .map(String.class::cast)
                .findFirst();
    }
}
