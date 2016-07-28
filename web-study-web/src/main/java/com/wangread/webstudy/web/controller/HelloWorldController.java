package com.wangread.webstudy.web.controller;

import com.wangread.webstudy.domain.mvc.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yfwangrui on 2015/8/24.
 * <p>
 * spring controller test
 */
@Controller
@RequestMapping
public class HelloWorldController {

    @ResponseBody
    @RequestMapping(value = "/hello", produces = "application/json;charset=utf-8")
    public Map<String, Object> hello(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", "0");
        map.put("msg", "hello world, " + user.getName() + "!");
        return map;
    }
}
