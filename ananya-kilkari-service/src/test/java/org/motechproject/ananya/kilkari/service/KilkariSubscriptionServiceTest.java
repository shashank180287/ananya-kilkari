package org.motechproject.ananya.kilkari.service;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.motechproject.ananya.kilkari.builder.ChangeSubscriptionWebRequestBuilder;
import org.motechproject.ananya.kilkari.builder.SubscriptionWebRequestBuilder;
import org.motechproject.ananya.kilkari.factory.SubscriptionStateHandlerFactory;
import org.motechproject.ananya.kilkari.message.domain.CampaignMessageAlert;
import org.motechproject.ananya.kilkari.message.service.CampaignMessageAlertService;
import org.motechproject.ananya.kilkari.messagecampaign.service.MessageCampaignService;
import org.motechproject.ananya.kilkari.obd.domain.CampaignMessage;
import org.motechproject.ananya.kilkari.obd.domain.Channel;
import org.motechproject.ananya.kilkari.obd.service.CampaignMessageService;
import org.motechproject.ananya.kilkari.request.*;
import org.motechproject.ananya.kilkari.subscription.builder.SubscriptionBuilder;
import org.motechproject.ananya.kilkari.subscription.domain.CampaignRescheduleRequest;
import org.motechproject.ananya.kilkari.subscription.domain.DeactivationRequest;
import org.motechproject.ananya.kilkari.subscription.domain.Subscription;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionPack;
import org.motechproject.ananya.kilkari.subscription.exceptions.DuplicateSubscriptionException;
import org.motechproject.ananya.kilkari.subscription.exceptions.ValidationException;
import org.motechproject.ananya.kilkari.subscription.repository.KilkariPropertiesData;
import org.motechproject.ananya.kilkari.subscription.service.ChangeSubscriptionService;
import org.motechproject.ananya.kilkari.subscription.service.SubscriptionService;
import org.motechproject.ananya.kilkari.subscription.service.request.ChangeMsisdnRequest;
import org.motechproject.ananya.kilkari.subscription.service.request.ChangeSubscriptionRequest;
import org.motechproject.ananya.kilkari.subscription.service.request.SubscriberRequest;
import org.motechproject.ananya.kilkari.subscription.service.request.SubscriptionRequest;
import org.motechproject.scheduler.MotechSchedulerService;

import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;


public class KilkariSubscriptionServiceTest {

    private KilkariSubscriptionService kilkariSubscriptionService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private SubscriptionPublisher subscriptionPublisher;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private MessageCampaignService messageCampaignService;
    @Mock
    private SubscriptionStateHandlerFactory subscriptionStateHandlerFactory;
    @Mock
    private MotechSchedulerService motechSchedulerService;
    @Mock
    private KilkariPropertiesData kilkariPropertiesData;
    @Mock
    private ChangeSubscriptionService changeSubscriptionService;
    @Mock
    private CampaignMessageAlertService campaignMessageAlertService;
    @Mock
    private CampaignMessageService campaignMessageService;

    @Before
    public void setup() {
        initMocks(this);
        kilkariSubscriptionService = new KilkariSubscriptionService(subscriptionPublisher, subscriptionService, motechSchedulerService,
                changeSubscriptionService, kilkariPropertiesData, campaignMessageAlertService, campaignMessageService);
        DateTimeUtils.setCurrentMillisFixed(DateTime.now().getMillis());
    }

    @After
    public void clear() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldCreateSubscription() {
        SubscriptionWebRequest subscriptionWebRequest = new SubscriptionWebRequest();
        kilkariSubscriptionService.createSubscriptionAsync(subscriptionWebRequest);
        verify(subscriptionPublisher).createSubscription(subscriptionWebRequest);
    }

    @Test
    public void shouldGetSubscriptionsFor() {
        String msisdn = "1234567890";

        kilkariSubscriptionService.getSubscriptionDetails(msisdn, Channel.IVR);

        verify(subscriptionService).getSubscriptionDetails(msisdn, Channel.IVR);
    }

    @Test
    public void shouldThrowAnExceptionForInvalidMsisdnNumbers() {
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Invalid msisdn 12345");

        kilkariSubscriptionService.getSubscriptionDetails("12345", Channel.CONTACT_CENTER);
    }

    @Test
    public void shouldThrowAnExceptionForNonNumericMsisdn() {
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Invalid msisdn 123456789a");

        kilkariSubscriptionService.getSubscriptionDetails("123456789a", Channel.CONTACT_CENTER);
    }

    @Test
    public void shouldCreateSubscriptionRequestAsynchronously() {
        SubscriptionWebRequest subscriptionWebRequest = new SubscriptionWebRequest();
        subscriptionWebRequest.setCreatedAt(DateTime.now());

        kilkariSubscriptionService.createSubscriptionAsync(subscriptionWebRequest);

        verify(subscriptionPublisher).createSubscription(subscriptionWebRequest);
    }

    @Test
    public void shouldCreateSubscriptionSynchronously() {
        SubscriptionWebRequest subscriptionWebRequest = new SubscriptionWebRequestBuilder().withDefaults().build();

        kilkariSubscriptionService.createSubscription(subscriptionWebRequest);

        ArgumentCaptor<SubscriptionRequest> subscriptionArgumentCaptor = ArgumentCaptor.forClass(SubscriptionRequest.class);
        verify(subscriptionService).createSubscription(subscriptionArgumentCaptor.capture(), eq(Channel.CONTACT_CENTER));
        SubscriptionRequest actualSubscription = subscriptionArgumentCaptor.getValue();
        assertEquals(subscriptionWebRequest.getMsisdn(), actualSubscription.getMsisdn());
    }

    @Test
    public void shouldValidateSubscriptionRequest() {
        SubscriptionWebRequest subscriptionWebRequest = new SubscriptionWebRequestBuilder().withDefaults().withMsisdn("abcd").build();
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Invalid msisdn abcd");

        kilkariSubscriptionService.createSubscription(subscriptionWebRequest);

        verify(subscriptionService, never()).createSubscription(any(SubscriptionRequest.class), any(Channel.class));
    }

    @Test
    public void shouldJustLogIfSubscriptionAlreadyExists() {
        SubscriptionWebRequest subscriptionWebRequest = new SubscriptionWebRequestBuilder().withDefaults().build();

        when(subscriptionService.createSubscription(any(SubscriptionRequest.class), any(Channel.class))).thenThrow(new DuplicateSubscriptionException(""));

        try {
            kilkariSubscriptionService.createSubscription(subscriptionWebRequest);
        } catch (Exception e) {
            Assert.fail("Unexpected Exception " + e.getMessage());
        }
    }

    @Test
    public void shouldReturnSubscriptionGivenASubscriptionId() {
        Subscription exptectedSubscription = new SubscriptionBuilder().withDefaults().build();
        String susbscriptionid = exptectedSubscription.getSubscriptionId();
        when(subscriptionService.findBySubscriptionId(susbscriptionid)).thenReturn(exptectedSubscription);

        Subscription subscription = kilkariSubscriptionService.findBySubscriptionId(susbscriptionid);

        assertEquals(exptectedSubscription, subscription);
    }

    @Test
    public void shouldScheduleASubscriptionCompletionEventAtExpiryDate_IfMessageIsNotReadyToBeSent() {
        DateTime now = DateTime.now();
        Subscription subscription = new SubscriptionBuilder().withDefaults().withPack(SubscriptionPack.BARI_KILKARI).withScheduleStartDate(now).build();
        String subscriptionId = subscription.getSubscriptionId();
        String campaignName = MessageCampaignService.SIXTEEN_MONTHS_CAMPAIGN_KEY;
        DateTime expiryDate = subscription.getCurrentWeeksMessageExpiryDate();
        CampaignMessageAlert campaignMessageAlert = new CampaignMessageAlert(subscriptionId, "WEEK64", false, expiryDate);
        when(campaignMessageAlertService.findBy(subscriptionId)).thenReturn(campaignMessageAlert);

        kilkariSubscriptionService.processSubscriptionCompletion(subscription, campaignName);

        verify(subscriptionService).scheduleCompletion(subscription, expiryDate);
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionService).updateSubscription(captor.capture());
        Subscription actualSubscription = captor.getValue();
        assertTrue(actualSubscription.isCampaignCompleted());
    }

    @Test
    public void shouldScheduleASubscriptionCompletionEventNow_IfSubscriptionIsAlreadyRenewed() {
        DateTime expectedCompletionDate = DateTime.now().withMillisOfSecond(0);
        Subscription subscription = new SubscriptionBuilder().withDefaults().withPack(SubscriptionPack.BARI_KILKARI).withScheduleStartDate(expectedCompletionDate).build();
        String subscriptionId = subscription.getSubscriptionId();
        String campaignName = MessageCampaignService.SIXTEEN_MONTHS_CAMPAIGN_KEY;
        String messageId = "WEEK64";
        CampaignMessageAlert campaignMessageAlert = new CampaignMessageAlert(subscriptionId, messageId, true, expectedCompletionDate.plusWeeks(1));
        when(campaignMessageAlertService.findBy(subscriptionId)).thenReturn(campaignMessageAlert);

        kilkariSubscriptionService.processSubscriptionCompletion(subscription, campaignName);

        verify(campaignMessageService, never()).find(subscriptionId, messageId);
        ArgumentCaptor<DateTime> dateTimeArgumentCaptor = ArgumentCaptor.forClass(DateTime.class);
        verify(subscriptionService).scheduleCompletion(eq(subscription), dateTimeArgumentCaptor.capture());
        DateTime actualCompletionDate = dateTimeArgumentCaptor.getValue();
        assertEquals(expectedCompletionDate, actualCompletionDate.withMillisOfSecond(0));
    }

    @Test
    public void shouldScheduleASubscriptionCompletionEventNow_IfSubscriptionHasLastMessageInOBD() {
        DateTime expectedCompletionDate = DateTime.now().withMillisOfSecond(0);
        SubscriptionPack pack = SubscriptionPack.BARI_KILKARI;
        Subscription subscription = new SubscriptionBuilder().withDefaults().withPack(pack)
                .withScheduleStartDate(expectedCompletionDate.minusWeeks(pack.getTotalWeeks() - 1)).build();
        String subscriptionId = subscription.getSubscriptionId();
        String messageId = "WEEK64";
        String campaignName = MessageCampaignService.SIXTEEN_MONTHS_CAMPAIGN_KEY;
        when(campaignMessageAlertService.findBy(subscriptionId)).thenReturn(null);
        when(campaignMessageService.find(subscriptionId, messageId)).thenReturn(new CampaignMessage());

        kilkariSubscriptionService.processSubscriptionCompletion(subscription, campaignName);

        ArgumentCaptor<DateTime> dateTimeArgumentCaptor = ArgumentCaptor.forClass(DateTime.class);
        verify(subscriptionService).scheduleCompletion(eq(subscription), dateTimeArgumentCaptor.capture());
        DateTime actualCompletionDate = dateTimeArgumentCaptor.getValue();
        assertEquals(expectedCompletionDate, actualCompletionDate.withMillisOfSecond(0));
    }

    @Test
    public void shouldUnsubscribe() {
        String subscriptionId = "abcd1234";
        UnSubscriptionWebRequest unSubscriptionWebRequest = new UnSubscriptionWebRequest();
        unSubscriptionWebRequest.setChannel(Channel.CONTACT_CENTER.name());

        kilkariSubscriptionService.requestUnsubscription(subscriptionId, unSubscriptionWebRequest);

        ArgumentCaptor<DeactivationRequest> deactivationRequestArgumentCaptor = ArgumentCaptor.forClass(DeactivationRequest.class);
        verify(subscriptionService).requestUnsubscription(deactivationRequestArgumentCaptor.capture());
        DeactivationRequest deactivationRequest = deactivationRequestArgumentCaptor.getValue();

        assertEquals(subscriptionId, deactivationRequest.getSubscriptionId());
        assertEquals(Channel.CONTACT_CENTER, deactivationRequest.getChannel());
    }

    @Test
    public void shouldValidateSubscriptionWhileDeactivatingSubscription() {
        UnSubscriptionWebRequest unSubscriptionWebRequest = new UnSubscriptionWebRequest();
        unSubscriptionWebRequest.setChannel("some channel");

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Invalid channel some channel");

        kilkariSubscriptionService.requestUnsubscription("subscriptionId", unSubscriptionWebRequest);

        verify(subscriptionService, never()).requestDeactivation(any(DeactivationRequest.class));
    }

    @Test
    public void shouldValidateCampaignChangeRequest() {
        CampaignChangeRequest campaignChangeRequest = new CampaignChangeRequest();
        String subscriptionId = "subscriptionId";
        String reason = "some reason";
        String channel = "some channel";
        campaignChangeRequest.setReason(reason);
        campaignChangeRequest.setChannel(channel);

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Invalid channel some channel,Invalid reason some reason");

        kilkariSubscriptionService.processCampaignChange(campaignChangeRequest, subscriptionId);

        verify(subscriptionService, never()).rescheduleCampaign(any(CampaignRescheduleRequest.class));
    }

    @Test
    public void shouldProcessCampaignChange() {
        CampaignChangeRequest campaignChangeRequest = new CampaignChangeRequest();
        String subscriptionId = "subscriptionId";
        String reason = "MISCARRIAGE";
        String channel = Channel.CONTACT_CENTER.name();
        DateTime createdAt = DateTime.now();
        campaignChangeRequest.setReason(reason);
        campaignChangeRequest.setCreatedAt(createdAt);
        campaignChangeRequest.setChannel(channel);

        kilkariSubscriptionService.processCampaignChange(campaignChangeRequest, subscriptionId);

        ArgumentCaptor<CampaignRescheduleRequest> campaignRescheduleRequestArgumentCaptor = ArgumentCaptor.forClass(CampaignRescheduleRequest.class);
        verify(subscriptionService).rescheduleCampaign(campaignRescheduleRequestArgumentCaptor.capture());
        CampaignRescheduleRequest campaignRescheduleRequest = campaignRescheduleRequestArgumentCaptor.getValue();
        assertEquals(subscriptionId, campaignRescheduleRequest.getSubscriptionId());
        assertEquals(reason, campaignRescheduleRequest.getReason().name());
        assertEquals(createdAt, campaignRescheduleRequest.getCreatedAt());
    }

    @Test
    public void shouldValidateSubscriptionWebRequest() {
        SubscriberWebRequest request = new SubscriberWebRequest();
        request.setBeneficiaryAge("23a");
        request.setChannel(Channel.CONTACT_CENTER.name());
        request.setCreatedAt(DateTime.now());
        String subscriptionId = "subscriptionId";

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Invalid beneficiary age 23a");
        kilkariSubscriptionService.updateSubscriberDetails(request, subscriptionId);
    }

    @Test
    public void shouldUpdateSubscriberDetails() {
        SubscriberWebRequest request = new SubscriberWebRequest();
        request.setBeneficiaryAge("23");
        request.setChannel(Channel.IVR.name());
        request.setCreatedAt(DateTime.now());
        LocationRequest location = new LocationRequest() {{
            setState("state");
            setDistrict("district");
            setBlock("block");
            setPanchayat("panchayat");
        }};
        request.setLocation(location);
        String subscriptionId = "subscriptionId";

        kilkariSubscriptionService.updateSubscriberDetails(request, subscriptionId);

        ArgumentCaptor<SubscriberRequest> captor = ArgumentCaptor.forClass(SubscriberRequest.class);
        verify(subscriptionService).updateSubscriberDetails(captor.capture());
        SubscriberRequest subscriberRequest = captor.getValue();
        assertEquals(Integer.valueOf(request.getBeneficiaryAge()), subscriberRequest.getBeneficiaryAge());
        assertEquals(request.getChannel(), subscriberRequest.getChannel());
        assertEquals(request.getCreatedAt(), subscriberRequest.getCreatedAt());
        assertEquals(request.getLocation().getBlock(), subscriberRequest.getLocation().getBlock());
        assertEquals(subscriptionId, subscriberRequest.getSubscriptionId());
    }

    @Test
    public void shouldProcessValidChangePackRequest() {
        ChangeSubscriptionWebRequest changeSubscriptionWebRequest = new ChangeSubscriptionWebRequestBuilder().withDefaults().withEDD(DateTime.now().plusMonths(5).toString("dd-MM-yyyy")).build();
        String subscriptionId = "subscriptionId";
        kilkariSubscriptionService.changeSubscription(changeSubscriptionWebRequest, subscriptionId);

        ArgumentCaptor<ChangeSubscriptionRequest> changePackRequestArgumentCaptor = ArgumentCaptor.forClass(ChangeSubscriptionRequest.class);
        verify(changeSubscriptionService).process(changePackRequestArgumentCaptor.capture());
        ChangeSubscriptionRequest changeSubscriptionRequest = changePackRequestArgumentCaptor.getValue();

        assertEquals(subscriptionId, changeSubscriptionRequest.getSubscriptionId());
        assertEquals(changeSubscriptionWebRequest.getPack(), changeSubscriptionRequest.getPack().name());
        assertEquals(changeSubscriptionWebRequest.getChannel(), changeSubscriptionRequest.getChannel().name());
        assertEquals(changeSubscriptionWebRequest.getCreatedAt(), changeSubscriptionRequest.getCreatedAt());
    }

    @Test
    public void shouldValidateChangeMsisdnRequest() {
        ChangeMsisdnWebRequest changeMsisdnWebRequest = new ChangeMsisdnWebRequest();
        changeMsisdnWebRequest.setOldMsisdn("123456789");
        changeMsisdnWebRequest.setNewMsisdn("987654321");
        changeMsisdnWebRequest.setChannel("some channel");
        ArrayList<String> packs = new ArrayList<>();
        packs.add("some pack");
        changeMsisdnWebRequest.setPacks(packs);

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Invalid channel some channel,Invalid msisdn 123456789,Invalid msisdn 987654321,Invalid subscription pack some pack");

        kilkariSubscriptionService.changeMsisdn(changeMsisdnWebRequest);

        verify(subscriptionService, never()).changeMsisdn(any(ChangeMsisdnRequest.class));
    }

    @Test
    public void shouldProcessChangeMsisdnRequest() {
        String oldMsisdn = "1234567890";
        String newMsisdn = "9876543210";
        String channel = Channel.CONTACT_CENTER.name();
        String pack = SubscriptionPack.BARI_KILKARI.name();
        ChangeMsisdnWebRequest changeMsisdnWebRequest = new ChangeMsisdnWebRequest();
        changeMsisdnWebRequest.setOldMsisdn(oldMsisdn);
        changeMsisdnWebRequest.setNewMsisdn(newMsisdn);
        changeMsisdnWebRequest.setChannel(channel);
        ArrayList<String> packs = new ArrayList<>();
        packs.add(pack);
        changeMsisdnWebRequest.setPacks(packs);

        kilkariSubscriptionService.changeMsisdn(changeMsisdnWebRequest);

        ArgumentCaptor<ChangeMsisdnRequest> captor = ArgumentCaptor.forClass(ChangeMsisdnRequest.class);
        verify(subscriptionService).changeMsisdn(captor.capture());
        ChangeMsisdnRequest msisdnRequest = captor.getValue();

        assertEquals(oldMsisdn, msisdnRequest.getOldMsisdn());
        assertEquals(newMsisdn, msisdnRequest.getNewMsisdn());
        assertEquals(channel, msisdnRequest.getChannel().name());
        assertEquals(pack, msisdnRequest.getPacks().get(0).name());
    }

    @Test
    public void shouldThrowExceptionIfValidChangePackRequestIsInvalid() {
        ChangeSubscriptionWebRequest changeSubscriptionWebRequest = new ChangeSubscriptionWebRequestBuilder().withDefaults()
                .withChangeType("wrong change type")
                .build();
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Invalid change type wrong change type");

        kilkariSubscriptionService.changeSubscription(changeSubscriptionWebRequest, "subscriptionId");

        verify(changeSubscriptionService, never()).process(any(ChangeSubscriptionRequest.class));
    }
}
