package com.study.interview.springjdbc;

import com.alibaba.fastjson.JSON;
import com.study.interview.springjdbc.demo.entity.Member;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-15 10:41
 */
public class JdbcTest2 {

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.jdbc.Driver");

            Connection conn = DriverManager.getConnection("jdbc:mysql://192.168.8.245:3306/sp2p_qhjrl", "root", "a123321");
            PreparedStatement ps = conn.prepareStatement("select * from t_member;");

            ResultSet rs = ps.executeQuery();
            int count = rs.getMetaData().getColumnCount();
            Class clazz = Member.class;
            List<Object> result = new ArrayList<Object>();
            while (rs.next()) {
                Object obj = clazz.newInstance();
                for (int i = 1; i <= count; i++) {
                    String columnName = rs.getMetaData().getColumnName(i);
                    Field field = clazz.getDeclaredField(columnName);
                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();
                    if (fieldType == String.class) {
                        field.set(obj, rs.getString(i));
                    } else if (fieldType == Long.class) {
                        field.set(obj, rs.getLong(i));
                    }
                }
                result.add(obj);
            }

            System.out.println("JSON.toJSONString(result) = " + JSON.toJSONString(result));
            
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }


    }

}
