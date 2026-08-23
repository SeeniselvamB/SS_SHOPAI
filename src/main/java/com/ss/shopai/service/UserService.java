package com.ss.shopai.service;

import com.ss.shopai.dto.request.ChangePasswordRequest;
import com.ss.shopai.dto.request.UpdateProfileRequest;
import com.ss.shopai.dto.response.PagedResponse;
import com.ss.shopai.dto.response.UserResponse;
import com.ss.shopai.entity.User;
import com.ss.shopai.exception.BadRequestException;
import com.ss.shopai.exception.ResourceNotFoundException;
import com.ss.shopai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = findUserById(userId);
        return authService.mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        User saved = userRepository.save(user);
        return authService.mapToUserResponse(saved);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(String keyword, Pageable pageable) {
        Page<User> page;
        if (keyword != null && !keyword.isBlank()) {
            page = userRepository.searchUsers(keyword, pageable);
        } else {
            page = userRepository.findAll(pageable);
        }
        Page<UserResponse> mapped = page.map(authService::mapToUserResponse);
        return PagedResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        return authService.mapToUserResponse(findUserById(userId));
    }

    @Transactional
    public UserResponse toggleUserEnabled(Long userId) {
        User user = findUserById(userId);
        user.setEnabled(!user.isEnabled());
        User saved = userRepository.save(user);
        return authService.mapToUserResponse(saved);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}