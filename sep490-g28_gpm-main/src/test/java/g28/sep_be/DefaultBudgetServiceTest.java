package g28.sep_be;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.com.fpt.sep490_g28_summer2024_be.common.ErrorCode;
import vn.com.fpt.sep490_g28_summer2024_be.dto.budget.BudgetRequestDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.budget.BudgetResponseDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.pageinfo.PageResponse;
import vn.com.fpt.sep490_g28_summer2024_be.entity.*;
import vn.com.fpt.sep490_g28_summer2024_be.exception.AppException;
import vn.com.fpt.sep490_g28_summer2024_be.repository.AccountRepository;
import vn.com.fpt.sep490_g28_summer2024_be.repository.BudgetRepository;
import vn.com.fpt.sep490_g28_summer2024_be.repository.ProjectRepository;
import vn.com.fpt.sep490_g28_summer2024_be.sercurity.CustomAccountDetails;
import vn.com.fpt.sep490_g28_summer2024_be.service.budget.BudgetServiceImpl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;

class DefaultBudgetServiceTest {

    @InjectMocks
    private BudgetServiceImpl budgetService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @Test
    void viewBudgetByFilter_Success() {
        int page = 0;
        int size = 10;
        String title = "Test Budget";
        BigInteger projectId = BigInteger.ONE;

        Project project = new Project();
        project.setProjectId(projectId);
        project.setTotalBudget(BigDecimal.valueOf(1000));

        Budget budget = new Budget();
        budget.setBudgetId(BigInteger.ONE);
        budget.setTitle(title);
        budget.setUnitPrice(BigDecimal.valueOf(500));
        budget.setProject(project);

        List<Budget> budgetList = Collections.singletonList(budget);
        Page<Budget> budgetPage = new PageImpl<>(budgetList, PageRequest.of(page, size), 1);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(budgetRepository.findBudgetByFilterAndProjectId(title, projectId, PageRequest.of(page, size))).thenReturn(budgetPage);

        PageResponse<BudgetResponseDTO> response = budgetService.viewBudgetByFilter(page, size, title, projectId);

        assertEquals(1, response.getTotal());
        assertEquals(1, response.getContent().size());
        assertEquals(title, response.getContent().get(0).getTitle());
    }

    @Test
    void viewBudgetByFilter_ProjectNotFound() {
        int page = 0;
        int size = 10;
        String title = "Test Budget";
        BigInteger projectId = BigInteger.ONE;

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> {
            budgetService.viewBudgetByFilter(page, size, title, projectId);
        });

        assertEquals(ErrorCode.PROJECT_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void getBudgetById_Success() {
        BigInteger budgetId = BigInteger.ONE;

        Budget budget = new Budget();
        budget.setBudgetId(budgetId);
        budget.setTitle("Test Budget");

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));

        BudgetResponseDTO response = budgetService.getBudgetById(budgetId);

        assertEquals(budgetId, response.getBudgetId());
        assertEquals("Test Budget", response.getTitle());
    }

    @Test
    void getBudgetById_BudgetNotFound() {
        BigInteger budgetId = BigInteger.ONE;

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> budgetService.getBudgetById(budgetId));

        assertEquals(ErrorCode.BUDGET_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void addBudgetsToProject_Success() {
        BigInteger projectId = BigInteger.ONE;
        List<BudgetRequestDTO> budgetRequestDTOs = Arrays.asList(
                BudgetRequestDTO.builder().title("Budget 1").unitPrice("100").note("Note 1").build(),
                BudgetRequestDTO.builder().title("Budget 2").unitPrice("200").note("Note 2").build()
        );

        Project project = new Project();
        project.setProjectId(projectId);
        project.setTotalBudget(BigDecimal.ZERO);
        project.setAmountNeededToRaise(BigDecimal.ZERO);

        Account account = new Account();
        account.setEmail("test@example.com");

        Role role = new Role();
        role.setRoleName("admin");
        account.setRole(role);

        CustomAccountDetails accountDetails = mock(CustomAccountDetails.class);
        when(accountDetails.getUsername()).thenReturn("test@example.com");

        // Mock the security context
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(accountDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<BudgetResponseDTO> response = budgetService.addBudgetsToProject(budgetRequestDTOs, projectId);

        assertEquals(2, response.size());
        verify(budgetRepository, times(2)).save(any(Budget.class));
        verify(projectRepository, times(2)).save(project);
    }


    @Test
    void addBudgetsToProject_ProjectNotFound() {
        BigInteger projectId = BigInteger.ONE;
        List<BudgetRequestDTO> budgetRequestDTOs = Arrays.asList(
                BudgetRequestDTO.builder().title("Budget 1").unitPrice("100").note("Note 1").build(),
                BudgetRequestDTO.builder().title("Budget 2").unitPrice("200").note("Note 2").build()
        );

        // Mocking the account details
        CustomAccountDetails accountDetails = mock(CustomAccountDetails.class);
        when(accountDetails.getUsername()).thenReturn("test@example.com");

        // Mock the security context
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(accountDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Mock the account repository to return an account
        Account account = new Account();
        account.setEmail("test@example.com");
        Role role = new Role();
        role.setRoleName("admin");
        account.setRole(role);

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));

        // Mock the project repository to return empty when looking for the project
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // Test the method and assert exception
        AppException exception = assertThrows(AppException.class, () -> {
            budgetService.addBudgetsToProject(budgetRequestDTOs, projectId);
        });

        assertEquals(ErrorCode.PROJECT_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void updateBudget_Success() {
        BigInteger budgetId = BigInteger.ONE;
        BudgetRequestDTO budgetRequestDTO = BudgetRequestDTO.builder()
                .title("Updated Budget")
                .unitPrice("150")
                .note("Updated Note")
                .build();

        Account account = new Account();
        account.setEmail("test@example.com");

        Role role = new Role();
        role.setRoleName("admin");
        account.setRole(role);

        CustomAccountDetails accountDetails = mock(CustomAccountDetails.class);
        when(accountDetails.getUsername()).thenReturn("test@example.com");

        Budget budget = new Budget();
        budget.setBudgetId(budgetId);
        budget.setTitle("Old Budget");
        budget.setUnitPrice(BigDecimal.valueOf(100));
        budget.setNote("Old Note");
        budget.setStatus(1);

        Project project = new Project();
        project.setTotalBudget(BigDecimal.valueOf(100));
        project.setAmountNeededToRaise(BigDecimal.valueOf(50)); // Đảm bảo amountNeededToRaise không phải là null
        budget.setProject(project);

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(Budget.class))).thenReturn(budget);
        when(securityContext.getAuthentication().getPrincipal()).thenReturn(accountDetails);

        BudgetResponseDTO response = budgetService.updateBudget(budgetId, budgetRequestDTO);

        assertNotNull(response);
        assertEquals(budgetId, response.getBudgetId());
        assertEquals("Updated Budget", response.getTitle());
        assertEquals(0, BigDecimal.valueOf(150).compareTo(response.getUnitPrice()));
        assertEquals("Updated Note", response.getNote());
    }

    @Test
    void updateBudget_BudgetNotFound() {
        BigInteger budgetId = BigInteger.ONE;
        BudgetRequestDTO budgetRequestDTO = BudgetRequestDTO.builder()
                .title("Updated Budget")
                .unitPrice("150")
                .note("Updated Note")
                .build();

        // Mocking the account details
        CustomAccountDetails accountDetails = mock(CustomAccountDetails.class);
        when(accountDetails.getUsername()).thenReturn("test@example.com");

        // Mock the security context
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(accountDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Mock the account repository to return an account
        Account account = new Account();
        account.setEmail("test@example.com");
        Role role = new Role();
        role.setRoleName("admin");
        account.setRole(role);

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(account));

        // Mock the budget repository to return empty when looking for the budget
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.empty());

        // Test the method and assert exception
        AppException exception = assertThrows(AppException.class, () -> {
            budgetService.updateBudget(budgetId, budgetRequestDTO);
        });

        assertEquals(ErrorCode.BUDGET_NOT_FOUND, exception.getErrorCode());
    }


    @Test
    void deleteBudget_Success() {
        BigInteger budgetId = BigInteger.ONE;

        Budget budget = new Budget();
        budget.setBudgetId(budgetId);
        budget.setUnitPrice(BigDecimal.valueOf(100));

        Project project = new Project();
        project.setTotalBudget(BigDecimal.valueOf(100));
        project.setAmountNeededToRaise(BigDecimal.valueOf(100));
        budget.setProject(project);

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));

        budgetService.deleteBudget(budgetId);

        verify(budgetRepository, times(1)).delete(budget);
        assertEquals(BigDecimal.ZERO, project.getTotalBudget());
        assertEquals(BigDecimal.ZERO, project.getAmountNeededToRaise());
    }

    @Test
    void deleteBudget_BudgetNotFound() {
        BigInteger budgetId = BigInteger.ONE;

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> {
            budgetService.deleteBudget(budgetId);
        });

        assertEquals(ErrorCode.BUDGET_NOT_FOUND, exception.getErrorCode());
    }
}
