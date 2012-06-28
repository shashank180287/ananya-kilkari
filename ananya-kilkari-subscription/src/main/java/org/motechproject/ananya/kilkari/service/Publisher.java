package org.motechproject.ananya.kilkari.service;

import org.motechproject.ananya.kilkari.domain.*;
import org.motechproject.scheduler.context.EventContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class Publisher {
    @Autowired
    private EventContext eventContext;

    @Autowired
    public Publisher(@Qualifier("eventContext") EventContext eventContext) {
        this.eventContext = eventContext;
    }

    public void processSubscription(SubscriptionActivationRequest subscriptionActivationRequest) {
        eventContext.send(SubscriptionEventKeys.PROCESS_SUBSCRIPTION, subscriptionActivationRequest);
    }

    public void reportSubscriptionCreation(SubscriptionCreationReportRequest subscriptionCreationReportRequest) {
        eventContext.send(SubscriptionEventKeys.REPORT_SUBSCRIPTION_CREATION, subscriptionCreationReportRequest);
    }

    public void reportSubscriptionStateChange(SubscriptionStateChangeReportRequest subscriptionStateChangeReportRequest) {
        eventContext.send(SubscriptionEventKeys.REPORT_SUBSCRIPTION_STATE_CHANGE, subscriptionStateChangeReportRequest);
    }
}
