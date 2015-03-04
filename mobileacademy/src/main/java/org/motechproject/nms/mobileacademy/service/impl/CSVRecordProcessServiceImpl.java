package org.motechproject.nms.mobileacademy.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.motechproject.mtraining.domain.Course;
import org.motechproject.mtraining.domain.CourseUnitState;
import org.motechproject.nms.mobileacademy.commons.ContentType;
import org.motechproject.nms.mobileacademy.commons.CourseFlags;
import org.motechproject.nms.mobileacademy.commons.FileType;
import org.motechproject.nms.mobileacademy.commons.MobileAcademyConstants;
import org.motechproject.nms.mobileacademy.commons.Record;
import org.motechproject.nms.mobileacademy.domain.ChapterContent;
import org.motechproject.nms.mobileacademy.domain.CourseProcessedContent;
import org.motechproject.nms.mobileacademy.domain.CourseRawContent;
import org.motechproject.nms.mobileacademy.domain.LessonContent;
import org.motechproject.nms.mobileacademy.domain.QuestionContent;
import org.motechproject.nms.mobileacademy.domain.QuizContent;
import org.motechproject.nms.mobileacademy.domain.ScoreContent;
import org.motechproject.nms.mobileacademy.repository.ChapterContentDataService;
import org.motechproject.nms.mobileacademy.service.CSVRecordProcessService;
import org.motechproject.nms.mobileacademy.service.CoursePopulateService;
import org.motechproject.nms.mobileacademy.service.CourseProcessedContentService;
import org.motechproject.nms.mobileacademy.service.CourseRawContentService;
import org.motechproject.nms.mobileacademy.service.MasterDataService;
import org.motechproject.nms.util.BulkUploadError;
import org.motechproject.nms.util.CsvProcessingSummary;
import org.motechproject.nms.util.constants.ErrorCategoryConstants;
import org.motechproject.nms.util.helper.DataValidationException;
import org.motechproject.nms.util.helper.ParseDataHelper;
import org.motechproject.nms.util.service.BulkUploadErrLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This class contains the implementation for RecordProcessService to process
 * CSV records
 * 
 * @author Yogesh
 *
 */
@Service("CSVRecordProcessService")
public class CSVRecordProcessServiceImpl implements CSVRecordProcessService {

    @Autowired
    private CourseRawContentService courseRawContentService;

    @Autowired
    private CourseProcessedContentService courseProcessedContentService;

    @Autowired
    private ChapterContentDataService chapterContentDataService;

    @Autowired
    private CoursePopulateService coursePopulateService;

    @Autowired
    private MasterDataService masterDataService;

    @Autowired
    private BulkUploadErrLogService bulkUploadErrLogService;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(CSVRecordProcessServiceImpl.class);

    @Override
    public String processRawRecords(List<CourseRawContent> courseRawContents,
            String csvFileName) {

        BulkUploadError errorDetail = new BulkUploadError();
        CsvProcessingSummary result = new CsvProcessingSummary();
        String userName = "";

        String errorFileName = BulkUploadError
                .createBulkUploadErrLogFileName(csvFileName);

        Map<Integer, List<CourseRawContent>> mapForAddRecords = new HashMap<Integer, List<CourseRawContent>>();
        Map<String, List<CourseRawContent>> mapForModifyRecords = new HashMap<String, List<CourseRawContent>>();
        Map<Integer, List<CourseRawContent>> mapForDeleteRecords = new HashMap<Integer, List<CourseRawContent>>();

        List<Integer> listOfExistingLLCinCPC = courseProcessedContentService
                .getListOfAllExistingLLcs();

        if (CollectionUtils.isNotEmpty(courseRawContents)) {
            userName = courseRawContents.get(0).getOwner();
            Iterator<CourseRawContent> recordIterator = courseRawContents
                    .iterator();

            while (recordIterator.hasNext()) {
                CourseRawContent courseRawContent = recordIterator.next();
                try {
                    validateSchema(courseRawContent);
                    validateCircleAndLLC(courseRawContent);
                } catch (DataValidationException ex) {
                    processError(errorDetail, ex,
                            courseRawContent.getContentId());
                    bulkUploadErrLogService.writeBulkUploadErrLog(
                            errorFileName, errorDetail);
                    result.incrementFailureCount();

                    recordIterator.remove();
                    courseRawContentService.delete(courseRawContent);
                    LOGGER.warn("Schema Validation failed for Content ID: {}",
                            courseRawContent.getContentId());
                    continue;
                }

                if (MobileAcademyConstants.COURSE_DEL
                        .equalsIgnoreCase(courseRawContent.getOperation())) {
                    putRecordInDeleteMap(mapForDeleteRecords, courseRawContent);
                    LOGGER.info(
                            "Record moved to Delete Map for Content ID: {}",
                            courseRawContent.getContentId());
                } else {
                    int LLC = Integer.parseInt(courseRawContent
                            .getLanguageLocationCode());
                    if (listOfExistingLLCinCPC.contains(LLC)) {
                        courseRawContent
                                .setOperation(MobileAcademyConstants.COURSE_MOD);
                        putRecordInModifyMap(mapForModifyRecords,
                                courseRawContent);
                        LOGGER.info(
                                "Record moved to Modify Map for Content ID: {}",
                                courseRawContent.getContentId());
                    } else {
                        courseRawContent
                                .setOperation(MobileAcademyConstants.COURSE_ADD);
                        putRecordInAddMap(mapForAddRecords, courseRawContent);
                        LOGGER.info(
                                "Record moved to Addition Map for Content ID: {}",
                                courseRawContent.getContentId());
                    }
                }
                recordIterator.remove();
            }
        }

        processAddRecords(mapForAddRecords, errorFileName, result);
        processModificationRecords(mapForModifyRecords, errorFileName, result);
        processDeleteRecords(mapForDeleteRecords, errorFileName, result);

        bulkUploadErrLogService.writeBulkUploadProcessingSummary(userName,
                csvFileName, errorFileName, result);
        LOGGER.info("Finished processing CircleCsv-import success");

        return "Records Processed Successfully";
    }

    /*
     * This function validates if the CourseRawContent contains valid circle and
     * LLC
     */
    private boolean validateCircleAndLLC(CourseRawContent courseRawContent)
            throws DataValidationException {
        String circle = courseRawContent.getCircle();
        int llc = Integer.parseInt(courseRawContent.getLanguageLocationCode());
        if (!masterDataService.isCircleValid(circle)) {
            LOGGER.info("circle is not valid for content ID: {}",
                    courseRawContent.getContentId());
            throw new DataValidationException(
                    MobileAcademyConstants.INCONSISTENT_DATA_MESSAGE,
                    ErrorCategoryConstants.INCONSISTENT_DATA, "Circle");
        }
        if (!masterDataService.isLLCValidInCircle(circle, llc)) {
            LOGGER.info("LLC doesn't exist in circle for content ID: {}",
                    courseRawContent.getContentId());
            throw new DataValidationException(
                    MobileAcademyConstants.INCONSISTENT_DATA_MESSAGE,
                    ErrorCategoryConstants.INCONSISTENT_DATA,
                    "Language Location Code");
        }
        return true;
    }

    /*
     * This function adds the records into a Map having LLC of record as key
     * 
     * The map will be process afterwards for processing ADD Records
     */
    private void putRecordInAddMap(
            Map<Integer, List<CourseRawContent>> mapForAddRecords,
            CourseRawContent courseRawContent) {
        int LLC = Integer.parseInt(courseRawContent.getLanguageLocationCode());
        if (mapForAddRecords.containsKey(LLC)) {
            mapForAddRecords.get(LLC).add(courseRawContent);
        } else {
            ArrayList<CourseRawContent> listOfRecords = new ArrayList<CourseRawContent>();
            listOfRecords.add(courseRawContent);
            mapForAddRecords.put(LLC, listOfRecords);
        }
    }

    /*
     * This function adds the records into a Map having contentName of record as
     * key
     * 
     * The map will be process afterwards for processing "MOD"ify Records
     */
    private void putRecordInModifyMap(
            Map<String, List<CourseRawContent>> mapForModifyRecords,
            CourseRawContent courseRawContent) {
        String key = courseRawContent.getContentName();
        if (mapForModifyRecords.containsKey(key)) {
            mapForModifyRecords.get(key).add(courseRawContent);
        } else {
            ArrayList<CourseRawContent> listOfRecords = new ArrayList<CourseRawContent>();
            listOfRecords.add(courseRawContent);
            mapForModifyRecords.put(key, listOfRecords);
        }
    }

    /*
     * This function adds the records into a Map having LLC of record as key
     * 
     * The map will be process afterwards for processing "DEL"ete Records
     */
    private void putRecordInDeleteMap(
            Map<Integer, List<CourseRawContent>> mapForDeleteRecords,
            CourseRawContent courseRawContent) {
        int LLC = Integer.parseInt(courseRawContent.getLanguageLocationCode());
        if (mapForDeleteRecords.containsKey(LLC)) {
            mapForDeleteRecords.get(LLC).add(courseRawContent);
        } else {
            ArrayList<CourseRawContent> listOfRecords = new ArrayList<CourseRawContent>();
            listOfRecords.add(courseRawContent);
            mapForDeleteRecords.put(LLC, listOfRecords);
        }
    }

    /*
     * This function does the schema level validation on a CourseRawContent
     * Record. In case a erroneous field, throws DataValidationException
     */
    private void validateSchema(CourseRawContent courseRawContent)
            throws DataValidationException {

        ParseDataHelper.parseInt("Content ID", courseRawContent.getContentId(),
                true);

        ParseDataHelper.parseInt("Language Location Code",
                courseRawContent.getLanguageLocationCode(), true);

        ParseDataHelper.parseString("Contet Name",
                courseRawContent.getContentName(), true);

        ParseDataHelper.parseInt("Content Duration",
                courseRawContent.getContentDuration(), true);

        ParseDataHelper.parseString("Content File",
                courseRawContent.getContentFile(), true);
    }

    /*
     * This function takes The Map having CourserawContent Records for the
     * modification and processes them
     */
    private void processModificationRecords(
            Map<String, List<CourseRawContent>> mapForModifyRecords,
            String errorFileName, CsvProcessingSummary result) {

        BulkUploadError errorDetail = new BulkUploadError();

        // Map<String, List<CourseRawContent>> fileNameChangeRecords = new
        // HashMap<String, List<CourseRawContent>>();

        if (!mapForModifyRecords.isEmpty()) {
            Iterator<String> contentNamesIterator = mapForModifyRecords
                    .keySet().iterator();
            while (contentNamesIterator.hasNext()) {
                String contentName = contentNamesIterator.next();
                List<CourseRawContent> courseRawContents = mapForModifyRecords
                        .get(contentName);
                if (CollectionUtils.isNotEmpty(courseRawContents)) {
                    Iterator<CourseRawContent> courseRawContentsIterator = courseRawContents
                            .iterator();

                    while (courseRawContentsIterator.hasNext()) {
                        CourseRawContent courseRawContent = courseRawContentsIterator
                                .next();

                        Record record = new Record();
                        try {
                            validateRawContent(courseRawContent, record);
                        } catch (DataValidationException exc) {
                            LOGGER.warn(
                                    "Record Validation failed for content ID: {}",
                                    courseRawContent.getContentId());
                            processError(errorDetail, exc,
                                    courseRawContent.getContentId());

                            bulkUploadErrLogService.writeBulkUploadErrLog(
                                    errorFileName, errorDetail);
                            result.incrementFailureCount();

                            courseRawContentsIterator.remove();
                            courseRawContentService.delete(courseRawContent);
                            continue;
                        }

                        if (isRecordChangingTheFileName(record)) {
                            continue;
                        } else {
                            int LLC = Integer.parseInt(courseRawContent
                                    .getLanguageLocationCode());
                            CourseProcessedContent CPC = courseProcessedContentService
                                    .getRecordforModification(courseRawContent
                                            .getCircle(), LLC, courseRawContent
                                            .getContentName().toUpperCase());

                            if (CPC != null) {
                                LOGGER.info(
                                        "ContentID and duration updated for content name: {}, LLC: {}",
                                        courseRawContent.getContentName(),
                                        courseRawContent
                                                .getLanguageLocationCode());
                                CPC.setContentDuration(Integer
                                        .parseInt(courseRawContent
                                                .getContentDuration()));
                                CPC.setContentID(Integer
                                        .parseInt(courseRawContent
                                                .getContentId()));
                                courseProcessedContentService.update(CPC);

                            }
                            result.incrementSuccessCount();
                            courseRawContentsIterator.remove();
                            courseRawContentService.delete(courseRawContent);
                        }
                    }
                }
                courseRawContents = mapForModifyRecords.get(contentName);
                if (CollectionUtils.isEmpty(courseRawContents)) {
                    contentNamesIterator.remove();
                }
            }
        }

        // Start Processing the file change records:
        if (!mapForModifyRecords.isEmpty()) {

            Iterator<String> contentNamesIterator = mapForModifyRecords
                    .keySet().iterator();
            while (contentNamesIterator.hasNext()) {
                String contentName = contentNamesIterator.next();
                boolean updateContentFile = true;

                // Getting new List as the list return is unmodifiable
                List<Integer> listOfExistingLlc = new ArrayList<Integer>(
                        courseProcessedContentService
                                .getListOfAllExistingLLcs());

                List<CourseRawContent> courseRawContents = mapForModifyRecords
                        .get(contentName);
                if (courseRawContents.size() < listOfExistingLlc.size()) {
                    LOGGER.warn(
                            "Records corresponding to all the existing LLCs not received for modifying file with content name: {}",
                            contentName);
                    bulkUploadErrLogService
                            .writeBulkUploadErrLog(
                                    errorFileName,
                                    new BulkUploadError(
                                            "",
                                            ErrorCategoryConstants.INCONSISTENT_DATA,
                                            String.format(
                                                    MobileAcademyConstants.INSUFFICIENT_RECORDS_FOR_MODIFY,
                                                    courseRawContents.get(0)
                                                            .getContentName())));

                    contentNamesIterator.remove();
                    deleteCourseRawContentsByList(courseRawContents, true,
                            result);

                    continue;
                }

                String fileName = mapForModifyRecords.get(contentName).get(0)
                        .getContentFile();

                Iterator<CourseRawContent> courseRawContentsIterator = courseRawContents
                        .iterator();
                while (courseRawContentsIterator.hasNext()) {
                    CourseRawContent courseRawContent = courseRawContentsIterator
                            .next();
                    int LLC = Integer.parseInt(courseRawContent
                            .getLanguageLocationCode());
                    if (!fileName.equals(courseRawContent.getContentFile())) {
                        LOGGER.warn(
                                "Content file name does not match for content name: {}, contentID: {}",
                                contentName, courseRawContent.getContentId());
                        bulkUploadErrLogService
                                .writeBulkUploadErrLog(
                                        errorFileName,
                                        new BulkUploadError(
                                                courseRawContents.get(0)
                                                        .getContentId(),
                                                ErrorCategoryConstants.INCONSISTENT_DATA,
                                                String.format(
                                                        MobileAcademyConstants.INCONSISTENT_RECORD_FOR_MODIFY,
                                                        courseRawContents
                                                                .get(0)
                                                                .getContentName())));

                        deleteCourseRawContentsByList(courseRawContents, true,
                                result);
                        contentNamesIterator.remove();
                        updateContentFile = false;
                        break;
                    }
                    listOfExistingLlc.remove(new Integer(LLC));
                }

                if (!updateContentFile) {
                    contentNamesIterator.remove();
                    continue;
                }

                // If data has arrived for all the existing LLCS.
                if (CollectionUtils.isEmpty(listOfExistingLlc)) {

                    Record record = new Record();

                    /*
                     * This is done just for setting up the record object. It
                     * can never throw error
                     */

                    try {
                        if (mapForModifyRecords.get(contentName) != null) {
                            validateRawContent(
                                    mapForModifyRecords.get(contentName).get(0),
                                    record);
                        }
                    } catch (DataValidationException e) {
                    }

                    determineTypeAndUpdateChapterContent(record);

                    List<CourseRawContent> fileModifyingRecords = mapForModifyRecords
                            .get(contentName);

                    Iterator<CourseRawContent> fileModifyingRecordsIterator = fileModifyingRecords
                            .iterator();

                    while (fileModifyingRecordsIterator.hasNext()) {
                        CourseRawContent courseRawContent = fileModifyingRecordsIterator
                                .next();
                        int LLC = Integer.parseInt(courseRawContent
                                .getLanguageLocationCode());
                        CourseProcessedContent CPC = courseProcessedContentService
                                .getRecordforModification(courseRawContent
                                        .getCircle().toUpperCase(), LLC,
                                        contentName.toUpperCase());
                        if (CPC != null) {
                            CPC.setContentFile(fileName);
                            CPC.setContentDuration(Integer
                                    .parseInt(courseRawContent
                                            .getContentDuration()));
                            CPC.setContentID(Integer.parseInt(courseRawContent
                                    .getContentId()));

                            courseProcessedContentService.update(CPC);
                        }

                        result.incrementSuccessCount();
                        fileModifyingRecordsIterator.remove();
                        courseRawContentService.delete(courseRawContent);
                    }
                    LOGGER.warn("Course modified for content name: {}",
                            contentName);
                } else { // Not sufficient records for a course
                    LOGGER.warn("Course not modified for content name: {}",
                            contentName);
                    LOGGER.warn("Records for all exisiting LLCs not recieved");
                    bulkUploadErrLogService
                            .writeBulkUploadErrLog(
                                    errorFileName,
                                    new BulkUploadError(
                                            "",
                                            ErrorCategoryConstants.INCONSISTENT_DATA,
                                            String.format(
                                                    MobileAcademyConstants.INSUFFICIENT_RECORDS_FOR_MODIFY,
                                                    courseRawContents.get(0)
                                                            .getContentName())));

                    deleteCourseRawContentsByList(
                            mapForModifyRecords.get(contentName), true, result);
                }
            }
        }
    }

    private void deleteCourseRawContentsByList(
            List<CourseRawContent> courseRawContents, Boolean hasErrorOccured,
            CsvProcessingSummary result) {
        if (CollectionUtils.isNotEmpty(courseRawContents)) {

            for (CourseRawContent courseRawContent : courseRawContents) {
                if (hasErrorOccured) {
                    result.incrementFailureCount();
                } else {
                    result.incrementSuccessCount();
                }
                courseRawContentService.delete(courseRawContent);
            }
        }
    }

    private void processError(BulkUploadError errorDetail,
            DataValidationException ex, String contentId) {
        errorDetail.setErrorCategory(ex.getErrorCode());
        errorDetail.setRecordDetails(contentId);
        errorDetail.setErrorDescription(ex.getErrorDesc());
    }

    /*
     * This function is used to update the filename on the basis of File-type in
     * record object, into courseContent tables
     */
    private void determineTypeAndUpdateChapterContent(Record record) {
        if (record.getType() == FileType.LESSON_CONTENT) {
            coursePopulateService
                    .setLessonContent(record.getChapterId(),
                            record.getLessonId(),
                            MobileAcademyConstants.CONTENT_LESSON,
                            record.getFileName());
        } else if (record.getType() == FileType.LESSON_END_MENU) {
            coursePopulateService.setLessonContent(record.getChapterId(),
                    record.getLessonId(), MobileAcademyConstants.CONTENT_MENU,
                    record.getFileName());
        } else if (record.getType() == FileType.QUESTION_CONTENT) {
            coursePopulateService.setQuestionContent(record.getChapterId(),
                    record.getQuestionId(),
                    MobileAcademyConstants.CONTENT_QUESTION,
                    record.getFileName());
        } else if (record.getType() == FileType.CORRECT_ANSWER) {
            coursePopulateService.setQuestionContent(record.getChapterId(),
                    record.getQuestionId(),
                    MobileAcademyConstants.CONTENT_CORRECT_ANSWER,
                    record.getFileName());
        } else if (record.getType() == FileType.WRONG_ANSWER) {
            coursePopulateService.setQuestionContent(record.getChapterId(),
                    record.getQuestionId(),
                    MobileAcademyConstants.CONTENT_WRONG_ANSWER,
                    record.getFileName());
        } else if (record.getType() == FileType.CHAPTER_END_MENU) {
            coursePopulateService.setChapterContent(record.getChapterId(),
                    MobileAcademyConstants.CONTENT_MENU, record.getFileName());
        } else if (record.getType() == FileType.QUIZ_HEADER) {
            coursePopulateService.setQuizContent(record.getChapterId(),
                    MobileAcademyConstants.CONTENT_QUIZ_HEADER,
                    record.getFileName());
        } else if (record.getType() == FileType.SCORE) {
            coursePopulateService.setScore(record.getChapterId(),
                    record.getScoreID(), MobileAcademyConstants.SCORE,
                    record.getFileName());
        }
    }

    /*
     * This checks if a modify record is also changing the name of the file
     * currently existing the system. If yes, it returns true.
     */
    private boolean isRecordChangingTheFileName(Record record) {
        boolean status = false;
        if (record.getType() == FileType.LESSON_CONTENT) {
            LessonContent lessonContent = coursePopulateService
                    .getLessonContent(record.getChapterId(),
                            record.getLessonId(),
                            MobileAcademyConstants.CONTENT_LESSON);
            if (!lessonContent.getAudioFile().equals(record.getFileName())) {
                status = true;
            }
        } else if (record.getType() == FileType.LESSON_END_MENU) {
            LessonContent lessonContent = coursePopulateService
                    .getLessonContent(record.getChapterId(),
                            record.getLessonId(),
                            MobileAcademyConstants.CONTENT_MENU);
            if (!lessonContent.getAudioFile().equals(record.getFileName())) {
                status = true;
            }
        } else if (record.getType() == FileType.QUESTION_CONTENT) {
            QuestionContent questionContent = coursePopulateService
                    .getQuestionContent(record.getChapterId(),
                            record.getQuestionId(),
                            MobileAcademyConstants.CONTENT_QUESTION);
            if (!questionContent.getAudioFile().equals(record.getFileName())) {
                status = true;
            }
        } else if (record.getType() == FileType.CORRECT_ANSWER) {
            QuestionContent questionContent = coursePopulateService
                    .getQuestionContent(record.getChapterId(),
                            record.getQuestionId(),
                            MobileAcademyConstants.CONTENT_CORRECT_ANSWER);
            if (!questionContent.getAudioFile().equals(record.getFileName())) {
                status = true;
            }
        } else if (record.getType() == FileType.WRONG_ANSWER) {
            QuestionContent questionContent = coursePopulateService
                    .getQuestionContent(record.getChapterId(),
                            record.getQuestionId(),
                            MobileAcademyConstants.CONTENT_WRONG_ANSWER);
            if (!questionContent.getAudioFile().equals(record.getFileName())) {
                status = true;
            }
        } else if (record.getType() == FileType.CHAPTER_END_MENU) {
            ChapterContent chapterContent = coursePopulateService
                    .getChapterContent(record.getChapterId(),
                            MobileAcademyConstants.CONTENT_MENU);
            if (!chapterContent.getAudioFile().equals(record.getFileName())) {
                status = true;
            }
        } else if (record.getType() == FileType.QUIZ_HEADER) {
            QuizContent quizContent = coursePopulateService.getQuizContent(
                    record.getChapterId(),
                    MobileAcademyConstants.CONTENT_QUIZ_HEADER);
            if (!quizContent.getAudioFile().equals(record.getFileName())) {
                status = true;
            }
        } else if (record.getType() == FileType.SCORE) {
            ScoreContent scoreContent = coursePopulateService.getScore(
                    record.getChapterId(), record.getScoreID(),
                    MobileAcademyConstants.SCORE);
            if (!scoreContent.getAudioFile().equals(record.getFileName())) {
                status = true;
            }
        }
        return status;
    }

    /*
     * This function takes the list of CourseRawContent records against which
     * the file need to be added into the course
     */
    private void processAddRecords(
            Map<Integer, List<CourseRawContent>> mapForAddRecords,
            String errorFileName, CsvProcessingSummary result) {
        boolean populateCourseStructure = false;
        CourseFlags courseFlags = new CourseFlags();
        List<Record> answerOptionRecordList = new ArrayList<Record>();
        BulkUploadError errorDetail = new BulkUploadError();
        boolean abortAdditionProcess = false;

        if (!mapForAddRecords.isEmpty()) {
            Iterator<Integer> distictLLCIterator = mapForAddRecords.keySet()
                    .iterator();
            while (distictLLCIterator.hasNext()) {
                abortAdditionProcess = false;
                populateCourseStructure = false;
                Integer LLC = distictLLCIterator.next();
                List<CourseRawContent> courseRawContents = mapForAddRecords
                        .get(LLC);
                if (CollectionUtils.isNotEmpty(courseRawContents)) {
                    if (courseRawContents.size() != MobileAcademyConstants.MIN_FILES_PER_COURSE) {
                        LOGGER.warn(
                                "There must be exact {} records to populate the course corresponding to LLC:{}.",
                                MobileAcademyConstants.MIN_FILES_PER_COURSE,
                                LLC);

                        deleteCourseRawContentsByList(courseRawContents, true,
                                result);
                        bulkUploadErrLogService
                                .writeBulkUploadErrLog(
                                        errorFileName,
                                        new BulkUploadError(
                                                courseRawContents.get(0)
                                                        .getContentId(),
                                                ErrorCategoryConstants.INCONSISTENT_DATA,
                                                String.format(
                                                        MobileAcademyConstants.INSUFFICIENT_RECORDS_FOR_ADD,
                                                        LLC)));

                        distictLLCIterator.remove();
                        continue;
                    }

                    Course course = coursePopulateService.getMtrainingCourse();
                    if (course == null) {
                        course = coursePopulateService
                                .populateMtrainingCourseData();
                        populateCourseStructure = true;
                    } else if (coursePopulateService.findCourseState() == CourseUnitState.Inactive) {
                        populateCourseStructure = true;
                    }

                    courseFlags.resetTheFlags();
                    answerOptionRecordList.clear();

                    List<ChapterContent> chapterContents = coursePopulateService
                            .getAllChapterContents();

                    if (CollectionUtils.isEmpty(chapterContents)) {
                        chapterContents = createChapterContentPrototype();
                    }

                    Iterator<CourseRawContent> courseRawContentsIterator = courseRawContents
                            .iterator();
                    while (courseRawContentsIterator.hasNext()) {
                        CourseRawContent courseRawContent = courseRawContentsIterator
                                .next();
                        Record record = new Record();
                        try {
                            validateRawContent(courseRawContent, record);
                        } catch (DataValidationException exc) {

                            abortAdditionProcess = true;

                            LOGGER.warn(
                                    "Record validation failed for content ID: {}",
                                    courseRawContent.getContentId());

                            processError(errorDetail, exc,
                                    courseRawContent.getContentId());

                            bulkUploadErrLogService.writeBulkUploadErrLog(
                                    errorFileName, errorDetail);

                            bulkUploadErrLogService
                                    .writeBulkUploadErrLog(
                                            errorFileName,
                                            new BulkUploadError(
                                                    courseRawContent
                                                            .getContentId(),
                                                    ErrorCategoryConstants.INCONSISTENT_DATA,
                                                    String.format(
                                                            MobileAcademyConstants.INCONSISTENT_RECORDS_FOR_ADD,
                                                            LLC)));

                            deleteCourseRawContentsByList(courseRawContents,
                                    true, result);
                            distictLLCIterator.remove();
                            break;
                        }

                        if (populateCourseStructure) {
                            if (record.getType() == FileType.QUESTION_CONTENT) {
                                answerOptionRecordList.add(record);
                            }
                            checkTypeAndAddToChapterContent(record,
                                    chapterContents, courseFlags);
                        } else {
                            if (!checkRecordConsistencyAndMarkFlag(record,
                                    chapterContents, courseFlags)) {
                                LOGGER.warn(
                                        "Record with content ID: {} is not consistent with the data already existing in the system",
                                        courseRawContent.getContentId());

                                bulkUploadErrLogService
                                        .writeBulkUploadErrLog(
                                                errorFileName,
                                                new BulkUploadError(
                                                        courseRawContent
                                                                .getContentId(),
                                                        ErrorCategoryConstants.INCONSISTENT_DATA,
                                                        String.format(
                                                                MobileAcademyConstants.INCONSISTENT_RECORDS_FOR_ADD,
                                                                LLC)));

                                distictLLCIterator.remove();
                                deleteCourseRawContentsByList(
                                        courseRawContents, true, result);
                                abortAdditionProcess = true;
                                break;
                            }
                        }
                    }
                    if (abortAdditionProcess) {
                        continue;
                    }

                    if (courseFlags.hasCompleteCourseArrived()) {
                        courseRawContentsIterator = courseRawContents
                                .iterator();
                        while (courseRawContentsIterator.hasNext()) {
                            CourseRawContent courseRawContent = courseRawContentsIterator
                                    .next();
                            result.incrementSuccessCount();
                            updateRecordInContentProcessedTable(courseRawContent);
                            courseRawContentService.delete(courseRawContent);
                            courseRawContentsIterator.remove();
                        }
                        // Update Course
                        if (populateCourseStructure) {
                            for (int chapterCounter = 0; chapterCounter < MobileAcademyConstants.NUM_OF_CHAPTERS; chapterCounter++) {
                                chapterContentDataService
                                        .create(chapterContents
                                                .get(chapterCounter));
                            }
                            // Update AnswerOptionList
                            // Change the state to Active
                            processAnswerOptionRecordList(answerOptionRecordList);
                            coursePopulateService
                                    .updateCourseState(CourseUnitState.Active);
                            LOGGER.info(
                                    "Course Added successfully for LLC: {}",
                                    LLC);
                        }
                    } else {
                        bulkUploadErrLogService
                                .writeBulkUploadErrLog(
                                        errorFileName,
                                        new BulkUploadError(
                                                "",
                                                ErrorCategoryConstants.INCONSISTENT_DATA,
                                                String.format(
                                                        MobileAcademyConstants.INSUFFICIENT_RECORDS_FOR_ADD,
                                                        LLC)));
                        LOGGER.warn(
                                "Record for complete course haven't arrived to add the course for LLC: {}",
                                LLC);
                        deleteCourseRawContentsByList(courseRawContents, true,
                                result);
                    }

                }
            }
        }

    }

    /*
     * This function takes the list of CourseRawContent records against which
     * the file need to be deleted from the course
     */
    public void processDeleteRecords(
            Map<Integer, List<CourseRawContent>> mapForDeleteRecords,
            String errorFileName, CsvProcessingSummary result) {

        BulkUploadError errorDetail = new BulkUploadError();
        List<Integer> listOfExistingtLLC = courseProcessedContentService
                .getListOfAllExistingLLcs();

        if (!mapForDeleteRecords.isEmpty()) {
            Iterator<Integer> distictLLCIterator = mapForDeleteRecords.keySet()
                    .iterator();

            while (distictLLCIterator.hasNext()) {

                Integer LLC = distictLLCIterator.next();
                CourseFlags courseFlags = new CourseFlags();
                courseFlags.resetTheFlags();

                List<CourseRawContent> courseRawContents = mapForDeleteRecords
                        .get(LLC);

                if (CollectionUtils.isNotEmpty(listOfExistingtLLC)) {
                    if (!listOfExistingtLLC.contains(LLC)) {
                        deleteCourseRawContentsByList(courseRawContents, false,
                                result);
                        LOGGER.info(
                                "No record exists in content processed table for LLC: {}",
                                LLC);
                        continue;
                    }
                }

                if (courseRawContents.size() < MobileAcademyConstants.MIN_FILES_PER_COURSE) {
                    LOGGER.warn(
                            "Sufficient records not recieved to delete the course for LLC: {}",
                            LLC);
                    bulkUploadErrLogService
                            .writeBulkUploadErrLog(
                                    errorFileName,
                                    new BulkUploadError(
                                            "",
                                            ErrorCategoryConstants.INCONSISTENT_DATA,
                                            String.format(
                                                    MobileAcademyConstants.INSUFFICIENT_RECORDS_FOR_DEL,
                                                    LLC)));
                    distictLLCIterator.remove();
                    deleteCourseRawContentsByList(courseRawContents, true,
                            result);
                    continue;
                }

                Iterator<CourseRawContent> courseRawContentsIterator = courseRawContents
                        .iterator();
                while (courseRawContentsIterator.hasNext()) {
                    CourseRawContent courseRawContent = courseRawContentsIterator
                            .next();
                    Record record = new Record();
                    try {
                        validateContentName(courseRawContent, record);
                    } catch (DataValidationException exc) {
                        LOGGER.warn(
                                "No record exists in content processed table for LLC: {}",
                                LLC);
                        processError(errorDetail, exc,
                                courseRawContent.getContentId());

                        bulkUploadErrLogService.writeBulkUploadErrLog(
                                errorFileName, errorDetail);

                        result.incrementFailureCount();
                        courseRawContentService.delete(courseRawContent);
                        courseRawContentsIterator.remove();
                        continue;
                    }
                    checkRecordTypeAndMarkCourseFlag(record, courseFlags);
                }

                if (courseFlags.hasCompleteCourseArrived()) {
                    courseProcessedContentService.deleteRecordsByLLC(LLC);
                    // If this was the last LLC in CPC
                    if (courseProcessedContentService
                            .getListOfAllExistingLLcs().size() == 0) {
                        coursePopulateService
                                .updateCourseState(CourseUnitState.Inactive);
                        deleteChapterContentTable();
                    }
                    LOGGER.info("Course Deleted successfully for LLC: {}", LLC);
                    deleteCourseRawContentsByList(courseRawContents, false,
                            result);
                } else {
                    LOGGER.warn(
                            "Not all the records found to delete the course for LLC: {}",
                            LLC);
                    bulkUploadErrLogService
                            .writeBulkUploadErrLog(
                                    errorFileName,
                                    new BulkUploadError(
                                            "",
                                            ErrorCategoryConstants.INCONSISTENT_DATA,
                                            String.format(
                                                    MobileAcademyConstants.INCOMPLETE_RECORDS_FOR_DEL,
                                                    LLC)));
                    deleteCourseRawContentsByList(courseRawContents, true,
                            result);
                }
                distictLLCIterator.remove();
            }
        }
    }

    private void deleteChapterContentTable() {
        List<ChapterContent> chapterContents = chapterContentDataService
                .retrieveAll();
        Iterator<ChapterContent> chapterContentsIterator = chapterContents
                .iterator();
        while (chapterContentsIterator.hasNext()) {
            chapterContentDataService.delete(chapterContentsIterator.next());
        }
    }

    /*
     * this function checks the type of file to which record points to and based
     * on that it sets the flag for successful arrival of that file
     */
    private void checkRecordTypeAndMarkCourseFlag(Record record,
            CourseFlags courseFlags) {
        if (record.getType() == FileType.LESSON_CONTENT) {
            courseFlags.markLessonContent(record.getChapterId(),
                    record.getLessonId());
        } else if (record.getType() == FileType.LESSON_END_MENU) {
            courseFlags.markLessonEndMenu(record.getChapterId(),
                    record.getLessonId());
        } else if (record.getType() == FileType.QUIZ_HEADER) {
            courseFlags.markQuizHeader(record.getChapterId());
        } else if (record.getType() == FileType.QUESTION_CONTENT) {
            courseFlags.markQuestionContent(record.getChapterId(),
                    record.getQuestionId());
        } else if (record.getType() == FileType.CORRECT_ANSWER) {
            courseFlags.markQuestionCorrectAnswer(record.getChapterId(),
                    record.getQuestionId());
        } else if (record.getType() == FileType.WRONG_ANSWER) {
            courseFlags.markQuestionWrongAnswer(record.getChapterId(),
                    record.getQuestionId());
        } else if (record.getType() == FileType.CHAPTER_END_MENU) {
            courseFlags.markChapterEndMenu(record.getChapterId());
        } else if (record.getType() == FileType.SCORE) {
            courseFlags.markScoreFile(record.getChapterId(),
                    record.getScoreID());
        }
    }

    /*
     * this function updates the correct option for different questions in the
     * mTraining module.
     */
    private void processAnswerOptionRecordList(
            List<Record> answerOptionRecordList) {
        for (Record answerRecord : answerOptionRecordList) {
            coursePopulateService
                    .updateCorrectAnswer(
                            MobileAcademyConstants.CHAPTER
                                    + String.format(
                                            MobileAcademyConstants.TWO_DIGIT_INTEGER_FORMAT,
                                            answerRecord.getChapterId()),
                            MobileAcademyConstants.QUESTION
                                    + String.format(
                                            MobileAcademyConstants.TWO_DIGIT_INTEGER_FORMAT,
                                            answerRecord.getQuestionId()),
                            String.valueOf(answerRecord.getAnswerId()));
        }
    }

    /*
     * This function checks whether a ADD record is having the same file Name
     * for a file which is currently existing in the system. In positive
     * scenarios, it also marks for successful arrival of the file in the course
     * flags
     */
    private boolean checkRecordConsistencyAndMarkFlag(Record record,
            List<ChapterContent> chapterContents, CourseFlags courseFlags) {
        boolean status = true;
        ChapterContent chapterContent = null;
        for (ChapterContent chapter : chapterContents) {
            if (chapter.getChapterNumber() == record.getChapterId()) {
                chapterContent = chapter;
                break;
            }
        }

        if (chapterContent == null) {
            return false;
        }

        if (record.getType() == FileType.LESSON_CONTENT) {
            for (LessonContent lessonContent : chapterContent.getLessons()) {
                if (lessonContent.getLessonNumber() == record.getLessonId()
                        && MobileAcademyConstants.CONTENT_LESSON
                                .equalsIgnoreCase(lessonContent.getName())) {
                    if (!lessonContent.getAudioFile().equals(
                            record.getFileName())) {
                        LOGGER.debug("original file name: {}",
                                lessonContent.getAudioFile());
                        LOGGER.debug("new file name: {}", record.getFileName());
                        status = false;
                    } else {
                        courseFlags.markLessonContent(record.getChapterId(),
                                record.getLessonId());
                    }
                    break;
                }
            }
        } else if (record.getType() == FileType.LESSON_END_MENU) {
            for (LessonContent lessonContent : chapterContent.getLessons()) {
                if ((lessonContent.getLessonNumber() == record.getLessonId())
                        && (MobileAcademyConstants.CONTENT_MENU
                                .equalsIgnoreCase(lessonContent.getName()))) {
                    if (!lessonContent.getAudioFile().equals(
                            record.getFileName())) {
                        LOGGER.debug("original file name: {}",
                                lessonContent.getAudioFile());
                        LOGGER.debug("new file name: {}", record.getFileName());
                        status = false;
                    } else {
                        courseFlags.markLessonEndMenu(record.getChapterId(),
                                record.getLessonId());
                    }
                    break;
                }
            }
        } else if (record.getType() == FileType.QUIZ_HEADER) {
            QuizContent quizContent = chapterContent.getQuiz();
            if ((MobileAcademyConstants.CONTENT_QUIZ_HEADER
                    .equalsIgnoreCase(quizContent.getName()))) {
                if (!quizContent.getAudioFile().equals(record.getFileName())) {
                    LOGGER.debug("original file name: {}",
                            quizContent.getAudioFile());
                    LOGGER.debug("new file name: {}", record.getFileName());
                    status = false;
                } else {
                    courseFlags.markQuizHeader(record.getChapterId());
                }
            }
        } else if (record.getType() == FileType.QUESTION_CONTENT) {
            for (QuestionContent questionContent : chapterContent.getQuiz()
                    .getQuestions()) {
                if ((questionContent.getQuestionNumber() == record
                        .getQuestionId())
                        && (MobileAcademyConstants.CONTENT_QUESTION
                                .equalsIgnoreCase(questionContent.getName()))) {
                    if ((!questionContent.getAudioFile().equals(
                            record.getFileName()))
                            || !answerOptionMatcher(record)) {
                        LOGGER.debug("Correct Option or fileName doesn't macth");
                        LOGGER.debug("original file name: {}",
                                questionContent.getAudioFile());
                        LOGGER.debug("new file name: {}", record.getFileName());
                        status = false;
                    } else {
                        courseFlags.markQuestionContent(record.getChapterId(),
                                record.getQuestionId());
                    }
                    break;
                }
            }
        } else if (record.getType() == FileType.CORRECT_ANSWER) {
            for (QuestionContent questionContent : chapterContent.getQuiz()
                    .getQuestions()) {
                if ((questionContent.getQuestionNumber() == record
                        .getQuestionId())
                        && (MobileAcademyConstants.CONTENT_CORRECT_ANSWER
                                .equalsIgnoreCase(questionContent.getName()))) {
                    if (!questionContent.getAudioFile().equals(
                            record.getFileName())) {
                        LOGGER.debug("original file name: {}",
                                questionContent.getAudioFile());
                        LOGGER.debug("new file name: {}", record.getFileName());
                        status = false;
                    } else {
                        courseFlags.markQuestionCorrectAnswer(
                                record.getChapterId(), record.getQuestionId());
                    }
                    break;
                }
            }
        } else if (record.getType() == FileType.WRONG_ANSWER) {
            for (QuestionContent questionContent : chapterContent.getQuiz()
                    .getQuestions()) {
                if ((questionContent.getQuestionNumber() == record
                        .getQuestionId())
                        && (MobileAcademyConstants.CONTENT_WRONG_ANSWER
                                .equalsIgnoreCase(questionContent.getName()))) {
                    if (!questionContent.getAudioFile().equals(
                            record.getFileName())) {
                        LOGGER.debug("original file name: {}",
                                questionContent.getAudioFile());
                        LOGGER.debug("new file name: {}", record.getFileName());
                        status = false;
                    } else {
                        courseFlags.markQuestionWrongAnswer(
                                record.getChapterId(), record.getQuestionId());
                    }
                    break;
                }
            }
        } else if (record.getType() == FileType.CHAPTER_END_MENU) {
            if (MobileAcademyConstants.CONTENT_MENU
                    .equalsIgnoreCase(chapterContent.getName())) {
                if (!chapterContent.getAudioFile().equals(record.getFileName())) {
                    LOGGER.debug("original file name: {}",
                            chapterContent.getAudioFile());
                    LOGGER.debug("new file name: {}", record.getFileName());
                    status = false;
                } else {
                    courseFlags.markChapterEndMenu(record.getChapterId());
                }
            }
        } else if (record.getType() == FileType.SCORE) {
            for (ScoreContent scoreContent : chapterContent.getScores()) {
                if ((MobileAcademyConstants.SCORE + String.format(
                        MobileAcademyConstants.TWO_DIGIT_INTEGER_FORMAT,
                        record.getScoreID())).equalsIgnoreCase(scoreContent
                        .getName())) {
                    if (!scoreContent.getAudioFile().equals(
                            record.getFileName())) {
                        LOGGER.debug("original file name: {}",
                                scoreContent.getAudioFile());
                        LOGGER.debug("new file name: {}", record.getFileName());
                        status = false;
                    } else {
                        courseFlags.markScoreFile(record.getChapterId(),
                                record.getScoreID());
                    }
                    break;
                }
            }
        }
        return status;
    }

    private boolean answerOptionMatcher(Record record) {

        int questionNo = record.getQuestionId();
        int answerNo = record.getAnswerId();
        int chapterNo = record.getChapterId();

        return coursePopulateService.matchAnswerOption(chapterNo, questionNo,
                answerNo);
    }

    /*
     * This function is used to create the static course data in the content
     * tables.
     */
    private List<ChapterContent> createChapterContentPrototype() {
        List<ChapterContent> listOfChapters = new ArrayList<ChapterContent>();

        for (int chapterCount = 1; chapterCount <= MobileAcademyConstants.NUM_OF_CHAPTERS; chapterCount++) {
            List<LessonContent> lessons = createListOfLesson();
            List<QuestionContent> questions = createListOfQuestion();
            List<ScoreContent> scoreContents = createListOfScores();
            QuizContent quiz = new QuizContent(
                    MobileAcademyConstants.CONTENT_QUIZ_HEADER, null, questions);
            ChapterContent chapterContent = new ChapterContent(chapterCount,
                    MobileAcademyConstants.CONTENT_MENU, null, lessons,
                    scoreContents, quiz);
            listOfChapters.add(chapterContent);
        }

        LOGGER.info("Course Prototype created in content table");
        return listOfChapters;
    }

    /*
     * This function creates theList of Score content files to be included in a
     * chapter
     */
    private List<ScoreContent> createListOfScores() {
        List<ScoreContent> scoreList = new ArrayList<>();
        for (int scoreCount = 0; scoreCount <= MobileAcademyConstants.NUM_OF_SCORES; scoreCount++) {
            ScoreContent scoreContent = new ScoreContent(
                    MobileAcademyConstants.SCORE
                            + String.format(
                                    MobileAcademyConstants.TWO_DIGIT_INTEGER_FORMAT,
                                    scoreCount), null);
            scoreList.add(scoreContent);
        }
        return scoreList;
    }

    /*
     * This function creates the List of QuestionContent files to be included in
     * a quiz of chapter
     */
    private List<QuestionContent> createListOfQuestion() {
        List<QuestionContent> questionList = new ArrayList<>();
        for (int questionCount = 1; questionCount <= MobileAcademyConstants.NUM_OF_QUESTIONS; questionCount++) {
            QuestionContent questionContent = new QuestionContent(
                    questionCount, MobileAcademyConstants.CONTENT_QUESTION,
                    null);
            questionList.add(questionContent);
            questionContent = new QuestionContent(questionCount,
                    MobileAcademyConstants.CONTENT_CORRECT_ANSWER, null);
            questionList.add(questionContent);
            questionContent = new QuestionContent(questionCount,
                    MobileAcademyConstants.CONTENT_WRONG_ANSWER, null);
            questionList.add(questionContent);
        }
        return questionList;
    }

    /*
     * This function creates the List of LessonContent files to be included in a
     * chapter
     */
    private List<LessonContent> createListOfLesson() {
        List<LessonContent> lessonList = new ArrayList<>();
        for (int lessonCount = 1; lessonCount <= MobileAcademyConstants.NUM_OF_LESSONS; lessonCount++) {
            LessonContent lessonContent = new LessonContent(lessonCount,
                    MobileAcademyConstants.CONTENT_MENU, null);
            lessonList.add(lessonContent);
            lessonContent = new LessonContent(lessonCount,
                    MobileAcademyConstants.CONTENT_LESSON, null);
            lessonList.add(lessonContent);
        }
        return lessonList;
    }

    /*
     * This function takes the CourserRawContent record as input and based on
     * that It creates a CourseProcessedContent Record in CourseProcessedContent
     * table chapter
     */
    private void updateRecordInContentProcessedTable(
            CourseRawContent courseRawContent) {
        String metaData = "";
        ContentType contentType = ContentType.CONTENT;

        if (StringUtils.isNotEmpty(courseRawContent.getMetaData())) {
            metaData = courseRawContent.getMetaData().toUpperCase();
        }
        if (StringUtils.isNotEmpty(courseRawContent.getContentType())) {
            contentType = ContentType.findByName(courseRawContent
                    .getContentType());
        }
        courseProcessedContentService.create(new CourseProcessedContent(Integer
                .parseInt(courseRawContent.getContentId()), courseRawContent
                .getCircle().toUpperCase(), Integer.parseInt(courseRawContent
                .getLanguageLocationCode()), courseRawContent.getContentName()
                .toUpperCase(), contentType, courseRawContent.getContentFile(),
                Integer.parseInt(courseRawContent.getContentDuration()),
                metaData));
    }

    /*
     * This function validates the CourseRawContent record and returns the
     * record object, populated on the basis of contentName in the raw record.
     * In case of error in the record, it returns null.
     */
    private void validateRawContent(CourseRawContent courseRawContent,
            Record record) throws DataValidationException {

        validateContentName(courseRawContent, record);

        if (record.getType() == FileType.QUESTION_CONTENT) {
            String metaData = ParseDataHelper.parseString("METADETA",
                    courseRawContent.getMetaData(), true);

            if (!("CorrectAnswer").equalsIgnoreCase(metaData.substring(0,
                    metaData.indexOf(':')))) {
                throw new DataValidationException(
                        MobileAcademyConstants.INCONSISTENT_DATA_MESSAGE,
                        ErrorCategoryConstants.INCONSISTENT_DATA, "METADETA");
            } else {
                record.setAnswerId(ParseDataHelper.parseInt("",
                        metaData.substring(metaData.indexOf(':') + 1), true));
            }
        }

        record.setFileName(courseRawContent.getContentFile());
    }

    /*
     * This function validates the content Name in a CourseRawContent Record. In
     * case of Sunny Scenario, it sets the indices in the record object and
     * return true. while in case of any error in the content name field, it
     * returns false.
     */
    private void validateContentName(CourseRawContent courseRawContent,
            Record record) throws DataValidationException {
        String contentName = courseRawContent.getContentName().trim();
        boolean recordDataValidation = true;
        if (contentName.indexOf('_') == -1) {
            throw new DataValidationException(
                    MobileAcademyConstants.INCONSISTENT_DATA_MESSAGE,
                    ErrorCategoryConstants.INCONSISTENT_DATA,
                    MobileAcademyConstants.CONTENT_NAME);
        }

        String chapterString = contentName.substring(0,
                contentName.indexOf('_'));
        String subString = contentName.substring(1 + contentName.indexOf('_'));

        if (StringUtils.isBlank(subString)
                || !("Chapter").equalsIgnoreCase(chapterString.substring(0,
                        chapterString.length() - 2))) {
            throw new DataValidationException(
                    MobileAcademyConstants.INCONSISTENT_DATA_MESSAGE,
                    ErrorCategoryConstants.INCONSISTENT_DATA,
                    MobileAcademyConstants.CONTENT_NAME);
        }

        try {
            record.setChapterId(Integer.parseInt(chapterString
                    .substring(chapterString.length() - 2)));
        } catch (NumberFormatException exception) {
            LOGGER.info(exception.getMessage());
            throw new DataValidationException(
                    MobileAcademyConstants.INCONSISTENT_DATA_MESSAGE,
                    ErrorCategoryConstants.INCONSISTENT_DATA,
                    MobileAcademyConstants.CONTENT_NAME);
        }

        if (!verifyRange(record.getChapterId(), 1,
                MobileAcademyConstants.NUM_OF_CHAPTERS)) {
            recordDataValidation = false;
        }

        if ((!recordDataValidation) || (!isTypeDeterminable(record, subString))) {
            throw new DataValidationException(
                    MobileAcademyConstants.INCONSISTENT_DATA_MESSAGE,
                    ErrorCategoryConstants.INCONSISTENT_DATA,
                    MobileAcademyConstants.CONTENT_NAME);
        }

    }

    /*
     * This function checks if the type of the file to which the records points
     * to is determinable from the substring in content Name. in case of sunny
     * Scenario, it sets the file-type in record object and returns true, while
     * in case of any error, it returns false.
     */
    private boolean isTypeDeterminable(Record record, String subString) {

        // If the substring is "QuizHeader" or "EndMenu", it will be determined.
        FileType fileType = FileType.getFor(subString);
        if (fileType != null) {
            record.setType(fileType);
            return true;
        }

        String type = subString.substring(0, subString.length() - 2);
        String indexString = subString.substring(subString.length() - 2);
        int index;

        try {
            index = Integer.parseInt(indexString);
        } catch (NumberFormatException exception) {
            return false;
        }
        fileType = FileType.getFor(type);

        record.setType(fileType);

        if (fileType == FileType.LESSON_CONTENT) {
            if (!verifyRange(index, 1, MobileAcademyConstants.NUM_OF_LESSONS)) {
                return false;
            }
            record.setLessonId(index);
            return true;
        } else if (fileType == FileType.LESSON_END_MENU) {
            if (!verifyRange(index, 1, MobileAcademyConstants.NUM_OF_LESSONS)) {
                return false;
            }
            record.setLessonId(index);
            return true;
        } else if (fileType == FileType.QUESTION_CONTENT) {
            if (!verifyRange(index, 1, MobileAcademyConstants.NUM_OF_QUESTIONS)) {
                return false;
            }
            record.setQuestionId(index);
            return true;
        } else if (fileType == FileType.CORRECT_ANSWER) {
            if (!verifyRange(index, 1, MobileAcademyConstants.NUM_OF_QUESTIONS)) {
                return false;
            }
            record.setQuestionId(index);
            return true;
        } else if (fileType == FileType.WRONG_ANSWER) {
            if (!verifyRange(index, 1, MobileAcademyConstants.NUM_OF_QUESTIONS)) {
                return false;
            }
            record.setQuestionId(index);
            return true;
        } else if (fileType == FileType.SCORE) {
            if (!verifyRange(index, 0, MobileAcademyConstants.NUM_OF_SCORES)) {
                return false;
            }
            record.setScoreID(index);
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param value : CURRENT VALUE OF PARAM
     * @param minValue : The minimum possible value
     * @param maxValue : The maximum possible value
     * @return : true if the current value lies in range
     */
    private boolean verifyRange(int value, int minValue, int maxValue) {
        if (value < minValue || value > maxValue) {
            return false;
        }
        return true;
    }

    /*
     * This function checks the file-type to which the record refers and on the
     * basis of that, it populates the chapterContent Prototype Object and marks
     * the flag in courseFlags for successful arrival of the file.
     */
    private void checkTypeAndAddToChapterContent(Record record,
            List<ChapterContent> chapterContents, CourseFlags courseFlags) {
        ChapterContent chapterContent = null;
        for (ChapterContent chapter : chapterContents) {
            if (chapter.getChapterNumber() == record.getChapterId()) {
                chapterContent = chapter;
                break;
            }
        }
        if (chapterContent == null) {
            return;
        }
        if (record.getType() == FileType.LESSON_CONTENT) {
            List<LessonContent> lessons = chapterContent.getLessons();
            for (LessonContent lesson : lessons) {
                if ((lesson.getLessonNumber() == record.getLessonId())
                        && (MobileAcademyConstants.CONTENT_LESSON
                                .equalsIgnoreCase(lesson.getName()))) {
                    lesson.setAudioFile(record.getFileName());
                    break;
                }
            }
            courseFlags.markLessonContent(record.getChapterId(),
                    record.getLessonId());

        } else if (record.getType() == FileType.LESSON_END_MENU) {
            List<LessonContent> lessons = chapterContent.getLessons();
            for (LessonContent lesson : lessons) {
                if ((lesson.getLessonNumber() == record.getLessonId())
                        && (MobileAcademyConstants.CONTENT_MENU
                                .equalsIgnoreCase(lesson.getName()))) {
                    lesson.setAudioFile(record.getFileName());
                    break;
                }
            }
            courseFlags.markLessonEndMenu(record.getChapterId(),
                    record.getLessonId());

        } else if (record.getType() == FileType.QUIZ_HEADER) {
            QuizContent quiz = chapterContent.getQuiz();
            if ((MobileAcademyConstants.CONTENT_QUIZ_HEADER
                    .equalsIgnoreCase(quiz.getName()))) {
                quiz.setAudioFile(record.getFileName());
            }
            courseFlags.markQuizHeader(record.getChapterId());

        } else if (record.getType() == FileType.QUESTION_CONTENT) {
            List<QuestionContent> questions = chapterContent.getQuiz()
                    .getQuestions();
            for (QuestionContent question : questions) {
                if ((question.getQuestionNumber() == record.getQuestionId())
                        && (MobileAcademyConstants.CONTENT_QUESTION
                                .equalsIgnoreCase(question.getName()))) {
                    question.setAudioFile(record.getFileName());
                    break;
                }
            }
            courseFlags.markQuestionContent(record.getChapterId(),
                    record.getQuestionId());

        } else if (record.getType() == FileType.CORRECT_ANSWER) {
            List<QuestionContent> questions = chapterContent.getQuiz()
                    .getQuestions();
            for (QuestionContent question : questions) {
                if ((question.getQuestionNumber() == record.getQuestionId())
                        && (MobileAcademyConstants.CONTENT_CORRECT_ANSWER
                                .equalsIgnoreCase(question.getName()))) {
                    question.setAudioFile(record.getFileName());
                    break;
                }
            }
            courseFlags.markQuestionCorrectAnswer(record.getChapterId(),
                    record.getQuestionId());

        } else if (record.getType() == FileType.WRONG_ANSWER) {
            List<QuestionContent> questions = chapterContent.getQuiz()
                    .getQuestions();
            for (QuestionContent question : questions) {
                if ((question.getQuestionNumber() == record.getQuestionId())
                        && (MobileAcademyConstants.CONTENT_WRONG_ANSWER
                                .equalsIgnoreCase(question.getName()))) {
                    question.setAudioFile(record.getFileName());
                    break;
                }
            }
            courseFlags.markQuestionWrongAnswer(record.getChapterId(),
                    record.getQuestionId());

        } else if (record.getType() == FileType.CHAPTER_END_MENU) {
            if (MobileAcademyConstants.CONTENT_MENU
                    .equalsIgnoreCase(chapterContent.getName())) {
                chapterContent.setAudioFile(record.getFileName());
            }
            courseFlags.markChapterEndMenu(record.getChapterId());
        } else if (record.getType() == FileType.SCORE) {
            List<ScoreContent> scoreContents = chapterContent.getScores();
            for (ScoreContent scoreContent : scoreContents) {
                if ((MobileAcademyConstants.SCORE + String.format(
                        MobileAcademyConstants.TWO_DIGIT_INTEGER_FORMAT,
                        record.getScoreID())).equalsIgnoreCase(scoreContent
                        .getName())) {
                    scoreContent.setAudioFile(record.getFileName());
                    break;
                }
            }
            courseFlags.markScoreFile(record.getChapterId(),
                    record.getScoreID());

        }
    }

}