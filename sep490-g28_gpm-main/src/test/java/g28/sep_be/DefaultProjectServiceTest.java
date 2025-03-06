package g28.sep_be;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import vn.com.fpt.sep490_g28_summer2024_be.common.ErrorCode;
import vn.com.fpt.sep490_g28_summer2024_be.dto.campaign.CampaignProjectsDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.campaign.CampaignResponseDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.construction.ConstructionUpdateRequestDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.pageinfo.PageResponse;
import vn.com.fpt.sep490_g28_summer2024_be.dto.project.ProjectRequestDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.project.ProjectResponseDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.project.ProjectUpdateRequestDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.project.interfacedto.ProjectInterfaceDTO;
import vn.com.fpt.sep490_g28_summer2024_be.entity.*;
import vn.com.fpt.sep490_g28_summer2024_be.exception.AppException;
import vn.com.fpt.sep490_g28_summer2024_be.firebase.FirebaseService;
import vn.com.fpt.sep490_g28_summer2024_be.repository.*;
import vn.com.fpt.sep490_g28_summer2024_be.sercurity.CustomAccountDetails;
import vn.com.fpt.sep490_g28_summer2024_be.service.project.DefaultProjectService;
import vn.com.fpt.sep490_g28_summer2024_be.utils.CodeUtils;
import vn.com.fpt.sep490_g28_summer2024_be.utils.SlugUtils;

class DefaultProjectServiceTest {

    @InjectMocks
    private DefaultProjectService projectService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private AssignRepository assignRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private SlugUtils slugUtils;
    @Mock
    private ConstructionRepository constructionRepository;
    @Mock
    private SponsorRepository sponsorRepository;

    @Mock
    private FirebaseService firebaseService;
    @Mock
    private CodeUtils codeUtils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    private ProjectInterfaceDTO createProjectInterfaceDTO() {
        return new ProjectInterfaceDTO() {
            @Override
            public BigInteger getProjectId() {
                return BigInteger.ONE;
            }

            @Override
            public String getTitle() {
                return "Project Title";
            }

            @Override
            public String getCode() {
                return "P001";
            }

            @Override
            public BigInteger getCampaignId() {
                return BigInteger.ONE;
            }

            @Override
            public String getCampaignTitle() {
                return "Campaign Title";
            }

            @Override
            public String getAddress() {
                return "Address";
            }

            @Override
            public String getWard() {
                return "Ward";
            }

            @Override
            public String getBackground() {
                return "Background Info";
            }

            @Override
            public String getDistrict() {
                return "District";
            }

            @Override
            public String getProvince() {
                return "Province";
            }

            @Override
            public BigDecimal getTotalBudget() {
                return BigDecimal.valueOf(2000);
            }

            @Override
            public BigDecimal getAmountNeededToRaise() {
                return BigDecimal.valueOf(1000);
            }

            @Override
            public BigDecimal getTotalDonation() {
                return BigDecimal.valueOf(500);
            }

            @Override
            public LocalDateTime getCreatedAt() {
                return LocalDateTime.now();
            }

            @Override
            public Integer getStatus() {
                return 1;
            }
        };
    }

    @Test
    void viewProjectCards_Success() {
        // Arrange
        Integer page = 0;
        Integer size = 10;
        String title = "Test";
        BigInteger campaignId = null;
        Integer status = null;
        Integer year = 2022;
        BigDecimal minTotalBudget = null;
        BigDecimal maxTotalBudget = null;

        Pageable pageable = PageRequest.of(page, size);

        ProjectInterfaceDTO projectInterfaceDTO = createProjectInterfaceDTO();
        Page<ProjectInterfaceDTO> projectsPage = new PageImpl<>(List.of(projectInterfaceDTO), pageable, 1);

        when(projectRepository.findProjectCards(anyString(), any(), any(), anyInt(), any(), any(), any(Pageable.class)))
                .thenReturn(projectsPage);
        when(sponsorRepository.countSponsorsByProjectId(any())).thenReturn(10);
        when(projectRepository.findById(any())).thenReturn(Optional.of(
                Project.builder()
                        .projectId(BigInteger.ONE)
                        .projectImages(Collections.singletonList(
                                ProjectImage.builder()
                                        .image("image_url")
                                        .build()))
                        .build()));

        // Act
        PageResponse<?> response = projectService.viewProjectCards(page, size, title, campaignId, status, year, minTotalBudget, maxTotalBudget);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        ProjectResponseDTO dto = (ProjectResponseDTO) response.getContent().get(0);
        assertEquals("Project Title", dto.getTitle());
        assertEquals(1, dto.getStatus());
        assertEquals("image_url", dto.getImages().get(0).getImage());
        verify(projectRepository, times(1)).findProjectCards(anyString(), any(), any(), anyInt(), any(), any(), any(Pageable.class));
    }

    @Test
    void viewByFilter_EmptyResult() {
        // Arrange
        Integer page = 0;
        Integer size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ProjectInterfaceDTO> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(projectRepository.findProjectByFilters(anyString(), any(), anyString(), anyString(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        PageResponse<?> response = projectService.viewByFilter(page, size, "Test", null, null, "Test Province", "2022");

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getContent().size());
        verify(projectRepository, times(1)).findProjectByFilters(anyString(), any(), anyString(), anyString(), any(), any(Pageable.class));
    }
    @Test
    void addProject_UnauthorizedUser() {
        // Arrange
        ProjectRequestDTO projectDTO = new ProjectRequestDTO();
        MultipartFile[] projectImages = {};
        MultipartFile[] projectFiles = {};

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            projectService.addProject(projectDTO, projectImages, projectFiles);
        });

        assertEquals(ErrorCode.HTTP_UNAUTHORIZED, exception.getErrorCode());
        verify(accountRepository, times(1)).findByEmail(anyString());
    }
    @Test
    void updateProject_DuplicateTitleDuringUpdate() {
        // Arrange
        BigInteger id = BigInteger.valueOf(1);
        ProjectUpdateRequestDTO projectDTO = new ProjectUpdateRequestDTO();
        projectDTO.setTitle("New Project Title");

        // Initialize the CampaignResponseDTO to avoid NullPointerException
        CampaignResponseDTO campaignResponseDTO = new CampaignResponseDTO();
        campaignResponseDTO.setCampaignId(BigInteger.valueOf(1));
        projectDTO.setCampaign(campaignResponseDTO);

        Project existingProject = new Project();
        existingProject.setTitle("Existing Project Title");

        when(projectRepository.findById(id)).thenReturn(Optional.of(existingProject));
        when(campaignRepository.findById(any())).thenReturn(Optional.of(new Campaign()));
        when(projectRepository.existsByTitle(anyString())).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            projectService.updateProject(projectDTO, id, null, null);
        });

        assertEquals(ErrorCode.DUPLICATE_TITLE, exception.getErrorCode());
        verify(projectRepository, times(1)).existsByTitle(anyString());
    }

    @Test
    void getProjectsByStatus_NoProjectsFound() {
        // Arrange
        when(projectRepository.findProjectByStatus(any())).thenReturn(Collections.emptyList());

        // Act
        List<CampaignProjectsDTO> result = projectService.getProjectsByStatus();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(projectRepository, times(1)).findProjectByStatus(any());
    }

    @Test
    void getProjectsByStatus_GroupedProjectsByCampaign() {
        // Arrange
        Campaign campaign1 = new Campaign();
        campaign1.setCampaignId(BigInteger.valueOf(1));
        campaign1.setTitle("Campaign 1");

        Campaign campaign2 = new Campaign();
        campaign2.setCampaignId(BigInteger.valueOf(2));
        campaign2.setTitle("Campaign 2");

        Project project1 = new Project();
        project1.setProjectId(BigInteger.valueOf(1));
        project1.setTitle("Project 1");
        project1.setCampaign(campaign1);

        Project project2 = new Project();
        project2.setProjectId(BigInteger.valueOf(2));
        project2.setTitle("Project 2");
        project2.setCampaign(campaign1);

        Project project3 = new Project();
        project3.setProjectId(BigInteger.valueOf(3));
        project3.setTitle("Project 3");
        project3.setCampaign(campaign2);

        when(projectRepository.findProjectByStatus(any())).thenReturn(List.of(project1, project2, project3));

        // Act
        List<CampaignProjectsDTO> result = projectService.getProjectsByStatus();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());  // 2 campaigns

        // Verify campaign 1
        CampaignProjectsDTO campaignDTO1 = result.stream()
                .filter(campaignDTO -> campaignDTO.getCampaignId().equals(campaign1.getCampaignId()))
                .findFirst()
                .orElse(null);

        assertNotNull(campaignDTO1);
        assertEquals("Campaign 1", campaignDTO1.getTitle());
        assertEquals(2, campaignDTO1.getProjects().size());  // Campaign 1 has 2 projects

        List<BigInteger> projectIds1 = campaignDTO1.getProjects().stream()
                .map(ProjectResponseDTO::getProjectId)
                .collect(Collectors.toList());

        assertTrue(projectIds1.contains(project1.getProjectId()));
        assertTrue(projectIds1.contains(project2.getProjectId()));

        // Verify campaign 2
        CampaignProjectsDTO campaignDTO2 = result.stream()
                .filter(campaignDTO -> campaignDTO.getCampaignId().equals(campaign2.getCampaignId()))
                .findFirst()
                .orElse(null);

        assertNotNull(campaignDTO2);
        assertEquals("Campaign 2", campaignDTO2.getTitle());
        assertEquals(1, campaignDTO2.getProjects().size());  // Campaign 2 has 1 project

        List<BigInteger> projectIds2 = campaignDTO2.getProjects().stream()
                .map(ProjectResponseDTO::getProjectId)
                .collect(Collectors.toList());

        assertTrue(projectIds2.contains(project3.getProjectId()));

        // Verify repository interaction
        verify(projectRepository, times(1)).findProjectByStatus(any());
    }



    @Test
    void updateProject_EmptyConstructionList() {
        // Arrange
        BigInteger id = BigInteger.valueOf(1);
        ProjectUpdateRequestDTO projectDTO = new ProjectUpdateRequestDTO();
        projectDTO.setConstructions(Collections.emptyList());

        // Initialize the CampaignResponseDTO to avoid NullPointerException
        CampaignResponseDTO campaignResponseDTO = new CampaignResponseDTO();
        campaignResponseDTO.setCampaignId(BigInteger.valueOf(1));
        projectDTO.setCampaign(campaignResponseDTO);

        Project existingProject = new Project();
        existingProject.setTitle("Existing Project");

        // Initialize lists in the existing project to avoid NullPointerExceptions
        existingProject.setProjectImages(new ArrayList<>());
        existingProject.setRelatedFile(new ArrayList<>());
        existingProject.setConstructions(new ArrayList<>());

        when(projectRepository.findById(id)).thenReturn(Optional.of(existingProject));
        when(campaignRepository.findById(any())).thenReturn(Optional.of(new Campaign()));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            projectService.updateProject(projectDTO, id, null, null);
        });

        assertEquals(ErrorCode.PROJECT_CONSTRUCTION_CONFLICT, exception.getErrorCode());
    }

    @Test
    void addProject_NoConstructionsProvided() throws IOException {
        // Arrange
        ProjectRequestDTO projectDTO = new ProjectRequestDTO();
        projectDTO.setTitle("Test Project");
        projectDTO.setBackground("Test Background");
        projectDTO.setAddress("123 Test Street");
        projectDTO.setWard("Test Ward");
        projectDTO.setDistrict("Test District");
        projectDTO.setProvince("Test Province");
        projectDTO.setTotalBudget("1000");  // These fields are assumed to be strings in your DTO
        projectDTO.setAmountNeededToRaise("500");
        projectDTO.setConstructions(Collections.emptyList());  // No constructions provided

        // Initialize the CampaignResponseDTO to avoid NullPointerException
        CampaignResponseDTO campaignResponseDTO = new CampaignResponseDTO();
        campaignResponseDTO.setCampaignId(BigInteger.valueOf(1));
        projectDTO.setCampaign(campaignResponseDTO);

        MultipartFile[] projectImages = {};  // No images provided
        MultipartFile[] projectFiles = {};   // No files provided

        CustomAccountDetails customAccountDetails = mock(CustomAccountDetails.class);
        when(customAccountDetails.getUsername()).thenReturn("test@example.com");

        SecurityContextHolder.getContext().setAuthentication(mock(Authentication.class));
        when(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).thenReturn(customAccountDetails);

        Role role = new Role();
        role.setRoleName("User");  // or "Admin" depending on your test case

        Account account = new Account();
        account.setRole(role);

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(campaignRepository.findById(any())).thenReturn(Optional.of(new Campaign()));
        when(projectRepository.existsByTitle(anyString())).thenReturn(false);
        when(codeUtils.genCode(anyString(), any())).thenReturn("PROJECT_CODE");
        when(slugUtils.genSlug(anyString())).thenReturn("test-project-slug");

        // Mock the firebaseService to return true for image file validation
        when(firebaseService.filesIsImage(any())).thenReturn(true);

        // Act
        ProjectResponseDTO response = projectService.addProject(projectDTO, projectImages, projectFiles);

        // Assert
        assertNotNull(response);
        assertEquals("Test Project", response.getTitle());
        verify(constructionRepository, times(0)).save(any());  // No constructions should be saved
        verify(projectRepository, times(2)).save(any(Project.class));  // Ensure the project was saved twice (before and after setting the code and slug)
        verify(budgetRepository, times(0)).save(any(Budget.class));  // No budgets provided, so nothing to save
        verify(assignRepository, times(0)).save(any(Assign.class));  // No assignments provided, so nothing to save
    }

    @Test
    void viewProjectCards_NoProjectsFound() {
        // Arrange
        Integer page = 0;
        Integer size = 10;
        String title = "Test";
        BigInteger campaignId = null;
        Integer status = null;
        Integer year = 2022;
        BigDecimal minTotalBudget = null;
        BigDecimal maxTotalBudget = null;

        Pageable pageable = PageRequest.of(page, size);
        Page<ProjectInterfaceDTO> projectsPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(projectRepository.findProjectCards(anyString(), any(), any(), anyInt(), any(), any(), any(Pageable.class)))
                .thenReturn(projectsPage);

        // Act
        PageResponse<?> response = projectService.viewProjectCards(page, size, title, campaignId, status, year, minTotalBudget, maxTotalBudget);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getContent().size());
        verify(projectRepository, times(1)).findProjectCards(anyString(), any(), any(), anyInt(), any(), any(), any(Pageable.class));
    }

    @Test
    void viewProjectCards_ProjectNotExist() {
        // Arrange
        Integer page = 0;
        Integer size = 10;
        String title = "Test";
        BigInteger campaignId = null;
        Integer status = null;
        Integer year = 2022;
        BigDecimal minTotalBudget = null;
        BigDecimal maxTotalBudget = null;

        Pageable pageable = PageRequest.of(page, size);

        ProjectInterfaceDTO projectInterfaceDTO = createProjectInterfaceDTO();
        Page<ProjectInterfaceDTO> projectsPage = new PageImpl<>(List.of(projectInterfaceDTO), pageable, 1);

        when(projectRepository.findProjectCards(anyString(), any(), any(), anyInt(), any(), any(), any(Pageable.class)))
                .thenReturn(projectsPage);
        when(sponsorRepository.countSponsorsByProjectId(any())).thenReturn(10);
        when(projectRepository.findById(any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AppException.class, () -> {
            projectService.viewProjectCards(page, size, title, campaignId, status, year, minTotalBudget, maxTotalBudget);
        });

        verify(projectRepository, times(1)).findProjectCards(anyString(), any(), any(), anyInt(), any(), any(), any(Pageable.class));
    }

    @Test
    void viewByFilter_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProjectInterfaceDTO> projectsPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(projectRepository.findProjectByFilters(anyString(), any(), anyString(), anyString(), any(), any(Pageable.class))).thenReturn(projectsPage);

        // Act
        PageResponse<?> response = projectService.viewByFilter(0, 10, "Test", null, null, "Test Province", "2022");

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getContent().size());
        verify(projectRepository, times(1)).findProjectByFilters(anyString(), any(), anyString(), anyString(), any(), any(Pageable.class));
    }

    @Test
    void viewProjectsByAccountId_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Account account = new Account();
        account.setAccountId(BigInteger.valueOf(1));
        account.setRole(new Role(BigInteger.valueOf(2), "User", "User Role", LocalDateTime.now(), LocalDateTime.now(), true, new ArrayList<>()));

        Page<ProjectInterfaceDTO> projectsPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(projectRepository.findProjectsByAccountId(any(), anyString(), any(), anyString(), anyString(), any(), any(Pageable.class))).thenReturn(projectsPage);

        // Act
        PageResponse<ProjectResponseDTO> response = projectService.viewProjectsByAccountId(0, 10, "test@example.com", "Test", null, null, "Test Province", "2022");

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getContent().size());
        verify(accountRepository, times(1)).findByEmail(anyString());
        verify(projectRepository, times(1)).findProjectsByAccountId(any(), anyString(), any(), anyString(), anyString(), any(), any(Pageable.class));
    }

    @Test
    void viewProjectsByAccountId_AdminAccessDenied() {
        // Arrange
        Account account = new Account();
        account.setRole(new Role(BigInteger.valueOf(1), "Admin", "Administrator Role", LocalDateTime.now(), LocalDateTime.now(), true, new ArrayList<>()));

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            projectService.viewProjectsByAccountId(0, 10, "test@example.com", "Test", null, null, "Test Province", "2022");
        });

        assertEquals(ErrorCode.ADMIN_ACCESS_DENIED, exception.getErrorCode());
        verify(accountRepository, times(1)).findByEmail(anyString());
    }

    @Test
    void viewProjectsByAccountId_UserNotFound() {
        // Arrange
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            projectService.viewProjectsByAccountId(0, 10, "test@example.com", "Test", null, null, "Test Province", "2022");
        });

        assertEquals(ErrorCode.HTTP_UNAUTHORIZED, exception.getErrorCode());
        verify(accountRepository, times(1)).findByEmail(anyString());
    }

    @Test
    void getProjectById_Success() {
        // Arrange
        BigInteger id = BigInteger.valueOf(1);
        Project project = new Project();
        project.setProjectId(id);
        project.setConstructions(new ArrayList<>());
        project.setProjectImages(new ArrayList<>());
        project.setRelatedFile(new ArrayList<>());
        Campaign campaign = new Campaign();
        campaign.setCampaignId(BigInteger.valueOf(1));
        campaign.setTitle("Test Campaign");
        project.setCampaign(campaign);

        when(projectRepository.findById(id)).thenReturn(Optional.of(project));

        // Act
        ProjectResponseDTO response = projectService.getProjectById(id);

        // Assert
        assertNotNull(response);
        assertEquals(id, response.getProjectId());
        verify(projectRepository, times(1)).findById(id);
    }

    @Test
    void getProjectById_NotFound() {
        // Arrange
        BigInteger id = BigInteger.valueOf(1);
        when(projectRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            projectService.getProjectById(id);
        });

        assertEquals(ErrorCode.PROJECT_NOT_EXISTED, exception.getErrorCode());
        verify(projectRepository, times(1)).findById(id);
    }

    @Test
    void addProject_Success() throws IOException {
        // Arrange
        ProjectRequestDTO projectDTO = new ProjectRequestDTO();
        projectDTO.setTitle("Test Project");
        projectDTO.setCampaign(new CampaignResponseDTO());
        projectDTO.getCampaign().setCampaignId(BigInteger.valueOf(1));
        projectDTO.setTotalBudget("1000");
        projectDTO.setAmountNeededToRaise("500");

        MultipartFile[] projectImages = {mock(MultipartFile.class)};
        MultipartFile[] projectFiles = {mock(MultipartFile.class)};

        when(projectImages[0].getContentType()).thenReturn("image/jpeg");
        when(projectFiles[0].getContentType()).thenReturn("application/pdf");

        CustomAccountDetails customAccountDetails = mock(CustomAccountDetails.class);
        when(customAccountDetails.getUsername()).thenReturn("test@example.com");

        SecurityContextHolder.getContext().setAuthentication(mock(Authentication.class));
        when(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).thenReturn(customAccountDetails);

        Account account = new Account();
        account.setRole(new Role(BigInteger.valueOf(2), "User", "User Role", LocalDateTime.now(), LocalDateTime.now(), true, new ArrayList<>()));
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(campaignRepository.findById(any())).thenReturn(Optional.of(new Campaign()));
        when(projectRepository.existsByTitle(anyString())).thenReturn(false);
        when(codeUtils.genCode(anyString(), any())).thenReturn("PROJECT_CODE");
        when(slugUtils.genSlug(anyString())).thenReturn("test-project-slug");
        when(firebaseService.filesIsImage(any())).thenReturn(true);
        when(firebaseService.uploadMultipleFile(any(), any(), any())).thenReturn(Collections.emptyList());

        // Act
        ProjectResponseDTO response = projectService.addProject(projectDTO, projectImages, projectFiles);

        // Assert
        assertNotNull(response);
        assertEquals("Test Project", response.getTitle());
        verify(projectRepository, times(2)).save(any(Project.class));
    }

    @Test
    void addProject_DuplicateTitle() throws IOException {
        // Arrange
        ProjectRequestDTO projectDTO = new ProjectRequestDTO();
        projectDTO.setTitle("Test Project");
        projectDTO.setCampaign(new CampaignResponseDTO());
        projectDTO.getCampaign().setCampaignId(BigInteger.valueOf(1));
        MultipartFile[] projectImages = {};
        MultipartFile[] projectFiles = {};
        CustomAccountDetails customAccountDetails = mock(CustomAccountDetails.class);
        when(customAccountDetails.getUsername()).thenReturn("test@example.com");

        SecurityContextHolder.getContext().setAuthentication(mock(Authentication.class));
        when(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).thenReturn(customAccountDetails);

        Account account = new Account();
        account.setRole(new Role(BigInteger.valueOf(2), "User", "User Role", LocalDateTime.now(), LocalDateTime.now(), true, new ArrayList<>()));
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(campaignRepository.findById(any())).thenReturn(Optional.of(new Campaign()));
        when(projectRepository.existsByTitle(anyString())).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            projectService.addProject(projectDTO, projectImages, projectFiles);
        });

        assertEquals(ErrorCode.DUPLICATE_TITLE, exception.getErrorCode());
        verify(projectRepository, times(1)).existsByTitle(anyString());
    }

    @Test
    void updateProject_Success() {
        // Arrange
        BigInteger id = BigInteger.valueOf(1);
        ProjectUpdateRequestDTO projectDTO = new ProjectUpdateRequestDTO();
        projectDTO.setTitle("Updated Project");
        projectDTO.setCampaign(new CampaignResponseDTO());
        projectDTO.getCampaign().setCampaignId(BigInteger.valueOf(1));
        projectDTO.setConstructions(new ArrayList<>()); // Ensure constructions is not null or empty

        ConstructionUpdateRequestDTO constructionUpdateRequestDTO = new ConstructionUpdateRequestDTO();
        constructionUpdateRequestDTO.setTitle("Construction Title");
        constructionUpdateRequestDTO.setQuantity(1);
        constructionUpdateRequestDTO.setUnit("Unit");
        constructionUpdateRequestDTO.setNote("Note");

        projectDTO.getConstructions().add(constructionUpdateRequestDTO); // Add at least one construction

        MultipartFile[] images = {};
        MultipartFile[] files = {};

        Project existingProject = new Project();
        existingProject.setProjectId(id);
        existingProject.setTitle("Existing Project");
        existingProject.setConstructions(new ArrayList<>());
        existingProject.setProjectImages(new ArrayList<>());
        existingProject.setRelatedFile(new ArrayList<>());

        when(projectRepository.findById(id)).thenReturn(Optional.of(existingProject));
        when(campaignRepository.findById(any())).thenReturn(Optional.of(new Campaign()));
        when(projectRepository.save(any(Project.class))).thenReturn(existingProject);

        // Act
        ProjectResponseDTO response = projectService.updateProject(projectDTO, id, images, files);

        // Assert
        assertNotNull(response);
        assertEquals(id, response.getProjectId());
        verify(projectRepository, times(1)).save(any(Project.class));
    }


    @Test
    void updateProject_NotFound() {
        // Arrange
        BigInteger id = BigInteger.valueOf(1);
        ProjectUpdateRequestDTO projectDTO = new ProjectUpdateRequestDTO();
        when(projectRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            projectService.updateProject(projectDTO, id, null, null);
        });

        assertEquals(ErrorCode.PROJECT_NOT_EXISTED, exception.getErrorCode());
        verify(projectRepository, times(1)).findById(id);
    }

    @Test
    void updateProjectStatus_Success() {
        // Arrange
        BigInteger id = BigInteger.valueOf(1);
        Project project = new Project();
        project.setProjectId(id);
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));

        // Act
        ProjectResponseDTO response = projectService.updateProjectStatus(id, 2);

        // Assert
        assertNotNull(response);
        assertEquals(id, response.getProjectId());
        assertEquals(2, project.getStatus());
        verify(projectRepository, times(1)).save(project);
    }

    @Test
    void updateProjectStatus_ProjectNotFound() {
        // Arrange
        BigInteger id = BigInteger.valueOf(1);
        when(projectRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            projectService.updateProjectStatus(id, 2);
        });

        assertEquals(ErrorCode.PROJECT_NOT_EXISTED, exception.getErrorCode());
        verify(projectRepository, times(1)).findById(id);
    }

    @Test
    void viewProjectsClientByCampaignId_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProjectInterfaceDTO> projectsPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(projectRepository.findProjectsClientByCampaignId(any(), any(), any(), any(), any(Pageable.class))).thenReturn(projectsPage);

        // Act
        PageResponse<ProjectResponseDTO> response = projectService.viewProjectsClientByCampaignId(0, 10, 1, BigInteger.valueOf(1), BigDecimal.valueOf(1000), BigDecimal.valueOf(5000));

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getContent().size());
        verify(projectRepository, times(1)).findProjectsClientByCampaignId(any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void getProjectDetailClient_Success() {
        // Arrange
        BigInteger id = BigInteger.valueOf(1);
        Project project = new Project();
        project.setProjectId(id);
        project.setCode("PROJECT_CODE");
        project.setTitle("Test Project");
        project.setSlug("test-project-slug");
        project.setCreatedAt(LocalDateTime.now());
        project.setTotalBudget(BigDecimal.valueOf(1000));
        project.setAmountNeededToRaise(BigDecimal.valueOf(500));

        Campaign campaign = new Campaign();
        campaign.setCampaignId(BigInteger.valueOf(1));
        campaign.setTitle("Test Campaign");
        project.setCampaign(campaign);

        ProjectInterfaceDTO projectInterfaceDTO = mock(ProjectInterfaceDTO.class);
        when(projectInterfaceDTO.getProjectId()).thenReturn(id);
        when(projectInterfaceDTO.getTotalDonation()).thenReturn(BigDecimal.ZERO);

        when(projectRepository.getProjectDetailByProjectId(id)).thenReturn(projectInterfaceDTO);
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));

        // Act
        ProjectResponseDTO response = projectService.getProjectDetailClient(id);

        // Assert
        assertNotNull(response);
        assertEquals(id, response.getProjectId());
        assertEquals("PROJECT_CODE", response.getCode());
        assertEquals("Test Project", response.getTitle());
        assertEquals("test-project-slug", response.getSlug());
        assertEquals("Test Campaign", response.getCampaign().getTitle());
        verify(projectRepository, times(1)).getProjectDetailByProjectId(id);
        verify(projectRepository, times(1)).findById(id);
    }

    @Test
    void getProjectDetailClient_NotFound() {
        // Arrange
        BigInteger id = BigInteger.valueOf(1);
        when(projectRepository.getProjectDetailByProjectId(id)).thenReturn(null);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            projectService.getProjectDetailClient(id);
        });

        assertEquals(ErrorCode.PROJECT_NOT_EXISTED, exception.getErrorCode());
        verify(projectRepository, times(1)).getProjectDetailByProjectId(id);
    }

    @Test
    void getProjectDetailClient_ProjectNotFound() {
        // Arrange
        BigInteger id = BigInteger.valueOf(1);
        ProjectInterfaceDTO projectInterfaceDTO = mock(ProjectInterfaceDTO.class);
        when(projectInterfaceDTO.getProjectId()).thenReturn(id);
        when(projectRepository.getProjectDetailByProjectId(id)).thenReturn(projectInterfaceDTO);
        when(projectRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            projectService.getProjectDetailClient(id);
        });

        assertEquals(ErrorCode.PROJECT_NOT_EXISTED, exception.getErrorCode());
        verify(projectRepository, times(1)).getProjectDetailByProjectId(id);
        verify(projectRepository, times(1)).findById(id);
    }
}
