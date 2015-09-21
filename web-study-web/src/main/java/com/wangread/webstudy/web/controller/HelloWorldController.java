package com.wangread.webstudy.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * Created by yfwangrui on 2015/8/24.
 *
 * spring controller test
 */
@Controller
@RequestMapping("/hello")
public class HelloWorldController {

    private Map<String, String> helloMap;

    @RequestMapping(value = "/query", method = RequestMethod.GET)
    @ResponseBody
    public Model hello(Model view) {
        view.addAttribute("code", "0");
        view.addAttribute("msg", "hello world!");
        return view;
    }

    public void setHelloMap(Map<String, String> helloMap) {
        this.helloMap = helloMap;
    }
}
