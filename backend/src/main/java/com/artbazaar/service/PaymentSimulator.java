package com.artbazaar.service;

import com.artbazaar.dto.order.PaymentRequest;
import com.artbazaar.entity.PaymentMethod;
import com.artbazaar.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Stands in for a payment gateway. It validates input the way a real checkout form would,
 * then approves or declines deterministically so failures can be demonstrated.
 * Replacing this class with a Razorpay/Stripe client is the only change needed for real payments.
 */
@Component
public class PaymentSimulator {

    static final String DECLINED_CARD = "4000000000000002";
    static final String DECLINED_UPI = "fail@upi";

    private static final Pattern UPI = Pattern.compile("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$");
    private static final Pattern EXPIRY = Pattern.compile("^(0[1-9]|1[0-2])/(\\d{2})$");
    private static final Pattern CVV = Pattern.compile("^\\d{3}$");
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Clock clock;

    public PaymentSimulator(Clock clock) {
        this.clock = clock;
    }

    public sealed interface Result permits Approved, Declined {
    }

    public record Approved(String reference) implements Result {
    }

    public record Declined(String reason) implements Result {
    }

    public Result charge(PaymentRequest req) {
        return req.method() == PaymentMethod.UPI ? upi(req.upiId()) : card(req);
    }

    private Result upi(String upiId) {
        if (!StringUtils.hasText(upiId) || !UPI.matcher(upiId.trim()).matches()) {
            throw new BadRequestException("Enter a valid UPI ID, like name@bank");
        }
        if (upiId.trim().toLowerCase(Locale.ROOT).equals(DECLINED_UPI)) {
            return new Declined("The UPI payment was declined. No money was taken.");
        }
        return new Approved(reference());
    }

    private Result card(PaymentRequest req) {
        String number = req.cardNumber() == null ? "" : req.cardNumber().replaceAll("[\\s-]", "");
        if (!number.matches("^\\d{16}$") || !luhnValid(number)) {
            throw new BadRequestException("Enter a valid 16-digit card number");
        }
        var expiry = req.cardExpiry() == null ? null : EXPIRY.matcher(req.cardExpiry().trim());
        if (expiry == null || !expiry.matches()) {
            throw new BadRequestException("Enter the expiry date as MM/YY");
        }
        YearMonth exp = YearMonth.of(2000 + Integer.parseInt(expiry.group(2)), Integer.parseInt(expiry.group(1)));
        if (exp.isBefore(YearMonth.now(clock.withZone(ZoneOffset.UTC)))) {
            throw new BadRequestException("This card has expired");
        }
        if (req.cardCvv() == null || !CVV.matcher(req.cardCvv().trim()).matches()) {
            throw new BadRequestException("Enter the 3-digit CVV");
        }
        if (number.equals(DECLINED_CARD)) {
            return new Declined("Your card was declined. No money was taken.");
        }
        return new Approved(reference());
    }

    static boolean luhnValid(String digits) {
        int sum = 0;
        boolean dbl = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (dbl) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            dbl = !dbl;
        }
        return sum % 10 == 0;
    }

    private static String reference() {
        StringBuilder sb = new StringBuilder("SIMPAY-");
        for (int i = 0; i < 10; i++) sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}
