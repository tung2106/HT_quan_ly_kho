package com.vatlieu.web;

import com.vatlieu.service.PhoneRegistrationService;
import com.vatlieu.service.PhoneRegistrationService.Challenge;
import com.vatlieu.service.PhoneRegistrationService.VerifiedRegistration;
import com.vatlieu.service.PhoneVerification;
import jakarta.servlet.http.HttpServletRequest;
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
  private static final String VERIFIED = "verifiedPhoneRegistration";
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
    return view(model, "phone", "Đăng ký bằng số điện thoại");
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
      session.removeAttribute(VERIFIED);
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
    return view(model, "otp", "Nhập mã xác thực");
  }

  @PostMapping("/verify")
  public String verify(
    @SessionAttribute(name = CHALLENGE, required = false) Challenge challenge,
    @RequestParam String code,
    HttpServletRequest request,
    HttpSession session,
    Model model
  ) {
    if (challenge == null) return "redirect:/register";
    try {
      VerifiedRegistration verified = registrations.verify(challenge, code);
      request.changeSessionId();
      session.removeAttribute(CHALLENGE);
      session.setAttribute(VERIFIED, verified);
      return "redirect:/register/details";
    } catch (IllegalArgumentException e) {
      model.addAttribute("error", e.getMessage());
      return verifyForm(challenge, model);
    }
  }

  @GetMapping("/details")
  public String detailsForm(
    @SessionAttribute(name = VERIFIED, required = false) VerifiedRegistration verified,
    Model model
  ) {
    if (verified == null) return "redirect:/register";
    if (verified.expired()) return "redirect:/register?expired";
    return view(model, "details", "Hoàn tất đăng ký");
  }

  @PostMapping("/details")
  public String complete(
    @SessionAttribute(name = VERIFIED, required = false) VerifiedRegistration verified,
    @RequestParam String fullName,
    @RequestParam String password,
    HttpSession session,
    Model model
  ) {
    if (verified == null) return "redirect:/register";
    try {
      registrations.complete(verified, fullName, password);
      session.removeAttribute(VERIFIED);
      return "redirect:/login?registered";
    } catch (IllegalArgumentException e) {
      model.addAttribute("fullName", fullName);
      model.addAttribute("error", e.getMessage());
      return detailsForm(verified, model);
    }
  }

  private String view(Model model, String step, String title) {
    model.addAttribute("registrationStep", step);
    model.addAttribute("pageTitle", title);
    return "register";
  }
}
