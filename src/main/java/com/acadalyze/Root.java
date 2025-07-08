package com.acadalyze;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 *
 * @author Ralph Gervacio
 */
@Controller
public class Root {

    @GetMapping("/")
    public String redirectToLogin() {
        return "redirect:/auth/login";
    }
}
