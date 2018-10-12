package com.study.interview.springmvc.framework.servlet;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.*;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-11 11:14
 */
public class YQViewResolver {

    private String name;
    private File file;

    public YQViewResolver(String name, File file) {
        this.name = name;
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }

    protected String parse(YQModelAndView mv) throws Exception{

        StringBuffer sb = new StringBuffer();

        RandomAccessFile ra = new RandomAccessFile(this.file, "r");

        try{
            //模板框架的语法是非常复杂，但是，原理是一样的
            //无非都是用正则表达式来处理字符串而已
            //就这么简单，不要认为这个模板框架的语法是有多么的高大上
            //来我现在来做一个最接地气的模板，也就是咕泡学院独创的模板语法
            String line = null;
            while(null != (line = ra.readLine())){
                Matcher m = matcher(line);
                while (m.find()) {
                    for (int i = 1; i <= m.groupCount(); i ++) {
                        String paramName = m.group(i);
                        Object paramValue = mv.getModel().get(paramName);
                        if(null == paramValue){ continue; }
                        line = line.replaceAll("@\\{" + paramName + "\\}", paramValue.toString());
                    }
                }

                sb.append(line);
            }
        }finally{
            ra.close();
        }
        return sb.toString();
    }

    private Matcher matcher(String str){
        Pattern pattern = compile("@\\{(.+?)\\}", CASE_INSENSITIVE);
        Matcher m = pattern.matcher(str);
        return m;
    }
}
