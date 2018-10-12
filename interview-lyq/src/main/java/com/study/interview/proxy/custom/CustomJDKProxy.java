package com.study.interview.proxy.custom;

import java.lang.reflect.Method;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-12 14:03
 */
public class CustomJDKProxy implements CustomInvokehandler {

    private Object target;

    public Object getInstance(Object target) {
        this.target = target;
        return CustomProxy.newProxyInstance(new CustomClassLoader(), target.getClass().getInterfaces(), this);
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(target, args);
    }
}
