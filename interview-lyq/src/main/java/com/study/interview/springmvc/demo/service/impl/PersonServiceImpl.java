package com.study.interview.springmvc.demo.service.impl;

import com.study.interview.springmvc.demo.service.IPersonService;
import com.study.interview.springmvc.framework.annotation.YQService;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-09 16:19
 */
@YQService
public class PersonServiceImpl implements IPersonService {

    @Override
    public void firstName(String firstName) {
        System.out.println("firstName = " + firstName);
    }
}
