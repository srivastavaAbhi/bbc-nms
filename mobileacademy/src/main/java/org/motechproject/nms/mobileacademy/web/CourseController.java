package org.motechproject.nms.mobileacademy.web;

import org.apache.log4j.Logger;
import org.motechproject.mtraining.domain.Course;
import org.motechproject.mtraining.domain.CourseUnitState;
import org.motechproject.nms.mobileacademy.commons.MobileAcademyConstants;
import org.motechproject.nms.mobileacademy.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 
 * CourseController handles requests for Get Course and get Course version
 *
 */
@Controller
public class CourseController extends BaseController {

    private static final Logger LOGGER = Logger
            .getLogger(CourseController.class);

    @Autowired
    private CourseService courseService;

    /**
     * Get Course API
     * 
     * @return the current course in system in JSON format
     */
    @RequestMapping(value = "/course", method = RequestMethod.GET)
    public ResponseEntity<String> getCourse() {
        LOGGER.info("getCourse: Started");
        Course course = courseService.getMtrainingCourse();
        /* In case of no course present */
        if (course == null) {
            LOGGER.error(MobileAcademyConstants.NO_COURSE_PRESENT);
            LOGGER.info("getCourse: Ended");
            return getErrorResponse(MobileAcademyConstants.NO_COURSE_PRESENT);
        }
        /* In case of course upload in progress, course state inactive */
        else if (course.getState() == CourseUnitState.Inactive) {
            LOGGER.error(MobileAcademyConstants.COURSE_UPLOAD_ONGOING);
            LOGGER.info("getCourse: Ended");
            return getErrorResponse(MobileAcademyConstants.COURSE_UPLOAD_ONGOING);
        }
        /* In the case when course is present and its state is active */
        else {
            String courseJson = courseService.getCourseJson();
            LOGGER.info("getCourse: Ended");
            return new ResponseEntity<String>(courseJson, HttpStatus.OK);
        }
    }

    /**
     * Get Course Version API
     * 
     * @return the version of the current course in the system in Integer
     */
    @RequestMapping(value = "/courseVersion", method = RequestMethod.GET)
    public ResponseEntity<String> getCourseVersion() {
        LOGGER.info("getCourseVersion: Started");
        Course course = courseService.getMtrainingCourse();
        /* Case: when there is no course present in the system */
        if (course == null) {
            LOGGER.error(MobileAcademyConstants.NO_COURSE_PRESENT);
            LOGGER.info("getCourseVersion: Ended");
            return getErrorResponse(MobileAcademyConstants.NO_COURSE_PRESENT);
        }
        /* Case: when the course upload is ongoing and state is inactive */
        else if (course.getState() == CourseUnitState.Inactive) {
            LOGGER.error(MobileAcademyConstants.COURSE_UPLOAD_ONGOING);
            LOGGER.info("getCourseVersion: Ended");
            return getErrorResponse(MobileAcademyConstants.COURSE_UPLOAD_ONGOING);
        }
        /* Case: when the course is present and its state is active */
        else {
            int courseVersion = courseService.getCurrentCourseVersion();
            LOGGER.info("getCourseVersion: Ended");
            return new ResponseEntity<String>(getJsonNode(
                    MobileAcademyConstants.COURSE_KEY_VERSION, courseVersion),
                    HttpStatus.OK);
        }
    }

    /*
     * This provides a JSON node in form of key-value node for string value
     */
    private String getJsonNode(String key, String value) {
        String response;
        response = "{\"" + key + "\":\"" + value + "\"}";
        return response;
    }

    /*
     * This provides a JSON node in form of key-value node for integer value
     */
    private String getJsonNode(String key, int value) {
        String response;
        response = "{\"" + key + "\":" + value + "}";
        return response;
    }

    /*
     * This function creates error response for Internal Server Error for a
     * error cause
     */
    private ResponseEntity<String> getErrorResponse(String errorCause) {
        return new ResponseEntity<String>(getJsonNode(
                MobileAcademyConstants.FAILURE_REASON, errorCause),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
