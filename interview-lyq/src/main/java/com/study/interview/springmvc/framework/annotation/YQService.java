package com.study.interview.springmvc.framework.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@YQComponent
public @interface YQService {
	String value() default "";
}
