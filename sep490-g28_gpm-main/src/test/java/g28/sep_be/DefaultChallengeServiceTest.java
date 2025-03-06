package g28.sep_be;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import vn.com.fpt.sep490_g28_summer2024_be.common.ErrorCode;
import vn.com.fpt.sep490_g28_summer2024_be.dto.challenge.ChallengeRequestDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.challenge.ChallengeResponseDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.challenge.interfacedto.ChallengeInterfaceDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.pageinfo.PageResponse;
import vn.com.fpt.sep490_g28_summer2024_be.entity.*;
import vn.com.fpt.sep490_g28_summer2024_be.exception.AppException;
import vn.com.fpt.sep490_g28_summer2024_be.firebase.FirebaseService;
import vn.com.fpt.sep490_g28_summer2024_be.repository.*;
import vn.com.fpt.sep490_g28_summer2024_be.service.challenge.ChallengerServiceImpl;
import vn.com.fpt.sep490_g28_summer2024_be.utils.CodeUtils;
import vn.com.fpt.sep490_g28_summer2024_be.utils.SlugUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DefaultChallengeServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private FirebaseService firebaseService;

    @Mock
    private CodeUtils codeUtils;

    @Mock
    private SlugUtils slugUtils;

    @Mock
    private ChallengeProjectRepository challengeProjectRepository;

    @Mock
    private DonationRepository donationRepository;

    @InjectMocks
    private ChallengerServiceImpl challengeService;

    private static final BigInteger SYSTEM_USER_ROLE_ID = BigInteger.valueOf(4);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAddChallenge_Success() throws Exception {
        ChallengeRequestDTO requestDTO = ChallengeRequestDTO.builder()
                .title("New Challenge")
                .finishedAt(LocalDate.now().plusDays(10))
                .build();

        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.getContentType()).thenReturn("image/jpeg");
        when(thumbnail.getSize()).thenReturn(1024L);

        Account account = Account.builder()
                .email("admin@example.com")
                .role(Role.builder().roleId(SYSTEM_USER_ROLE_ID).build())
                .build();

        // Ensure the saved Challenge is properly set up with all fields.
        Challenge challenge = Challenge.builder()
                .challengeId(BigInteger.ONE)
                .title("New Challenge") // Title is set
                .createdAt(LocalDateTime.now())
                .build();

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(challengeRepository.save(any(Challenge.class))).thenReturn(challenge);
        when(codeUtils.genCode(anyString(), any())).thenReturn("CH001");
        when(slugUtils.genSlug(anyString())).thenReturn("ch001-new-challenge");
        when(firebaseService.uploadOneFile(any(MultipartFile.class), any(), anyString())).thenReturn("https://firebase.com/image.jpg");

        ChallengeResponseDTO result = challengeService.addChallenge(requestDTO, "admin@example.com", thumbnail);

        assertNotNull(result);
        assertEquals("CH001", result.getChallengeCode());
        assertEquals("New Challenge", result.getTitle()); // This should now match correctly
        verify(challengeRepository, times(2)).save(any(Challenge.class));
        verify(firebaseService, times(1)).uploadOneFile(thumbnail, BigInteger.ONE, "challenge-images/thumbnail");
    }

    @Test
    void testAddChallenge_DuplicateTitle() {
        ChallengeRequestDTO requestDTO = ChallengeRequestDTO.builder()
                .title("Existing Challenge")
                .build();

        when(challengeRepository.existsByTitle(anyString())).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> {
            challengeService.addChallenge(requestDTO, "admin@example.com", null);
        });

        assertEquals(ErrorCode.DUPLICATE_TITLE, exception.getErrorCode());
        verify(challengeRepository, times(1)).existsByTitle(anyString());
    }

    @Test
    void testAddChallenge_InvalidFinishDate() {
        ChallengeRequestDTO requestDTO = ChallengeRequestDTO.builder()
                .title("New Challenge")
                .finishedAt(LocalDate.now().minusDays(1))
                .build();

        Account account = Account.builder()
                .email("admin@example.com")
                .role(Role.builder().roleId(SYSTEM_USER_ROLE_ID).build())
                .build();

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));

        AppException exception = assertThrows(AppException.class, () -> {
            challengeService.addChallenge(requestDTO, "admin@example.com", null);
        });

        assertEquals(ErrorCode.INVALID_FINISH_DATE, exception.getErrorCode());
    }

    @Test
    void testGetChallengeById_Success() {
        Challenge challenge = Challenge.builder()
                .challengeId(BigInteger.ONE)
                .title("Sample Challenge")
                .createdAt(LocalDateTime.now())
                .challengeProjects(Collections.emptyList())  // Initialize with an empty list or mock data
                .build();

        when(challengeRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(challenge));
        when(donationRepository.sumDonationsByChallengeId(any(BigInteger.class))).thenReturn(BigDecimal.TEN);

        ChallengeResponseDTO result = challengeService.getChallengeById(BigInteger.ONE);

        assertNotNull(result);
        assertEquals(BigInteger.ONE, result.getChallengeId());
        assertEquals("Sample Challenge", result.getTitle());
        assertEquals(BigDecimal.TEN, result.getTotalDonation());
    }


    @Test
    void testGetChallengeById_NotFound() {
        when(challengeRepository.findById(any(BigInteger.class))).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> {
            challengeService.getChallengeById(BigInteger.ONE);
        });

        assertEquals(ErrorCode.CHALLENGE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void testDeleteChallenge_Success() throws Exception {
        Challenge challenge = Challenge.builder()
                .challengeId(BigInteger.ONE)
                .thumbnail("https://firebase.com/image.jpg")
                .build();

        when(challengeRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(challenge));

        challengeService.deleteChallenge(BigInteger.ONE);

        verify(challengeProjectRepository, times(1)).deleteAll(anyList());
        verify(firebaseService, times(1)).deleteFileByPath(anyString());
        verify(challengeRepository, times(1)).delete(any(Challenge.class));
    }

    @Test
    void testDeleteChallenge_NotFound() {
        when(challengeRepository.findById(any(BigInteger.class))).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> {
            challengeService.deleteChallenge(BigInteger.ONE);
        });

        assertEquals(ErrorCode.CHALLENGE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void testUpdateChallenge_Success() throws Exception {
        ChallengeRequestDTO requestDTO = ChallengeRequestDTO.builder()
                .title("Updated Challenge")
                .finishedAt(LocalDate.now().plusDays(5))
                .build();

        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.getContentType()).thenReturn("image/jpeg");
        when(thumbnail.getSize()).thenReturn(1024L);

        Account account = Account.builder()
                .email("admin@example.com")
                .role(Role.builder().roleId(SYSTEM_USER_ROLE_ID).build())
                .build();

        Challenge challenge = Challenge.builder()
                .challengeId(BigInteger.ONE)
                .createdBy(account)
                .finishedAt(LocalDate.now().plusDays(10))
                .build();

        when(challengeRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(challenge));
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(challengeRepository.save(any(Challenge.class))).thenReturn(challenge);

        ChallengeResponseDTO result = challengeService.updateChallenge(requestDTO, BigInteger.ONE, thumbnail, "admin@example.com");

        assertNotNull(result);
        assertEquals("Updated Challenge", result.getTitle());
        verify(challengeRepository, times(1)).save(any(Challenge.class));
    }

    @Test
    void testUpdateChallenge_NotFound() {
        ChallengeRequestDTO requestDTO = ChallengeRequestDTO.builder().build();

        when(challengeRepository.findById(any(BigInteger.class))).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> {
            challengeService.updateChallenge(requestDTO, BigInteger.ONE, null, "admin@example.com");
        });

        assertEquals(ErrorCode.CHALLENGE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void testUpdateChallenge_Forbidden() {
        ChallengeRequestDTO requestDTO = ChallengeRequestDTO.builder().build();

        Account account = Account.builder()
                .email("admin@example.com")
                .role(Role.builder().roleId(SYSTEM_USER_ROLE_ID).build())
                .build();

        Challenge challenge = Challenge.builder()
                .challengeId(BigInteger.ONE)
                .createdBy(Account.builder().build()) // Different account
                .build();

        when(challengeRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(challenge));
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));

        AppException exception = assertThrows(AppException.class, () -> {
            challengeService.updateChallenge(requestDTO, BigInteger.ONE, null, "admin@example.com");
        });

        assertEquals(ErrorCode.HTTP_UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void testViewActiveChallengesByFilter_Success() {
        Page<ChallengeInterfaceDTO> challengePage = new PageImpl<>(Collections.emptyList());
        when(challengeRepository.findOngoingChallengesByAccountCode(anyString(), any(Pageable.class)))
                .thenReturn(challengePage);

        PageResponse<ChallengeResponseDTO> result = challengeService.viewActiveChallengesByFilter(0, 10, "ACC001");

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void testViewExpiredChallengesByFilter_Success() {
        Page<ChallengeInterfaceDTO> challengePage = new PageImpl<>(Collections.emptyList());
        when(challengeRepository.findExpiredChallengesByAccountCode(anyString(), any(Pageable.class)))
                .thenReturn(challengePage);

        PageResponse<ChallengeResponseDTO> result = challengeService.viewExpiredChallengesByFilter(0, 10, "ACC001");

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void testGetTopChallenges_Success() {
        when(challengeRepository.getTopChallenge(anyInt())).thenReturn(Collections.emptyList());

        List<ChallengeResponseDTO> result = challengeService.getTopChallenges(5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testAddChallenge_FileIsNotImage() {
        ChallengeRequestDTO requestDTO = ChallengeRequestDTO.builder()
                .title("New Challenge")
                .finishedAt(LocalDate.now().plusDays(10))
                .build();

        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.getContentType()).thenReturn("application/pdf"); // Not an image

        Account account = Account.builder()
                .email("admin@example.com")
                .role(Role.builder().roleId(SYSTEM_USER_ROLE_ID).build())
                .build();

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));

        // Ensure challengeRepository.save() is mocked to return a non-null Challenge
        Challenge challenge = Challenge.builder()
                .challengeId(BigInteger.ONE)
                .build();
        when(challengeRepository.save(any(Challenge.class))).thenReturn(challenge);

        AppException exception = assertThrows(AppException.class, () -> {
            challengeService.addChallenge(requestDTO, "admin@example.com", thumbnail);
        });

        assertEquals(ErrorCode.HTTP_FILE_IS_NOT_IMAGE, exception.getErrorCode());
    }


    @Test
    void testAddChallenge_FileSizeExceedsLimit() {
        ChallengeRequestDTO requestDTO = ChallengeRequestDTO.builder()
                .title("New Challenge")
                .finishedAt(LocalDate.now().plusDays(10))
                .build();

        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.getContentType()).thenReturn("image/jpeg");
        when(thumbnail.getSize()).thenReturn(3 * 1024 * 1024L); // 3MB, exceeds 2MB limit

        Account account = Account.builder()
                .email("admin@example.com")
                .role(Role.builder().roleId(SYSTEM_USER_ROLE_ID).build())
                .build();

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));

        // Properly mock the save operation to return a valid Challenge object.
        Challenge challenge = Challenge.builder()
                .challengeId(BigInteger.ONE)
                .title("New Challenge")
                .build();
        when(challengeRepository.save(any(Challenge.class))).thenReturn(challenge);

        AppException exception = assertThrows(AppException.class, () -> {
            challengeService.addChallenge(requestDTO, "admin@example.com", thumbnail);
        });

        assertEquals(ErrorCode.FILE_SIZE_EXCEEDS_LIMIT, exception.getErrorCode());
    }


    @Test
    void testUpdateChallenge_FileUploadFailed() throws Exception {
        ChallengeRequestDTO requestDTO = ChallengeRequestDTO.builder()
                .title("Updated Challenge")
                .finishedAt(LocalDate.now().plusDays(5))
                .build();

        MultipartFile thumbnail = mock(MultipartFile.class);
        when(thumbnail.getContentType()).thenReturn("image/jpeg");
        when(thumbnail.getSize()).thenReturn(1024L);

        Account account = Account.builder()
                .email("admin@example.com")
                .role(Role.builder().roleId(SYSTEM_USER_ROLE_ID).build())
                .build();

        Challenge challenge = Challenge.builder()
                .challengeId(BigInteger.ONE)
                .createdBy(account)
                .finishedAt(LocalDate.now().plusDays(10))
                .build();

        when(challengeRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(challenge));
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(firebaseService.uploadOneFile(any(MultipartFile.class), any(), anyString())).thenThrow(new IOException("Upload failed"));

        AppException exception = assertThrows(AppException.class, () -> {
            challengeService.updateChallenge(requestDTO, BigInteger.ONE, thumbnail, "admin@example.com");
        });

        assertEquals(ErrorCode.UPLOAD_FAILED, exception.getErrorCode());
    }

    @Test
    void testDeleteChallenge_FileDeleteFailed() throws Exception {
        Challenge challenge = Challenge.builder()
                .challengeId(BigInteger.ONE)
                .thumbnail("https://firebase.com/image.jpg")
                .build();

        when(challengeRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(challenge));
        doThrow(new IOException("Delete failed")).when(firebaseService).deleteFileByPath(anyString());

        AppException exception = assertThrows(AppException.class, () -> {
            challengeService.deleteChallenge(BigInteger.ONE);
        });

        assertEquals(ErrorCode.DELETE_FILE_FAILED, exception.getErrorCode());
    }
}
