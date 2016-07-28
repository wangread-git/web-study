package com.wangread.webstudy.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

/**
 * Created by bjyfwangrui on 2016/7/27.
 * <p>
 * upload file demo
 */
@Controller
@RequestMapping("/upload")
public class UploadController {

    @RequestMapping("/index")
    public String index() {
        return "/upload/index";
    }

    @ResponseBody
    @RequestMapping(value = "/doUpload")
    public String upload(@RequestParam("file") MultipartFile file) {
        return file.getOriginalFilename();
    }
}
