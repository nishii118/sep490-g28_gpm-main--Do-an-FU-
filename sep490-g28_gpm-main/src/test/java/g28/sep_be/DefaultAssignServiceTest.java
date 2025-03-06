package g28.sep_be;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.com.fpt.sep490_g28_summer2024_be.common.ErrorCode;
import vn.com.fpt.sep490_g28_summer2024_be.dto.account.admin.AccountDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.assign.AssignResponseDTO;
import vn.com.fpt.sep490_g28_summer2024_be.dto.pageinfo.PageResponse;
import vn.com.fpt.sep490_g28_summer2024_be.entity.*;
import vn.com.fpt.sep490_g28_summer2024_be.exception.AppException;
import vn.com.fpt.sep490_g28_summer2024_be.repository.AccountRepository;
import vn.com.fpt.sep490_g28_summer2024_be.repository.AssignRepository;
import vn.com.fpt.sep490_g28_summer2024_be.repository.ProjectRepository;
import vn.com.fpt.sep490_g28_summer2024_be.sercurity.CustomAccountDetails;
import vn.com.fpt.sep490_g28_summer2024_be.service.assign.AssignServiceImpl;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultAssignServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AssignRepository assignRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private AssignServiceImpl assignService;

    @BeforeEach
    void setUp() {
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void viewMemberInProjectByFilter_Success() {
        int page = 0;
        int size = 10;
        BigInteger projectId = BigInteger.ONE;
        BigInteger roleId = BigInteger.ONE;
        String email = "test@example.com";
        String name = "Test User";

        Pageable pageable = PageRequest.of(page, size);

        Project project = new Project();
        project.setProjectId(projectId);

        Account account = new Account();
        account.setAccountId(BigInteger.ONE);
        account.setEmail(email);
        account.setFullname(name);

        Assign assign = new Assign();
        assign.setAssignId(BigInteger.ONE);
        assign.setAccount(account);
        assign.setProject(project);

        List<Assign> assignList = Collections.singletonList(assign);
        Page<Assign> assignPage = new PageImpl<>(assignList, pageable, 1);

        when(assignRepository.findMembersInProject(projectId, roleId, email, name, pageable)).thenReturn(assignPage);

        PageResponse<AssignResponseDTO> response = assignService.viewMemberInProjectByFilter(page, size, projectId, roleId, email, name);

        assertNotNull(response);
        assertEquals(1, response.getTotal());
        assertEquals(page, response.getOffset());
        assertEquals(size, response.getLimit());
        assertEquals(1, response.getContent().size());

        verify(assignRepository, times(1)).findMembersInProject(projectId, roleId, email, name, pageable);
    }

    @Test
    void viewMembersNotAssignedToProject_Success() {
        BigInteger projectId = BigInteger.ONE;

        Project project = new Project();
        project.setProjectId(projectId);

        Account account = new Account();
        account.setAccountId(BigInteger.ONE);
        account.setEmail("test@example.com");

        List<Account> accountList = Collections.singletonList(account);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(accountRepository.findActiveAccountsNotAssignedToProject(projectId)).thenReturn(accountList);

        List<AccountDTO> response = assignService.viewMembersNotAssignedToProject(projectId);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(account.getAccountId(), response.get(0).getAccountId());
        assertEquals(account.getEmail(), response.get(0).getEmail());

        verify(projectRepository, times(1)).findById(projectId);
        verify(accountRepository, times(1)).findActiveAccountsNotAssignedToProject(projectId);
    }

    @Test
    void addMembersToProject_Success() {
        BigInteger projectId = BigInteger.ONE;
        List<BigInteger> accountIds = Arrays.asList(BigInteger.valueOf(2), BigInteger.valueOf(3));

        Account loggedAccount = new Account();
        loggedAccount.setEmail("admin@example.com");
        Role adminRole = new Role();
        adminRole.setRoleName("Admin");
        loggedAccount.setRole(adminRole);

        Project project = new Project();
        project.setProjectId(projectId);

        Account account1 = new Account();
        account1.setAccountId(BigInteger.valueOf(2));
        Role pmRole = new Role();
        pmRole.setRoleName("project manager");
        account1.setRole(pmRole);

        Account account2 = new Account();
        account2.setAccountId(BigInteger.valueOf(3));
        account2.setRole(pmRole);

        List<Account> accounts = Arrays.asList(account1, account2);

        CustomAccountDetails customAccountDetails = mock(CustomAccountDetails.class);
        when(customAccountDetails.getUsername()).thenReturn("admin@example.com");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customAccountDetails);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(loggedAccount));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(accountRepository.findAllById(accountIds)).thenReturn(accounts);
        when(assignRepository.findByProject_ProjectIdAndAccount_AccountIdIn(projectId, accountIds)).thenReturn(Collections.emptyList());
        when(assignRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Assign> response = assignService.addMembersToProject(accountIds, projectId);

        assertNotNull(response);
        assertEquals(2, response.size());

        verify(accountRepository, times(1)).findByEmail(anyString());
        verify(projectRepository, times(1)).findById(projectId);
        verify(accountRepository, times(1)).findAllById(accountIds);
        verify(assignRepository, times(1)).findByProject_ProjectIdAndAccount_AccountIdIn(projectId, accountIds);
        verify(assignRepository, times(1)).saveAll(any());
    }


    @Test
    void addMembersToProject_ProjectNotFound() {
        BigInteger projectId = BigInteger.ONE;
        List<BigInteger> accountIds = Arrays.asList(BigInteger.valueOf(2), BigInteger.valueOf(3));

        // Mock CustomAccountDetails and SecurityContext
        CustomAccountDetails customAccountDetails = mock(CustomAccountDetails.class);
        when(customAccountDetails.getUsername()).thenReturn("admin@example.com");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customAccountDetails);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Mock accountRepository.findByEmail
        Account loggedAccount = new Account();
        loggedAccount.setEmail("admin@example.com");
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(loggedAccount));

        // Mock projectRepository.findById to return empty
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // Execute the service method and assert the exception
        AppException exception = assertThrows(AppException.class, () -> {
            assignService.addMembersToProject(accountIds, projectId);
        });

        // Verify the correct error code
        assertEquals(ErrorCode.PROJECT_NOT_EXISTED, exception.getErrorCode());

        // Verify interactions with repositories
        verify(projectRepository, times(1)).findById(projectId);
        verify(accountRepository, never()).findAllById(anyList());
        verify(assignRepository, never()).findByProject_ProjectIdAndAccount_AccountIdIn(any(), anyList());
        verify(assignRepository, never()).saveAll(anyList());
    }


    @Test
    void addMembersToProject_AccountAlreadyAssigned() {
        BigInteger projectId = BigInteger.ONE;
        List<BigInteger> accountIds = Arrays.asList(BigInteger.valueOf(2), BigInteger.valueOf(3));

        Project project = new Project();
        project.setProjectId(projectId);

        Account account1 = new Account();
        account1.setAccountId(BigInteger.valueOf(2));
        Role pmRole = new Role();
        pmRole.setRoleName("project manager");
        account1.setRole(pmRole);

        List<Assign> existingAssignments = Collections.singletonList(new Assign(BigInteger.valueOf(1), project, account1, null, null, null, null));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(accountRepository.findAllById(accountIds)).thenReturn(Arrays.asList(account1));
        when(assignRepository.findByProject_ProjectIdAndAccount_AccountIdIn(projectId, accountIds)).thenReturn(existingAssignments);

        Account loggedAccount = new Account();
        loggedAccount.setEmail("admin@example.com");
        Role adminRole = new Role();
        adminRole.setRoleName("Admin");
        loggedAccount.setRole(adminRole);
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(loggedAccount));

        // Mock Authentication and SecurityContext for the test
        CustomAccountDetails customAccountDetails = mock(CustomAccountDetails.class);
        when(customAccountDetails.getUsername()).thenReturn("admin@example.com");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customAccountDetails);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        when(securityContext.getAuthentication()).thenReturn(authentication);

        AppException exception = assertThrows(AppException.class, () -> {
            assignService.addMembersToProject(accountIds, projectId);
        });

        assertEquals(ErrorCode.MEMBER_ALREADY_ASSIGNED, exception.getErrorCode());

        verify(projectRepository, times(1)).findById(projectId);
        verify(accountRepository, times(1)).findAllById(accountIds);
        verify(assignRepository, times(1)).findByProject_ProjectIdAndAccount_AccountIdIn(projectId, accountIds);
        verify(assignRepository, never()).saveAll(anyList());
    }
    @Test
    void addMembersToProject_UnauthorizedUser() {
        BigInteger projectId = BigInteger.ONE;
        List<BigInteger> accountIds = Arrays.asList(BigInteger.valueOf(2), BigInteger.valueOf(3));

        CustomAccountDetails customAccountDetails = mock(CustomAccountDetails.class);
        when(customAccountDetails.getUsername()).thenReturn("non_admin@example.com");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customAccountDetails);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> {
            assignService.addMembersToProject(accountIds, projectId);
        });

        assertEquals(ErrorCode.HTTP_UNAUTHORIZED, exception.getErrorCode());

        verify(accountRepository, times(1)).findByEmail(anyString());
        verify(projectRepository, never()).findById(any());
        verify(accountRepository, never()).findAllById(anyList());
        verify(assignRepository, never()).findByProject_ProjectIdAndAccount_AccountIdIn(any(), anyList());
        verify(assignRepository, never()).saveAll(anyList());
    }

    @Test
    void removeMember_Success() {
        BigInteger assignId = BigInteger.ONE;

        Assign assign = new Assign();
        assign.setAssignId(assignId);

        when(assignRepository.findById(assignId)).thenReturn(Optional.of(assign));

        assignService.removeMember(assignId);

        verify(assignRepository, times(1)).findById(assignId);
        verify(assignRepository, times(1)).delete(assign);
    }
}
