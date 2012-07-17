package org.motechproject.ananya.kilkari.domain;

import org.junit.Test;
import org.motechproject.ananya.kilkari.subscription.domain.Channel;

import static org.junit.Assert.assertEquals;

public class SubscriberCareRequestMapperTest {
    @Test
    public void shouldMapSubscriberCareRequestToSubscriberCareDoc() {
        String msisdn = "1234567890";
        SubscriberCareReasons reason = SubscriberCareReasons.HELP;
        String channel = "ivr";
        SubscriberCareRequest subscriberCareRequest = new SubscriberCareRequest(msisdn, reason.name().toLowerCase(), channel);

        SubscriberCareDoc subscriberCareDoc = SubscriberCareRequestMapper.map(subscriberCareRequest);

        assertEquals(msisdn, subscriberCareDoc.getMsisdn());
        assertEquals(reason, subscriberCareDoc.getReason());
        assertEquals(Channel.IVR, subscriberCareDoc.getChannel());
        assertEquals(subscriberCareRequest.getCreatedAt(), subscriberCareDoc.getCreatedAt());
    }
}
