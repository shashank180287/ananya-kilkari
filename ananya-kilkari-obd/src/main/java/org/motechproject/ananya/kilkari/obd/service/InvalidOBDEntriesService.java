package org.motechproject.ananya.kilkari.obd.service;

import org.motechproject.ananya.kilkari.obd.domain.InvalidCallRecord;
import org.motechproject.ananya.kilkari.obd.repository.AllInvalidCallRecords;
import org.motechproject.ananya.kilkari.obd.service.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class InvalidOBDEntriesService {
    private AllInvalidCallRecords allInvalidCallRecords;

    private static final Logger logger = LoggerFactory.getLogger(InvalidOBDEntriesService.class);

    @Autowired
    InvalidOBDEntriesService(AllInvalidCallRecords allInvalidCallRecords) {
        this.allInvalidCallRecords = allInvalidCallRecords;
    }

    public void processInvalidCallRecords(List<InvalidCallRecord> invalidCallRecords) {
        if (invalidCallRecords.isEmpty()) {
            return;
        }
        for (InvalidCallRecord record : invalidCallRecords) {
            allInvalidCallRecords.add(record);
        }
        logger.error(String.format("Received obd callback for %s invalid call records." + System.lineSeparator() + "Subscription Id of Invalid call records are : %s" + System.lineSeparator(),
                invalidCallRecords.size(), JsonUtils.toJson(getListOfSubscriptionId(invalidCallRecords))));
    }

    private List<String> getListOfSubscriptionId(List<InvalidCallRecord> invalidCallRecords) {
        List<String> subscriptionIdList = new ArrayList<>();
        for (InvalidCallRecord invalidCallRecord : invalidCallRecords) {
            subscriptionIdList.add(invalidCallRecord.getId());
        }
        return subscriptionIdList;
    }

    public void deleteInvalidCallRecordsFor(String subscriptionId) {
        allInvalidCallRecords.deleteFor(subscriptionId);
    }
}