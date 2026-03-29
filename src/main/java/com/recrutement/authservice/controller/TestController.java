package com.recrutement.authservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test/public")
    public String test() {
        return "Auth service works!";
    }

    @GetMapping("/test/private")
    public String privateEndpoint() {
        return "Private endpoint works";
    }
}