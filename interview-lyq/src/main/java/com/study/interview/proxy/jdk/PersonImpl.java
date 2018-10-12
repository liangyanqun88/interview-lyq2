package com.study.interview.proxy.jdk;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-12 10:04
 */
public class PersonImpl implements IPerson{
    @Override
    public String sayHello(String name) {
        System.out.println("name = " + name);
        return name;
    }
}
