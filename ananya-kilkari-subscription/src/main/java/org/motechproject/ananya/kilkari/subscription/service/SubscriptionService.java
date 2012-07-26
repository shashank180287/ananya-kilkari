package org.motechproject.ananya.kilkari.subscription.service;

import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.messagecampaign.contract.MessageCampaignRequest;
import org.motechproject.ananya.kilkari.messagecampaign.service.MessageCampaignService;
import org.motechproject.ananya.kilkari.reporting.domain.SubscriptionStateChangeReportRequest;
import org.motechproject.ananya.kilkari.reporting.service.ReportingService;
import org.motechproject.ananya.kilkari.subscription.domain.CampaignRescheduleRequest;
import org.motechproject.ananya.kilkari.subscription.domain.Channel;
import org.motechproject.ananya.kilkari.subscription.domain.DeactivationRequest;
import org.motechproject.ananya.kilkari.subscription.domain.Subscription;
import org.motechproject.ananya.kilkari.subscription.exceptions.ValidationException;
import org.motechproject.ananya.kilkari.subscription.repository.AllSubscriptions;
import org.motechproject.ananya.kilkari.subscription.service.mapper.SubscriptionMapper;
import org.motechproject.ananya.kilkari.subscription.service.request.SubscriptionRequest;
import org.motechproject.ananya.kilkari.subscription.service.response.SubscriptionResponse;
import org.motechproject.ananya.kilkari.subscription.validators.SubscriptionValidator;
import org.motechproject.common.domain.PhoneNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SubscriptionService {
    private AllSubscriptions allSubscriptions;
    private OnMobileSubscriptionManagerPublisher onMobileSubscriptionManagerPublisher;
    private SubscriptionValidator subscriptionValidator;
    private ReportingService reportingService;
    private KilkariInboxService kilkariInboxService;
    private MessageCampaignService messageCampaignService;

    @Autowired
    public SubscriptionService(AllSubscriptions allSubscriptions, OnMobileSubscriptionManagerPublisher onMobileSubscriptionManagerPublisher,
                               SubscriptionValidator subscriptionValidator, ReportingService reportingService,
                               KilkariInboxService kilkariInboxService, MessageCampaignService messageCampaignService) {
        this.allSubscriptions = allSubscriptions;
        this.onMobileSubscriptionManagerPublisher = onMobileSubscriptionManagerPublisher;
        this.subscriptionValidator = subscriptionValidator;
        this.reportingService = reportingService;
        this.kilkariInboxService = kilkariInboxService;
        this.messageCampaignService = messageCampaignService;
    }

    public Subscription createSubscription(SubscriptionRequest subscriptionRequest, Channel channel) {
        subscriptionValidator.validate(subscriptionRequest);
        Subscription subscription = new Subscription(subscriptionRequest.getMsisdn(), subscriptionRequest.getPack(), subscriptionRequest.getCreationDate());
        allSubscriptions.add(subscription);

        SubscriptionMapper subscriptionMapper = new SubscriptionMapper();

        scheduleCampaign(subscription);
        onMobileSubscriptionManagerPublisher.sendActivationRequest(subscriptionMapper.createOMSubscriptionRequest(subscription, channel));
        reportingService.reportSubscriptionCreation(
                subscriptionMapper.createSubscriptionCreationReportRequest(subscription, channel, subscriptionRequest.getLocation(), subscriptionRequest.getSubscriber()));

        return subscription;
    }

    public List<SubscriptionResponse> findByMsisdn(String msisdn) {
        validateMsisdn(msisdn);
        List<Subscription> subscriptions = allSubscriptions.findByMsisdn(msisdn);
        return mapToSubscriptionResponse(subscriptions);
    }

    private List<SubscriptionResponse> mapToSubscriptionResponse(List<Subscription> subscriptions) {
        List<SubscriptionResponse> subscriptionResponses = new ArrayList<>();
        for (Subscription subscription : subscriptions) {
            subscriptionResponses.add(new SubscriptionResponse(subscription.getMsisdn(), subscription.getOperator(), subscription.getSubscriptionId(),
                    subscription.getCreationDate(), subscription.endDate(), subscription.getStatus(), subscription.getPack()));
        }
        return subscriptionResponses;
    }

    public void activate(String subscriptionId, DateTime activatedOn, final String operator) {
        updateStatusAndReport(subscriptionId, activatedOn, null, operator, null, new Action<Subscription>() {
            @Override
            public void perform(Subscription subscription) {
                subscription.activate(operator);
            }
        });
    }

    public void activationFailed(String subscriptionId, DateTime updatedOn, String reason, final String operator) {
        updateStatusAndReport(subscriptionId, updatedOn, reason, operator, null, new Action<Subscription>() {
            @Override
            public void perform(Subscription subscription) {
                subscription.activationFailed(operator);
            }
        });
    }

    public void activationRequested(String subscriptionId) {
        updateStatusAndReport(subscriptionId, DateTime.now(), null, null, null, new Action<Subscription>() {
            @Override
            public void perform(Subscription subscription) {
                subscription.activationRequestSent();
            }
        });
    }

    public void requestDeactivation(DeactivationRequest deactivationRequest) {
        String subscriptionId = deactivationRequest.getSubscriptionId();
        updateStatusAndReport(subscriptionId, deactivationRequest.getCreatedAt(), null, null, null, new Action<Subscription>() {
            @Override
            public void perform(Subscription subscription) {
                subscription.deactivationRequestReceived();
            }
        });
        Subscription subscription = allSubscriptions.findBySubscriptionId(subscriptionId);
        onMobileSubscriptionManagerPublisher.processDeactivation(new SubscriptionMapper().createOMSubscriptionRequest(subscription, deactivationRequest.getChannel()));
    }

    public void deactivationRequested(String subscriptionId) {
        updateStatusAndReport(subscriptionId, DateTime.now(), null, null, null, new Action<Subscription>() {
            @Override
            public void perform(Subscription subscription) {
                subscription.deactivationRequestSent();
                kilkariInboxService.scheduleInboxDeletion(subscription);
            }
        });
    }

    public void renewSubscription(String subscriptionId, final DateTime renewedDate, Integer graceCount) {
        updateStatusAndReport(subscriptionId, renewedDate, null, null, graceCount, new Action<Subscription>() {
            @Override
            public void perform(Subscription subscription) {
                subscription.activateOnRenewal();
            }
        });
    }

    public void suspendSubscription(String subscriptionId, final DateTime renewalDate, String reason, Integer graceCount) {
        updateStatusAndReport(subscriptionId, renewalDate, reason, null, graceCount, new Action<Subscription>() {
            @Override
            public void perform(Subscription subscription) {
                subscription.suspendOnRenewal();
            }
        });
    }

    public void deactivateSubscription(String subscriptionId, final DateTime deactivationDate, String reason, Integer graceCount) {
        updateStatusAndReport(subscriptionId, deactivationDate, reason, null, graceCount, new Action<Subscription>() {
            @Override
            public void perform(Subscription subscription) {
                subscription.deactivate();
                kilkariInboxService.scheduleInboxDeletion(subscription);
            }
        });
    }

    public void subscriptionComplete(String subscriptionId) {
        updateStatusAndReport(subscriptionId, DateTime.now(), "Subscription completed", null, null, new Action<Subscription>() {
            @Override
            public void perform(Subscription subscription) {
                subscription.complete();
                kilkariInboxService.scheduleInboxDeletion(subscription);
            }
        });
    }

    public Subscription findBySubscriptionId(String subscriptionId) {
        return allSubscriptions.findBySubscriptionId(subscriptionId);
    }

    public void rescheduleCampaign(CampaignRescheduleRequest campaignRescheduleRequest) {
        Subscription subscription = findBySubscriptionId(campaignRescheduleRequest.getSubscriptionId());
        unScheduleCampaign(subscription);
        scheduleCampaign(campaignRescheduleRequest);
    }

    private void scheduleCampaign(CampaignRescheduleRequest campaignRescheduleRequest) {
        MessageCampaignRequest enrollRequest = new MessageCampaignRequest(campaignRescheduleRequest.getSubscriptionId(), campaignRescheduleRequest.getReason().name(), campaignRescheduleRequest.getCreatedAt());
        messageCampaignService.start(enrollRequest);
    }

    private void scheduleCampaign(Subscription subscription) {
        MessageCampaignRequest campaignRequest = new MessageCampaignRequest(
                subscription.getSubscriptionId(), subscription.getPack().name(), subscription.getCreationDate());
        messageCampaignService.start(campaignRequest);
    }

    private void unScheduleCampaign(Subscription subscription) {
        MessageCampaignRequest unEnrollRequest = new MessageCampaignRequest(subscription.getSubscriptionId(), subscription.getPack().name(), subscription.getCreationDate());
        messageCampaignService.stop(unEnrollRequest);
    }

    private void updateStatusAndReport(String subscriptionId, DateTime updatedOn, String reason, String operator, Integer graceCount, Action<Subscription> action) {
        Subscription subscription = allSubscriptions.findBySubscriptionId(subscriptionId);
        action.perform(subscription);
        allSubscriptions.update(subscription);
        reportingService.reportSubscriptionStateChange(new SubscriptionStateChangeReportRequest(subscription.getSubscriptionId(), subscription.getStatus().name(), updatedOn, reason, operator, graceCount));
    }

    private void validateMsisdn(String msisdn) {
        if (PhoneNumber.isNotValid(msisdn))
            throw new ValidationException(String.format("Invalid msisdn %s", msisdn));
    }
}