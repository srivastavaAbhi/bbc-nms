package org.motechproject.nms.masterdata.ut.domain;

import org.junit.Before;
import org.junit.Test;
import org.motechproject.nms.masterdata.domain.District;
import org.motechproject.nms.masterdata.domain.State;
import org.motechproject.nms.masterdata.it.TestHelper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Created by abhishek on 11/3/15.
 */
public class DistrictTest {

    @Before
    public void init() {
        initMocks(this);
    }

    @Test
    public void testEquals(){

        District district = TestHelper.getDistrictData();

        assertTrue(district.equals(district));
    }

    @Test
    public void testUnEquals(){

        District district = TestHelper.getDistrictData();

        State state = TestHelper.getStateData();

        assertFalse(district.equals(state));
    }

    @Test
    public void testEqualsWithDifferentStateCode(){

        District district = TestHelper.getDistrictData();

        assertFalse(getDistrictDataWithDifferentStateCode().equals(district));
    }

    @Test
    public void testEqualsForDistrictCode(){

        District district = TestHelper.getDistrictData();

        assertFalse(getDistrictDataWithDifferentDistrictCode().equals(district));
    }

    private District getDistrictDataWithDifferentStateCode() {

        District district = new District();
        district.setStateCode(1L);
        district.setDistrictCode(456L);

        return district;
    }

    private District getDistrictDataWithDifferentDistrictCode() {

        District district = new District();
        district.setStateCode(123L);
        district.setDistrictCode(4L);

        return district;
    }

}
