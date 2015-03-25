package org.motechproject.nms.kilkari.service.impl;

import org.motechproject.nms.kilkari.domain.AbortionType;
import org.motechproject.nms.kilkari.domain.EntryType;
import org.motechproject.nms.kilkari.domain.MctsCsv;
import org.motechproject.nms.kilkari.domain.Subscriber;
import org.motechproject.nms.kilkari.service.CommonValidatorService;
import org.motechproject.nms.masterdata.domain.*;
import org.motechproject.nms.masterdata.service.CircleService;
import org.motechproject.nms.masterdata.service.LanguageLocationCodeService;
import org.motechproject.nms.masterdata.service.LocationService;
import org.motechproject.nms.masterdata.service.OperatorService;
import org.motechproject.nms.util.constants.ErrorCategoryConstants;
import org.motechproject.nms.util.constants.ErrorDescriptionConstants;
import org.motechproject.nms.util.helper.DataValidationException;
import org.motechproject.nms.util.helper.ParseDataHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This class is used to validate locations
 */
@Service("commonValidatorService")
public class CommonValidatorServiceImpl implements CommonValidatorService {
    
    @Autowired
    private LocationService locationService;


    @Autowired
    private OperatorService operatorService;

    @Autowired
    private CircleService circleService;

    @Autowired
    private LanguageLocationCodeService languageLocationCodeService;
    
    /**
     *  This method is used to fetch state from DB based stateCode
     * 
     *  @param stateCode csv uploaded stateCode
     */
    public State checkAndGetState(Long stateCode) throws DataValidationException {
        State state = locationService.getStateByCode(stateCode);
        if (state == null) {
            ParseDataHelper.raiseInvalidDataException("State Code", stateCode.toString());
        }
        return state;
    }

    /**
     *  This method is used to fetch district from DB based on stateId and districtCode
     * 
     *  @param state State Object
     *  @param districtCode csv uploaded districtCode
     */
    public District checkAndGetDistrict(State state, Long districtCode) throws DataValidationException {
        District district = locationService.getDistrictByCode(state.getId(), districtCode);
        if (district == null) {
            ParseDataHelper.raiseInvalidDataException("District Code", districtCode.toString());
        }
        return district;
    }
    
    /**
     *  This method is used to fetch Taluka from DB 
     *  based on districtId and talukaCode
     * 
     *  @param district District object
     *  @param talukaCode csv uploaded districtCode
     */
    public Taluka checkAndGetTaluka(District district, Long talukaCode) throws DataValidationException {
        Taluka taluka = null;
        if (talukaCode != null) {
            taluka = locationService.getTalukaByCode(district.getId(), talukaCode);
            if (taluka == null) {
                ParseDataHelper.raiseInvalidDataException("Taluka Code", talukaCode.toString());
            }
        }
        return taluka;
    }
    
    /**
     *  This method is used to fetch Health Block from DB 
     *  based on talukaId and healthBlockCode
     * 
     *  @param talukaCode csv uploaded talukaCode
     *  @param taluka Taluka object
     *  @param talukaCode csv uploaded healthBlockCode
     */
    public HealthBlock checkAndGetHealthBlock(Long talukaCode,
                                              Taluka taluka, Long healthBlockCode) throws DataValidationException {
        HealthBlock healthBlock = null;
        if (healthBlockCode != null) {
            if (taluka != null) {
                healthBlock = locationService.getHealthBlockByCode(taluka.getId(), healthBlockCode);
                if (healthBlock == null) {
                    ParseDataHelper.raiseInvalidDataException("Block Code", healthBlockCode.toString());
                }
            } else {
                ParseDataHelper.raiseMissingDataException("Taluka Code", talukaCode.toString());
            }
        }
        return healthBlock;
    }


    /**
     *  This method is used to fetch HealthFacility(phc) from DB 
     *  based on healthBloackId and phcCode
     * 
     *  @param healthBlockCode csv uploaded healthBlockCode
     *  @param healthBlock HealthBlock Object
     *  @param phcCode csv uploaded phcCode
     */
    public HealthFacility checkAndGetPhc(Long healthBlockCode,
                                         HealthBlock healthBlock, Long phcCode)
            throws DataValidationException {
        HealthFacility healthFacility = null;
        if (phcCode != null) {
            if (healthBlock != null) {
                healthFacility = locationService.getHealthFacilityByCode(healthBlock.getId(), phcCode);
                if (healthFacility == null) {
                    ParseDataHelper.raiseInvalidDataException("Phc Code", phcCode.toString());
                }
            } else {
                ParseDataHelper.raiseMissingDataException("Block Code", healthBlockCode.toString()); //Missing Block ID
            }
        }
        return healthFacility;
    }
    
    /**
     *  This method is used to fetch HealthSubFacility(subCenter) from DB 
     *  based on healthFacilityId(phc) and HealthSubFacilityCode(subCenterCode)
     * 
     *  @param phcCode csv uploaded phcCode
     *  @param healthFacility HealthFacility Object
     *  @param subCenterCode csv uploaded subCenterCode
     */
    public HealthSubFacility checkAndGetSubCenter(Long phcCode,
                                                  HealthFacility healthFacility, Long subCenterCode)
            throws DataValidationException {
        HealthSubFacility healthSubFacility = null;
        if (subCenterCode != null) {
            if (healthFacility != null) {
                healthSubFacility = locationService.getHealthSubFacilityByCode(healthFacility.getId(), subCenterCode);
                if (healthSubFacility == null) {
                    ParseDataHelper.raiseInvalidDataException("Sub centered Code", subCenterCode.toString());
                }
            } else {
                ParseDataHelper.raiseMissingDataException("Phc Code", phcCode.toString());
            }
        }
        return healthSubFacility;
    }

    /**
     *  This method is used to fetch village from DB based on talukaId and villageCode
     * 
     *  @param talukaCode csv uploaded talukaCode
     *  @param taluka Taluka Object
     *  @param villageCode csv uploaded districtCode
     */
    public Village checkAndGetVillage(Long talukaCode, Taluka taluka, Long villageCode) throws DataValidationException {
        Village village = null;
        if (villageCode != null) {
            if (taluka != null) {
                village = locationService.getVillageByCode(taluka.getId(), villageCode);
                if (village == null) {
                    ParseDataHelper.raiseInvalidDataException("Village Code", villageCode.toString());
                }
            } else {
                ParseDataHelper.raiseMissingDataException("Taluka Code", talukaCode.toString());
            }
        }
        return village;
    }

    /**
     * This method validates and map location to subscriber
     * @param mctsCsv MctsCsv type object
     * @param subscriber Subscriber type object
     * @return Subscriber type object
     * @throws DataValidationException
     */
    @Override
    public Subscriber validateAndMapMctsLocationToSubscriber(MctsCsv mctsCsv,
                                                             Subscriber subscriber) throws DataValidationException {
        
        Long stateCode = ParseDataHelper.validateAndParseLong("State Code", mctsCsv.getStateCode(),  true);
        State state = checkAndGetState(stateCode);

        Long districtCode = ParseDataHelper.validateAndParseLong("District Code", mctsCsv.getDistrictCode(), true);
        District district = checkAndGetDistrict(state, districtCode);

        Long talukaCode = ParseDataHelper.validateAndParseLong("Taluka Code", mctsCsv.getTalukaCode(), false);
        Taluka taluka = checkAndGetTaluka(district, talukaCode);

        Long healthBlockCode = ParseDataHelper.validateAndParseLong("Health Block Code", mctsCsv.getHealthBlockCode(), false);
        HealthBlock healthBlock = checkAndGetHealthBlock(talukaCode, taluka, healthBlockCode);

        Long phcCode = ParseDataHelper.validateAndParseLong("Phc Code", mctsCsv.getPhcCode(), false);
        HealthFacility healthFacility = checkAndGetPhc(healthBlockCode, healthBlock, phcCode);

        Long subCenterCode = ParseDataHelper.validateAndParseLong("Sub centered Code", mctsCsv.getSubCentreCode(), false);
        HealthSubFacility healthSubFacility = checkAndGetSubCenter(phcCode, healthFacility, subCenterCode);

        Long villageCode = ParseDataHelper.validateAndParseLong("Village Code", mctsCsv.getVillageCode(), false);
        Village village = checkAndGetVillage(talukaCode, taluka, villageCode);

        subscriber.setState(state);
        subscriber.setDistrict(district);
        subscriber.setTaluka(taluka);
        subscriber.setHealthBlock(healthBlock);
        subscriber.setPhc(healthFacility);
        subscriber.setSubCentre(healthSubFacility);
        subscriber.setVillage(village);
        return subscriber;
    }

    /**
     * Validates the entry type value and raises exception if not valid
     * @param entryType string to be validated
     * @throws DataValidationException
     */
    @Override
    public void checkValidEntryType(String entryType) throws DataValidationException {
        if (entryType!=null) {
            boolean foundEntryType = EntryType.checkValidEntryType(entryType);
            if(!foundEntryType){
                ParseDataHelper.raiseInvalidDataException("Entry Type", entryType);
            }
        }
    }

    /**
     * Validates the Abortion type value and raises exception if not valid
     * @param abortion string to be validated
     * @throws DataValidationException
     */
    @Override
    public void checkValidAbortionType(String abortion) throws DataValidationException {
        if(abortion!=null){
            boolean foundAbortionType = AbortionType.checkValidAbortionType(abortion);
            if(!foundAbortionType){
                ParseDataHelper.raiseInvalidDataException("Abortion", abortion);
            }
        }
    }

    /**
     * This method validate the operatorCode from database
     * @param operatorCode code of the operator
     * @throws DataValidationException
     */
    @Override
    public void validateOperator(String operatorCode) throws DataValidationException {
        if (operatorCode != null) {
            Operator operator = operatorService.getRecordByCode(operatorCode);
            if (operator == null) {
                String errMessage = String.format(DataValidationException.INVALID_FORMAT_MESSAGE, "operatorCode", operatorCode);
                String errDesc = String.format(ErrorDescriptionConstants.INVALID_API_PARAMETER_DESCRIPTION, "operatorCode");
                throw new DataValidationException(errMessage, ErrorCategoryConstants.INVALID_DATA, errDesc, "operatorCode");
            }
        }
    }

    /**
     * This method validate the circleCode from database
     * @param circleCode code of the circle
     * @throws DataValidationException
     */
    @Override
    public void validateCircle(String circleCode) throws DataValidationException {
        if (circleCode != null) {
            Circle circle = circleService.getRecordByCode(circleCode);
            if (circle == null) {
                String errMessage = String.format(DataValidationException.INVALID_FORMAT_MESSAGE, "circleCode", circleCode);
                String errDesc = String.format(ErrorDescriptionConstants.INVALID_API_PARAMETER_DESCRIPTION, "circleCode");
                throw new DataValidationException(errMessage, ErrorCategoryConstants.INVALID_DATA, errDesc, "circleCode");
            }
        }
    }

    /**
     * This method validate the languageLocationCode from database
     * @param llcCode value of the languageLocationCode
     * @throws DataValidationException
     */
    @Override
    public void validateLanguageLocationCode(Integer llcCode) throws DataValidationException {
        if (llcCode != null) {
            LanguageLocationCode languageLocationCode = languageLocationCodeService.findLLCByCode(llcCode);
            if (languageLocationCode == null) {
                String errMessage = String.format(DataValidationException.INVALID_FORMAT_MESSAGE, "languageLocationCode", llcCode);
                String errDesc = String.format(ErrorDescriptionConstants.INVALID_API_PARAMETER_DESCRIPTION, "languageLocationCode");
                throw new DataValidationException(errMessage, ErrorCategoryConstants.INVALID_DATA, errDesc, "languageLocationCode");
            }
        }
    }
}