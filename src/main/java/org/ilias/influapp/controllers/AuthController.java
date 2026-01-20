package org.ilias.influapp.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.RegisterRequest;
import org.ilias.influapp.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                 BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registerRequest", bindingResult);
            redirectAttributes.addFlashAttribute("registerRequest", request);
            return "redirect:/register";
        }

        try {
            userService.register(request);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("registerError", ex.getMessage());
            redirectAttributes.addFlashAttribute("registerRequest", request);
            return "redirect:/register";
        }
        return "redirect:/login?registered";
    }
}
