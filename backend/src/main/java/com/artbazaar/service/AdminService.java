package com.artbazaar.service;

import com.artbazaar.dto.PageResponse;
import com.artbazaar.dto.admin.AdminOrderResponse;
import com.artbazaar.dto.admin.AdminStatsResponse;
import com.artbazaar.dto.admin.AdminUserResponse;
import com.artbazaar.entity.ArtworkStatus;
import com.artbazaar.entity.OrderStatus;
import com.artbazaar.entity.PaymentStatus;
import com.artbazaar.entity.Role;
import com.artbazaar.entity.User;
import com.artbazaar.exception.BadRequestException;
import com.artbazaar.exception.ResourceNotFoundException;
import com.artbazaar.repository.ArtistProfileRepository;
import com.artbazaar.repository.ArtworkRepository;
import com.artbazaar.repository.OrderRepository;
import com.artbazaar.repository.UserRepository;
import com.artbazaar.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.artbazaar.repository.OrderItemRepository.toBigDecimal;

/** Platform overview, user management and the all-orders view for admins. */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final ArtworkRepository artworkRepository;
    private final OrderRepository orderRepository;
    private final RefreshTokenService refreshTokenService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AdminStatsResponse stats() {
        Map<Role, Long> byRole = new EnumMap<>(Role.class);
        for (Object[] row : userRepository.countByRole()) byRole.put((Role) row[0], (Long) row[1]);
        Object[] all = orderRepository.paidTotals(null).getFirst();
        Object[] recent = orderRepository.paidTotals(clock.instant().minus(Duration.ofDays(30))).getFirst();
        return new AdminStatsResponse(
                byRole.getOrDefault(Role.BUYER, 0L), byRole.getOrDefault(Role.ARTIST, 0L),
                artistProfileRepository.countByApproved(false), artworkRepository.countByStatus(ArtworkStatus.ACTIVE),
                ((Number) all[1]).longValue(), toBigDecimal(all[0]),
                ((Number) recent[1]).longValue(), toBigDecimal(recent[0]),
                orderRepository.countByStatusIn(List.of(OrderStatus.PAID, OrderStatus.PARTIALLY_SHIPPED)));
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> users(Role role, String q, int page, int size) {
        String pattern = StringUtils.hasText(q) ? "%" + escape(q.trim().toLowerCase(Locale.ROOT)) + "%" : null;
        Page<User> users = userRepository.search(role, pattern, PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        List<Long> artistIds = users.stream().filter(u -> u.getRole() == Role.ARTIST).map(User::getId).toList();
        Set<Long> approved = artistIds.isEmpty() ? Set.of() : artistProfileRepository.findApprovedUserIds(artistIds);
        return PageResponse.of(users, u -> new AdminUserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(),
                u.isEnabled(), u.getRole() == Role.ARTIST ? approved.contains(u.getId()) : null, u.getCreatedAt()));
    }

    /**
     * Disabling takes effect at once: every request reloads the user, so their access token stops
     * working immediately, and their refresh tokens are revoked so they cannot sign back in silently.
     */
    @Transactional
    public AdminUserResponse setEnabled(UserPrincipal me, Long userId, boolean enabled) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getId().equals(me.id())) {
            throw new BadRequestException("You can't change your own account status");
        }
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Admin accounts can't be disabled here");
        }
        user.setEnabled(enabled);
        if (!enabled) {
            refreshTokenService.revokeAll(user.getId());
        }
        Boolean approved = user.getRole() == Role.ARTIST
                ? !artistProfileRepository.findApprovedUserIds(List.of(user.getId())).isEmpty() : null;
        return new AdminUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isEnabled(),
                approved, user.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminOrderResponse> orders(OrderStatus status, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        var orders = status == null ? orderRepository.findAll(pageable) : orderRepository.findByStatus(status, pageable);
        return PageResponse.of(orders, AdminOrderResponse::from);
    }

    static String escape(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
