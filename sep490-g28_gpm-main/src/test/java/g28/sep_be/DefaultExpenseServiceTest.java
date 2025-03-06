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
import vn.com.fpt.sep490_g28_summer2024_be.dto.expense.ExpenseDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.expense.ExpenseFileDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.pageinfo.PageResponse;
import vn.com.fpt.sep490_g28_summer2024_be.dto.project.ProjectResponseDTO;
import vn.com.fpt.sep490_g28_summer2024_be.entity.*;
import vn.com.fpt.sep490_g28_summer2024_be.exception.AppException;
import vn.com.fpt.sep490_g28_summer2024_be.firebase.FirebaseServiceImpl;
import vn.com.fpt.sep490_g28_summer2024_be.repository.ExpenseFileRepository;
import vn.com.fpt.sep490_g28_summer2024_be.repository.ExpenseRepository;
import vn.com.fpt.sep490_g28_summer2024_be.repository.ProjectRepository;
import vn.com.fpt.sep490_g28_summer2024_be.service.expense.ExpenseServiceImpl;

public class DefaultExpenseServiceTest {

    @Mock
    private FirebaseServiceImpl firebaseService;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseFileRepository expenseFileRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ExpenseServiceImpl expenseService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void addExpense_Success() throws IOException {
        // Arrange
        ExpenseDTO expenseDTO = ExpenseDTO.builder()
                .title("Test Title")
                .unitPrice(new BigDecimal("1000.00"))
                .project(ProjectResponseDTO.builder().projectId(BigInteger.valueOf(1)).title("Test Project").build())
                .build();

        MultipartFile[] newFiles = new MultipartFile[1];
        MultipartFile mockFile = mock(MultipartFile.class);
        newFiles[0] = mockFile;

        Project project = new Project();
        project.setProjectId(BigInteger.valueOf(1));
        when(projectRepository.findById(BigInteger.valueOf(1))).thenReturn(Optional.of(project));

        Expense expense = new Expense();
        expense.setExpenseId(BigInteger.valueOf(1));
        expense.setTitle("Test Title");
        expense.setUnitPrice(new BigDecimal("1000.00"));
        expense.setProject(project);
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        when(firebaseService.uploadMultipleFile(any(), any(), any())).thenReturn(List.of("file_url"));

        // Act
        ExpenseDTO result = expenseService.addExpense(expenseDTO, newFiles);

        // Assert
        assertNotNull(result);
        assertEquals("Test Title", result.getTitle());
        assertEquals(new BigDecimal("1000.00"), result.getUnitPrice());

        verify(expenseRepository, times(1)).save(any(Expense.class));
        verify(expenseFileRepository, times(1)).save(any(ExpenseFile.class));
        verify(firebaseService, times(1)).uploadMultipleFile(any(), any(), any());
    }

    @Test
    public void addExpense_ProjectNotFound() {
        // Arrange
        ExpenseDTO expenseDTO = ExpenseDTO.builder()
                .title("Test Title")
                .unitPrice(new BigDecimal("1000.00"))
                .project(ProjectResponseDTO.builder().projectId(BigInteger.valueOf(1)).title("Test Project").build())
                .build();

        when(projectRepository.findById(BigInteger.valueOf(1))).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            expenseService.addExpense(expenseDTO, null);
        });

        assertEquals(ErrorCode.PROJECT_NOT_EXISTED, exception.getErrorCode());
        verify(projectRepository, times(1)).findById(BigInteger.valueOf(1));
        verify(expenseRepository, never()).save(any(Expense.class));
        verify(expenseFileRepository, never()).save(any(ExpenseFile.class));
    }

    @Test
    public void getExpenseById_Success() {
        // Arrange
        BigInteger expenseId = BigInteger.valueOf(1);
        Expense expense = new Expense();
        expense.setExpenseId(expenseId);
        expense.setTitle("Test Title");
        expense.setUnitPrice(new BigDecimal("1000.00"));
        Project project = new Project();
        project.setProjectId(BigInteger.valueOf(1));
        expense.setProject(project);

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        // Act
        ExpenseDTO result = expenseService.getExpenseById(expenseId);

        // Assert
        assertNotNull(result);
        assertEquals("Test Title", result.getTitle());
        assertEquals(new BigDecimal("1000.00"), result.getUnitPrice());

        verify(expenseRepository, times(1)).findById(expenseId);
    }

    @Test
    public void getExpenseById_ExpenseNotFound() {
        // Arrange
        BigInteger expenseId = BigInteger.valueOf(1);
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            expenseService.getExpenseById(expenseId);
        });

        assertEquals(ErrorCode.EXPENSE_NOT_FOUND, exception.getErrorCode());
        verify(expenseRepository, times(1)).findById(expenseId);
    }

    @Test
    public void updateExpense_Success() throws IOException {
        // Arrange
        BigInteger expenseId = BigInteger.valueOf(1);
        ExpenseDTO expenseDTO = ExpenseDTO.builder()
                .title("Updated Title")
                .unitPrice(new BigDecimal("1500.00"))
                .build();

        MultipartFile[] newFiles = new MultipartFile[1];
        MultipartFile mockFile = mock(MultipartFile.class);
        newFiles[0] = mockFile;

        Expense expense = new Expense();
        expense.setExpenseId(expenseId);
        expense.setTitle("Old Title");
        expense.setUnitPrice(new BigDecimal("1000.00"));
        expense.setExpenseFiles(new ArrayList<>()); // Initialize expenseFiles

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
        when(firebaseService.uploadMultipleFile(any(), eq(expenseId), any())).thenReturn(List.of("updated_file_url"));

        // Act
        ExpenseDTO result = expenseService.updateExpense(expenseDTO, expenseId, newFiles);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals(new BigDecimal("1500.00"), result.getUnitPrice());

        verify(expenseRepository, times(1)).findById(expenseId);
        verify(expenseRepository, times(1)).save(any(Expense.class));
        verify(expenseFileRepository, times(1)).save(any(ExpenseFile.class));
        verify(firebaseService, times(1)).uploadMultipleFile(any(), eq(expenseId), any());
    }
    @Test
    public void updateExpense_ExpenseNotFound() throws IOException {
        // Arrange
        BigInteger expenseId = BigInteger.valueOf(1);
        ExpenseDTO expenseDTO = ExpenseDTO.builder()
                .title("Updated Title")
                .unitPrice(new BigDecimal("1500.00"))
                .build();

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            expenseService.updateExpense(expenseDTO, expenseId, null);
        });

        assertEquals(ErrorCode.EXPENSE_NOT_FOUND, exception.getErrorCode());
        verify(expenseRepository, times(1)).findById(expenseId);
        verify(expenseRepository, never()).save(any(Expense.class));
        verify(expenseFileRepository, never()).save(any(ExpenseFile.class));
        verify(firebaseService, never()).uploadMultipleFile(any(), any(), any());
    }

    @Test
    public void deleteExpense_Success() throws IOException {
        // Arrange
        BigInteger expenseId = BigInteger.valueOf(1);
        Expense expense = new Expense();
        expense.setExpenseId(expenseId);

        List<ExpenseFile> files = new ArrayList<>();
        ExpenseFile file = new ExpenseFile();
        file.setFile("file_url");
        files.add(file);
        expense.setExpenseFiles(files);

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        // Act
        expenseService.deleteExpense(expenseId);

        // Assert
        verify(expenseRepository, times(1)).findById(expenseId);
        verify(expenseRepository, times(1)).delete(expense);
        verify(expenseFileRepository, times(1)).delete(file);
        verify(firebaseService, times(1)).deleteFileByPath("file_url");
    }

    @Test
    public void deleteExpense_ExpenseNotFound() throws IOException {
        // Arrange
        BigInteger expenseId = BigInteger.valueOf(1);
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            expenseService.deleteExpense(expenseId);
        });

        assertEquals(ErrorCode.EXPENSE_NOT_FOUND, exception.getErrorCode());
        verify(expenseRepository, times(1)).findById(expenseId);
        verify(expenseRepository, never()).delete(any(Expense.class));
        verify(expenseFileRepository, never()).delete(any(ExpenseFile.class));
        verify(firebaseService, never()).deleteFileByPath(anyString());
    }
    @Test
    public void viewByFilter_Success() {
        // Arrange
        int page = 0;
        int size = 10;
        String title = "Test Title";
        BigInteger projectId = BigInteger.valueOf(1);
        Pageable pageable = PageRequest.of(page, size);

        // Mock project repository to return a valid project
        Project project = new Project();
        project.setProjectId(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        // Create an expense and set its project
        Expense expense = new Expense();
        expense.setExpenseId(BigInteger.valueOf(1));
        expense.setTitle("Expense Title");
        expense.setUnitPrice(new BigDecimal("1000.00"));
        expense.setCreatedAt(LocalDateTime.now());
        expense.setUpdatedAt(LocalDateTime.now());
        expense.setProject(project);

        // Create a page of expenses
        List<Expense> expenses = new ArrayList<>();
        expenses.add(expense);
        Page<Expense> pageExpenses = new PageImpl<>(expenses, pageable, 1);
        when(expenseRepository.findExpenseByFilterAndProjectId(title, projectId, pageable)).thenReturn(pageExpenses);

        // Act
        PageResponse<ExpenseDTO> response = expenseService.viewByFilter(page, size, title, projectId);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotal());
        assertEquals(page, response.getOffset());
        assertEquals(size, response.getLimit());

        verify(projectRepository, times(1)).findById(projectId);
        verify(expenseRepository, times(1)).findExpenseByFilterAndProjectId(title, projectId, pageable);
    }


    @Test
    public void viewByFilter_ProjectNotFound() {
        // Arrange
        int page = 0;
        int size = 10;
        String title = "Test Title";
        BigInteger projectId = BigInteger.valueOf(1);
        Pageable pageable = PageRequest.of(page, size);

        // Mock the project repository to return empty when looking for the project
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            expenseService.viewByFilter(page, size, title, projectId);
        });

        assertEquals(ErrorCode.PROJECT_NOT_EXISTED, exception.getErrorCode());
        verify(projectRepository, times(1)).findById(projectId);
        verify(expenseRepository, never()).findExpenseByFilterAndProjectId(title, projectId, pageable);
    }



}
