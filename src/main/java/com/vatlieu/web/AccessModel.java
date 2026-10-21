package com.vatlieu.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AccessModel {
    @ModelAttribute("loginName") public String name(Authentication auth) { return auth==null?"":auth.getName(); }
    @ModelAttribute("isOwner") public boolean owner(Authentication auth) {
        return auth!=null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));
    }
}
