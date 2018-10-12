package com.study.interview.proxy.custom;

import java.lang.reflect.Method;

/**
 * Created by Administrator on 2018/10/12.
 */
public interface CustomInvokehandler {

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable;

}
