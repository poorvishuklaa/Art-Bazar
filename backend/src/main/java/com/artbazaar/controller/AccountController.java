package com.artbazaar.controller;

import com.artbazaar.dto.account.AccountResponse;
import com.artbazaar.dto.account.ChangePasswordRequest;
import com.artbazaar.dto.account.UpdateProfileRequest;
import com.artbazaar.dto.auth.AuthResponse;
import com.artbazaar.security.UserPrincipal;
import com.artbazaar.service.AccountService;
import com.artbazaar.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's own account (any role). */
@Tag(name = "Account")
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AuthService authService;
    private final AuthController authController;

    @Operation(summary = "My profile")
    @GetMapping
    public AccountResponse get(@AuthenticationPrincipal UserPrincipal me) {
        return accountService.get(me);
    }

    @Operation(summary = "Update my name (and bio/city for artists)")
    @PutMapping
    public AccountResponse update(@AuthenticationPrincipal UserPrincipal me, @Valid @RequestBody UpdateProfileRequest req) {
        return accountService.update(me, req);
    }

    @Operation(summary = "Change my password; signs out every other session and returns new tokens for this one")
    @PostMapping("/password")
    public ResponseEntity<AuthResponse> changePassword(@AuthenticationPrincipal UserPrincipal me,
                                                       @Valid @RequestBody ChangePasswordRequest req) {
        return authController.sessionResponse(authService.changePassword(me.id(), req.currentPassword(), req.newPassword()));
    }
}
