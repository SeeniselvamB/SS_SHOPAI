package com.ss.shopai.controller;

import com.ss.shopai.dto.request.AddressRequest;
import com.ss.shopai.dto.response.AddressResponse;
import com.ss.shopai.dto.response.ApiResponse;
import com.ss.shopai.security.CurrentUser;
import com.ss.shopai.security.UserPrincipal;
import com.ss.shopai.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getUserAddresses(@CurrentUser UserPrincipal principal) {
        List<AddressResponse> response = addressService.getUserAddresses(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(@CurrentUser UserPrincipal principal,
                                                                     @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.addAddress(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Address added", response));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(@CurrentUser UserPrincipal principal,
                                                                        @PathVariable Long addressId,
                                                                        @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.updateAddress(principal.getId(), addressId, request);
        return ResponseEntity.ok(ApiResponse.success("Address updated", response));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@CurrentUser UserPrincipal principal,
                                                             @PathVariable Long addressId) {
        addressService.deleteAddress(principal.getId(), addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted", null));
    }

    @PutMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(@CurrentUser UserPrincipal principal,
                                                                            @PathVariable Long addressId) {
        AddressResponse response = addressService.setDefaultAddress(principal.getId(), addressId);
        return ResponseEntity.ok(ApiResponse.success("Default address updated", response));
    }
}