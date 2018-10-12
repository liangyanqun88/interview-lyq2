package com.study.interview.springmvc.framework.context;

import com.study.interview.springmvc.framework.annotation.YQAutowired;
import com.study.interview.springmvc.framework.annotation.YQController;
import com.study.interview.springmvc.framework.annotation.YQService;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-08 15:04
 */
public class YQApplicationContext {

    private Map<String,Object> instanceMap = new ConcurrentHashMap<String, Object>();

    private List<String> cacheClass = new ArrayList<String>();

    private Properties config = new Properties();

    public YQApplicationContext(String name){
        //定位
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(name);

        try {
            //载入
            config.load(is);
           //注册
           String scanPackage = config.getProperty("scanPackage");
            doRegister(scanPackage);
            //初始化
            doCreateBean();
            //注入
            populate();
            

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void populate() {
        if (instanceMap.size() == 0) {
            return;
        }
        for (Map.Entry<String, Object> i : instanceMap.entrySet()) {
            Field[] fields = i.getValue().getClass().getDeclaredFields();
            for (Field f : fields) {
                if (f.isAnnotationPresent(YQAutowired.class)) {
                    //查看是否有自定义的属性名
                    YQAutowired autowired = f.getAnnotation(YQAutowired.class);
                    String id = autowired.value().trim();
                    if ("".equals(id)) {
                        id = f.getType().getName();
                    }

                    f.setAccessible(true);
                    try {
                        f.set(i.getValue(), instanceMap.get(id));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                }

            }


        }


    }

    private void doCreateBean() {
        if (cacheClass.size() == 0) {
            return;
        }
        for (String className : cacheClass) {

            try {
                Class<?> clazz = Class.forName(className);
                //将加有YQController、YQService、YQComponent的实例化
                if(clazz.isAnnotationPresent(YQController.class)){
                    String name = clazz.getSimpleName();
                    String id = lowerFirstChar(name);
                    instanceMap.put(id, clazz.newInstance());
                }
                if(clazz.isAnnotationPresent(YQService.class)){

                    //查看是否有自定义的名字
                   YQService service = clazz.getAnnotation(YQService.class);
                   String id = service.value();
                   if(!"".equals(id)){
                       instanceMap.put(id, clazz.newInstance());
                       continue;
                   }

                   //如果这个类是接口
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces) {
                        instanceMap.put(i.getName(), clazz.newInstance());
                    }
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }


    }

    private String lowerFirstChar(String name) {
        char[] arr = name.toCharArray();
        char c = arr[0];
        arr[0] = c+=32;
        return String.valueOf(arr);
    }

    private void doRegister(String packageName) {
        URL url= this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
      File dir = new File(url.getFile());
        for (File file:dir.listFiles()) {
            if(file.isDirectory()){
                doRegister(packageName+"."+file.getName());
            }else{
                cacheClass.add(packageName+"."+file.getName().replace(".class","").trim());
            }
        }
    }


    public YQApplicationContext(){
    }

    public Object getBean(String name){
        return null;
    }


    public Map<String,Object> getAll(){
        return instanceMap;
    }

    public Properties getConfig() {
        return config;
    }
}
