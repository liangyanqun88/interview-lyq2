package com.study.interview.springmvc.framework.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@YQMapping
public @interface YQRequestMapping {
	String value() default "";
}
