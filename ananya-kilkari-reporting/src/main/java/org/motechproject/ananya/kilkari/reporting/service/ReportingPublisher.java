package org.motechproject.ananya.kilkari.reporting.service;

import org.motechproject.ananya.kilkari.reporting.domain.CampaignMessageDeliveryReportRequest;
import org.motechproject.ananya.kilkari.reporting.domain.ReportingEventKeys;
import org.motechproject.ananya.kilkari.reporting.domain.SubscriptionCreationReportRequest;
import org.motechproject.ananya.kilkari.reporting.domain.SubscriptionStateChangeReportRequest;
import org.motechproject.scheduler.context.EventContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ReportingPublisher {
    @Autowired
    private EventContext eventContext;

    @Autowired
    public ReportingPublisher(@Qualifier("eventContext") EventContext eventContext) {
        this.eventContext = eventContext;
    }

    public void reportSubscriptionCreation(SubscriptionCreationReportRequest subscriptionCreationReportRequest) {
        eventContext.send(ReportingEventKeys.REPORT_SUBSCRIPTION_CREATION, subscriptionCreationReportRequest);
    }

    public void reportSubscriptionStateChange(SubscriptionStateChangeReportRequest subscriptionStateChangeReportRequest) {
        eventContext.send(ReportingEventKeys.REPORT_SUBSCRIPTION_STATE_CHANGE, subscriptionStateChangeReportRequest);
    }

    public void reportCampaignMessageDelivered(CampaignMessageDeliveryReportRequest campaignMessageDeliveryReportRequest) {
        eventContext.send(ReportingEventKeys.REPORT_CAMPAIGN_MESSAGE_DELIVERED, campaignMessageDeliveryReportRequest);
    }
}