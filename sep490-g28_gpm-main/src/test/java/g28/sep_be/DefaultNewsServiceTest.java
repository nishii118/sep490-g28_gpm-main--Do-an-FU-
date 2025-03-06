package g28.sep_be;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.multipart.MultipartFile;
import vn.com.fpt.sep490_g28_summer2024_be.dto.authentication.UserResponse;
import vn.com.fpt.sep490_g28_summer2024_be.dto.category.CategoryResponseDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.news.*;
import vn.com.fpt.sep490_g28_summer2024_be.dto.pageinfo.PageResponse;
import vn.com.fpt.sep490_g28_summer2024_be.entity.*;
import vn.com.fpt.sep490_g28_summer2024_be.exception.AppException;
import vn.com.fpt.sep490_g28_summer2024_be.firebase.FirebaseService;
import vn.com.fpt.sep490_g28_summer2024_be.repository.*;
import vn.com.fpt.sep490_g28_summer2024_be.sercurity.CustomAccountDetails;
import vn.com.fpt.sep490_g28_summer2024_be.service.news.DefaultNewsService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

@ExtendWith(MockitoExtension.class)
public class DefaultNewsServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private FirebaseService firebaseService;

    @InjectMocks
    private DefaultNewsService defaultNewsService;

    @Captor
    private ArgumentCaptor<News> newsCaptor;

    private CustomAccountDetails customAccountDetails;
    private Account loggedAccount;

    @BeforeEach
    public void setUp() {
        UserResponse userResponse = UserResponse.builder()
                .email("test@example.com")
                .password("password")
                .fullname("Test User")
                .isActive(true)
                .scope("admin")
                .build();

        customAccountDetails = new CustomAccountDetails(userResponse);

        loggedAccount = new Account();
        loggedAccount.setEmail("test@example.com");
        loggedAccount.setAccountId(BigInteger.valueOf(1));
        Role role = new Role();
        role.setRoleName("Social Staff");
        loggedAccount.setRole(role);

        SecurityContextHolder.setContext(new SecurityContextImpl());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(customAccountDetails, null));
    }




    @Test
    public void testViewDetailOfNonExistentNews() {
        when(newsRepository.findById(any(BigInteger.class))).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> defaultNewsService.viewDetail(BigInteger.valueOf(99)));
    }

    @Test
    public void testViewNewsClientByFilterWithNoMatchingResults() {
        Page<News> emptyPage = new PageImpl<>(Collections.emptyList());

        when(newsRepository.findNewsByTitleAndCategories(any(), any(), any())).thenReturn(emptyPage);

        PageResponse<?> response = defaultNewsService.viewNewsClientByFilter(0, 10, null, "Non-existent title");

        assertNotNull(response);
        assertEquals(0, response.getContent().size());
    }

    @Test
    public void testUpdateNewsStatusWithInvalidStatus() {
        NewsChangeStatusDTO statusDTO = new NewsChangeStatusDTO();
        statusDTO.setNewsId(BigInteger.valueOf(1));
        statusDTO.setStatus(99); // Invalid status

        News existingNews = new News();

        when(newsRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(existingNews));

        assertThrows(AppException.class, () -> defaultNewsService.update(statusDTO));
    }


    @Test
    public void testCreateNewsWithDuplicateTitle() {
        NewsDTO newsDTO = new NewsDTO();
        newsDTO.setTitle("Duplicate News");

        when(newsRepository.findByTitle(anyString())).thenReturn(Optional.of(new News()));

        assertThrows(AppException.class, () -> defaultNewsService.create(newsDTO, null));
    }



    @Test
    public void testUpdateNewsStatusSuccessfully() {
        NewsChangeStatusDTO statusDTO = new NewsChangeStatusDTO();
        statusDTO.setNewsId(BigInteger.valueOf(1));
        statusDTO.setStatus(2);

        News existingNews = new News();
        existingNews.setCategory(new Category());

        when(newsRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(existingNews));
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(loggedAccount));
        when(newsRepository.save(any(News.class))).thenReturn(existingNews);

        NewsResponseDTO response = defaultNewsService.update(statusDTO);

        verify(newsRepository, times(1)).save(newsCaptor.capture());
        News savedNews = newsCaptor.getValue();

        assertEquals(2, savedNews.getStatus());
        assertEquals(loggedAccount, savedNews.getUpdatedBy());
        assertNotNull(response);
    }

    @Test
    public void testViewDetailSuccessfully() {
        News existingNews = new News();
        existingNews.setNewsId(BigInteger.valueOf(1));
        existingNews.setTitle("Detail News");
        Category category = new Category();
        category.setCategoryId(BigInteger.valueOf(1));
        existingNews.setCategory(category);

        when(newsRepository.findById(any(BigInteger.class))).thenReturn(Optional.of(existingNews));

        NewsResponseDTO response = defaultNewsService.viewDetail(BigInteger.valueOf(1));

        assertNotNull(response);
        assertEquals("Detail News", response.getTitle());
    }

    @Test
    public void testViewDetailWithNonExistentNews() {
        when(newsRepository.findById(any(BigInteger.class))).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> defaultNewsService.viewDetail(BigInteger.valueOf(1)));
    }

    @Test
    public void testViewNewsByAccountSuccessfully() {
        List<News> newsList = Arrays.asList(new News(), new News());

        for (News news : newsList) {
            Category category = new Category();
            category.setCategoryId(BigInteger.valueOf(1));
            category.setTitle("Test Category");

            Account createdBy = new Account();
            createdBy.setAccountId(BigInteger.valueOf(1));
            createdBy.setFullname("Created User");

            Account updatedBy = new Account();
            updatedBy.setAccountId(BigInteger.valueOf(2));
            updatedBy.setFullname("Updated User");

            news.setCategory(category);
            news.setCreatedBy(createdBy); // Ensure createdBy is set
            news.setUpdatedBy(updatedBy); // Ensure updatedBy is set
        }

        Page<News> newsPage = new PageImpl<>(newsList);

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(loggedAccount));
        when(newsRepository.findNewsByAccount(any(), any(), any(), any(), any(), any(), any())).thenReturn(newsPage);

        PageResponse<?> response = defaultNewsService.viewNewsByAccount(0, 10, "test@example.com", null, null, null, null, null);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
    }


    @Test
    public void testViewNewsClientByFilterSuccessfully() {
        List<News> newsList = Arrays.asList(new News(), new News());
        for (News news : newsList) {
            news.setCategory(new Category());
        }
        Page<News> newsPage = new PageImpl<>(newsList);

        when(newsRepository.findNewsByTitleAndCategories(any(), any(), any())).thenReturn(newsPage);

        PageResponse<?> response = defaultNewsService.viewNewsClientByFilter(0, 10, null, null);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
    }

}
