package g28.sep_be;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.web.multipart.MultipartFile;

import vn.com.fpt.sep490_g28_summer2024_be.common.ErrorCode;
import vn.com.fpt.sep490_g28_summer2024_be.dto.pageinfo.PageResponse;
import vn.com.fpt.sep490_g28_summer2024_be.dto.project.ProjectResponseDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.tracking.GroupedTrackingImageDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.tracking.TrackingDTO;
import vn.com.fpt.sep490_g28_summer2024_be.entity.Project;
import vn.com.fpt.sep490_g28_summer2024_be.entity.Tracking;
import vn.com.fpt.sep490_g28_summer2024_be.entity.TrackingImage;
import vn.com.fpt.sep490_g28_summer2024_be.exception.AppException;
import vn.com.fpt.sep490_g28_summer2024_be.firebase.FirebaseServiceImpl;
import vn.com.fpt.sep490_g28_summer2024_be.repository.ProjectRepository;
import vn.com.fpt.sep490_g28_summer2024_be.repository.TrackingImageRepository;
import vn.com.fpt.sep490_g28_summer2024_be.repository.TrackingRepository;
import vn.com.fpt.sep490_g28_summer2024_be.service.tracking.TrackingServiceImpl;

public class DefaultTrackingServiceTest {

    @Mock
    private FirebaseServiceImpl firebaseService;

    @Mock
    private TrackingRepository trackingRepository;

    @Mock
    private TrackingImageRepository trackingImageRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private TrackingServiceImpl trackingService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void addTracking_Success() throws IOException {
        // Arrange
        TrackingDTO trackingDTO = TrackingDTO.builder()
                .title("Test Title")
                .content("Test Content")
                .date(LocalDate.now())
                .project(ProjectResponseDTO.builder().projectId(BigInteger.valueOf(1)).title("Test Project").build())
                .build();

        MultipartFile[] newImages = new MultipartFile[1];
        MultipartFile mockFile = mock(MultipartFile.class);
        newImages[0] = mockFile;

        when(mockFile.getContentType()).thenReturn("image/jpeg");

        Project project = new Project();
        project.setProjectId(BigInteger.valueOf(1));
        when(projectRepository.findById(BigInteger.valueOf(1))).thenReturn(Optional.of(project));

        Tracking tracking = new Tracking();
        tracking.setTrackingId(BigInteger.valueOf(1));
        tracking.setTitle("Test Title"); // Set title
        tracking.setContent("Test Content"); // Set content
        tracking.setDate(LocalDate.now()); // Set date
        tracking.setProject(project); // Initialize the project field
        when(trackingRepository.save(any(Tracking.class))).thenReturn(tracking);

        when(firebaseService.filesIsImage(newImages)).thenReturn(true);
        when(firebaseService.uploadMultipleFile(any(), any(), any())).thenReturn(List.of("image_url"));

        // Act
        TrackingDTO result = trackingService.addTracking(trackingDTO, newImages);

        // Assert
        assertNotNull(result);
        assertEquals("Test Title", result.getTitle());
        assertEquals("Test Content", result.getContent());
        assertEquals(trackingDTO.getDate(), result.getDate());

        verify(trackingRepository, times(1)).save(any(Tracking.class));
        verify(trackingImageRepository, times(1)).save(any(TrackingImage.class));
        verify(firebaseService, times(1)).uploadMultipleFile(any(), any(), any());
    }


    @Test
    public void addTracking_ProjectNotFound() {
        // Arrange
        TrackingDTO trackingDTO = TrackingDTO.builder()
                .project(ProjectResponseDTO.builder().projectId(BigInteger.valueOf(1)).title("Test Project").build())
                .build();

        when(projectRepository.findById(BigInteger.valueOf(1))).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            trackingService.addTracking(trackingDTO, null);
        });

        assertEquals(ErrorCode.PROJECT_NOT_EXISTED, exception.getErrorCode());
        verify(projectRepository, times(1)).findById(BigInteger.valueOf(1));
        verify(trackingRepository, never()).save(any(Tracking.class));
    }

    @Test
    public void addTracking_NullImages_Success() throws IOException {
        // Arrange
        TrackingDTO trackingDTO = TrackingDTO.builder()
                .title("Test Title")
                .content("Test Content")
                .date(LocalDate.now())
                .project(ProjectResponseDTO.builder().projectId(BigInteger.valueOf(1)).title("Test Project").build())
                .build();

        Project project = new Project();
        project.setProjectId(BigInteger.valueOf(1));
        when(projectRepository.findById(BigInteger.valueOf(1))).thenReturn(Optional.of(project));

        Tracking tracking = new Tracking();
        tracking.setTrackingId(BigInteger.valueOf(1));
        tracking.setTitle("Test Title");
        tracking.setContent("Test Content");
        tracking.setDate(LocalDate.now());
        tracking.setProject(project);
        when(trackingRepository.save(any(Tracking.class))).thenReturn(tracking);

        // Act
        TrackingDTO result = trackingService.addTracking(trackingDTO, null);

        // Assert
        assertNotNull(result);
        assertEquals("Test Title", result.getTitle());
        assertEquals("Test Content", result.getContent());

        verify(trackingRepository, times(1)).save(any(Tracking.class));
        verify(trackingImageRepository, never()).save(any(TrackingImage.class));
        verify(firebaseService, never()).uploadMultipleFile(any(), any(), any());
    }

    @Test
    public void addTracking_ImageSizeExceedsLimit_ThrowsException() throws IOException {
        // Arrange
        TrackingDTO trackingDTO = TrackingDTO.builder()
                .title("Test Title")
                .content("Test Content")
                .date(LocalDate.now())
                .project(ProjectResponseDTO.builder().projectId(BigInteger.valueOf(1)).title("Test Project").build())
                .build();

        MultipartFile[] newImages = new MultipartFile[1];
        MultipartFile mockFile = mock(MultipartFile.class);
        newImages[0] = mockFile;

        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getSize()).thenReturn(3 * 1024 * 1024L); // 3MB

        Project project = new Project();
        project.setProjectId(BigInteger.valueOf(1));
        when(projectRepository.findById(BigInteger.valueOf(1))).thenReturn(Optional.of(project));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            trackingService.addTracking(trackingDTO, newImages);
        });

        assertEquals(ErrorCode.FILE_SIZE_EXCEEDS_LIMIT, exception.getErrorCode());

        // Verify that the save method was never called
        verify(trackingRepository, never()).save(any(Tracking.class));
        verify(trackingImageRepository, never()).save(any(TrackingImage.class));
        verify(firebaseService, never()).uploadMultipleFile(any(), any(), any());
    }

    @Test
    public void addTracking_InvalidImageType_ThrowsException() throws IOException {
        // Arrange
        TrackingDTO trackingDTO = TrackingDTO.builder()
                .title("Test Title")
                .content("Test Content")
                .date(LocalDate.now())
                .project(ProjectResponseDTO.builder().projectId(BigInteger.valueOf(1)).title("Test Project").build())
                .build();

        MultipartFile[] newImages = new MultipartFile[1];
        MultipartFile mockFile = mock(MultipartFile.class);
        newImages[0] = mockFile;

        when(mockFile.getContentType()).thenReturn("application/pdf");

        Project project = new Project();
        project.setProjectId(BigInteger.valueOf(1));
        when(projectRepository.findById(BigInteger.valueOf(1))).thenReturn(Optional.of(project));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            trackingService.addTracking(trackingDTO, newImages);
        });

        assertEquals(ErrorCode.HTTP_FILE_IS_NOT_IMAGE, exception.getErrorCode());

        // Verify that the save method was never called
        verify(trackingRepository, never()).save(any(Tracking.class));
        verify(trackingImageRepository, never()).save(any(TrackingImage.class));
        verify(firebaseService, never()).uploadMultipleFile(any(), any(), any());
    }



    @Test
    public void getTrackingById_Success() {
        // Arrange
        BigInteger trackingId = BigInteger.valueOf(1);
        Tracking tracking = new Tracking();
        tracking.setTrackingId(trackingId);
        tracking.setTitle("Test Title");
        tracking.setContent("Test Content");
        tracking.setDate(LocalDate.now());
        Project project = new Project();
        project.setProjectId(BigInteger.valueOf(1));
        tracking.setProject(project);
        tracking.setTrackingImages(new ArrayList<>()); // Initialize the trackingImages list

        when(trackingRepository.findById(trackingId)).thenReturn(Optional.of(tracking));

        // Act
        TrackingDTO result = trackingService.getTrackingById(trackingId);

        // Assert
        assertNotNull(result);
        assertEquals(trackingId, result.getTrackingId());
        assertEquals("Test Title", result.getTitle());
        assertEquals("Test Content", result.getContent());

        verify(trackingRepository, times(1)).findById(trackingId);
    }

    @Test
    public void getTrackingById_TrackingNotFound() {
        // Arrange
        BigInteger trackingId = BigInteger.valueOf(1);

        when(trackingRepository.findById(trackingId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            trackingService.getTrackingById(trackingId);
        });

        assertEquals(ErrorCode.HTTP_TRACKING_NOT_FOUND, exception.getErrorCode());
        verify(trackingRepository, times(1)).findById(trackingId);
    }

    @Test
    public void updateTracking_Success() throws IOException {
        // Arrange
        BigInteger trackingId = BigInteger.valueOf(1);
        TrackingDTO trackingDTO = TrackingDTO.builder()
                .title("Updated Title")
                .content("Updated Content")
                .date(LocalDate.now())
                .build();

        MultipartFile[] newImages = new MultipartFile[1];
        MultipartFile mockFile = mock(MultipartFile.class);
        newImages[0] = mockFile;
        when(mockFile.getContentType()).thenReturn("image/jpeg");

        Tracking tracking = new Tracking();
        tracking.setTrackingId(trackingId);
        tracking.setProject(new Project());

        when(trackingRepository.findById(trackingId)).thenReturn(Optional.of(tracking));
        when(firebaseService.uploadMultipleFile(any(), any(), any())).thenReturn(List.of("new_image_url"));

        // Act
        TrackingDTO result = trackingService.updateTracking(trackingDTO, trackingId, newImages);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Content", result.getContent());

        verify(trackingRepository, times(1)).findById(trackingId);
        verify(trackingRepository, times(1)).save(any(Tracking.class));
        verify(trackingImageRepository, times(1)).save(any(TrackingImage.class));
        verify(firebaseService, times(1)).uploadMultipleFile(any(), any(), any());
    }


    @Test
    public void updateTracking_TrackingNotFound() {
        // Arrange
        BigInteger trackingId = BigInteger.valueOf(1);
        TrackingDTO trackingDTO = TrackingDTO.builder().build();

        when(trackingRepository.findById(trackingId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            trackingService.updateTracking(trackingDTO, trackingId, null);
        });

        assertEquals(ErrorCode.HTTP_TRACKING_NOT_FOUND, exception.getErrorCode());
        verify(trackingRepository, times(1)).findById(trackingId);
        verify(trackingRepository, never()).save(any(Tracking.class));
    }

    @Test
    public void viewByFilter_Success() {
        // Arrange
        int page = 0;
        int size = 10;
        String title = "Test Title";
        BigInteger projectId = BigInteger.valueOf(1);
        Pageable pageable = PageRequest.of(page, size);

        Project project = new Project();
        project.setProjectId(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        List<Tracking> trackingList = new ArrayList<>();
        Tracking tracking = new Tracking();
        tracking.setTrackingId(BigInteger.valueOf(1));
        tracking.setTitle("Test Title");
        tracking.setContent("Test Content");
        tracking.setDate(LocalDate.now());
        tracking.setProject(project); // Initialize the project field
        trackingList.add(tracking);

        Page<Tracking> pageTrackings = new PageImpl<>(trackingList, pageable, 1);
        when(trackingRepository.findTrackingByFilterAndProjectId(title, projectId, pageable)).thenReturn(pageTrackings);

        // Act
        PageResponse<TrackingDTO> response = trackingService.viewByFilter(page, size, title, projectId);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotal());
        assertEquals(page, response.getOffset());
        assertEquals(size, response.getLimit());

        verify(trackingRepository, times(1)).findTrackingByFilterAndProjectId(title, projectId, pageable);
        verify(projectRepository, times(1)).findById(projectId);
    }

    @Test
    public void viewByFilter_NoResults_ReturnsEmptyPageResponse() {
        // Arrange
        int page = 0;
        int size = 10;
        String title = "Nonexistent Title";
        BigInteger projectId = BigInteger.valueOf(1);
        Pageable pageable = PageRequest.of(page, size);

        Project project = new Project();
        project.setProjectId(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        Page<Tracking> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
        when(trackingRepository.findTrackingByFilterAndProjectId(title, projectId, pageable)).thenReturn(emptyPage);

        // Act
        PageResponse<TrackingDTO> response = trackingService.viewByFilter(page, size, title, projectId);

        // Assert
        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotal());
        assertEquals(page, response.getOffset());
        assertEquals(size, response.getLimit());

        verify(trackingRepository, times(1)).findTrackingByFilterAndProjectId(title, projectId, pageable);
    }


    @Test
    public void deleteTracking_Success() throws IOException {
        // Arrange
        BigInteger trackingId = BigInteger.valueOf(1);
        Tracking tracking = new Tracking();
        tracking.setTrackingId(trackingId);

        List<TrackingImage> images = new ArrayList<>();
        TrackingImage image = new TrackingImage();
        image.setImage("image_url");
        images.add(image);
        tracking.setTrackingImages(images);

        when(trackingRepository.findById(trackingId)).thenReturn(Optional.of(tracking));

        // Act
        trackingService.deleteTracking(trackingId);

        // Assert
        verify(trackingRepository, times(1)).findById(trackingId);
        verify(trackingRepository, times(1)).delete(tracking);
        verify(trackingImageRepository, times(1)).delete(image);
        verify(firebaseService, times(1)).deleteFileByPath("image_url");
    }

    @Test
    public void deleteTracking_NoImages_Success() throws IOException {
        // Arrange
        BigInteger trackingId = BigInteger.valueOf(1);
        Tracking tracking = new Tracking();
        tracking.setTrackingId(trackingId);
        tracking.setTrackingImages(new ArrayList<>());

        when(trackingRepository.findById(trackingId)).thenReturn(Optional.of(tracking));

        // Act
        trackingService.deleteTracking(trackingId);

        // Assert
        verify(trackingRepository, times(1)).findById(trackingId);
        verify(trackingRepository, times(1)).delete(tracking);
        verify(trackingImageRepository, never()).delete(any(TrackingImage.class));
        verify(firebaseService, never()).deleteFileByPath(anyString());
    }

    @Test
    public void deleteTracking_TrackingNotFound() throws IOException {
        // Arrange
        BigInteger trackingId = BigInteger.valueOf(1);

        when(trackingRepository.findById(trackingId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            trackingService.deleteTracking(trackingId);
        });

        assertEquals(ErrorCode.HTTP_TRACKING_NOT_FOUND, exception.getErrorCode());
        verify(trackingRepository, times(1)).findById(trackingId);
        verify(trackingRepository, never()).delete(any(Tracking.class));
        verify(trackingImageRepository, never()).delete(any(TrackingImage.class));
        verify(firebaseService, never()).deleteFileByPath(anyString());
    }

    @Test
    public void getImagesByProjectIdAndTitles_Success() {
        // Arrange
        BigInteger projectId = BigInteger.valueOf(1);
        List<String> titles = List.of("Hiện trạng", "Tiến độ", "Hoàn thiện");

        Tracking tracking = new Tracking();
        tracking.setTitle("Hiện trạng");

        TrackingImage trackingImage = new TrackingImage();
        trackingImage.setImage("image_url");
        tracking.setTrackingImages(List.of(trackingImage));

        when(trackingRepository.findByProjectIdAndTitles(projectId, titles)).thenReturn(List.of(tracking));

        // Act
        List<GroupedTrackingImageDTO> result = trackingService.getImagesByProjectIdAndTitles(projectId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Hiện trạng", result.get(0).getTitle());
        assertEquals(1, result.get(0).getImageUrls().size());
        assertEquals("image_url", result.get(0).getImageUrls().get(0));

        verify(trackingRepository, times(1)).findByProjectIdAndTitles(projectId, titles);
    }

    @Test
    public void getImagesByProjectIdAndTitles_NoImages_ReturnsEmptyList() {
        // Arrange
        BigInteger projectId = BigInteger.valueOf(1);
        List<String> titles = List.of("Hiện trạng", "Tiến độ", "Hoàn thiện");

        when(trackingRepository.findByProjectIdAndTitles(projectId, titles)).thenReturn(new ArrayList<>());

        // Act
        List<GroupedTrackingImageDTO> result = trackingService.getImagesByProjectIdAndTitles(projectId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(trackingRepository, times(1)).findByProjectIdAndTitles(projectId, titles);
    }

}
