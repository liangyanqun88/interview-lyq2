package com.study.interview.proxy.jdk;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-12 9:52
 */
public class JDKProxyTest {

    public static void main(String[] args) {
        YQJDKProxy proxy = new YQJDKProxy();
        IPerson person = (IPerson) proxy.getInstance(new PersonImpl());
        String result = person.sayHello("liangyanqun");
        System.out.println("result = " + result);
    }
}
