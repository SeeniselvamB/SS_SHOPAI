package com.ss.shopai.controller.admin;

import com.ss.shopai.dto.response.ApiResponse;
import com.ss.shopai.dto.response.PagedResponse;
import com.ss.shopai.dto.response.UserResponse;
import com.ss.shopai.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<UserResponse> response = userService.getAllUsers(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/toggle-enabled")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserEnabled(@PathVariable Long id) {
        UserResponse response = userService.toggleUserEnabled(id);
        return ResponseEntity.ok(ApiResponse.success("User status updated", response));
    }
}