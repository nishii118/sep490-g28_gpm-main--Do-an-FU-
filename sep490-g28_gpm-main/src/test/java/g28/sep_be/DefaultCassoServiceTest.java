package g28.sep_be;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.com.fpt.sep490_g28_summer2024_be.common.AppConfig;
import vn.com.fpt.sep490_g28_summer2024_be.common.ErrorCode;
import vn.com.fpt.sep490_g28_summer2024_be.dto.casso.*;
import vn.com.fpt.sep490_g28_summer2024_be.dto.donation.DonationResponseDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.project.interfacedto.ProjectTransactionDTO;
import vn.com.fpt.sep490_g28_summer2024_be.entity.*;
import vn.com.fpt.sep490_g28_summer2024_be.exception.AppException;
import vn.com.fpt.sep490_g28_summer2024_be.repository.*;
import vn.com.fpt.sep490_g28_summer2024_be.service.casso.DefaultCassoService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultCassoServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private WrongDonationRepository wrongDonationRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private OkHttpClient okHttpClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Executor executor;

    @InjectMocks
    private DefaultCassoService cassoService;

    private TransactionDataDTO transactionDataDTO;
    private Donation donation;

    @BeforeEach
    void setUp() {
        transactionDataDTO = TransactionDataDTO.builder()
                .id(1L)
                .tid("tid123")
                .description("Donation Description")
                .amount(new BigDecimal("1000.00"))
                .when(LocalDateTime.now())
                .bankSubAccId("BANK123")
                .corresponsiveName("John Doe")
                .corresponsiveAccount("123456789")
                .corresponsiveBankId("BANKID")
                .corresponsiveBankName("Bank Name")
                .build();

        donation = Donation.builder()
                .donationId(BigInteger.ONE)
                .id("1")
                .tid("tid123")
                .value(new BigDecimal("1000.00"))
                .createdAt(LocalDateTime.now())
                .description("Donation Description")
                .bankSubAccId("BANK123")
                .corresponsiveName("John Doe")
                .corresponsiveAccount("123456789")
                .corresponsiveBankId("BANKID")
                .corresponsiveBankName("Bank Name")
                .build();
    }


    @Test
    void handleInPayment_WithRefer_Success() {
        donation.setDescription(AppConfig.REFER_PREFIX + " some description");
        when(donationRepository.save(any(Donation.class))).thenReturn(donation);
        when(accountRepository.findReferAccountByDescription(anyString())).thenReturn(new Account());

        DonationResponseDTO responseDTO = cassoService.handleInPayment(transactionDataDTO);

        assertNotNull(responseDTO);
        verify(accountRepository, times(1)).findReferAccountByDescription(anyString());
    }

    @Test
    void handleInPayment_WithChallenge_Success() {
        donation.setDescription(AppConfig.CHALLENGE_PREFIX + " some description");
        when(donationRepository.save(any(Donation.class))).thenReturn(donation);
        when(challengeRepository.findChallengeByDescription(anyString())).thenReturn(new Challenge());

        DonationResponseDTO responseDTO = cassoService.handleInPayment(transactionDataDTO);

        assertNotNull(responseDTO);
        verify(challengeRepository, times(1)).findChallengeByDescription(anyString());
    }

    @Test
    void handleInPayment_WithAccount_Success() {
        donation.setDescription(AppConfig.ACCOUNT_PREFIX + " some description");
        when(donationRepository.save(any(Donation.class))).thenReturn(donation);
        when(accountRepository.findAccountByDescription(anyString())).thenReturn(new Account());

        DonationResponseDTO responseDTO = cassoService.handleInPayment(transactionDataDTO);

        assertNotNull(responseDTO);
        verify(accountRepository, times(1)).findAccountByDescription(anyString());
    }

    @Test
    void handleOutPayment_WithProject_Success() {
        transactionDataDTO.setDescription(AppConfig.PROJECT_PREFIX + " some description");
        ProjectTransactionDTO projectTransactionDTO = mock(ProjectTransactionDTO.class);
        when(projectRepository.findProjectByCampaignIdAndDonationDescriptionAndStatus(anyString(), any(), any(), eq(true)))
                .thenReturn(projectTransactionDTO);

        DonationResponseDTO responseDTO = cassoService.handleOutPayment(transactionDataDTO);

        assertNotNull(responseDTO);
        verify(projectRepository, times(1)).findProjectByCampaignIdAndDonationDescriptionAndStatus(anyString(), any(), any(), eq(true));
    }
}
