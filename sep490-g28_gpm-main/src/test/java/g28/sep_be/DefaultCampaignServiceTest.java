package g28.sep_be;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;
import vn.com.fpt.sep490_g28_summer2024_be.common.ErrorCode;
import vn.com.fpt.sep490_g28_summer2024_be.dto.campaign.CampaignRequestDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.campaign.CampaignResponseDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.campaign.CampaignStatisticsResponse;
import vn.com.fpt.sep490_g28_summer2024_be.dto.pageinfo.PageResponse;
import vn.com.fpt.sep490_g28_summer2024_be.entity.Campaign;
import vn.com.fpt.sep490_g28_summer2024_be.exception.AppException;
import vn.com.fpt.sep490_g28_summer2024_be.firebase.FirebaseServiceImpl;
import vn.com.fpt.sep490_g28_summer2024_be.repository.CampaignRepository;
import vn.com.fpt.sep490_g28_summer2024_be.repository.ProjectRepository;
import vn.com.fpt.sep490_g28_summer2024_be.service.campaign.CampaignServiceImpl;
import vn.com.fpt.sep490_g28_summer2024_be.utils.SlugUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;

class DefaultCampaignServiceTest {

    @InjectMocks
    private CampaignServiceImpl campaignService;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private FirebaseServiceImpl firebaseService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SlugUtils slugUtils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void viewByFilter_Success() {
        int page = 0;
        int size = 10;
        String title = "Campaign Title";
        Boolean isActive = true;

        Campaign campaign = createMockCampaign(BigInteger.ONE, title, "Description", isActive);
        List<Campaign> campaigns = List.of(campaign);
        Page<Campaign> campaignPage = new PageImpl<>(campaigns, PageRequest.of(page, size), 1);

        when(campaignRepository.findCampaignsByFilters(title, isActive, PageRequest.of(page, size)))
                .thenReturn(campaignPage);

        PageResponse<?> response = campaignService.viewByFilter(page, size, title, isActive);

        assertEquals(1, response.getTotal());
        assertEquals(1, response.getContent().size());
        assertEquals(title, ((CampaignResponseDTO) response.getContent().get(0)).getTitle());
    }

    @Test
    void getCampaignById_Success() {
        BigInteger campaignId = BigInteger.ONE;
        Campaign campaign = createMockCampaign(campaignId, "Campaign Title", "Description", true);

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));

        CampaignResponseDTO response = campaignService.getCampaignById(campaignId);

        assertEquals(campaignId, response.getCampaignId());
        assertEquals("Campaign Title", response.getTitle());
    }

    @Test
    void getCampaignById_Failure() {
        BigInteger campaignId = BigInteger.ONE;

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> campaignService.getCampaignById(campaignId));

        assertEquals(ErrorCode.CAMPAIGN_NO_CONTENT, exception.getErrorCode());
    }

    @Test
    void addCampaign_Success() throws IOException {
        CampaignRequestDTO campaignDTO = CampaignRequestDTO.builder()
                .title("New Campaign")
                .description("Description")
                .build();

        MultipartFile newImage = mock(MultipartFile.class);
        when(newImage.getContentType()).thenReturn("image/jpeg");
        when(newImage.getSize()).thenReturn(1024L);
        when(firebaseService.uploadOneFile(any(MultipartFile.class), any(BigInteger.class), anyString()))
                .thenReturn("image_url");

        when(slugUtils.genSlug(anyString())).thenReturn("new-campaign-slug");
        when(campaignRepository.existsByTitle(campaignDTO.getTitle())).thenReturn(false);
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(i -> {
            Campaign campaign = i.getArgument(0);
            campaign.setCampaignId(BigInteger.ONE);
            return campaign;
        });

        CampaignResponseDTO response = campaignService.addCampaign(campaignDTO, newImage);

        assertNotNull(response);
        assertEquals("New Campaign", response.getTitle());
        assertEquals("new-campaign-slug", response.getSlug());
        assertEquals("image_url", response.getThumbnail());
        verify(campaignRepository, times(3)).save(any(Campaign.class));
        verify(firebaseService, times(1)).uploadOneFile(any(MultipartFile.class), any(BigInteger.class), anyString());
    }


    @Test
    void addCampaign_Failure_DuplicateTitle() {
        CampaignRequestDTO campaignDTO = CampaignRequestDTO.builder()
                .title("Duplicate Campaign")
                .description("Description")
                .build();

        when(campaignRepository.existsByTitle(campaignDTO.getTitle())).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> campaignService.addCampaign(campaignDTO, null));

        assertEquals(ErrorCode.DUPLICATE_TITLE, exception.getErrorCode());
    }

    @Test
    void updateCampaign_Success() throws IOException {
        BigInteger campaignId = BigInteger.ONE;
        CampaignRequestDTO campaignDTO = CampaignRequestDTO.builder()
                .title("Updated Campaign")
                .description("Updated Description")
                .build();

        Campaign campaign = createMockCampaign(campaignId, "Old Campaign", "Old Description", true);

        MultipartFile newImage = mock(MultipartFile.class);
        when(newImage.getContentType()).thenReturn("image/jpeg");
        when(newImage.getSize()).thenReturn(1024L);
        when(firebaseService.uploadOneFile(any(MultipartFile.class), any(BigInteger.class), anyString()))
                .thenReturn("image_url");

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(campaignRepository.existsByTitle(campaignDTO.getTitle())).thenReturn(false);
        when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

        CampaignResponseDTO response = campaignService.updateCampaign(campaignDTO, campaignId, newImage);

        assertNotNull(response);
        assertEquals("Updated Campaign", response.getTitle());
        verify(campaignRepository, times(1)).save(any(Campaign.class));
    }
    @Test
    void viewByFilter_NoCampaignsFound() {
        int page = 0;
        int size = 10;
        String title = "Non-existent Campaign";
        Boolean isActive = true;

        Page<Campaign> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);

        when(campaignRepository.findCampaignsByFilters(title, isActive, PageRequest.of(page, size)))
                .thenReturn(emptyPage);

        PageResponse<?> response = campaignService.viewByFilter(page, size, title, isActive);

        assertEquals(0, response.getTotal());
        assertEquals(0, response.getContent().size());
    }
    @Test
    void addCampaign_Failure_InvalidFileType() {
        CampaignRequestDTO campaignDTO = CampaignRequestDTO.builder()
                .title("New Campaign")
                .description("Description")
                .build();

        MultipartFile newImage = mock(MultipartFile.class);
        when(newImage.getContentType()).thenReturn("application/pdf"); // Invalid file type

        AppException exception = assertThrows(AppException.class, () ->
                campaignService.addCampaign(campaignDTO, newImage));

        assertEquals(ErrorCode.HTTP_FILE_IS_NOT_IMAGE, exception.getErrorCode());
    }
    @Test
    void addCampaign_Failure_FileSizeExceedsLimit() {
        CampaignRequestDTO campaignDTO = CampaignRequestDTO.builder()
                .title("New Campaign")
                .description("Description")
                .build();

        MultipartFile newImage = mock(MultipartFile.class);
        when(newImage.getContentType()).thenReturn("image/jpeg");
        when(newImage.getSize()).thenReturn(3 * 1024 * 1024L); // Exceeds 2MB limit

        AppException exception = assertThrows(AppException.class, () ->
                campaignService.addCampaign(campaignDTO, newImage));

        assertEquals(ErrorCode.FILE_SIZE_EXCEEDS_LIMIT, exception.getErrorCode());
    }

    @Test
    void updateCampaign_Success_ReplaceImage() throws IOException {
        BigInteger campaignId = BigInteger.ONE;
        CampaignRequestDTO campaignDTO = CampaignRequestDTO.builder()
                .title("Updated Campaign")
                .description("Updated Description")
                .build();

        Campaign campaign = createMockCampaign(campaignId, "Old Campaign", "Old Description", true);

        MultipartFile newImage = mock(MultipartFile.class);
        when(newImage.getContentType()).thenReturn("image/jpeg");
        when(newImage.getSize()).thenReturn(1024L);
        when(firebaseService.uploadOneFile(any(MultipartFile.class), any(BigInteger.class), anyString()))
                .thenReturn("new_image_url");

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(campaignRepository.existsByTitle(campaignDTO.getTitle())).thenReturn(false);
        when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

        CampaignResponseDTO response = campaignService.updateCampaign(campaignDTO, campaignId, newImage);

        assertNotNull(response);
        assertEquals("Updated Campaign", response.getTitle());
        assertEquals("new_image_url", response.getThumbnail());
        verify(campaignRepository, times(1)).save(any(Campaign.class));
        verify(firebaseService, times(1)).uploadOneFile(any(MultipartFile.class), any(BigInteger.class), anyString());
    }
    @Test
    void updateCampaign_Success_DeleteImage() throws IOException {
        BigInteger campaignId = BigInteger.ONE;
        CampaignRequestDTO campaignDTO = CampaignRequestDTO.builder()
                .title("Updated Campaign")
                .description("Updated Description")
                .thumbnail("") // Indicating that the thumbnail should be deleted
                .build();

        Campaign campaign = createMockCampaign(campaignId, "Old Campaign", "Old Description", true);
        campaign.setThumbnail("old_image_url");

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(campaignRepository.existsByTitle(campaignDTO.getTitle())).thenReturn(false);
        when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

        CampaignResponseDTO response = campaignService.updateCampaign(campaignDTO, campaignId, null);

        assertNotNull(response);
        assertEquals("Updated Campaign", response.getTitle());
        assertNull(response.getThumbnail());
        verify(firebaseService, times(1)).deleteFileByPath("old_image_url");
        verify(campaignRepository, times(1)).save(any(Campaign.class));
    }
    @Test
    void getCountProjectsGroupedByCampaignAndStatus_EmptyResult() {
        when(projectRepository.countProjectsGroupedByCampaignAndStatus()).thenReturn(Collections.emptyList());

        CampaignStatisticsResponse response = campaignService.getCountProjectsGroupedByCampaignAndStatus();

        assertNotNull(response);
        assertEquals(0, response.getData().size());
        assertEquals(0, response.getTotalProjects());
    }
    @Test
    void getAllCampaigns_EmptyList() {
        when(campaignRepository.findAll()).thenReturn(Collections.emptyList());

        List<CampaignResponseDTO> response = campaignService.getAllCampaigns();

        assertNotNull(response);
        assertEquals(0, response.size());
    }

    @Test
    void updateCampaign_Failure_DuplicateTitle() {
        BigInteger campaignId = BigInteger.ONE;
        CampaignRequestDTO campaignDTO = CampaignRequestDTO.builder()
                .title("Duplicate Campaign")
                .description("Description")
                .build();

        Campaign campaign = createMockCampaign(campaignId, "Old Campaign", "Old Description", true);

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(campaignRepository.existsByTitle(campaignDTO.getTitle())).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () ->
                campaignService.updateCampaign(campaignDTO, campaignId, null));

        assertEquals(ErrorCode.DUPLICATE_TITLE, exception.getErrorCode());
    }

    @Test
    void getAllCampaigns_Success() {
        Campaign campaign = createMockCampaign(BigInteger.ONE, "Campaign Title", "Description", true);

        when(campaignRepository.findAll()).thenReturn(List.of(campaign));

        List<CampaignResponseDTO> response = campaignService.getAllCampaigns();

        assertEquals(1, response.size());
        assertEquals("Campaign Title", response.get(0).getTitle());
    }

    @Test
    void getCampaignClientById_Success() {
        BigInteger campaignId = BigInteger.ONE;
        Campaign campaign = createMockCampaign(campaignId, "Campaign Title", "Description", true);

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));

        CampaignResponseDTO response = campaignService.getCampaignClientById(campaignId);

        assertEquals(campaignId, response.getCampaignId());
        assertEquals("Campaign Title", response.getTitle());
    }

    @Test
    void getCampaignClientById_Failure() {
        BigInteger campaignId = BigInteger.ONE;

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> campaignService.getCampaignClientById(campaignId));

        assertEquals(ErrorCode.CAMPAIGN_NO_CONTENT, exception.getErrorCode());
    }

    @Test
    void getCountProjectsGroupedByCampaignAndStatus_Success() {
        // Mock data returned by the repository
        Object[] result1 = new Object[]{1L, 1, 2L};  // {campaignId, status, count}
        Object[] result2 = new Object[]{1L, 2, 3L};  // {campaignId, status, count}
        List<Object[]> results = List.of(result1, result2);

        // Mock campaign
        Campaign campaign = createMockCampaign(BigInteger.valueOf(1L), "Campaign Title", "Description", true);

        // Mock repository methods
        when(projectRepository.countProjectsGroupedByCampaignAndStatus()).thenReturn(results);
        when(campaignRepository.findById(BigInteger.valueOf(1L))).thenReturn(Optional.of(campaign));

        // Call the service method
        CampaignStatisticsResponse response = campaignService.getCountProjectsGroupedByCampaignAndStatus();

        // Check the number of status groups for the campaign
        assertEquals(1, response.getData().size());  // Ensure there is 1 campaign
        assertEquals("Campaign Title", response.getData().get(0).getTitle());

        // Check individual status counts
        assertEquals(2, response.getData().get(0).getStatus1());
        assertEquals(3, response.getData().get(0).getStatus2());

        // Ensure total project count is 5 (2 + 3)
        assertEquals(5, response.getTotalProjects());
    }

    private Campaign createMockCampaign(BigInteger id, String title, String description, Boolean isActive) {
        Campaign campaign = new Campaign();
        campaign.setCampaignId(id);
        campaign.setTitle(title);
        campaign.setDescription(description);
        campaign.setCreatedAt(LocalDate.now());
        campaign.setUpdatedAt(LocalDate.now());
        campaign.setIsActive(isActive);
        return campaign;
    }
}
