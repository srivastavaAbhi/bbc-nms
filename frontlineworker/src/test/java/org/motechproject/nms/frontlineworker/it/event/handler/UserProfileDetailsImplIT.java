package org.motechproject.nms.frontlineworker.it.event.handler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.nms.frontlineworker.Designation;
import org.motechproject.nms.frontlineworker.ServicesUsingFrontLineWorker;
import org.motechproject.nms.frontlineworker.Status;
import org.motechproject.nms.frontlineworker.domain.FrontLineWorker;
import org.motechproject.nms.frontlineworker.domain.UserProfile;
import org.motechproject.nms.frontlineworker.service.FrontLineWorkerCsvService;
import org.motechproject.nms.frontlineworker.service.FrontLineWorkerService;
import org.motechproject.nms.frontlineworker.service.UserProfileDetailsService;
import org.motechproject.nms.frontlineworker.service.impl.UserProfileDetailsServiceImpl;
import org.motechproject.nms.masterdata.domain.Circle;
import org.motechproject.nms.masterdata.domain.District;
import org.motechproject.nms.masterdata.domain.LanguageLocationCode;
import org.motechproject.nms.masterdata.domain.Operator;
import org.motechproject.nms.masterdata.domain.State;
import org.motechproject.nms.masterdata.service.CircleService;
import org.motechproject.nms.masterdata.service.DistrictService;
import org.motechproject.nms.masterdata.service.LanguageLocationCodeService;
import org.motechproject.nms.masterdata.service.OperatorService;
import org.motechproject.nms.masterdata.service.StateService;
import org.motechproject.nms.util.helper.DataValidationException;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.inject.Inject;

import static org.junit.Assert.*;

/**
 * This class models the IT of UserProfileDetails
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class UserProfileDetailsImplIT extends BasePaxIT {
    @Inject
    private StateService stateService;

    @Inject
    private DistrictService districtService;

    @Inject
    private CircleService circleService;

    @Inject
    private LanguageLocationCodeService languageLocationCodeService;

    @Inject
    private OperatorService operatorService;


    @Inject
    private UserProfileDetailsService userProfileDetailsService;

    @Inject
    private FrontLineWorkerService frontLineWorkerService;

    @Inject
    private FrontLineWorkerCsvService frontLineWorkerCsvService;

    private UserProfileDetailsServiceImpl userProfileDetailsImpl;

    private static boolean setUpIsDone = false;

    private State state = null;

    private District district = null;

    private Circle circle = null;

    private LanguageLocationCode languageLocationCode = null;

    private Operator operator = null;

    private TestHelper testHelper = new TestHelper();

    @Before
    public void setUp() {

        if (!setUpIsDone) {
            state = testHelper.createState();
            stateService.create(state);
            assertNotNull(state);

            district = testHelper.createDistrict();
            State stateData = stateService.findRecordByStateCode(district.getStateCode());
            stateData.getDistrict().add(district);
            stateService.update(stateData);
            assertNotNull(district);


            circle = testHelper.createCircle();
            circleService.create(circle);
            assertNotNull(circle);

            languageLocationCode = testHelper.createLanguageLocationCode();

            languageLocationCode.setDistrict(district);
            languageLocationCode.setState(stateData);
            languageLocationCode.setCircle(circle);
            languageLocationCode.setCircleCode(circle.getCode());
            languageLocationCode.setDistrictCode(district.getDistrictCode());
            languageLocationCode.setStateCode(stateData.getStateCode());


            languageLocationCodeService.create(languageLocationCode);
            assertNotNull(languageLocationCode);

            operator = testHelper.createOperator();
            operatorService.create(operator);
            assertNotNull(operator);

            FrontLineWorker frontLineWorker;
            FrontLineWorker frontLineWorkerdb;

            // Record 1 defaultLanguageLocationCodeId is null And Status is ACTIVE

            frontLineWorker = new FrontLineWorker(150L, "1234512345", "Rashi", Designation.USHA,
                    123L, 12L, stateData, district, null, null, null,
                    null, null, null, null, Status.ACTIVE, 123, null);

            frontLineWorker.setCreator("Etasha");
            frontLineWorker.setModifiedBy("Etasha");
            frontLineWorker.setOwner("Etasha");

            frontLineWorkerService.createFrontLineWorker(frontLineWorker);
            frontLineWorkerdb = frontLineWorkerService.getFlwBycontactNo("1234512345");
            assertNotNull(frontLineWorkerdb);

        }
        // do the setup
        setUpIsDone = true;

    }


    @Test
    public void testUserProfileDetailsAll() throws DataValidationException {

        UserProfile userProfile;

        // Record 1 defaultLanguageLocationCodeId is null

        userProfile = userProfileDetailsService.processUserDetails("1234512345", "12", "123", ServicesUsingFrontLineWorker.MOBILEACADEMY);

        assertEquals("12", userProfile.getCircle());
        assertEquals("1234512345", userProfile.getMsisdn());
        assertTrue(123 == userProfile.getLanguageLocationCode());
        assertTrue(10 == userProfile.getMaxStateLevelCappingValue());
        assertEquals(false, userProfile.isCreated());
        assertEquals(false, userProfile.isDefaultLanguageLocationCode());

    }

}

