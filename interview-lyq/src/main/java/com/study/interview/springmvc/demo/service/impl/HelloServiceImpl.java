package com.study.interview.springmvc.demo.service.impl;

import com.study.interview.springmvc.demo.service.IHelloService;
import com.study.interview.springmvc.demo.service.IPersonService;
import com.study.interview.springmvc.framework.annotation.YQAutowired;
import com.study.interview.springmvc.framework.annotation.YQService;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-09 9:21
 */
@YQService
public class HelloServiceImpl implements IHelloService {

    @YQAutowired
    private IPersonService personService;

    @Override
    public void sayHello(String name) {

        personService.firstName(name);
        System.out.println("name = " + name);

    }
}
