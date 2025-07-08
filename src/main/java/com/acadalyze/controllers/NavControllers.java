package com.acadalyze.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 *
 * @author Ralph Gervacio
 */
@Controller
public class NavControllers {



    @GetMapping("/Grades")
    public String getGradesPage() {
        return "pages/grades";
    }

    @GetMapping("/Performance")
    public String getPerformancePage() {
        return "pages/performance";
    }

    @GetMapping("/Advice")
    public String getAdvicePage() {
        return "pages/suggestions";
    }
    
    @GetMapping("/Settings")
    public String getSettingsPage() {
        return "pages/settings";
    }

}
