package g28.sep_be;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import vn.com.fpt.sep490_g28_summer2024_be.common.ErrorCode;
import vn.com.fpt.sep490_g28_summer2024_be.dto.donation.DonationResponseDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.pageinfo.PageResponse;
import vn.com.fpt.sep490_g28_summer2024_be.dto.project.interfacedto.ProjectDonationInformattionDTO;
import vn.com.fpt.sep490_g28_summer2024_be.entity.Account;
import vn.com.fpt.sep490_g28_summer2024_be.entity.Donation;
import vn.com.fpt.sep490_g28_summer2024_be.entity.Project;
import vn.com.fpt.sep490_g28_summer2024_be.exception.AppException;
import vn.com.fpt.sep490_g28_summer2024_be.repository.AccountRepository;
import vn.com.fpt.sep490_g28_summer2024_be.repository.DonationRepository;
import vn.com.fpt.sep490_g28_summer2024_be.repository.ProjectRepository;
import vn.com.fpt.sep490_g28_summer2024_be.service.donation.DefaultDonationService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultDonationServiceTest {

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private DefaultDonationService donationService;

    private BigInteger projectId;
    private Donation donation;
    private ProjectDonationInformattionDTO projectInfo;
    private Account account;

    @BeforeEach
    void setUp() {
        projectId = BigInteger.ONE;

        donation = Donation.builder()
                .donationId(BigInteger.ONE)
                .bankSubAccId("BANK123")
                .createdAt(LocalDateTime.now())
                .value(new BigDecimal("1000.00"))
                .description("Test donation")
                .build();

        projectInfo = new ProjectDonationInformattionDTO() {
            @Override
            public BigInteger getProjectId() {
                return null;
            }

            @Override
            public BigDecimal getTarget() {
                return new BigDecimal("5000.00");
            }

            @Override
            public BigDecimal getTotalDonation() {
                return new BigDecimal("3000.00");
            }
        };

        account = Account.builder()
                .accountId(BigInteger.ONE)
                .email("test@example.com")
                .build();
    }

    @Test
    void viewListDonations_Success() {
        int page = 0;
        int size = 10;
        String description = "test";

        Pageable pageable = PageRequest.of(page, size);
        Page<Donation> donationsPage = new PageImpl<>(Collections.singletonList(donation), pageable, 1);

        when(donationRepository.findDonationsByFilters(projectId, description, pageable)).thenReturn(donationsPage);
        when(projectRepository.getDonationInformationTotal(projectId)).thenReturn(projectInfo);

        PageResponse<DonationResponseDTO> response = donationService.viewListDonations(page, size, projectId, description);

        assertNotNull(response);
        assertEquals(1, response.getTotal());
        assertEquals(page, response.getOffset());
        assertEquals(size, response.getLimit());
        assertEquals(1, response.getContent().size());
        assertEquals(projectInfo.getTarget(), response.getSummary().get("target"));
        assertEquals(projectInfo.getTotalDonation(), response.getSummary().get("total_donation"));

        verify(donationRepository, times(1)).findDonationsByFilters(projectId, description, pageable);
        verify(projectRepository, times(1)).getDonationInformationTotal(projectId);
    }

    @Test
    void viewListDonations_Empty() {
        int page = 0;
        int size = 10;
        String description = "test";

        Pageable pageable = PageRequest.of(page, size);
        Page<Donation> donationsPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(donationRepository.findDonationsByFilters(projectId, description, pageable)).thenReturn(donationsPage);
        when(projectRepository.getDonationInformationTotal(projectId)).thenReturn(projectInfo);

        PageResponse<DonationResponseDTO> response = donationService.viewListDonations(page, size, projectId, description);

        assertNotNull(response);
        assertEquals(0, response.getTotal());
        assertEquals(page, response.getOffset());
        assertEquals(size, response.getLimit());
        assertTrue(response.getContent().isEmpty());
        assertEquals(projectInfo.getTarget(), response.getSummary().get("target"));
        assertEquals(projectInfo.getTotalDonation(), response.getSummary().get("total_donation"));

        verify(donationRepository, times(1)).findDonationsByFilters(projectId, description, pageable);
        verify(projectRepository, times(1)).getDonationInformationTotal(projectId);
    }

    @Test
    void viewListDonationsAdmin_Success() {
        int page = 0;
        int size = 10;
        String description = "test";

        Pageable pageable = PageRequest.of(page, size);
        Page<Donation> donationsPage = new PageImpl<>(Collections.singletonList(donation), pageable, 1);

        when(donationRepository.findDonationsAdminByFilters(projectId, description, pageable)).thenReturn(donationsPage);

        PageResponse<DonationResponseDTO> response = donationService.viewListDonationsAdmin(page, size, projectId, description);

        assertNotNull(response);
        assertEquals(1, response.getTotal());
        assertEquals(page, response.getOffset());
        assertEquals(size, response.getLimit());
        assertEquals(1, response.getContent().size());

        verify(donationRepository, times(1)).findDonationsAdminByFilters(projectId, description, pageable);
    }

    @Test
    void viewListDonationsAdmin_Empty() {
        int page = 0;
        int size = 10;
        String description = "test";

        Pageable pageable = PageRequest.of(page, size);
        Page<Donation> donationsPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(donationRepository.findDonationsAdminByFilters(projectId, description, pageable)).thenReturn(donationsPage);

        PageResponse<DonationResponseDTO> response = donationService.viewListDonationsAdmin(page, size, projectId, description);

        assertNotNull(response);
        assertEquals(0, response.getTotal());
        assertEquals(page, response.getOffset());
        assertEquals(size, response.getLimit());
        assertTrue(response.getContent().isEmpty());

        verify(donationRepository, times(1)).findDonationsAdminByFilters(projectId, description, pageable);
    }

    @Test
    void viewDonationsByChallengeId_Success() {
        int page = 0;
        int size = 10;
        BigInteger challengeId = BigInteger.ONE;
        String description = "test";

        Pageable pageable = PageRequest.of(page, size);
        Page<Donation> donationsPage = new PageImpl<>(Collections.singletonList(donation), pageable, 1);

        when(donationRepository.findDonationsByChallengeIdAndDescription(challengeId, description, pageable)).thenReturn(donationsPage);

        PageResponse<DonationResponseDTO> response = donationService.viewDonationsByChallengeId(page, size, challengeId, description);

        assertNotNull(response);
        assertEquals(1, response.getTotal());
        assertEquals(page, response.getOffset());
        assertEquals(size, response.getLimit());
        assertEquals(1, response.getContent().size());

        verify(donationRepository, times(1)).findDonationsByChallengeIdAndDescription(challengeId, description, pageable);
    }

    @Test
    void viewDonationsByAccount_Success() {
        int page = 0;
        int size = 10;
        String email = "test@example.com";
        String description = "test";

        Pageable pageable = PageRequest.of(page, size);
        Page<Donation> donationsPage = new PageImpl<>(Collections.singletonList(donation), pageable, 1);

        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(donationRepository.findByCreatedByAndDescription(account.getAccountId(), description, pageable)).thenReturn(donationsPage);

        PageResponse<DonationResponseDTO> response = donationService.viewDonationsByAccount(page, size, email, description);

        assertNotNull(response);
        assertEquals(1, response.getTotal());
        assertEquals(page, response.getOffset());
        assertEquals(size, response.getLimit());
        assertEquals(1, response.getContent().size());

        verify(accountRepository, times(1)).findByEmail(email);
        verify(donationRepository, times(1)).findByCreatedByAndDescription(account.getAccountId(), description, pageable);
    }

    @Test
    void viewDonationsByAccount_Unauthorized() {
        int page = 0;
        int size = 10;
        String email = "test@example.com";
        String description = "test";

        when(accountRepository.findByEmail(email)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> {
            donationService.viewDonationsByAccount(page, size, email, description);
        });

        assertEquals(ErrorCode.HTTP_UNAUTHORIZED, exception.getErrorCode());

        verify(accountRepository, times(1)).findByEmail(email);
        verify(donationRepository, never()).findByCreatedByAndDescription(any(), any(), any());
    }

    @Test
    void viewDonationsByReferCode_Success() {
        int page = 0;
        int size = 10;
        String code = "refCode";

        when(accountRepository.findSystemUserAccountByAccountCode(code)).thenReturn(Optional.of(account));

        Pageable pageable = PageRequest.of(page, size);
        Page<Donation> donationsPage = new PageImpl<>(Collections.singletonList(donation), pageable, 1);

        when(donationRepository.getDonationsByReferId(null, account.getAccountId(), pageable)).thenReturn(donationsPage);
        when(donationRepository.getTotalDonationByReferId(account.getAccountId())).thenReturn(new BigDecimal("2000.00"));

        PageResponse<DonationResponseDTO> response = (PageResponse<DonationResponseDTO>) donationService.viewDonationsByReferCode(page, size, code);

        assertNotNull(response);
        assertEquals(1, response.getTotal());
        assertEquals(page, response.getOffset());
        assertEquals(size, response.getLimit());
        assertEquals(1, response.getContent().size());
        assertEquals(new BigDecimal("2000.00"), response.getSummary().get("total_donation_by_refer"));

        verify(accountRepository, times(1)).findSystemUserAccountByAccountCode(code);
        verify(donationRepository, times(1)).getDonationsByReferId(null, account.getAccountId(), pageable);
        verify(donationRepository, times(1)).getTotalDonationByReferId(account.getAccountId());
    }

    @Test
    void viewDonationsByReferCode_AccountNotFound() {
        int page = 0;
        int size = 10;
        String code = "refCode";

        when(accountRepository.findSystemUserAccountByAccountCode(code)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> {
            donationService.viewDonationsByReferCode(page, size, code);
        });

        assertEquals(ErrorCode.ACCOUNT_NO_CONTENT, exception.getErrorCode());

        verify(accountRepository, times(1)).findSystemUserAccountByAccountCode(code);
        verify(donationRepository, never()).getDonationsByReferId(any(), any(), any());
        verify(donationRepository, never()).getTotalDonationByReferId(any());
    }
}
