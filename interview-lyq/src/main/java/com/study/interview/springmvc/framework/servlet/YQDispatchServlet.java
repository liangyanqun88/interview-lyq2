package com.study.interview.springmvc.framework.servlet;

import com.study.interview.springmvc.framework.annotation.YQController;
import com.study.interview.springmvc.framework.annotation.YQRequestMapping;
import com.study.interview.springmvc.framework.annotation.YQRequestParam;
import com.study.interview.springmvc.framework.context.YQApplicationContext;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class YQDispatchServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String CONTEXT_CONFIG_LOCATION = "contextConfigLocation";

    private static List<YQHandler> handerMapping = new ArrayList<YQHandler>();

    private static Map<YQHandler, YQHandlerAdapter> handlerAdapter = new ConcurrentHashMap<YQHandler, YQHandlerAdapter>();

    private static List<YQViewResolver> viewResolvers = new ArrayList<YQViewResolver>();

    //初始化IOC容器
    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("======init=======");
        String value = config.getInitParameter(CONTEXT_CONFIG_LOCATION);
        YQApplicationContext context = new YQApplicationContext(value);

        initLocaleResolver(context);
        initThemeResolver(context);
        initHandlerMappings(context);
        initHandlerAdapters(context);
        initHandlerExceptionResolvers(context);
        initRequestToViewNameTranslator(context);
        initViewResolvers(context);
        initFlashMapManager(context);

    }

    //用于存放url和方法间的关系
    private void initHandlerMappings(YQApplicationContext context) {
        //获得IOC容器
        Map<String, Object> contextAll = context.getAll();
        if (contextAll.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : contextAll.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (clazz.isAnnotationPresent(YQController.class)) {
                if (clazz.isAnnotationPresent(YQRequestMapping.class)) {
                    String url = clazz.getAnnotation(YQRequestMapping.class).value();
                    Method[] methods = clazz.getDeclaredMethods();
                    for (Method method : methods) {
                        if (method.isAnnotationPresent(YQRequestMapping.class)) {
                            String regex = (url + method.getAnnotation(YQRequestMapping.class).value()).replaceAll("/+", "/");
                            Pattern pattern = Pattern.compile(regex);
                            handerMapping.add(new YQHandler(pattern, entry.getValue(), method));
                            System.out.println("regex=" + regex + ",method=" + method.getName());
                        }

                    }

                }

            }
        }
    }

    //用于存储方法和参数间的关系
    private void initHandlerAdapters(YQApplicationContext context) {
        if (handerMapping.isEmpty()) {
            return;
        }

        //参数类型作为key，参数的索引号作为值
        Map<String, Integer> paramMapping = new HashMap<String, Integer>();
        for (YQHandler handler : handerMapping) {
            Class<?>[] parameterTypes = handler.method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];
                if (parameterType == HttpServletRequest.class || parameterType == HttpServletResponse.class) {
                    paramMapping.put(parameterType.getName(), i);
                }
            }

            Annotation[][] annotations = handler.method.getParameterAnnotations();
            for (int i = 0; i < annotations.length; i++) {
                for (Annotation annotation : annotations[i]) {
                    if (annotation instanceof YQRequestParam) {
                        String paramName = ((YQRequestParam) annotation).value();
                        if (!"".equals(paramName.trim())) {
                            paramMapping.put(paramName, i);
                        }
                    }
                }

            }

            handlerAdapter.put(handler, new YQHandlerAdapter(paramMapping));
        }


    }

    private void initHandlerExceptionResolvers(YQApplicationContext context) {
    }

    private void initRequestToViewNameTranslator(YQApplicationContext context) {
    }

    private void initThemeResolver(YQApplicationContext context) {
    }

    private void initFlashMapManager(YQApplicationContext context) {
    }

    private void initViewResolvers(YQApplicationContext context) {
        Properties properties = context.getConfig();
        String templateRoot = properties.getProperty("templateRoot");
        URL dir = this.getClass().getClassLoader().getResource(templateRoot);
        File file = new File(dir.getFile());
        for (File f : file.listFiles()) {
            viewResolvers.add(new YQViewResolver(f.getName(), f));
        }
    }

    private void initLocaleResolver(YQApplicationContext context) {
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("======doGet=======");
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("======doPost=======");
        try {
            this.doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        YQHandler handler = null;
        try {
            handler = getHandler(req);

            YQHandlerAdapter ha = getHandlerAdapter(handler);
            YQModelAndView mv = ha.handle(req, resp, handler);

            applyDefaultViewName(resp, mv);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void applyDefaultViewName(HttpServletResponse response, YQModelAndView mv) {
        if (mv == null) {
            return;
        }
        if (viewResolvers.isEmpty()) {
            return;
        }
        for (YQViewResolver viewResolver : viewResolvers) {
            if (viewResolver.getName().equals(mv.getName())) {
                try {
                  String r =  viewResolver.parse(mv);

                    if(r != null){
                        response.getWriter().write(r);
                        break;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private YQHandlerAdapter getHandlerAdapter(YQHandler handler) {
        if (handlerAdapter.isEmpty()) {
            return null;
        }
        return handlerAdapter.get(handler);
    }

    private YQHandler getHandler(HttpServletRequest req) {
        String contextPath = req.getContextPath();
        String requestURI = req.getRequestURI();
        String url = requestURI.replaceAll(contextPath, "");
        for (YQHandler handler : handerMapping) {
            Matcher matcher = handler.pattern.matcher(url);
            if (matcher.matches()) {
                return handler;
            }

        }
        return null;
    }


}
