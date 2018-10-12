package com.study.interview.springjdbc.demo.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author liangyanqun
 * @description
 * @date 2018-10-12 15:20
 */
@Entity
@Table(name = "t_member")
public class Member {

    @Id
    private Long id;
    private String name;
    private Long age;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getAge() {
        return age;
    }

    public void setAge(Long age) {
        this.age = age;
    }
}
