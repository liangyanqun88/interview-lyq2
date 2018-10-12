package com.study.interview.proxy.cglib;


import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-12 10:12
 */
public class YQCGLibProxy implements MethodInterceptor {

    public Object getInstance(Class clazz){
        Enhancer enhancer = new Enhancer();
        enhancer.setCallback(this);
        enhancer.setSuperclass(clazz);
        return enhancer.create();
    }


    @Override
    public Object intercept(Object target, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        System.out.println("target = ");
        Object r = methodProxy.invokeSuper(target, args);
        System.out.println("r = " + r.toString());
        return r;
    }
}
