package com.study.interview.proxy.custom;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-12 14:00
 */
public class CustomJDKProxyTest {

    public static void main(String[] args) {

        CustomJDKProxy proxy = new CustomJDKProxy();
        IPerson person = (IPerson) proxy.getInstance(new PersonImpl());
        person.sayHello();
    }
}
