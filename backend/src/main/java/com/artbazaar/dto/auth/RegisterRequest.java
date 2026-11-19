package com.artbazaar.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be 2 to 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain a letter and a number")
        String password,

        @NotNull(message = "Choose whether you are joining as a buyer or an artist")
        AccountType accountType,

        @Size(max = 1000, message = "Bio can be at most 1000 characters")
        String bio,

        @Size(max = 100, message = "City can be at most 100 characters")
        String city) {

    /** Admins cannot self-register, so this is narrower than {@link com.artbazaar.entity.Role}. */
    public enum AccountType {
        BUYER, ARTIST
    }
}
