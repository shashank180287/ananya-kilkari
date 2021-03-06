package org.motechproject.ananya.kilkari.performance.tests.service.api;

import org.motechproject.ananya.kilkari.performance.tests.domain.BaseResponse;
import org.motechproject.ananya.kilkari.performance.tests.utils.HttpUtils;
import org.motechproject.ananya.kilkari.performance.tests.utils.TimedRunner;
import org.motechproject.ananya.kilkari.request.CallbackRequest;
import org.motechproject.ananya.kilkari.subscription.domain.Operator;
import org.motechproject.ananya.kilkari.subscription.domain.Subscription;
import org.motechproject.ananya.kilkari.subscription.repository.AllSubscriptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.motechproject.ananya.kilkari.performance.tests.utils.TestUtils.getRandomMsisdn;

@Service
public class SubscriptionApiService {

    @Autowired
    private AllSubscriptions allSubscriptions;

    @Autowired
    private HttpUtils httpUtils;

    public Subscription getSubscriptionData(final String msisdn, final String status) throws InterruptedException {
        return new TimedRunner<Subscription>(100, 1000) {
            @Override
            protected Subscription run() {
                List<Subscription> subscriptionList = allSubscriptions.findByMsisdn(msisdn);
                if (subscriptionList.size() != 0 && subscriptionList.get(0).getStatus().name().equals(status))
                    return subscriptionList.get(0);
                return null;
            }
        }.executeWithTimeout();
    }

    public void createASubscription() {
        Map<String, String> parametersMap = constructParameters();

        BaseResponse baseResponse = httpUtils.httpGetKilkariWithJsonResponse(parametersMap, "subscription");
        assertEquals("SUCCESS", baseResponse.getStatus());
    }

    public void activate(Subscription subscription) {
        CallbackRequest request = new CallbackRequest();
        request.setAction("ACT");
        request.setStatus("SUCCESS");
        request.setMsisdn(subscription.getMsisdn());
        request.setOperator(Operator.AIRTEL.name());
        httpUtils.put(request, "subscription/" + subscription.getSubscriptionId());
    }

    private Map<String, String> constructParameters() {
        Map<String, String> parametersMap = new HashMap<>();
        parametersMap.put("msisdn", getRandomMsisdn());
        parametersMap.put("channel", "IVR");
        parametersMap.put("pack", "bari_kilkari");
        return parametersMap;

    }

    public List<Subscription> getAll() {
        return allSubscriptions.getAll();
    }
}
