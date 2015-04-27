package org.motechproject.nms.kilkari.ut.dto;


import org.junit.Assert;
import org.junit.Test;
import org.motechproject.nms.kilkari.domain.SubscriptionPack;
import org.motechproject.nms.kilkari.dto.response.SubscriberDetailApiResponse;

import java.util.ArrayList;
import java.util.List;

public class SubscriberDetailApiResponseTest {

    @Test
    public void shouldSetValuesInSubscriberDetailApiResponse() {

        SubscriberDetailApiResponse subscriberDetailApiResponse = new SubscriberDetailApiResponse();

        List<String> subscriptionPacks = new ArrayList<String>(2);
        subscriptionPacks.add(SubscriptionPack.PACK_48_WEEKS.getValue());
        subscriptionPacks.add(SubscriptionPack.PACK_72_WEEKS.getValue());

        subscriberDetailApiResponse.setCircle("circle");
        Assert.assertEquals("circle", subscriberDetailApiResponse.getCircle());

        subscriberDetailApiResponse.setDefaultLanguageLocationCode("99");
        Assert.assertTrue("99".equals(subscriberDetailApiResponse.getDefaultLanguageLocationCode()));

        subscriberDetailApiResponse.setLanguageLocationCode("13");
        Assert.assertTrue("13".equals(subscriberDetailApiResponse.getLanguageLocationCode()));

        subscriberDetailApiResponse.setSubscriptionPackList(subscriptionPacks);
        Assert.assertEquals(subscriptionPacks, subscriberDetailApiResponse.getSubscriptionPackList());
    }
}
