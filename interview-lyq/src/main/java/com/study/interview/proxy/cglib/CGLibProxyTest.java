package com.study.interview.proxy.cglib;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-12 9:54
 */
public class CGLibProxyTest {

    public static void main(String[] args) {
        YQCGLibProxy proxy = new YQCGLibProxy();
        PersonImpl person = (PersonImpl)proxy.getInstance(PersonImpl.class);
        String result2 = person.sayHello("dddddddddd");
        System.out.println("result2 = " + result2);
    }

}
