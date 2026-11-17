package com.hotelmanagement.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Hidden
@Controller
public class SwaggerController {

    @GetMapping("/swagger-ui.html")
    public String swagger() {
        return "redirect:/docs/index.html";
    }
}
