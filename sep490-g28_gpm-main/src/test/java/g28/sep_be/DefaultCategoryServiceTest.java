//package g28.sep_be;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import vn.com.fpt.sep490_g28_summer2024_be.common.ErrorCode;
//import vn.com.fpt.sep490_g28_summer2024_be.dto.category.CategoryCountDTO;
//import vn.com.fpt.sep490_g28_summer2024_be.dto.category.CategoryResponseDTO;
//import vn.com.fpt.sep490_g28_summer2024_be.entity.Category;
//import vn.com.fpt.sep490_g28_summer2024_be.exception.AppException;
//import vn.com.fpt.sep490_g28_summer2024_be.repository.CategoryRepository;
//import vn.com.fpt.sep490_g28_summer2024_be.repository.NewsRepository;
//import vn.com.fpt.sep490_g28_summer2024_be.service.category.DefaultCategoryService;
//
//import java.math.BigInteger;
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class DefaultCategoryServiceTest {
//
//    @Mock
//    private CategoryRepository categoryRepository;
//
//    @Mock
//    private NewsRepository newsRepository;
//
//    @InjectMocks
//    private DefaultCategoryService categoryService;
//
//    private Category category1;
//    private Category category2;
//
//    @BeforeEach
//    void setUp() {
//
//        category1 = new Category();
//        category1.setCategoryId(BigInteger.valueOf(1));
//        category1.setTitle("Category 1");
//
//        category2 = new Category();
//        category2.setCategoryId(BigInteger.valueOf(2));
//        category2.setTitle("Category 2");
//    }
//
//    @Test
//    void create_Success() {
//        CategoryResponseDTO request = new CategoryResponseDTO();
//        request.setTitle("New Category");
//
//        Category category = new Category();
//        category.setCategoryId(BigInteger.ONE);
//        category.setTitle("New Category");
//        category.setCreatedAt(LocalDateTime.now());
//        category.setUpdatedAt(LocalDateTime.now());
//        category.setIsActive(true);
//
//        when(categoryRepository.existsCategoriesByTitle(request.getTitle())).thenReturn(false);
//        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
//            Category savedCategory = invocation.getArgument(0);
//            savedCategory.setCategoryId(BigInteger.ONE); // Set ID after saving
//            return savedCategory;
//        });
//
//        CategoryResponseDTO response = categoryService.create(request);
//
//        assertNotNull(response);
//        assertEquals("New Category", response.getTitle());
//        assertNotNull(response.getSlug()); // Ensure the slug is set
//
//        verify(categoryRepository, times(1)).existsCategoriesByTitle(request.getTitle());
//        verify(categoryRepository, times(2)).save(any(Category.class)); // Updated to expect 2 invocations
//    }
//
//
//    @Test
//    void create_DuplicateTitle() {
//        CategoryResponseDTO request = new CategoryResponseDTO();
//        request.setTitle("Existing Category");
//
//        when(categoryRepository.existsCategoriesByTitle(request.getTitle())).thenReturn(true);
//
//        AppException exception = assertThrows(AppException.class, () -> {
//            categoryService.create(request);
//        });
//
//        assertEquals(ErrorCode.DUPLICATE_TITLE, exception.getErrorCode());
//        verify(categoryRepository, times(1)).existsCategoriesByTitle(request.getTitle());
//        verify(categoryRepository, never()).save(any(Category.class));
//    }
//
//    @Test
//    void viewList_Success() {
//        Object[] result1 = new Object[]{1L, 5L};  // {categoryId, newsCount}
//        Object[] result2 = new Object[]{2L, 3L};  // {categoryId, newsCount}
//        List<Object[]> newsCounts = Arrays.asList(result1, result2);
//
//        when(newsRepository.countNewsByCategoryAndStatus()).thenReturn(newsCounts);
//        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category1, category2));
//
//        CategoryCountDTO response = categoryService.viewList();
//
//        assertNotNull(response);
//        assertEquals(2, response.getData().size());
//        assertEquals(8, response.getTotalNews()); // 5 + 3
//        verify(newsRepository, times(1)).countNewsByCategoryAndStatus();
//        verify(categoryRepository, times(1)).findAll();
//    }
//}
