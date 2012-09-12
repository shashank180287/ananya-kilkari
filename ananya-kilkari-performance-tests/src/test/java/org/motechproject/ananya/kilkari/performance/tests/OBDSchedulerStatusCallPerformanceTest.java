package org.motechproject.ananya.kilkari.performance.tests;

import org.joda.time.DateTime;
import org.junit.runner.RunWith;
import org.motechproject.ananya.kilkari.obd.domain.CampaignMessage;
import org.motechproject.ananya.kilkari.obd.domain.CampaignMessageStatus;
import org.motechproject.ananya.kilkari.performance.tests.service.api.OBDApiService;
import org.motechproject.ananya.kilkari.performance.tests.service.db.OBDDbService;
import org.motechproject.ananya.kilkari.performance.tests.service.db.SubscriptionDbService;
import org.motechproject.ananya.kilkari.performance.tests.utils.BasePerformanceTest;
import org.motechproject.ananya.kilkari.request.CallDurationWebRequest;
import org.motechproject.ananya.kilkari.request.OBDSuccessfulCallDetailsWebRequest;
import org.motechproject.ananya.kilkari.subscription.domain.Operator;
import org.motechproject.ananya.kilkari.subscription.domain.Subscription;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionPack;
import org.motechproject.performance.tests.LoadPerfBefore;
import org.motechproject.performance.tests.LoadPerfStaggered;
import org.motechproject.performance.tests.LoadRunner;

import java.util.ArrayList;
import java.util.List;

import static org.motechproject.ananya.kilkari.performance.tests.utils.TestUtils.*;

@RunWith(LoadRunner.class)
public class OBDSchedulerStatusCallPerformanceTest extends BasePerformanceTest {
    private Operator[] possibleOperators = Operator.values();
    private final static int numberOfMessagesInDb = 100;
    private final static int numberOfObdRequests = 100;
    private static String lockName = "lock";
    private static int index;
    private static List<CampaignMessage> campaignMessageList = new ArrayList<>();
    private SubscriptionDbService subscriptionDbService = new SubscriptionDbService();

    public OBDSchedulerStatusCallPerformanceTest(String testName) {
        super(testName);
    }

    private OBDApiService obdApiService = new OBDApiService();
    private OBDDbService obdDbService = new OBDDbService();

    @LoadPerfBefore(priority = 1, concurrentUsers = 10)
    public void loadSubscriptions() {
        for (int i = 0; i < numberOfMessagesInDb / 10; i++) {
            DateTime now = DateTime.now();
            String msisdn = getRandomMsisdn();
            String week = getRandomCampaignId();
            Operator operator = getRandomElementFromList(possibleOperators);

            Subscription subscription = new Subscription(msisdn, SubscriptionPack.BARI_KILKARI, now, now);
            subscription.activate(operator.toString(), now);
            subscriptionDbService.addSubscription(subscription);

            CampaignMessage campaignMessage = new CampaignMessage(
                    subscription.getSubscriptionId(), week, msisdn, operator.name(), now.plusWeeks(1));
            campaignMessage.setStatusCode(CampaignMessageStatus.NEW);
            campaignMessage.markSent();
            obdDbService.add(campaignMessage);

            campaignMessageList.add(campaignMessage);
        }
    }

    @LoadPerfStaggered(totalNumberOfUsers = numberOfObdRequests, minMaxRandomBatchSizes = {"5", "10"}, minDelayInMillis = 1000, delayVariation = 1000)
    public void shouldPerformanceTestOBDScheduling() {
        OBDSuccessfulCallDetailsWebRequest request = getOBDCallBackRequestToSend();
        obdApiService.sendOBDCallbackRequest(request);
    }

    private OBDSuccessfulCallDetailsWebRequest getOBDCallBackRequestToSend() {
        CampaignMessage campaignMessage = campaignMessageList.get(getIndex());
        OBDSuccessfulCallDetailsWebRequest obdSuccessfulCallDetailsWebRequest =
                new OBDSuccessfulCallDetailsWebRequest(campaignMessage.getMsisdn(), campaignMessage.getMessageId(),
                new CallDurationWebRequest(dateString(DateTime.now().minusSeconds(30)), dateString(DateTime.now())), null);
        obdSuccessfulCallDetailsWebRequest.setSubscriptionId(campaignMessage.getSubscriptionId());
        return obdSuccessfulCallDetailsWebRequest;
    }

    public int getIndex() {
        synchronized (lockName) {
            return index++;
        }
    }

}