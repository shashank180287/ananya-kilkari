package org.motechproject.ananya.kilkari.reporting.repository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.motechproject.ananya.kilkari.contract.request.CallDetailsReportRequest;
import org.motechproject.ananya.kilkari.contract.request.SubscriberReportRequest;
import org.motechproject.ananya.kilkari.contract.request.SubscriptionReportRequest;
import org.motechproject.ananya.kilkari.contract.request.SubscriptionStateChangeRequest;
import org.motechproject.ananya.kilkari.contract.response.LocationResponse;
import org.motechproject.http.client.service.HttpClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ReportingGatewayImplTest {
    private ReportingGateway reportingGateway;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private Properties kilkariProperties;
    @Mock
    private HttpClientService httpClientService;
    @Captor
    private ArgumentCaptor<Class<String>> responseTypeArgumentCaptor;
    @Captor
    private ArgumentCaptor<HashMap<String, String>> urlVariablesArgumentCaptor;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        initMocks(this);
        reportingGateway = new ReportingGatewayImpl(restTemplate, httpClientService, kilkariProperties);
    }

    @Test
    public void shouldInvokeReportingServiceWithGetLocations() {
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");
        LocationResponse expectedLocation = new LocationResponse("mydistrict", "myblock", "mypanchayat");
        when(restTemplate.getForEntity(any(String.class), any(Class.class))).thenReturn(new ResponseEntity(expectedLocation, HttpStatus.OK));

        LocationResponse actualLocation = reportingGateway.getLocation("mydistrict", "myblock", "mypanchayat");

        assertEquals(expectedLocation, actualLocation);

        ArgumentCaptor<String> urlArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> subscriberLocationCaptor = ArgumentCaptor.forClass(Class.class);
        verify(restTemplate).getForEntity(urlArgumentCaptor.capture(), subscriberLocationCaptor.capture());

        assertEquals(LocationResponse.class, subscriberLocationCaptor.getValue());

        verify(kilkariProperties).getProperty("reporting.service.base.url");
        String url = urlArgumentCaptor.getValue();
        assertTrue(url.startsWith("url/location?"));
        assertTrue(url.contains("district=mydistrict"));
        assertTrue(url.contains("block=myblock"));
        assertTrue(url.contains("panchayat=mypanchayat"));
    }

    @Test
    public void shouldReturnNullIfLocationNotPresent() {
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");
        when(restTemplate.getForEntity(any(String.class), any(Class.class))).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        LocationResponse actualLocation = reportingGateway.getLocation("mydistrict", "myblock", "mypanchayat");

        assertNull(actualLocation);

        ArgumentCaptor<String> urlArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> subscriberLocationCaptor = ArgumentCaptor.forClass(Class.class);
        verify(restTemplate).getForEntity(urlArgumentCaptor.capture(), subscriberLocationCaptor.capture());

        assertEquals(LocationResponse.class, subscriberLocationCaptor.getValue());

        verify(kilkariProperties).getProperty("reporting.service.base.url");
        String url = urlArgumentCaptor.getValue();
        assertTrue(url.startsWith("url/location?"));
        assertTrue(url.contains("district=mydistrict"));
        assertTrue(url.contains("block=myblock"));
        assertTrue(url.contains("panchayat=mypanchayat"));
    }

    @Test
    public void shouldRethrowAnyOtherExceptionOnGetLocation() {
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");
        when(restTemplate.getForEntity(any(String.class), any(Class.class))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        expectedException.expect(HttpClientErrorException.class);
        expectedException.expectMessage("400 BAD_REQUEST");

        reportingGateway.getLocation("mydistrict", "myblock", "mypanchayat");

        ArgumentCaptor<String> urlArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> subscriberLocationCaptor = ArgumentCaptor.forClass(Class.class);
        verify(restTemplate).getForEntity(urlArgumentCaptor.capture(), subscriberLocationCaptor.capture());

        assertEquals(LocationResponse.class, subscriberLocationCaptor.getValue());

        verify(kilkariProperties).getProperty("reporting.service.base.url");
        String url = urlArgumentCaptor.getValue();
        assertTrue(url.startsWith("url/location?"));
        assertTrue(url.contains("district=mydistrict"));
        assertTrue(url.contains("block=myblock"));
        assertTrue(url.contains("panchayat=mypanchayat"));
    }

    @Test
    public void shouldInvokeReportingServiceWithGetLocationsIfDistrctNotPresent() {
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");
        LocationResponse expectedLocation = new LocationResponse(null, "myblock", "mypanchayat");
        when(restTemplate.getForEntity(any(String.class), any(Class.class))).thenReturn(new ResponseEntity(expectedLocation, HttpStatus.OK));

        LocationResponse actualLocation = reportingGateway.getLocation(null, "myblock", "mypanchayat");

        assertEquals(expectedLocation, actualLocation);

        ArgumentCaptor<String> urlArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> subscriberLocationCaptor = ArgumentCaptor.forClass(Class.class);
        verify(restTemplate).getForEntity(urlArgumentCaptor.capture(), subscriberLocationCaptor.capture());

        assertEquals(LocationResponse.class, subscriberLocationCaptor.getValue());

        verify(kilkariProperties).getProperty("reporting.service.base.url");
        String url = urlArgumentCaptor.getValue();
        assertTrue(url.startsWith("url/location?"));
        assertTrue(url.contains("block=myblock"));
        assertTrue(url.contains("panchayat=mypanchayat"));
    }

    @Test
    public void shouldInvokeReportingServiceToGetLocationIfDistrictBlockAndPanchayatAreNotPresent() {
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");
        LocationResponse expectedLocation = new LocationResponse(null, "myblock", "mypanchayat");
        when(restTemplate.getForEntity(any(String.class), any(Class.class))).thenReturn(new ResponseEntity(expectedLocation, HttpStatus.OK));

        LocationResponse actualLocation = reportingGateway.getLocation(null, null, null);

        assertEquals(expectedLocation, actualLocation);

        ArgumentCaptor<String> urlArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> subscriberLocationCaptor = ArgumentCaptor.forClass(Class.class);
        verify(restTemplate).getForEntity(urlArgumentCaptor.capture(), subscriberLocationCaptor.capture());

        assertEquals(LocationResponse.class, subscriberLocationCaptor.getValue());

        verify(kilkariProperties).getProperty("reporting.service.base.url");
        String url = urlArgumentCaptor.getValue();
        assertEquals("url/location?", url);
    }

    @Test
    public void shouldReportASubscriptionCreation() {
        SubscriptionReportRequest subscriptionCreationReportRequest = mock(SubscriptionReportRequest.class);
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");

        reportingGateway.reportSubscriptionCreation(subscriptionCreationReportRequest);

        verify(httpClientService).post("url/subscription", subscriptionCreationReportRequest);
    }

    @Test
    public void shouldReportASubscriptionStateChange() {
        String subscriptionId = "subscriptionId";
        SubscriptionStateChangeRequest subscriptionStateChangeRequest = mock(SubscriptionStateChangeRequest.class);
        when(subscriptionStateChangeRequest.getSubscriptionId()).thenReturn(subscriptionId);
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");

        reportingGateway.reportSubscriptionStateChange(subscriptionStateChangeRequest);

        verify(httpClientService).put("url/subscription/" + subscriptionId, subscriptionStateChangeRequest);
    }

    @Test
    public void shouldReportASuccessfulCampaignMessageDelivery() {
        CallDetailsReportRequest campaignMessageDeliveryReportRequest = mock(CallDetailsReportRequest.class);
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");

        reportingGateway.reportCampaignMessageDeliveryStatus(campaignMessageDeliveryReportRequest);

        verify(httpClientService).post("url/callDetails", campaignMessageDeliveryReportRequest);
    }

    @Test
    public void shouldReportASubscriberUpdate() {
        String subscriptionId = "subscriptionId";
        String beneficiaryName = "Name";
        SubscriberReportRequest subscriberReportRequest = new SubscriberReportRequest(null, beneficiaryName, null, null, null, null);
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");

        reportingGateway.reportSubscriberDetailsChange(subscriptionId, subscriberReportRequest);

        ArgumentCaptor<SubscriberReportRequest> requestCaptor = ArgumentCaptor.forClass(SubscriberReportRequest.class);
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpClientService).put(urlCaptor.capture(), requestCaptor.capture());
        SubscriberReportRequest requestCaptorValue = requestCaptor.getValue();
        String urlCaptorValue = urlCaptor.getValue();
        assertEquals(beneficiaryName, requestCaptorValue.getBeneficiaryName());
        assertEquals("url/subscriber/" + subscriptionId, urlCaptorValue);
    }
}
