//package g28.sep_be;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import vn.com.fpt.sep490_g28_summer2024_be.dto.chart.ChartResponseDTO;
//import vn.com.fpt.sep490_g28_summer2024_be.dto.chart.interfacedto.StaticByCampaignInterfaceDTO;
//import vn.com.fpt.sep490_g28_summer2024_be.dto.chart.interfacedto.StaticInterfaceDTO;
//import vn.com.fpt.sep490_g28_summer2024_be.repository.CampaignRepository;
//import vn.com.fpt.sep490_g28_summer2024_be.repository.DonationRepository;
//import vn.com.fpt.sep490_g28_summer2024_be.repository.ProjectRepository;
//import vn.com.fpt.sep490_g28_summer2024_be.repository.SponsorRepository;
//import vn.com.fpt.sep490_g28_summer2024_be.service.dashboard.DefaultStaticService;
//
//import java.math.BigDecimal;
//import java.math.BigInteger;
//import java.util.Collections;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class DefaultStaticServiceTest {
//
//    @InjectMocks
//    private DefaultStaticService staticService;
//
//    @Mock
//    private ProjectRepository projectRepository;
//
//    @Mock
//    private DonationRepository donationRepository;
//
//    @Mock
//    private CampaignRepository campaignRepository;
//
//    @Mock
//    private SponsorRepository sponsorRepository;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    void getTotalSponsorValue_Success() {
//        Integer month = 7;
//        Integer year = 2024;
//        BigDecimal expectedValue = BigDecimal.valueOf(1000);
//
//        when(sponsorRepository.getAllSponsorValueByMonth(month, year)).thenReturn(expectedValue);
//
//        BigDecimal result = staticService.getTotalSponsorValue(month, year);
//
//        assertNotNull(result);
//        assertEquals(expectedValue, result);
//    }
//
//    @Test
//    void getTotalSponsorValue_NoData() {
//        Integer month = 7;
//        Integer year = 2024;
//
//        when(sponsorRepository.getAllSponsorValueByMonth(month, year)).thenReturn(null);
//
//        BigDecimal result = staticService.getTotalSponsorValue(month, year);
//
//        assertNull(result, "Expected result to be null when no sponsor value data is available");
//    }
//
//    @Test
//    void getTotalDonation_Success() {
//        Integer month = 7;
//        Integer year = 2024;
//        BigDecimal expectedValue = BigDecimal.valueOf(5000);
//
//        when(donationRepository.getTotalDonationByMonth(year, month)).thenReturn(expectedValue);
//
//        BigDecimal result = staticService.getTotalDonation(month, year);
//
//        assertNotNull(result);
//        assertEquals(expectedValue, result);
//    }
//
//    @Test
//    void getTotalDonation_NoData() {
//        Integer month = 7;
//        Integer year = 2024;
//
//        when(donationRepository.getTotalDonationByMonth(year, month)).thenReturn(null);
//
//        BigDecimal result = staticService.getTotalDonation(month, year);
//
//        assertNull(result, "Expected result to be null when no donation data is available");
//    }
//
//    @Test
//    void getTotalWrongDonation_Success() {
//        Integer month = 7;
//        Integer year = 2024;
//        BigDecimal expectedValue = BigDecimal.valueOf(200);
//
//        when(donationRepository.getTotalWrongDonationByMonth(year, month)).thenReturn(expectedValue);
//
//        BigDecimal result = staticService.getTotalWrongDonation(month, year);
//
//        assertNotNull(result);
//        assertEquals(expectedValue, result);
//    }
//
//    @Test
//    void getTotalWrongDonation_NoData() {
//        Integer month = 7;
//        Integer year = 2024;
//
//        when(donationRepository.getTotalWrongDonationByMonth(year, month)).thenReturn(null);
//
//        BigDecimal result = staticService.getTotalWrongDonation(month, year);
//
//        assertNull(result, "Expected result to be null when no wrong donation data is available");
//    }
//
//    @Test
//    void getTotalCountDonations_Success() {
//        Integer month = 7;
//        Integer year = 2024;
//        Long expectedCount = 150L;
//
//        when(donationRepository.getTotalCountDonationByMonth(year, month)).thenReturn(expectedCount);
//
//        Long result = staticService.getTotalCountDonations(month, year);
//
//        assertNotNull(result);
//        assertEquals(expectedCount, result);
//    }
//
//    @Test
//    void getTotalCountDonations_NoData() {
//        Integer month = 7;
//        Integer year = 2024;
//
//        when(donationRepository.getTotalCountDonationByMonth(year, month)).thenReturn(null);
//
//        Long result = staticService.getTotalCountDonations(month, year);
//
//        assertNull(result, "Expected result to be null when no count donation data is available");
//    }
//
//    @Test
//    void getDataPieChart_Success() {
//        Integer month = 7;
//        Integer year = 2024;
//        StaticByCampaignInterfaceDTO mockCampaignData = mock(StaticByCampaignInterfaceDTO.class);
//
//        when(mockCampaignData.getId()).thenReturn(BigInteger.ONE);
//        when(mockCampaignData.getValue()).thenReturn(BigDecimal.valueOf(1000));
//        when(mockCampaignData.getLabel()).thenReturn("Campaign 1");
//
//        when(campaignRepository.getCampaignPieChart(month, year)).thenReturn(List.of(mockCampaignData));
//
//        List<ChartResponseDTO> result = staticService.getDataPieChart(month, year);
//
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        assertEquals(BigInteger.ONE, result.get(0).getId());
//        assertEquals(BigDecimal.valueOf(1000), result.get(0).getValue());
//        assertEquals("Campaign 1", result.get(0).getLabel());
//    }
//
//    @Test
//    void getDataPieChart_NoData() {
//        Integer month = 7;
//        Integer year = 2024;
//
//        when(campaignRepository.getCampaignPieChart(month, year)).thenReturn(Collections.emptyList());
//
//        List<ChartResponseDTO> result = staticService.getDataPieChart(month, year);
//
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    void getDataLineChart_Success() {
//        Integer year = 2024;
//        StaticInterfaceDTO mockData = mock(StaticInterfaceDTO.class);
//
//        when(mockData.getMonthNumber()).thenReturn(7);
//        when(mockData.getValue()).thenReturn(BigDecimal.valueOf(1000));
//
//        when(donationRepository.getTotalDonationByYear(year)).thenReturn(List.of(mockData));
//
//        List<ChartResponseDTO> result = staticService.getDataLineChart(year);
//
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        assertEquals("Tháng 7", result.get(0).getLabel());
//        assertEquals(BigDecimal.valueOf(1000), result.get(0).getValue());
//    }
//
//    @Test
//    void getDataLineChart_NoData() {
//        Integer year = 2024;
//
//        when(donationRepository.getTotalDonationByYear(year)).thenReturn(null);
//
//        List<ChartResponseDTO> result = staticService.getDataLineChart(year);
//
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    void getDataBarChart_Success() {
//        Integer month = 7;
//        Integer year = 2024;
//        StaticInterfaceDTO mockData = mock(StaticInterfaceDTO.class);
//
//        when(mockData.getWeekOfMonth()).thenReturn(2);
//        when(mockData.getValue()).thenReturn(BigDecimal.valueOf(500));
//
//        when(donationRepository.getTotalDonationByWeekOfMonth(year, month)).thenReturn(List.of(mockData));
//
//        List<ChartResponseDTO> result = staticService.getDataBarChart(month, year);
//
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        assertEquals("Tuần 2", result.get(0).getLabel());
//        assertEquals(BigDecimal.valueOf(500), result.get(0).getValue());
//    }
//
//    @Test
//    void getDataBarChart_NoData() {
//        Integer month = 7;
//        Integer year = 2024;
//
//        when(donationRepository.getTotalDonationByWeekOfMonth(year, month)).thenReturn(null);
//
//        List<ChartResponseDTO> result = staticService.getDataBarChart(month, year);
//
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    void getDonationStaticDataByCampaign_Success() {
//        BigInteger campaignId = BigInteger.ONE;
//        Integer year = 2024;
//        StaticInterfaceDTO mockData = mock(StaticInterfaceDTO.class);
//
//        when(campaignRepository.getDonationStaticByCampaign(campaignId, year)).thenReturn(mockData);
//
//        StaticInterfaceDTO result = staticService.getDonationStaticDataByCampaign(campaignId, year);
//
//        assertNotNull(result);
//        assertEquals(mockData, result);
//    }
//
//    @Test
//    void getDonationStaticDataByCampaign_NoData() {
//        BigInteger campaignId = BigInteger.ONE;
//        Integer year = 2024;
//
//        when(campaignRepository.getDonationStaticByCampaign(campaignId, year)).thenReturn(null);
//
//        StaticInterfaceDTO result = staticService.getDonationStaticDataByCampaign(campaignId, year);
//
//        assertNull(result, "Expected result to be null when no donation static data is available");
//    }
//
//    @Test
//    void getProjectStaticDataByCampaign_Success() {
//        BigInteger campaignId = BigInteger.ONE;
//        Integer year = 2024;
//        StaticInterfaceDTO mockData = mock(StaticInterfaceDTO.class);
//
//        when(projectRepository.getProjectStaticByCampaign(campaignId, year)).thenReturn(mockData);
//
//        StaticInterfaceDTO result = staticService.getProjectStaticDataByCampaign(campaignId, year);
//
//        assertNotNull(result);
//        assertEquals(mockData, result);
//    }
//
//    @Test
//    void getProjectStaticDataByCampaign_NoData() {
//        BigInteger campaignId = BigInteger.ONE;
//        Integer year = 2024;
//
//        when(projectRepository.getProjectStaticByCampaign(campaignId, year)).thenReturn(null);
//
//        StaticInterfaceDTO result = staticService.getProjectStaticDataByCampaign(campaignId, year);
//
//        assertNull(result, "Expected result to be null when no project static data is available");
//    }
//}
