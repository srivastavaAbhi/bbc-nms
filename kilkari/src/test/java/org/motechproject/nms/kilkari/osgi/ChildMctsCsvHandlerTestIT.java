package org.motechproject.nms.kilkari.osgi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.event.MotechEvent;
import org.motechproject.nms.kilkari.domain.Channel;
import org.motechproject.nms.kilkari.domain.ChildMctsCsv;
import org.motechproject.nms.kilkari.domain.MotherMctsCsv;
import org.motechproject.nms.kilkari.domain.Subscriber;
import org.motechproject.nms.kilkari.domain.Subscription;
import org.motechproject.nms.kilkari.event.handler.ChildMctsCsvHandler;
import org.motechproject.nms.kilkari.event.handler.MotherMctsCsvHandler;
import org.motechproject.nms.kilkari.repository.ChildMctsCsvDataService;
import org.motechproject.nms.kilkari.repository.MotherMctsCsvDataService;
import org.motechproject.nms.kilkari.service.ChildMctsCsvService;
import org.motechproject.nms.kilkari.service.ConfigurationService;
import org.motechproject.nms.kilkari.service.LocationValidatorService;
import org.motechproject.nms.kilkari.service.MotherMctsCsvService;
import org.motechproject.nms.kilkari.service.SubscriberService;
import org.motechproject.nms.kilkari.service.SubscriptionService;
import org.motechproject.nms.masterdata.service.LanguageLocationCodeService;
import org.motechproject.nms.util.service.BulkUploadErrLogService;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
/**
 * Verify that HelloWorldRecordService present, functional.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class ChildMctsCsvHandlerTestIT extends CommonStructure {
    
    @Test
    public void createSubscriptionSubscriberTest() throws Exception {
        System.out.println("Inside createSubscriptionSubscriberTest");
        setUp();
        
        List<Long> uploadedIds = new ArrayList<Long>();
        ChildMctsCsv csv = new ChildMctsCsv();
        csv = createChildMcts(csv);
        csv.setWhomPhoneNo("1");
        csv.setIdNo("1");
        
        ChildMctsCsv dbCsv = childMctsCsvDataService.create(csv);
        uploadedIds.add(dbCsv.getId());
        callChildMctsCsvHandlerSuccessEvent(uploadedIds); //create new record
        
        Subscription dbSubscription = subscriptionService.getSubscriptionByMctsIdState(csv.getIdNo(), Long.parseLong(csv.getStateCode()));
        Subscriber dbSubscriber = dbSubscription.getSubscriber();
        assertNotNull(dbSubscription);
        assertNotNull(dbSubscriber);
        assertTrue(dbSubscription.getChannel().equals(Channel.MCTS));
        assertTrue(dbSubscriber.getName().equals(csv.getMotherName()));
        assertTrue(dbSubscriber.getState().getStateCode().toString().equals(csv.getStateCode()));
    } 
    
    @Test
    public void createSameMsisdnDifferentMcts() throws Exception {
        System.out.println("Inside createSameMsisdnDifferentMcts");
        setUp();
        
        List<Long> uploadedIds = new ArrayList<Long>();
        ChildMctsCsv csv = new ChildMctsCsv();
        csv = createChildMcts(csv);
        csv.setWhomPhoneNo("2");
        csv.setIdNo("2");
        ChildMctsCsv dbCsv = childMctsCsvDataService.create(csv);
        uploadedIds.add(dbCsv.getId());
        callChildMctsCsvHandlerSuccessEvent(uploadedIds); // Created New Record
        uploadedIds.clear();
        
        ChildMctsCsv csv1 = new ChildMctsCsv();
        csv1 = createChildMcts(csv1);
        csv1.setWhomPhoneNo("2");
        csv1.setIdNo("3");
        ChildMctsCsv dbCsv1 = childMctsCsvDataService.create(csv1);
        uploadedIds.add(dbCsv1.getId());
        callChildMctsCsvHandlerSuccessEvent(uploadedIds); // Record_Already Exist
        
        Subscription dbSubscription = subscriptionService.getSubscriptionByMctsIdState(csv.getIdNo(), Long.parseLong(csv.getStateCode()));
        assertNotNull(dbSubscription);
        dbSubscription = subscriptionService.getSubscriptionByMctsIdState(csv1.getIdNo(), Long.parseLong(csv1.getStateCode()));
        assertNull(dbSubscription);
    }
    
    @Test
    public void createSameMsisdnSameMcts() throws Exception {
        System.out.println("Inside createSameMsisdnSameMcts");
        setUp();
        
        List<Long> uploadedIds = new ArrayList<Long>();
        ChildMctsCsv csv = new ChildMctsCsv();
        csv = createChildMcts(csv);
        csv.setWhomPhoneNo("4");
        csv.setIdNo("4");
        ChildMctsCsv dbCsv = childMctsCsvDataService.create(csv);
        uploadedIds.add(dbCsv.getId());
        callChildMctsCsvHandlerSuccessEvent(uploadedIds); // Created New Record
        uploadedIds.clear();
        Subscription subscription = subscriptionService.getSubscriptionByMctsIdState(csv.getIdNo(), Long.parseLong(csv.getStateCode()));
        
        ChildMctsCsv csv1 = new ChildMctsCsv();
        csv1 = createChildMcts(csv1);
        csv1.setWhomPhoneNo("4");
        csv1.setIdNo("4");
        csv1.setMotherName("testing");
        csv1.setBirthdate("2015-01-20 08:08:08");
        ChildMctsCsv dbCsv1 = childMctsCsvDataService.create(csv1);
        uploadedIds.add(dbCsv1.getId());
        callChildMctsCsvHandlerSuccessEvent(uploadedIds); // Record update when matching Msisdn and Mctsid
        Subscription updateSubs = subscriptionService.getSubscriptionByMctsIdState(csv1.getIdNo(), Long.parseLong(csv1.getStateCode()));
        
        assertNotNull(subscription);
        assertNotNull(updateSubs);
        assertNotNull(subscription.getSubscriber());
        assertNotNull(updateSubs.getSubscriber());
        assertFalse(subscription.getSubscriber().getName().equals(updateSubs.getSubscriber().getName()));
        assertFalse(subscription.getSubscriber().getDob().equals(updateSubs.getSubscriber().getDob()));
    }
    
    @Test
    public void createDifferentMsisdnSameMcts() throws Exception {
        System.out.println("Inside createDifferentMsisdnSameMcts");
        setUp();
        List<Long> uploadedIds = new ArrayList<Long>();
        ChildMctsCsv csv = new ChildMctsCsv();
        csv = createChildMcts(csv);
        csv.setWhomPhoneNo("5");
        csv.setIdNo("5");
        ChildMctsCsv dbCsv = childMctsCsvDataService.create(csv);
        uploadedIds.add(dbCsv.getId());
        callChildMctsCsvHandlerSuccessEvent(uploadedIds); // Created New Record
        uploadedIds.clear();
        Subscription subscription = subscriptionService.getSubscriptionByMctsIdState(csv.getIdNo(), Long.parseLong(csv.getStateCode()));
        
        ChildMctsCsv csv1 = new ChildMctsCsv();
        csv1 = createChildMcts(csv1);
        csv1.setWhomPhoneNo("6");
        csv1.setIdNo("5");
        csv1.setMotherName("testDifferentName");
        csv1.setBirthdate("2015-01-22 08:08:08");
        ChildMctsCsv dbCsv1 = childMctsCsvDataService.create(csv1);
        uploadedIds.add(dbCsv1.getId());
        callChildMctsCsvHandlerSuccessEvent(uploadedIds); // Record update when different Msisdn and matching Mctsid
        Subscription updateSubs = subscriptionService.getSubscriptionByMctsIdState(csv1.getIdNo(), Long.parseLong(csv1.getStateCode()));

        
        assertNotNull(subscription);
        assertNotNull(updateSubs);
        assertNotNull(subscription.getSubscriber());
        assertNotNull(updateSubs.getSubscriber());
        assertTrue(!subscription.getSubscriber().getName().equals(updateSubs.getSubscriber().getName()));
        assertTrue(!subscription.getSubscriber().getDob().equals(updateSubs.getSubscriber().getDob()));
    }
    
    @Test
    public void createDeleteOperation() throws Exception {
        System.out.println("Inside  createDeleteOperation");
        setUp();
        
        List<Long> uploadedIds = new ArrayList<Long>();
        ChildMctsCsv csv = new ChildMctsCsv();
        csv = createChildMcts(csv);
        csv.setWhomPhoneNo("12");
        csv.setIdNo("7");
        ChildMctsCsv dbCsv = childMctsCsvDataService.create(csv);
        uploadedIds.add(dbCsv.getId());
        callChildMctsCsvHandlerSuccessEvent(uploadedIds); // Created New Record
        uploadedIds.clear();
        Subscription subscription = subscriptionService.getSubscriptionByMctsIdState(csv.getIdNo(), Long.parseLong(csv.getStateCode()));
        
        ChildMctsCsv csv1 = new ChildMctsCsv();
        csv1 = createChildMcts(csv1);
        csv1.setWhomPhoneNo("13");
        csv1.setIdNo("7");
        csv1.setOperation("Delete");
        ChildMctsCsv dbCsv1 = childMctsCsvDataService.create(csv1);
        uploadedIds.add(dbCsv1.getId());
        callChildMctsCsvHandlerSuccessEvent(uploadedIds); // Record update when different Msisdn and matching Mctsid
        Subscription updateSubs = subscriptionService.getSubscriptionByMctsIdState(csv1.getIdNo(), Long.parseLong(csv1.getStateCode())); //Operation Delete, Deactivate Subscription
        
        assertFalse(subscription.getStatus().equals(updateSubs.getStatus()));
        assertTrue(subscription.getSubscriber().getName().equals(updateSubs.getSubscriber().getName()));
    }
    
    @Test
    public void createDiffMsisdnDiffChildMctsSameMotherMcts() throws Exception {
        System.out.println("Inside  createDeleteOperation");
        setUp();
        
        List<Long> uploadedIds = new ArrayList<Long>();
        MotherMctsCsv csv = new MotherMctsCsv();
        csv = createMotherMcts(csv);
        csv.setWhomPhoneNo("20");
        csv.setIdNo("20");
        MotherMctsCsv dbCsv = motherMctsCsvDataService.create(csv);
        uploadedIds.add(dbCsv.getId());
        callMotherMctsCsvHandlerSuccessEvent(uploadedIds); // Created New Record
        uploadedIds.clear();
        Subscription subscription = subscriptionService.getSubscriptionByMctsIdState(csv.getIdNo(), Long.parseLong(csv.getStateCode()));
        
        ChildMctsCsv childCsv = new ChildMctsCsv();
        childCsv = createChildMcts(childCsv);
        childCsv.setWhomPhoneNo("21");
        childCsv.setIdNo("21");
        childCsv.setMotherId("20");
        ChildMctsCsv dbCsv1 = childMctsCsvDataService.create(childCsv);
        uploadedIds.add(dbCsv1.getId());
        callChildMctsCsvHandlerSuccessEvent(uploadedIds); // Created New Record
        uploadedIds.clear();
        Subscription updateSubs = subscriptionService.getSubscriptionByMctsIdState(childCsv.getIdNo(), Long.parseLong(childCsv.getStateCode()));
        
        assertNotNull(subscription);
        assertNotNull(updateSubs);
        assertFalse(subscription.getStatus()==updateSubs.getStatus());
        assertFalse(subscription.getPackName().equals(updateSubs.getPackName()));
        assertFalse(subscription.getSubscriber().getName().equals(updateSubs.getSubscriber().getName()));
    }
    
   
}