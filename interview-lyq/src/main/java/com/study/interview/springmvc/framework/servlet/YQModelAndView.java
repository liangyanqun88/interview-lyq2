package com.study.interview.springmvc.framework.servlet;

import java.util.Map;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-08 15:47
 */
public class YQModelAndView {

    private Map<String, Object> model;
    private String name;

    public YQModelAndView(String name, Map<String, Object> model) {
        this.model = model;
        this.name = name;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    public String getName() {
        return name;
    }
}
