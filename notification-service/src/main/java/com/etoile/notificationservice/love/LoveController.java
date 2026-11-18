package com.etoile.notificationservice.love;

import com.etoile.notificationservice.love.LoveDtos.LoveResponse;
import com.etoile.notificationservice.love.LoveDtos.SendLoveRequest;
import com.etoile.notificationservice.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/love")
public class LoveController {

    private final LoveService love;

    public LoveController(LoveService love) {
        this.love = love;
    }

    @GetMapping
    public LoveResponse status(@AuthenticationPrincipal Jwt jwt) {
        return love.view(CurrentUser.from(jwt).role());
    }

    @PostMapping("/send")
    public LoveResponse send(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SendLoveRequest request) {
        return love.send(CurrentUser.from(jwt), request.taps());
    }

    @PostMapping("/need")
    public LoveResponse need(@AuthenticationPrincipal Jwt jwt) {
        return love.need(CurrentUser.from(jwt));
    }

    @PostMapping("/seen")
    public LoveResponse seen(@AuthenticationPrincipal Jwt jwt) {
        return love.seen(CurrentUser.from(jwt));
    }
}
