package com.vatlieu.web;

import com.vatlieu.service.PhoneRegistrationService;
import com.vatlieu.service.PhoneRegistrationService.Challenge;
import com.vatlieu.service.PhoneVerification;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class PhoneRegistrationController {

  private static final String CHALLENGE = "phoneRegistration";
  private final PhoneRegistrationService registrations;
  private final PhoneVerification verification;

  @ModelAttribute("smsEnabled")
  public boolean smsEnabled() {
    return verification.configured();
  }

  @GetMapping
  public String form(Model model) {
    model.addAttribute("consentVersion", PhoneRegistrationService.CONSENT_VERSION);
    if (verification.configured()) {
      try {
        model.addAttribute("captchaSiteKey", verification.captchaSiteKey());
      } catch (IllegalArgumentException e) {
        model.addAttribute("error", e.getMessage());
      }
    }
    return "register";
  }

  @PostMapping
  public String send(
    @RequestParam String phone,
    @RequestParam(name = "g-recaptcha-response", defaultValue = "") String captcha,
    @RequestParam(defaultValue = "0") int consentVersion,
    @RequestParam(defaultValue = "false") boolean acceptedTerms,
    @RequestParam(defaultValue = "false") boolean acceptedPhoneProcessing,
    HttpSession session,
    Model model
  ) {
    try {
      session.setAttribute(
        CHALLENGE,
        registrations.start(phone, captcha, consentVersion, acceptedTerms, acceptedPhoneProcessing)
      );
      return "redirect:/register/verify";
    } catch (IllegalArgumentException e) {
      model.addAttribute("phone", phone);
      model.addAttribute("error", e.getMessage());
      if (consentVersion == PhoneRegistrationService.CONSENT_VERSION) {
        model.addAttribute("acceptedTerms", acceptedTerms);
        model.addAttribute("acceptedPhoneProcessing", acceptedPhoneProcessing);
      }
      return form(model);
    }
  }

  @GetMapping("/verify")
  public String verifyForm(
    @SessionAttribute(name = CHALLENGE, required = false) Challenge challenge,
    Model model
  ) {
    if (challenge == null) return "redirect:/register";
    model.addAttribute("challenge", challenge);
    return "register";
  }

  @PostMapping("/verify")
  public String verify(
    @SessionAttribute(name = CHALLENGE, required = false) Challenge challenge,
    @RequestParam String code,
    @RequestParam String fullName,
    @RequestParam String password,
    HttpSession session,
    Model model
  ) {
    if (challenge == null) return "redirect:/register";
    try {
      registrations.complete(challenge, code, fullName, password);
      session.removeAttribute(CHALLENGE);
      return "redirect:/login?registered";
    } catch (IllegalArgumentException e) {
      model.addAttribute("challenge", challenge);
      model.addAttribute("fullName", fullName);
      model.addAttribute("error", e.getMessage());
      return "register";
    }
  }
}
