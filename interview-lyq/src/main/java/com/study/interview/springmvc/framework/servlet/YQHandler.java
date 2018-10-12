package com.study.interview.springmvc.framework.servlet;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-10 10:20
 */
public class YQHandler {

    protected Pattern pattern;

    protected Object controller;

    protected Method method;

    protected YQHandler(Pattern pattern,Object controller,Method method){
        this.pattern = pattern;
        this.controller = controller;
        this.method = method;
    }
}
