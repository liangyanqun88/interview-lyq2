package com.study.interview.springmvc.framework.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-08 15:41
 */
public class YQHandlerAdapter {

    private Map<String,Integer> paramMapping;

    public YQHandlerAdapter(Map<String,Integer> paramMapping){
        this.paramMapping = paramMapping;
    }

    YQModelAndView handle(HttpServletRequest request, HttpServletResponse response, YQHandler handler) throws Exception {
        //为什么要传req、为什么要穿resp、为什么传handler
        Class<?>[] parameterTypes = handler.method.getParameterTypes();

        //要想给参数赋值，只能通过索引号来找到具体的某个参数
        Object[] parameterValue = new Object[parameterTypes.length];

        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
            String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
            if (!paramMapping.containsKey(param.getKey())) {
                continue;
            }
            Integer index = paramMapping.get(param.getKey());

            parameterValue[index] = castStringValue(value, parameterTypes[index]);
        }

       String requestName = HttpServletRequest.class.getName();
        if (paramMapping.containsKey(requestName)){
           Integer index = paramMapping.get(requestName);
           parameterValue[index] = request;
        }

        String responseName = HttpServletResponse.class.getName();
        if (paramMapping.containsKey(responseName)){
            Integer index = paramMapping.get(responseName);
            parameterValue[index] = response;
        }

        boolean isYQModelAndView =  handler.method.getReturnType() == YQModelAndView.class;
        Object result = handler.method.invoke(handler.controller, parameterValue);
        if(isYQModelAndView){
            return (YQModelAndView)result;
        }

        return null;
    }

    private Object castStringValue(String value, Class<?> clazz) {
        if (clazz == String.class){
           return value;
        }else if(clazz == Integer.class){
            return Integer.valueOf(value);
        }else if(clazz == int.class){
            return Integer.valueOf(value).intValue();
        }

        return null;
    }

}
