package com.study.interview.proxy.cglib;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-12 10:26
 */
public class PersonImpl {

    public String sayHello(String name) {
        System.out.println("PersonImpl.name2 = " + name);
        return name;
    }
}
