package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.request.UpdateUserRequest;
import com.nhom7.coworkingspace.dto.request.UserSearchRequest;
import com.nhom7.coworkingspace.dto.response.HostUpgradeResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserRoleResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserStatusResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserVerificationResponse;
import com.nhom7.coworkingspace.dto.response.UserProfileResponse;
import com.nhom7.coworkingspace.dto.response.UserSearchResponse;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.mapper.UserMapper;
import com.nhom7.coworkingspace.repository.RoleRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.FileStorageService;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
import com.nhom7.coworkingspace.service.UserService;
import com.nhom7.coworkingspace.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int IMAGE_SIGNED_URL_EXPIRES_IN_SECONDS = 3600;
    private static final String USER_ROLE_NAME = "USER";
    private static final String HOST_ROLE_NAME = "HOST";
    private static final String MODERATOR_ROLE_NAME = "MODERATOR";
    private static final String ADMIN_ROLE_NAME = "ADMIN";
    private static final String BUSINESS_LICENSE_SUBDIRECTORY = "business-license";

    private static final Set<String> ASSIGNABLE_ROLE_NAMES = Set.of(
            USER_ROLE_NAME,
            HOST_ROLE_NAME,
            MODERATOR_ROLE_NAME);

    private static final Set<String> ELEVATED_ROLE_NAMES = Set.of(
            HOST_ROLE_NAME,
            MODERATOR_ROLE_NAME,
            ADMIN_ROLE_NAME);

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "email", "phone", "status", "createdAt");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FileStorageService fileStorageService;
    private final UserMapper userMapper;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSearchResponse> searchUsers(UserSearchRequest request, String currentUserEmail) {
        log.debug("[UserService] Searching users with params: keyword={}, status={}, role={}",
                request.getKeyword(), request.getStatus(), request.getRole());

        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDir())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        String rawSortBy = (request.getSortBy() != null)
                ? request.getSortBy().trim()
                : "id";

        String sortBy = ALLOWED_SORT_FIELDS.contains(rawSortBy)
                ? rawSortBy
                : "id";

        int page = Math.max(0, request.getPage());
        int size = Math.min(Math.max(1, request.getSize()), 100);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortBy)
        );


        // A MODERATOR caller (without the ADMIN role) must never see ADMIN accounts in the list.
        User currentUser = findUserByEmail(currentUserEmail);
        boolean isCurrentAdmin = hasRole(currentUser, ADMIN_ROLE_NAME);
        String excludedRole = isCurrentAdmin ? null : ADMIN_ROLE_NAME;

        Specification<User> spec =
                UserSpecification.buildSearchSpecification(request, excludedRole);

        Page<User> userPage =
                userRepository.findAll(spec, pageable);

        Page<UserSearchResponse> dtoPage =
                userPage.map(this::buildUserSearchResponse);

        return PageResponse.fromPage(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSearchResponse getUserById(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new AppException(
                                "user.not.found",
                                HttpStatus.NOT_FOUND
                        )
                );

        return userMapper.toUserSearchResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAvailableRoleNames() {

        return roleRepository
                .findAll(
                        Sort.by(
                                Sort.Direction.ASC,
                                "name"
                        )
                )
                .stream()
                .map(Role::getName)
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .filter(ASSIGNABLE_ROLE_NAMES::contains)
                .toList();
    }

    @Override
    @Transactional
    public UpdateUserRoleResponse changeRole(
            Long userId,
            String roleName
    ) {

        if (!StringUtils.hasText(roleName)) {
            throw new AppException(
                    "validation.role.required",
                    HttpStatus.BAD_REQUEST
            );
        }

        String normalizedRoleName =
                roleName.trim().toUpperCase();

        /*
         * Chỉ cho phép gán trực tiếp:
         * USER, HOST, MODERATOR.
         *
         * ADMIN không được gán qua endpoint này.
         */
        if (!ASSIGNABLE_ROLE_NAMES.contains(
                normalizedRoleName
        )) {

            throw new AppException(
                    "role.assignment.not.allowed",
                    HttpStatus.BAD_REQUEST
            );
        }

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new AppException(
                                        "user.not.found",
                                        HttpStatus.NOT_FOUND
                                )
                        );

        Role targetRole =
                roleRepository
                        .findByName(normalizedRoleName)
                        .orElseThrow(() ->
                                new AppException(
                                        "role.not.found",
                                        HttpStatus.NOT_FOUND
                                )
                        );

        boolean hasAdminRole =
                user.getRoles()
                        .stream()
                        .anyMatch(role ->
                                ADMIN_ROLE_NAME.equalsIgnoreCase(
                                        role.getName()
                                )
                        );

        /*
         * Nếu user đang là ADMIN và đây là ADMIN cuối cùng
         * thì không được hạ role.
         */
        if (hasAdminRole
                && userRepository.countAdminUsers() <= 1) {

            throw new AppException(
                    "role.last.admin.cannot.change",
                    HttpStatus.BAD_REQUEST
            );
        }

        /*
         * USER là role nền.
         * Nếu user hiện không có USER thì lấy từ database.
         */
        Role baseUserRole =
                user.getRoles()
                        .stream()
                        .filter(role ->
                                USER_ROLE_NAME.equalsIgnoreCase(
                                        role.getName()
                                )
                        )
                        .findFirst()
                        .orElseGet(() ->
                                roleRepository
                                        .findByName(USER_ROLE_NAME)
                                        .orElseThrow(() ->
                                                new AppException(
                                                        "role.not.found",
                                                        HttpStatus.NOT_FOUND
                                                )
                                        )
                        );

        /*
         * Xóa role nâng cao cũ:
         * HOST / MODERATOR / ADMIN.
         */
        user.getRoles().removeIf(role -> {

            String currentRoleName =
                    role.getName();

            return StringUtils.hasText(currentRoleName)
                    && ELEVATED_ROLE_NAMES.contains(
                            currentRoleName.toUpperCase()
                    );
        });

        /*
         * USER luôn là role nền.
         */
        user.getRoles().add(baseUserRole);

        /*
         * Nếu đổi sang HOST hoặc MODERATOR
         * thì thêm role tương ứng.
         *
         * Nếu đổi về USER thì không thêm role nâng cao nào.
         */
        if (!USER_ROLE_NAME.equals(
                normalizedRoleName
        )) {

            user.getRoles().add(targetRole);
        }

        User updatedUser =
                userRepository.save(user);

        return buildUpdateUserRoleResponse(
                updatedUser
        );
    }

    @Override
    @Transactional
    public UpdateUserRoleResponse removeRole(
            Long userId,
            String roleName
    ) {

        // 1. Tìm user
        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "User not found with id: " + userId
                                )
                        );

        // 2. Chuẩn hóa role name
        String normalizedRoleName =
                roleName.trim().toUpperCase();

        // 3. Tìm role
        Role role =
                roleRepository.findByName(normalizedRoleName)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Role not found: " + normalizedRoleName
                                )
                        );

        // 4. USER là role nền -> không được xóa
        if (USER_ROLE_NAME.equals(
                normalizedRoleName
        )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "USER is the base role and cannot be removed"
            );
        }

        // 5. Kiểm tra user có role này hay không
        boolean hasRole =
                user.getRoles()
                        .stream()
                        .anyMatch(userRole ->
                                userRole.getName()
                                        .equalsIgnoreCase(
                                                normalizedRoleName
                                        )
                        );

        if (!hasRole) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User does not have role: "
                            + normalizedRoleName
            );
        }

        // 6. Nếu xóa ADMIN -> hệ thống phải còn ít nhất 1 ADMIN
        if (ADMIN_ROLE_NAME.equals(
                normalizedRoleName
        )) {

            long adminCount =
                    userRepository.countAdminUsers();

            if (adminCount <= 1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot remove the last ADMIN role"
                );
            }
        }

        // 7. Xóa role
        user.getRoles().removeIf(userRole ->
                userRole.getName()
                        .equalsIgnoreCase(
                                normalizedRoleName
                        )
        );

        // 8. Lưu lại
        User updatedUser =
                userRepository.save(user);

        return buildUpdateUserRoleResponse(
                updatedUser
        );
    }

    private UpdateUserRoleResponse buildUpdateUserRoleResponse(
            User user
    ) {

        Set<String> roleNames =
                user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet());

        return UpdateUserRoleResponse
                .builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roles(roleNames)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(
            String email
    ) {

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new AppException(
                                        "user.not.found",
                                        HttpStatus.NOT_FOUND
                                )
                        );

        return buildProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(
            String email,
            UpdateUserRequest request
    ) {

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new AppException(
                                        "user.not.found",
                                        HttpStatus.NOT_FOUND
                                )
                        );

        if (request.getName() != null) {

            String trimmedName =
                    request.getName().trim();

            if (trimmedName.isEmpty()) {
                throw new AppException(
                        "validation.name.required",
                        HttpStatus.BAD_REQUEST
                );
            }

            user.setName(trimmedName);
        }

        if (request.getPhone() != null) {

            String trimmedPhone =
                    request.getPhone().trim();

            if (userRepository.existsByPhoneAndIdNot(
                    trimmedPhone,
                    user.getId()
            )) {

                throw new AppException(
                        "user.phone.exists",
                        HttpStatus.CONFLICT
                );
            }

            user.setPhone(trimmedPhone);
        }

        MultipartFile cccdImage =
                request.getCccdImage();

        if (cccdImage != null
                && !cccdImage.isEmpty()) {

            String cccdPath =
                    fileStorageService.storeFile(
                            cccdImage,
                            "cccd"
                    );

            user.setCccdUrl(cccdPath);
        }

        User updatedUser =
                userRepository.save(user);

        return buildProfileResponse(
                updatedUser
        );
    }

    // noRollbackFor is essential here: a business license upload must be persisted
    // even when the very same call goes on to reject the upgrade
    // (e.g. verification still pending) - otherwise Spring's default rollback
    // on RuntimeException would undo the save() below every time an AppException
    // is thrown afterwards, leaving business_license_url permanently NULL.
    @Override
    @Transactional(
            noRollbackFor = AppException.class
    )
    public HostUpgradeResponse becomeHost(
            String email,
            MultipartFile businessLicense
    ) {

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new AppException(
                                        "user.not.found",
                                        HttpStatus.NOT_FOUND
                                )
                        );

        boolean alreadyHost =
                user.getRoles()
                        .stream()
                        .anyMatch(role ->
                                HOST_ROLE_NAME.equalsIgnoreCase(
                                        role.getName()
                                )
                        );

        if (alreadyHost) {

            return HostUpgradeResponse
                    .builder()
                    .profile(
                            buildProfileResponse(user)
                    )
                    .alreadyHost(true)
                    .build();
        }

        if (businessLicense != null
                && !businessLicense.isEmpty()) {

            String newHash =
                    sha256Hex(businessLicense);

            boolean isSameFileAlreadyOnFile =
                    StringUtils.hasText(
                            user.getBusinessLicenseUrl()
                    )
                            && newHash.equals(
                                    user.getBusinessLicenseHash()
                            );

            /*
             * Resubmitting exactly the same verified file
             * must not reset verification.
             */
            if (!isSameFileAlreadyOnFile) {

                String licensePath =
                        fileStorageService.storeFile(
                                businessLicense,
                                BUSINESS_LICENSE_SUBDIRECTORY
                        );

                user.setBusinessLicenseUrl(
                        licensePath
                );

                user.setBusinessLicenseHash(
                        newHash
                );

                user.setIsBusinessVerified(
                        false
                );

                userRepository.save(user);
            }
        }

        boolean isActive =
                user.getStatus()
                        == UserStatus.ACTIVE;

        boolean hasLicense =
                StringUtils.hasText(
                        user.getBusinessLicenseUrl()
                );

        boolean identityVerified =
                Boolean.TRUE.equals(
                        user.getIsIdentityVerified()
                );

        boolean businessVerified =
                Boolean.TRUE.equals(
                        user.getIsBusinessVerified()
                );

        if (!isActive) {
            throw new AppException(
                    "host.status.not.active",
                    HttpStatus.FORBIDDEN
            );
        }

        if (!hasLicense) {
            throw new AppException(
                    "host.license.required",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (!identityVerified) {
            throw new AppException(
                    "host.identity.required",
                    HttpStatus.FORBIDDEN
            );
        }

        if (!businessVerified) {
            throw new AppException(
                    "host.business.pending",
                    HttpStatus.FORBIDDEN
            );
        }

        Role hostRole =
                roleRepository
                        .findByName(HOST_ROLE_NAME)
                        .orElseThrow(() ->
                                new AppException(
                                        "role.not.found",
                                        HttpStatus.NOT_FOUND
                                )
                        );

        user.getRoles().add(hostRole);

        User updatedUser =
                userRepository.save(user);

        return HostUpgradeResponse
                .builder()
                .profile(
                        buildProfileResponse(
                                updatedUser
                        )
                )
                .alreadyHost(false)
                .build();
    }

    @Override
    @Transactional
    public UpdateUserStatusResponse updateUserStatus(
            Long targetUserId,
            UserStatus newStatus,
            String currentAdminEmail
    ) {

        log.info(
                "[UserService] Updating user status: targetUserId={}, newStatus={}, performedBy={}",
                targetUserId,
                newStatus,
                currentAdminEmail
        );

        User targetUser =
                findUserById(targetUserId);

        User currentUser =
                findUserByEmail(
                        currentAdminEmail
                );

        // Cannot deactivate or block your own account
        if (targetUser.getId()
                .equals(currentUser.getId())
                && newStatus != UserStatus.ACTIVE) {

            log.warn(
                    "[UserService] User {} attempted to deactivate/block their own account (id={})",
                    currentAdminEmail,
                    targetUserId
            );

            throw new AppException(
                    "user.cannot.block.self",
                    HttpStatus.BAD_REQUEST
            );
        }


        validateModeratorCannotModifyAdmin(
                currentUser,
                targetUser,
                currentAdminEmail
        );

        validateCannotModifySamePrivilegedRole(
                currentUser,
                targetUser,
                currentAdminEmail
        );


        if (targetUser.getStatus()
                == newStatus) {

            log.info(
                    "[UserService] User status already {}, no update required: targetUserId={}",
                    newStatus,
                    targetUserId
            );

            return userMapper
                    .toUpdateUserStatusResponse(
                            targetUser
                    );
        }

        targetUser.setStatus(
                newStatus
        );

        User savedUser =
                userRepository.save(
                        targetUser
                );

        // Revoke active tokens when user is blocked or deactivated
        if (newStatus == UserStatus.BLOCKED
                || newStatus == UserStatus.INACTIVE) {

            log.info(
                    "[UserService] Revoking active tokens for user: {}",
                    savedUser.getEmail()
            );

            tokenBlacklistService
                    .blacklistUserTokens(
                            savedUser.getEmail(),
                            new Date()
                    );
        }

        return userMapper
                .toUpdateUserStatusResponse(
                        savedUser
                );
    }

    @Override
    @Transactional
    public UpdateUserVerificationResponse updateIdentityVerification(
            Long targetUserId,
            boolean verified,
            String currentAdminEmail
    ) {

        log.info(
                "[UserService] Updating identity verification (CCCD): targetUserId={}, verified={}, performedBy={}",
                targetUserId,
                verified,
                currentAdminEmail
        );

        User targetUser =
                findUserById(
                        targetUserId
                );

        User currentUser =
                findUserByEmail(
                        currentAdminEmail
                );

        validateNotSelfVerification(
                currentUser,
                targetUser,
                currentAdminEmail
        );

        validateCannotVerifyPrivilegedRole(
                currentUser,
                targetUser,
                currentAdminEmail
        );

        if (Boolean.valueOf(verified)
                .equals(
                        targetUser.getIsIdentityVerified()
                )) {

            log.info(
                    "[UserService] Identity verification already {}, no update required: targetUserId={}",
                    verified,
                    targetUserId
            );

            return userMapper
                    .toUpdateUserVerificationResponse(
                            targetUser
                    );
        }

        // Cannot mark as verified if document is missing
        if (verified
                && (targetUser.getCccdUrl() == null
                || targetUser.getCccdUrl().isBlank())) {

            log.warn(
                    "[UserService] Cannot verify identity for user {} because CCCD document is missing",
                    targetUserId
            );

            throw new AppException(
                    "user.identity.document.missing",
                    HttpStatus.BAD_REQUEST
            );
        }

        targetUser.setIsIdentityVerified(
                verified
        );

        User savedUser =
                userRepository.save(
                        targetUser
                );

        return userMapper
                .toUpdateUserVerificationResponse(
                        savedUser
                );
    }

    @Override
    @Transactional
    public UpdateUserVerificationResponse updateBusinessVerification(
            Long targetUserId,
            boolean verified,
            String currentAdminEmail
    ) {

        log.info(
                "[UserService] Updating business verification (License): targetUserId={}, verified={}, performedBy={}",
                targetUserId,
                verified,
                currentAdminEmail
        );

        User targetUser =
                findUserById(
                        targetUserId
                );

        User currentUser =
                findUserByEmail(
                        currentAdminEmail
                );

        validateNotSelfVerification(
                currentUser,
                targetUser,
                currentAdminEmail
        );

        validateCannotVerifyPrivilegedRole(
                currentUser,
                targetUser,
                currentAdminEmail
        );

        // Cannot mark as verified if document is missing
        if (verified
                && (targetUser.getBusinessLicenseUrl() == null
                || targetUser.getBusinessLicenseUrl().isBlank())) {

            log.warn(
                    "[UserService] Cannot verify business for user {} because business license document is missing",
                    targetUserId
            );

            throw new AppException(
                    "user.business.document.missing",
                    HttpStatus.BAD_REQUEST
            );
        }

        boolean hasHostRole = targetUser.getRoles().stream()
                .anyMatch(role -> HOST_ROLE_NAME.equalsIgnoreCase(role.getName()));
        boolean roleChanged = false;

        if (verified && !hasHostRole) {
            Role hostRole = roleRepository.findByName(HOST_ROLE_NAME)
                    .orElseThrow(() -> new AppException("role.not.found", HttpStatus.NOT_FOUND));
            targetUser.getRoles().add(hostRole);
            roleChanged = true;
        } else if (!verified && hasHostRole) {
            roleChanged = targetUser.getRoles().removeIf(
                    role -> HOST_ROLE_NAME.equalsIgnoreCase(role.getName())
            );
        }

        boolean verificationChanged = !Boolean.valueOf(verified)
                .equals(targetUser.getIsBusinessVerified());

        if (!verificationChanged && !roleChanged) {
            log.info(
                    "[UserService] Business verification and HOST role already synchronized: targetUserId={}, verified={}",
                    targetUserId,
                    verified
            );
            return userMapper.toUpdateUserVerificationResponse(targetUser);
        }

        targetUser.setIsBusinessVerified(verified);

        User savedUser =
                userRepository.save(
                        targetUser
                );

        if (roleChanged) {
            log.info(
                    "[UserService] HOST role changed for user {}, revoking previously issued tokens",
                    savedUser.getEmail()
            );
            tokenBlacklistService.blacklistUserTokens(savedUser.getEmail(), new Date());
        }

        return userMapper
                .toUpdateUserVerificationResponse(
                        savedUser
                );
    }



    private UserSearchResponse buildUserSearchResponse(
            User user
    ) {
        UserSearchResponse response =
                userMapper.toUserSearchResponse(user);
        response.setCccdUrl(
                resolveSignedUrl(user.getCccdUrl())
        );
        response.setBusinessLicenseUrl(
                resolveSignedUrl(user.getBusinessLicenseUrl())
        );
        return response;
    }

    private UserProfileResponse buildProfileResponse(
            User user
    ) {


        Set<String> roleNames =
                user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet());

        return UserProfileResponse
                .builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .isIdentityVerified(
                        user.getIsIdentityVerified()
                )
                .isBusinessVerified(
                        user.getIsBusinessVerified()
                )
                .language(user.getLanguage())
                .cccdUrl(
                        resolveSignedUrl(
                                user.getCccdUrl()
                        )
                )
                .businessLicenseUrl(
                        resolveSignedUrl(
                                user.getBusinessLicenseUrl()
                        )
                )
                .roles(roleNames)
                .build();
    }

    private String resolveSignedUrl(
            String filePath
    ) {

        if (!StringUtils.hasText(filePath)) {
            return null;
        }

        return fileStorageService
                .createSignedUrl(
                        filePath,
                        IMAGE_SIGNED_URL_EXPIRES_IN_SECONDS
                );
    }

    private User findUserById(
            Long userId
    ) {

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new AppException(
                                "user.not.found",
                                HttpStatus.NOT_FOUND
                        )
                );
    }

    private User findUserByEmail(
            String email
    ) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new AppException(
                                "auth.invalid.credentials",
                                HttpStatus.UNAUTHORIZED
                        )
                );
    }

    private void validateModeratorCannotModifyAdmin(
            User currentUser,
            User targetUser,
            String currentEmail
    ) {

        boolean isTargetAdmin =
                hasRole(
                        targetUser,
                        ADMIN_ROLE_NAME
                );

        boolean isCurrentAdmin =
                hasRole(
                        currentUser,
                        ADMIN_ROLE_NAME
                );

        if (isTargetAdmin
                && !isCurrentAdmin) {

            log.warn(
                    "[UserService] Moderator {} attempted to modify Admin (id={})",
                    currentEmail,
                    targetUser.getId()
            );

            throw new AppException(
                    "user.cannot.modify.admin",
                    HttpStatus.FORBIDDEN
            );
        }
    }


    /**
     * Prevents peer privileged accounts from changing each other's status.
     *
     * <p>Rules:
     * <ul>
     *     <li>ADMIN cannot change another ADMIN's status.</li>
     *     <li>MODERATOR cannot change another MODERATOR's status.</li>
     *     <li>The restriction applies to BLOCKED, INACTIVE and ACTIVE (unlock).</li>
     * </ul>
     */
    private void validateCannotModifySamePrivilegedRole(
            User currentUser,
            User targetUser,
            String currentEmail
    ) {

        // Self-account restrictions are handled separately.
        if (targetUser.getId().equals(currentUser.getId())) {
            return;
        }

        boolean isCurrentAdmin =
                hasRole(
                        currentUser,
                        ADMIN_ROLE_NAME
                );

        boolean isCurrentModerator =
                hasRole(
                        currentUser,
                        MODERATOR_ROLE_NAME
                );

        boolean isTargetAdmin =
                hasRole(
                        targetUser,
                        ADMIN_ROLE_NAME
                );

        boolean isTargetModerator =
                hasRole(
                        targetUser,
                        MODERATOR_ROLE_NAME
                );

        if (isCurrentAdmin && isTargetAdmin) {

            log.warn(
                    "[UserService] Admin {} attempted to change another Admin's status (id={})",
                    currentEmail,
                    targetUser.getId()
            );

            throw new AppException(
                    "user.cannot.modify.peer.admin",
                    HttpStatus.FORBIDDEN
            );
        }

        if (!isCurrentAdmin
                && isCurrentModerator
                && isTargetModerator) {

            log.warn(
                    "[UserService] Moderator {} attempted to change another Moderator's status (id={})",
                    currentEmail,
                    targetUser.getId()
            );

            throw new AppException(
                    "user.cannot.modify.peer.moderator",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    /**
     * Prevents KYC / business verification across disallowed privileged-role boundaries.
     *
     * <p>ADMIN may verify MODERATOR accounts, but not another ADMIN.
     * MODERATOR may only verify accounts that do not have MODERATOR or ADMIN role.
     * A USER + MODERATOR account is therefore still treated as MODERATOR.
     */
    private void validateCannotVerifyPrivilegedRole(
            User currentUser,
            User targetUser,
            String currentEmail
    ) {

        boolean isCurrentAdmin =
                hasRole(
                        currentUser,
                        ADMIN_ROLE_NAME
                );

        boolean isCurrentModerator =
                hasRole(
                        currentUser,
                        MODERATOR_ROLE_NAME
                );

        boolean isTargetAdmin =
                hasRole(
                        targetUser,
                        ADMIN_ROLE_NAME
                );

        boolean isTargetModerator =
                hasRole(
                        targetUser,
                        MODERATOR_ROLE_NAME
                );

        boolean adminVerifyingAdmin =
                isCurrentAdmin
                        && isTargetAdmin;

        boolean moderatorVerifyingPrivilegedUser =
                !isCurrentAdmin
                        && isCurrentModerator
                        && (
                        isTargetModerator
                                || isTargetAdmin
                );

        if (adminVerifyingAdmin
                || moderatorVerifyingPrivilegedUser) {

            log.warn(
                    "[UserService] User {} attempted to verify privileged account (id={})",
                    currentEmail,
                    targetUser.getId()
            );

            throw new AppException(
                    "user.cannot.verify.privileged",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private void validateNotSelfVerification(
            User currentUser,
            User targetUser,
            String currentEmail
    ) {

        if (targetUser.getId()
                .equals(currentUser.getId())) {

            log.warn(
                    "[UserService] User {} attempted to self-verify their own KYC documents (id={})",
                    currentEmail,
                    targetUser.getId()
            );

            throw new AppException(
                    "user.cannot.verify.self",
                    HttpStatus.BAD_REQUEST
            );

        }
    }

    private boolean hasRole(
            User user,
            String roleName
    ) {

        return user.getRoles() != null
                && user.getRoles()
                .stream()
                .anyMatch(role ->
                        roleName.equalsIgnoreCase(
                                role.getName()
                        )
                );
    }

    private String sha256Hex(
            MultipartFile file
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            file.getBytes()
                    );

            return HexFormat.of()
                    .formatHex(hash);

        } catch (
                NoSuchAlgorithmException
                | IOException ex
        ) {

            throw new AppException(
                    "common.error",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
