package com.study.interview.springmvc.demo.controller;

import com.study.interview.springmvc.demo.service.IHelloService;
import com.study.interview.springmvc.framework.annotation.YQAutowired;
import com.study.interview.springmvc.framework.annotation.YQController;
import com.study.interview.springmvc.framework.annotation.YQRequestMapping;
import com.study.interview.springmvc.framework.annotation.YQRequestParam;
import com.study.interview.springmvc.framework.servlet.YQModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-09 9:15
 */
@YQController
@YQRequestMapping("/test")
public class HelloController {


    @YQAutowired
    private IHelloService helloService;

    @YQRequestMapping("/sayHello.json")
    public YQModelAndView sayHello(@YQRequestParam(value = "name", required = false) String name, @YQRequestParam(value = "addr", required = false) String addr) {
        helloService.sayHello(name);

        Map<String,Object> model = new HashMap<String, Object>();
        model.put("name",name);
        model.put("addr",addr);

        YQModelAndView modelAndView = new YQModelAndView("sayHello.html", model);
        return modelAndView;
    }
}
