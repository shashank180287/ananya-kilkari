package org.motechproject.ananya.kilkari.subscription.service;

import org.junit.Test;
import org.motechproject.ananya.kilkari.subscription.repository.OnMobileEndpoints;
import org.motechproject.ananya.kilkari.subscription.repository.SpringIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class OnMobileEndPointsIT extends SpringIntegrationTest {

    @Autowired
    private OnMobileEndpoints onMobileEndpoints;

    @Test
    public void shouldCreateSubscriptionActivationRequest() throws IOException {
        String url = onMobileEndpoints.activateSubscriptionURL();

        assertThat(url, is("http://localhost:1111/OMSM/ActivateSubscription?msisdn={msisdn}&srvkey={srvkey}&mode={mode}&refid={refid}"));
    }
}
