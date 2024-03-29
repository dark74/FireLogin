package com.dk.firelogin.bean;

import java.util.HashMap;
import java.util.Map;

/**
 * author : root
 * date : 19-10-10 下午5:31
 * description :
 */
public class User {
    private String name;
    private int id;
    private int sex;

    public User(String name, int id, int sex) {
        this.name = name;
        this.id = id;
        this.sex = sex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    public static Map<String, Object> format(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.id);
        map.put("name", user.name);
        map.put("sex", user.sex);
        return map;
    }
}
