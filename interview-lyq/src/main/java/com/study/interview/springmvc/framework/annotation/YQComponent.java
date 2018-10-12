package com.study.interview.springmvc.framework.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface YQComponent {
	 String value() default "";
}
