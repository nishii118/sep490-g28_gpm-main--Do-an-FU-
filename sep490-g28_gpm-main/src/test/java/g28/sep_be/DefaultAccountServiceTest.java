package g28.sep_be;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import vn.com.fpt.sep490_g28_summer2024_be.common.ErrorCode;
import vn.com.fpt.sep490_g28_summer2024_be.dto.ApiResponse;
import vn.com.fpt.sep490_g28_summer2024_be.dto.account.admin.AccountDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.account.admin.AccountForgotPasswordDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.account.admin.AccountProfilePageDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.account.admin.AccountRegisterDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.account.client.AmbassadorResponseDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.account.client.TopAmbassadorResponseDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.account.client.interfacedto.AmbassadorInterfaceDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.account.client.interfacedto.TopAmbassadorInterfaceDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.pageinfo.PageResponse;
import vn.com.fpt.sep490_g28_summer2024_be.dto.role.RoleDTO;
import vn.com.fpt.sep490_g28_summer2024_be.entity.Account;
import vn.com.fpt.sep490_g28_summer2024_be.entity.Role;
import vn.com.fpt.sep490_g28_summer2024_be.exception.AppException;
import vn.com.fpt.sep490_g28_summer2024_be.firebase.FirebaseService;
import vn.com.fpt.sep490_g28_summer2024_be.repository.AccountRepository;
import vn.com.fpt.sep490_g28_summer2024_be.repository.DonationRepository;
import vn.com.fpt.sep490_g28_summer2024_be.repository.RoleRepository;
import vn.com.fpt.sep490_g28_summer2024_be.sercurity.CustomAccountDetails;
import vn.com.fpt.sep490_g28_summer2024_be.service.account.DefaultAccountService;
import vn.com.fpt.sep490_g28_summer2024_be.service.email.EmailService;
import vn.com.fpt.sep490_g28_summer2024_be.utils.CodeUtils;
import vn.com.fpt.sep490_g28_summer2024_be.utils.Email;
import vn.com.fpt.sep490_g28_summer2024_be.utils.OtpUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultAccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpUtils otpUtils;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CodeUtils codeUtils;

    @Mock
    private EmailService emailService;

    @Mock
    private FirebaseService firebaseService;

    @InjectMocks
    private DefaultAccountService accountService;

    private Account account;
    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role(BigInteger.valueOf(1), "user", "User Role", LocalDateTime.now(), LocalDateTime.now(), true, null);
        account = new Account(BigInteger.valueOf(1), "ACC123", "REF123", "test@example.com", "password", "Test User", 1, "0123456789", "Address", "avatar.jpg", LocalDate.now(), LocalDateTime.now(), LocalDateTime.now(), true, role);
    }


    @Test
    void createAccount_Success() {
        AccountDTO accountDTO = AccountDTO.builder()
                .email("newuser@example.com")
                .password("password")
                .fullname("New User")
                .gender(1)
                .phone("0123456789")
                .address("New Address")
                .dob(LocalDate.now())
                .role(RoleDTO.builder().roleId(BigInteger.valueOf(1)).build())
                .build();

        Role role = new Role(BigInteger.valueOf(1), "user", "User Role", LocalDateTime.now(), LocalDateTime.now(), true, null);
        Account newAccount = new Account(BigInteger.valueOf(2), "ACC123", "REF123", "newuser@example.com", "encodedPassword", "New User", 1, "0123456789", "New Address", "avatar.jpg", LocalDate.now(), LocalDateTime.now(), LocalDateTime.now(), true, role);

        when(accountRepository.existsAccountByEmail(anyString())).thenReturn(false);
        when(roleRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(role));

        when(accountRepository.save(any(Account.class))).thenReturn(newAccount);
        lenient().when(codeUtils.genCode(anyString(), any())).thenReturn("ACC123");

        AccountDTO createdAccount = accountService.createAccount(accountDTO);

        assertNotNull(createdAccount);
        assertEquals("newuser@example.com", createdAccount.getEmail());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void viewSystemUserAccountsByFilter_Success() {
        List<Account> accounts = List.of(account);
        Page<Account> accountPage = new PageImpl<>(accounts);

        when(accountRepository.findSystemUsersAccountsByFilters(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(accountPage);

        PageResponse<AccountDTO> response = accountService.viewSystemUserAccountsByFilter(0, 10, null, null, null, null, null);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("test@example.com", response.getContent().get(0).getEmail());
    }

    @Test
    void updateAccountByAdmin_AccountNotFound() {
        AccountDTO accountDTO = AccountDTO.builder()
                .isActive(false)
                .role(RoleDTO.builder().roleId(BigInteger.valueOf(2)).roleName("admin").build())
                .build();

        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> accountService.updateAccountByAdmin(accountDTO, BigInteger.valueOf(1)));

        assertEquals(ErrorCode.ACCOUNT_NO_CONTENT, exception.getErrorCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void register_RoleNotFound() {
        AccountRegisterDTO registerDTO = AccountRegisterDTO.builder()
                .email("newuser@example.com")
                .password("password")
                .confirmPassword("password")
                .otp("123456")
                .build();

        when(otpUtils.get(anyString())).thenReturn("123456");
        when(roleRepository.findRoleByRoleName(anyString())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> accountService.register(registerDTO));

        assertEquals(ErrorCode.ROLE_NOT_EXISTED, exception.getErrorCode());
        verify(accountRepository, never()).save(any(Account.class));
    }
    
    @Test
    void viewByFilter_Success() {
        List<Account> accounts = List.of(account);
        Page<Account> accountPage = new PageImpl<>(accounts);

        // Giả lập tài khoản đăng nhập
        CustomAccountDetails customAccountDetails = mock(CustomAccountDetails.class);
        when(customAccountDetails.getUsername()).thenReturn("test@example.com");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customAccountDetails);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(accountRepository.findAccountsByFilters(any(), any(), any(), any(), any(Pageable.class))).thenReturn(accountPage);

        PageResponse<AccountDTO> response = accountService.viewByFilter(0, 10, "test@example.com", true, BigInteger.ONE);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("test@example.com", response.getContent().get(0).getEmail());
    }

    @Test
    void updateProfile_UserNotFound() throws IOException {
        AccountDTO accountDTO = AccountDTO.builder()
                .fullname("Updated User")
                .dob(LocalDate.now())
                .phone("0123456789")
                .address("Updated Address")
                .gender(1)
                .build();

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> accountService.updateProfile("test@example.com", accountDTO, null));

        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getAuthorNewsAccounts_Success() {
        List<Account> accounts = List.of(account);

        when(accountRepository.findAccountsByNewsCreatedBy()).thenReturn(accounts);

        List<AccountDTO> result = accountService.getAuthorNewsAccounts();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test User", result.get(0).getFullname());
    }

    @Test
    void getAuthorNewsAccounts_NoContent() {
        when(accountRepository.findAccountsByNewsCreatedBy()).thenReturn(Collections.emptyList());

        AppException exception = assertThrows(AppException.class, () -> accountService.getAuthorNewsAccounts());

        assertEquals(ErrorCode.ACCOUNT_NO_CONTENT, exception.getErrorCode());
    }

    @Test
    void getIdAndEmailProjectManagerAccounts_Success() {
        List<Account> accounts = List.of(account);

        when(accountRepository.findProjectManagerAccounts()).thenReturn(accounts);

        List<AccountDTO> result = accountService.getIdAndEmailProjectManagerAccounts();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
    }

    @Test
    void getIdAndEmailProjectManagerAccounts_NoContent() {
        when(accountRepository.findProjectManagerAccounts()).thenReturn(Collections.emptyList());

        AppException exception = assertThrows(AppException.class, () -> accountService.getIdAndEmailProjectManagerAccounts());

        assertEquals(ErrorCode.ACCOUNT_NO_CONTENT, exception.getErrorCode());
    }


    @Test
    void getAccountById_Success() {
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(account));

        AccountDTO result = accountService.getAccountById(BigInteger.ONE);

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void getTotalDonateAndCountDonateByAccountCode_Success() {

        Role adminRole = new Role(BigInteger.valueOf(4), "system user", "Admin Role", LocalDateTime.now(), LocalDateTime.now(), true, null);
        Account adminAccount = new Account(BigInteger.valueOf(1), "ACC123", "REF123", "test@example.com", "password", "Admin User", 1, "0123456789", "Address", "avatar.jpg", LocalDate.now(), LocalDateTime.now(), LocalDateTime.now(), true, adminRole);

        when(accountRepository.findByCode(anyString())).thenReturn(Optional.of(adminAccount));
        when(donationRepository.sumDonationsByAccountCode(anyString())).thenReturn(BigDecimal.TEN);
        when(donationRepository.getTotalDonationByReferId(any(BigInteger.class))).thenReturn(BigDecimal.TEN);
        when(donationRepository.getTotalDonationsChallengeByCreatedBy(any(BigInteger.class))).thenReturn(BigDecimal.TEN);
        when(donationRepository.countDonationsByAccountCode(anyString())).thenReturn(1L);
        when(donationRepository.countDonationsByReferId(any(BigInteger.class))).thenReturn(1L);
        when(donationRepository.countDonationsChallengeByCreatedBy(any(BigInteger.class))).thenReturn(1L);

        AccountProfilePageDTO result = accountService.getTotalDonateAndCountDonateByAccountCode("ACC123");

        assertNotNull(result);
        assertEquals(BigDecimal.TEN, result.getTotalDonations());
        assertEquals(BigDecimal.TEN.add(BigDecimal.TEN), result.getTotalDonationsRefer());
        assertEquals(1L + 1L, result.getTotalDonationReferCount());
    }

    @Test
    void createAccount_ExistingEmail() {
        AccountDTO accountDTO = AccountDTO.builder()
                .email("existinguser@example.com")
                .build();

        when(accountRepository.existsAccountByEmail(anyString())).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> accountService.createAccount(accountDTO));

        assertEquals(ErrorCode.USER_EXISTED, exception.getErrorCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void updateAccountByAdmin_Success() {
        AccountDTO accountDTO = AccountDTO.builder()
                .isActive(false)
                .role(RoleDTO.builder().roleId(BigInteger.valueOf(2)).roleName("admin").build())
                .build();

        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(account));

        AccountDTO updatedAccount = accountService.updateAccountByAdmin(accountDTO, BigInteger.valueOf(1));

        assertNotNull(updatedAccount);
        assertFalse(updatedAccount.getIsActive());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void updateProfile_Success() throws IOException {
        AccountDTO accountDTO = AccountDTO.builder()
                .fullname("Updated User")
                .dob(LocalDate.now())
                .phone("0123456789")
                .address("Updated Address")
                .gender(1)
                .build();

        MultipartFile avatarFile = mock(MultipartFile.class);
        when(avatarFile.getContentType()).thenReturn("image/jpeg");

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(firebaseService.uploadOneFile(any(MultipartFile.class), any(BigInteger.class), anyString())).thenReturn("uploaded_avatar.jpg");

        AccountDTO updatedProfile = accountService.updateProfile("test@example.com", accountDTO, avatarFile);

        assertNotNull(updatedProfile);
        assertEquals("Updated User", updatedProfile.getFullname());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void updateProfile_InvalidFileFormat() throws IOException {
        AccountDTO accountDTO = AccountDTO.builder()
                .fullname("Updated User")
                .dob(LocalDate.now())
                .phone("0123456789")
                .address("Updated Address")
                .gender(1)
                .build();

        MultipartFile avatarFile = mock(MultipartFile.class);
        when(avatarFile.getContentType()).thenReturn("application/pdf");

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));

        AppException exception = assertThrows(AppException.class, () -> accountService.updateProfile("test@example.com", accountDTO, avatarFile));

        assertEquals(ErrorCode.HTTP_FILE_IS_NOT_IMAGE, exception.getErrorCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void deactivateAccount_Success() {
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(account));

        AccountDTO deactivatedAccount = accountService.deactivateAccount(BigInteger.valueOf(1));

        assertNotNull(deactivatedAccount);
        assertFalse(deactivatedAccount.getIsActive());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void deactivateAccount_NotFound() {
        when(accountRepository.findById(any(BigInteger.class))).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> accountService.deactivateAccount(BigInteger.valueOf(1)));

        assertEquals(ErrorCode.ACCOUNT_NO_CONTENT, exception.getErrorCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void changePassword_Success() {
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("oldPassword", account.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("newPassword", account.getPassword())).thenReturn(false); // New password is different
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");

        AccountDTO changedPasswordAccount = accountService.changePassword("test@example.com", "oldPassword", "newPassword");

        assertNotNull(changedPasswordAccount);
        assertEquals("newPassword", changedPasswordAccount.getPassword());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void changePassword_OldPasswordIncorrect() {
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("incorrectOldPassword", account.getPassword())).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> accountService.changePassword("test@example.com", "incorrectOldPassword", "newPassword"));

        assertEquals(ErrorCode.OLD_PASSWORD_INCORRECT, exception.getErrorCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getTotalDonateAndCountDonateByAccountCode_AdminAccessDenied() {
        Role userRole = new Role(BigInteger.valueOf(2), "user", "User Role", LocalDateTime.now(), LocalDateTime.now(), true, null);
        Account nonAdminAccount = new Account(BigInteger.valueOf(1), "ACC123", "REF123", "test@example.com", "password", "Test User", 1, "0123456789", "Address", "avatar.jpg", LocalDate.now(), LocalDateTime.now(), LocalDateTime.now(), true, userRole);

        when(accountRepository.findByCode(anyString())).thenReturn(Optional.of(nonAdminAccount));

        AppException exception = assertThrows(AppException.class, () -> accountService.getTotalDonateAndCountDonateByAccountCode("ACC123"));

        assertEquals(ErrorCode.ADMIN_ACCESS_DENIED, exception.getErrorCode());
    }


    @Test
    void changePassword_NewPasswordSameAsOldPassword() {
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("oldPassword", account.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("oldPassword", account.getPassword())).thenReturn(true); // New password is same as old password

        AppException exception = assertThrows(AppException.class, () -> accountService.changePassword("test@example.com", "oldPassword", "oldPassword"));

        assertEquals(ErrorCode.NEW_PASSWORD_MUST_BE_DIFFERENT, exception.getErrorCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void findAccountByEmail_Success() {
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));

        AccountDTO result = accountService.findAccountByEmail("test@example.com");

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void findAccountByEmail_NotFound() {
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> accountService.findAccountByEmail("test@example.com"));

        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void register_Success() {
        AccountRegisterDTO registerDTO = AccountRegisterDTO.builder()
                .email("newuser@example.com")
                .password("password")
                .confirmPassword("password")
                .otp("123456")
                .build();

        when(otpUtils.get(anyString())).thenReturn("123456");
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findRoleByRoleName(anyString())).thenReturn(Optional.of(role));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(codeUtils.genCode(anyString(), any())).thenReturn("ACC123");

        ApiResponse<?> response = accountService.register(registerDTO);

        assertNotNull(response);
        assertEquals("200", response.getCode());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(otpUtils, times(1)).delete(anyString());
    }

    @Test
    void register_InvalidOtp() {
        AccountRegisterDTO registerDTO = AccountRegisterDTO.builder()
                .email("newuser@example.com")
                .password("password")
                .confirmPassword("password")
                .otp("123456")
                .build();

        when(otpUtils.get(anyString())).thenReturn("654321");

        AppException exception = assertThrows(AppException.class, () -> accountService.register(registerDTO));

        assertEquals(ErrorCode.HTTP_OTP_INVALID, exception.getErrorCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void sendOtp_Success() {
        Email email = new Email("newuser@example.com", "Title", "Body");
        when(accountRepository.existsAccountByEmail(anyString())).thenReturn(false);
        when(otpUtils.generateOtp()).thenReturn("123456");

        // Mock the sendEmail method to return a completed CompletableFuture
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        when(emailService.sendEmail(any(Email.class))).thenReturn(future);

        ApiResponse<?> response = accountService.sendOtp(email);

        assertNotNull(response);
        assertEquals("200", response.getCode());
        verify(emailService, times(1)).sendEmail(any(Email.class));
        verify(otpUtils, times(1)).add(anyString(), anyString());
    }

    @Test
    void sendOtp_EmailExists() {
        Email email = new Email("existinguser@example.com", "Title", "Body");
        when(accountRepository.existsAccountByEmail(anyString())).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> accountService.sendOtp(email));

        assertEquals(ErrorCode.EXIST_EMAIL, exception.getErrorCode());
        verify(emailService, never()).sendEmail(any(Email.class));
        verify(otpUtils, never()).add(anyString(), anyString());
    }

    @Test
    void forgotPassword_Success() {
        AccountForgotPasswordDTO request = new AccountForgotPasswordDTO("test@example.com");

        // Mock the necessary methods
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedRandomPassword");

        // Mock the sendEmail method to return a completed CompletableFuture
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        when(emailService.sendEmail(any(Email.class))).thenReturn(future);

        // Call the forgot method
        ApiResponse<?> response = accountService.forgot(request);

        // Verify the results
        assertNotNull(response);
        assertEquals("200", response.getCode());
        verify(emailService, times(1)).sendEmail(any(Email.class));
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void forgotPassword_UserNotFound() {
        AccountForgotPasswordDTO request = new AccountForgotPasswordDTO("nonexistent@example.com");
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> accountService.forgot(request));

        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
        verify(emailService, never()).sendEmail(any(Email.class));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getTopAmbassador_Success() {
        // Mock the TopAmbassadorInterfaceDTO implementation
        TopAmbassadorInterfaceDTO ambassadorDTO = new TopAmbassadorInterfaceDTO() {
            @Override
            public BigInteger getAccountId() {
                return BigInteger.valueOf(1);
            }

            @Override
            public String getCode() {
                return "AMB123";
            }

            @Override
            public String getFullname() {
                return "John Doe";
            }

            @Override
            public String getAvatar() {
                return "avatar.jpg";
            }

            @Override
            public BigDecimal getTotalDonation() {
                return new BigDecimal("1000.00");
            }

            @Override
            public Long getCountDonations() {
                return 10L;
            }
        };

        List<TopAmbassadorInterfaceDTO> topAmbassadors = List.of(ambassadorDTO);

        // Mock the repository method
        when(accountRepository.getTopAmbassador(anyInt())).thenReturn(topAmbassadors);

        // Call the service method
        List<TopAmbassadorResponseDTO> result = accountService.getTopAmbassador(5);

        // Validate the result
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getFullname());
        assertEquals("AMB123", result.get(0).getCode());
        assertEquals("avatar.jpg", result.get(0).getAvatar());
        assertEquals(new BigDecimal("1000.00"), result.get(0).getTotalDonation());
        assertEquals(10L, result.get(0).getCountDonations());

        // Verify repository interaction
        verify(accountRepository, times(1)).getTopAmbassador(anyInt());
    }


    @Test
    void getTopAmbassador_EmptyList() {
        when(accountRepository.getTopAmbassador(anyInt())).thenReturn(Collections.emptyList());

        List<TopAmbassadorResponseDTO> result = accountService.getTopAmbassador(5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(accountRepository, times(1)).getTopAmbassador(anyInt());
    }

    @Test
    void getAmbassadors_Success() {
        AmbassadorInterfaceDTO ambassadorInterfaceDTO = new AmbassadorInterfaceDTO() {
            @Override
            public BigInteger getAccountId() {
                return BigInteger.valueOf(1);
            }

            @Override
            public String getFullname() {
                return "John Doe";
            }

            @Override
            public String getCode() {
                return "AMB123";
            }

            @Override
            public String getAvatar() {
                return "avatar.jpg";
            }

            @Override
            public LocalDateTime getCreatedAt() {
                return LocalDateTime.now();
            }

            @Override
            public BigDecimal getTotalDonation() {
                return new BigDecimal("1000.00");
            }

            @Override
            public Long getCountChallenges() {
                return 5L;
            }
        };

        List<AmbassadorInterfaceDTO> ambassadors = List.of(ambassadorInterfaceDTO);
        Page<AmbassadorInterfaceDTO> ambassadorPage = new PageImpl<>(ambassadors);

        when(accountRepository.findAmbassadorsByFilters(anyString(), anyString(), anyString(), anyString(), any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class)))
                .thenReturn(ambassadorPage);

        PageResponse<?> response = accountService.getAmbassadors(0, 10, "John", "AMB123", "john@example.com", "1234567890", new BigDecimal("100.00"), new BigDecimal("2000.00"));

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("John Doe", ((AmbassadorResponseDTO) response.getContent().get(0)).getFullname());
        verify(accountRepository, times(1)).findAmbassadorsByFilters(anyString(), anyString(), anyString(), anyString(), any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class));
    }

    @Test
    void getAmbassadors_EmptyList() {
        Page<AmbassadorInterfaceDTO> ambassadorPage = new PageImpl<>(Collections.emptyList());

        when(accountRepository.findAmbassadorsByFilters(anyString(), anyString(), anyString(), anyString(), any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class)))
                .thenReturn(ambassadorPage);

        PageResponse<?> response = accountService.getAmbassadors(0, 10, "John", "AMB123", "john@example.com", "1234567890", new BigDecimal("100.00"), new BigDecimal("2000.00"));

        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
        verify(accountRepository, times(1)).findAmbassadorsByFilters(anyString(), anyString(), anyString(), anyString(), any(BigDecimal.class), any(BigDecimal.class), any(Pageable.class));
    }
}
