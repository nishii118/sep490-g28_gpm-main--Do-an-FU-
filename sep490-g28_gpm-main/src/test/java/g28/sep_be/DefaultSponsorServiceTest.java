package g28.sep_be;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import vn.com.fpt.sep490_g28_summer2024_be.common.ErrorCode;
import vn.com.fpt.sep490_g28_summer2024_be.dto.pageinfo.PageResponse;
import vn.com.fpt.sep490_g28_summer2024_be.dto.sponsor.SponsorRequestDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.sponsor.SponsorResponseDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.sponsor.SponsorUpdateRequestDTO;
import vn.com.fpt.sep490_g28_summer2024_be.entity.Account;
import vn.com.fpt.sep490_g28_summer2024_be.entity.Project;
import vn.com.fpt.sep490_g28_summer2024_be.entity.Role;
import vn.com.fpt.sep490_g28_summer2024_be.entity.Sponsor;
import vn.com.fpt.sep490_g28_summer2024_be.exception.AppException;
import vn.com.fpt.sep490_g28_summer2024_be.firebase.FirebaseService;
import vn.com.fpt.sep490_g28_summer2024_be.repository.AccountRepository;
import vn.com.fpt.sep490_g28_summer2024_be.repository.ProjectRepository;
import vn.com.fpt.sep490_g28_summer2024_be.repository.SponsorRepository;
import vn.com.fpt.sep490_g28_summer2024_be.sercurity.CustomAccountDetails;
import vn.com.fpt.sep490_g28_summer2024_be.service.sponsor.DefaultSponsorService;

public class DefaultSponsorServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SponsorRepository sponsorRepository;

    @Mock
    private FirebaseService firebaseService;

    @InjectMocks
    private DefaultSponsorService sponsorService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        CustomAccountDetails customAccountDetails = mock(CustomAccountDetails.class);
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(customAccountDetails);
        SecurityContextHolder.setContext(securityContext);

        Account account = new Account();
        account.setEmail("test@example.com");
        account.setRole(new Role(BigInteger.valueOf(1), "admin", "Administrator", LocalDateTime.now(), LocalDateTime.now(), true, new ArrayList<>()));

        when(customAccountDetails.getUsername()).thenReturn("test@example.com");
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
    }

    @Test
    public void addSponsorToProject_Success() throws IOException {
        // Arrange
        SponsorRequestDTO request = new SponsorRequestDTO();
        request.setCompanyName("Test Company");
        request.setBusinessField("Test Field");
        request.setRepresentative("Test Rep");
        request.setRepresentativeEmail("test@example.com");
        request.setPhoneNumber("1234567890");
        request.setValue("1000");
        request.setNote("Test Note");

        BigInteger projectId = BigInteger.valueOf(1);
        MultipartFile contract = mock(MultipartFile.class);
        when(contract.getSize()).thenReturn(1L);
        when(contract.getContentType()).thenReturn("application/pdf");

        Project project = new Project();
        project.setProjectId(projectId);
        project.setTotalBudget(new BigDecimal("10000"));
        project.setAmountNeededToRaise(new BigDecimal("9000"));
        project.setSponsors(new ArrayList<>());  // Initialize the sponsors list
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        Sponsor sponsor = Sponsor.builder()
                .companyName(request.getCompanyName())
                .businessField(request.getBusinessField())
                .project(project)
                .representative(request.getRepresentative())
                .representativeEmail(request.getRepresentativeEmail())
                .phoneNumber(request.getPhoneNumber())
                .value(new BigDecimal(request.getValue()))
                .note(request.getNote())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(sponsorRepository.save(any(Sponsor.class))).thenAnswer(invocation -> {
            Sponsor savedSponsor = invocation.getArgument(0);
            savedSponsor.setSponsorId(BigInteger.valueOf(1));
            return savedSponsor;
        });
        when(firebaseService.uploadOneFile(any(MultipartFile.class), any(BigInteger.class), anyString())).thenReturn("test_contract.pdf");

        // Act
        SponsorResponseDTO response = sponsorService.addSponsorToProject(request, projectId, contract);

        // Assert
        assertNotNull(response);
        assertEquals(BigInteger.valueOf(1), response.getSponsorId());

        verify(projectRepository, times(1)).findById(projectId);
        verify(sponsorRepository, times(2)).save(any(Sponsor.class));
        verify(firebaseService, times(1)).uploadOneFile(any(MultipartFile.class), eq(BigInteger.valueOf(1)), anyString());
    }



    @Test
    public void addSponsorToProject_ProjectNotFound() throws IOException {
        // Arrange
        SponsorRequestDTO request = new SponsorRequestDTO();
        BigInteger projectId = BigInteger.valueOf(1);

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            sponsorService.addSponsorToProject(request, projectId, null);
        });

        assertEquals(ErrorCode.PROJECT_NOT_EXISTED, exception.getErrorCode());
        verify(projectRepository, times(1)).findById(projectId);
        verify(sponsorRepository, never()).save(any(Sponsor.class));
        verify(firebaseService, never()).uploadOneFile(any(MultipartFile.class), any(BigInteger.class), anyString());
    }



    @Test
    public void viewDetail_Success() {
        // Arrange
        BigInteger sponsorId = BigInteger.valueOf(1);
        Sponsor sponsor = new Sponsor();
        sponsor.setSponsorId(sponsorId);
        sponsor.setCompanyName("Test Company");
        sponsor.setBusinessField("Test Field");
        sponsor.setRepresentative("Test Rep");
        sponsor.setRepresentativeEmail("test@example.com");
        sponsor.setPhoneNumber("1234567890");
        sponsor.setValue(new BigDecimal("1000"));
        sponsor.setContract("test_contract.pdf");

        when(sponsorRepository.findById(sponsorId)).thenReturn(Optional.of(sponsor));

        // Act
        SponsorResponseDTO response = sponsorService.viewDetail(sponsorId);

        // Assert
        assertNotNull(response);
        assertEquals(sponsorId, response.getSponsorId());
        assertEquals("Test Company", response.getCompanyName());
        assertEquals("Test Field", response.getBusinessField());
        assertEquals("Test Rep", response.getRepresentative());
        assertEquals("test@example.com", response.getRepresentativeEmail());
        assertEquals("1234567890", response.getPhoneNumber());
        assertEquals("1000", response.getValue());
        assertEquals("test_contract.pdf", response.getContract());

        verify(sponsorRepository, times(1)).findById(sponsorId);
    }
    @Test
    public void deleteSponsor_UpdatesProjectAmountNeededToRaise() {
        // Arrange
        BigInteger sponsorId = BigInteger.valueOf(1);

        Project project = new Project();
        project.setProjectId(BigInteger.valueOf(1));
        project.setTotalBudget(new BigDecimal("10000"));
        project.setAmountNeededToRaise(new BigDecimal("9000"));

        Sponsor sponsor = new Sponsor();
        sponsor.setSponsorId(sponsorId);
        sponsor.setValue(new BigDecimal("1000"));
        sponsor.setProject(project);

        when(sponsorRepository.findById(sponsorId)).thenReturn(Optional.of(sponsor));

        // Act
        sponsorService.delete(sponsorId);

        // Assert
        verify(sponsorRepository, times(1)).findById(sponsorId);
        verify(projectRepository, times(1)).save(any(Project.class));
        verify(sponsorRepository, times(1)).delete(sponsor);

        assertEquals(new BigDecimal("10000"), project.getAmountNeededToRaise());
    }

    @Test
    public void viewDetail_SponsorNotFound() {
        // Arrange
        BigInteger sponsorId = BigInteger.valueOf(1);

        when(sponsorRepository.findById(sponsorId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            sponsorService.viewDetail(sponsorId);
        });

        assertEquals(ErrorCode.SPONSOR_NOT_EXIST, exception.getErrorCode());
        verify(sponsorRepository, times(1)).findById(sponsorId);
    }
    @Test
    public void viewListSponsorInProject_NoSponsors_ReturnsEmptyList() {
        // Arrange
        int page = 0;
        int size = 10;
        String companyName = "Nonexistent Company";
        BigInteger projectId = BigInteger.valueOf(1);
        Pageable pageable = PageRequest.of(page, size);

        Project project = new Project();
        project.setProjectId(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        Page<Sponsor> sponsors = new PageImpl<>(new ArrayList<>(), pageable, 0);
        when(sponsorRepository.findSponsorsByFilters(anyString(), eq(projectId), eq(pageable))).thenReturn(sponsors);

        // Act
        PageResponse<?> response = sponsorService.viewListSponsorInProject(page, size, companyName, projectId);

        // Assert
        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
        assertEquals(page, response.getOffset());
        assertEquals(size, response.getLimit());

        verify(projectRepository, times(1)).findById(projectId);
        verify(sponsorRepository, times(1)).findSponsorsByFilters(anyString(), eq(projectId), eq(pageable));
    }

    @Test
    public void viewListSponsorInProject_Success() {
        // Arrange
        int page = 0;
        int size = 10;
        String companyName = "Test Company";
        BigInteger projectId = BigInteger.valueOf(1);
        Pageable pageable = PageRequest.of(page, size);

        Project project = new Project();
        project.setProjectId(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        Page<Sponsor> sponsors = new PageImpl<>(new ArrayList<>(), pageable, 0);
        when(sponsorRepository.findSponsorsByFilters(anyString(), eq(projectId), eq(pageable))).thenReturn(sponsors);

        // Act
        PageResponse<?> response = sponsorService.viewListSponsorInProject(page, size, companyName, projectId);

        // Assert
        assertNotNull(response);
        assertEquals(page, response.getOffset());
        assertEquals(size, response.getLimit());

        verify(projectRepository, times(1)).findById(projectId);
        verify(sponsorRepository, times(1)).findSponsorsByFilters(anyString(), eq(projectId), eq(pageable));
    }

    @Test
    public void updateSponsor_Success() throws IOException {
        // Arrange
        BigInteger sponsorId = BigInteger.valueOf(1);
        SponsorUpdateRequestDTO request = new SponsorUpdateRequestDTO();
        request.setCompanyName("Updated Company");
        request.setBusinessField("Updated Field");
        request.setRepresentative("Updated Rep");
        request.setRepresentativeEmail("updated@example.com");
        request.setPhoneNumber("0987654321");
        request.setValue("1500");
        request.setNote("Updated Note");

        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(1L);
        when(file.getContentType()).thenReturn("application/pdf");

        Project project = new Project();
        project.setProjectId(BigInteger.valueOf(1));
        project.setTotalBudget(new BigDecimal("10000"));
        project.setAmountNeededToRaise(new BigDecimal("9000"));
        project.setSponsors(new ArrayList<>());  // Initialize the sponsors list

        Sponsor sponsor = new Sponsor();
        sponsor.setSponsorId(sponsorId);
        sponsor.setProject(project);

        when(projectRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(project));
        when(sponsorRepository.findById(sponsorId)).thenReturn(Optional.of(sponsor));

        when(firebaseService.uploadOneFile(any(MultipartFile.class), eq(sponsorId), anyString())).thenReturn("updated_contract.pdf");

        // Act
        SponsorResponseDTO response = sponsorService.update(sponsorId, request, file);

        // Assert
        assertNotNull(response);
        assertEquals(sponsorId, response.getSponsorId());
        assertEquals("Updated Company", response.getCompanyName());

        verify(sponsorRepository, times(1)).findById(sponsorId);
        verify(sponsorRepository, times(2)).save(any(Sponsor.class));
        verify(firebaseService, times(1)).uploadOneFile(any(MultipartFile.class), eq(sponsorId), anyString());

        Sponsor updatedSponsor = sponsorRepository.findById(sponsorId).get();
        assertEquals("Updated Company", updatedSponsor.getCompanyName());
        assertEquals("Updated Field", updatedSponsor.getBusinessField());
        assertEquals("Updated Rep", updatedSponsor.getRepresentative());
        assertEquals("updated@example.com", updatedSponsor.getRepresentativeEmail());
        assertEquals("0987654321", updatedSponsor.getPhoneNumber());
        assertEquals(new BigDecimal("1500"), updatedSponsor.getValue());
        assertEquals("Updated Note", updatedSponsor.getNote());
        assertEquals("updated_contract.pdf", updatedSponsor.getContract());
    }

    @Test
    public void updateSponsor_SponsorNotFound() throws IOException {
        // Arrange
        BigInteger sponsorId = BigInteger.valueOf(1);
        SponsorUpdateRequestDTO request = new SponsorUpdateRequestDTO();

        when(sponsorRepository.findById(sponsorId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            sponsorService.update(sponsorId, request, null);
        });

        assertEquals(ErrorCode.SPONSOR_NOT_EXIST, exception.getErrorCode());
        verify(sponsorRepository, times(1)).findById(sponsorId);
        verify(sponsorRepository, never()).save(any(Sponsor.class));
        verify(firebaseService, never()).uploadOneFile(any(MultipartFile.class), any(BigInteger.class), anyString());
    }

    @Test
    public void deleteSponsor_Success() {
        // Arrange
        BigInteger sponsorId = BigInteger.valueOf(1);

        Project project = new Project();
        project.setProjectId(BigInteger.valueOf(1));
        project.setTotalBudget(new BigDecimal("10000"));
        project.setAmountNeededToRaise(new BigDecimal("9000"));

        Sponsor sponsor = new Sponsor();
        sponsor.setSponsorId(sponsorId);
        sponsor.setValue(new BigDecimal("1000"));
        sponsor.setProject(project);

        project.setSponsors(new ArrayList<>());
        project.getSponsors().add(sponsor);

        when(sponsorRepository.findById(sponsorId)).thenReturn(Optional.of(sponsor));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        sponsorService.delete(sponsorId);

        // Assert
        verify(sponsorRepository, times(1)).findById(sponsorId);
        verify(projectRepository, times(1)).save(any(Project.class));
        verify(sponsorRepository, times(1)).delete(sponsor);

        assertEquals(new BigDecimal("10000"), project.getAmountNeededToRaise());
    }

    @Test
    public void deleteSponsor_SponsorNotFound() {
        // Arrange
        BigInteger sponsorId = BigInteger.valueOf(1);

        when(sponsorRepository.findById(sponsorId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            sponsorService.delete(sponsorId);
        });

        assertEquals(ErrorCode.SPONSOR_NOT_EXIST, exception.getErrorCode());
        verify(sponsorRepository, times(1)).findById(sponsorId);
        verify(projectRepository, never()).save(any(Project.class));
        verify(sponsorRepository, never()).delete(any(Sponsor.class));
    }
}
