package com.study.interview.springmvc.framework.annotation;

import java.lang.annotation.*;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-09 9:26
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface YQAutowired {
    String value() default "";
    boolean required() default true;
}
