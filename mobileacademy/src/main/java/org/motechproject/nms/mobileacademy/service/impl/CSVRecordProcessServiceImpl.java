package org.motechproject.nms.mobileacademy.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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

    private String errorFileName;

    private CsvProcessingSummary result;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(CSVRecordProcessServiceImpl.class);

    @Override
    public String processRawRecords(List<CourseRawContent> courseRawContents,
            String csvFileName) {

        BulkUploadError errorDetail = new BulkUploadError();
        result = new CsvProcessingSummary();

        errorFileName = BulkUploadError
                .createBulkUploadErrLogFileName(csvFileName);

        Map<Integer, List<CourseRawContent>> mapForAddRecords = new HashMap<Integer, List<CourseRawContent>>();
        Map<String, List<CourseRawContent>> mapForModifyRecords = new HashMap<String, List<CourseRawContent>>();
        Map<Integer, List<CourseRawContent>> mapForDeleteRecords = new HashMap<Integer, List<CourseRawContent>>();

        List<Integer> listOfExistingLLCinCPC = courseProcessedContentService
                .getListOfAllExistingLLcs();

        for (CourseRawContent courseRawContent : courseRawContents) {
            try {
                validateSchema(courseRawContent);
            } catch (DataValidationException ex) {
                errorDetail.setErrorCategory(ex.getErrorCode());
                errorDetail.setRecordDetails(courseRawContent.getContentId());
                errorDetail.setErrorDescription(ex.getErrorDesc());
                bulkUploadErrLogService.writeBulkUploadErrLog(errorFileName,
                        errorDetail);
                result.incrementFailureCount();

                courseRawContentService.delete(courseRawContent);
                continue;
            }
            try {
                validateCircleAndLLC(courseRawContent);
            } catch (DataValidationException ex) {
                errorDetail.setErrorCategory(ex.getErrorCode());
                errorDetail.setRecordDetails(courseRawContent.getContentId());
                errorDetail.setErrorDescription(ex.getErrorDesc());
                bulkUploadErrLogService.writeBulkUploadErrLog(errorFileName,
                        errorDetail);
                result.incrementFailureCount();

                courseRawContentService.delete(courseRawContent);
                continue;
            }

            if (courseRawContent.getOperation().equalsIgnoreCase(
                    MobileAcademyConstants.COURSE_DEL)) {
                putRecordInDeleteMap(mapForDeleteRecords, courseRawContent);
            } else {
                int LLC = Integer.parseInt(courseRawContent
                        .getLanguageLocationCode());
                if (listOfExistingLLCinCPC.contains(LLC)) {
                    courseRawContent
                            .setOperation(MobileAcademyConstants.COURSE_MOD);
                    putRecordInModifyMap(mapForModifyRecords, courseRawContent);
                } else {
                    courseRawContent
                            .setOperation(MobileAcademyConstants.COURSE_ADD);
                    putRecordInAddMap(mapForAddRecords, courseRawContent);
                }
            }
        }

        int size = courseRawContents.size();
        for (int counter = size - 1; counter >= 0; counter--) {
            courseRawContents.remove(counter);
        }

        processAddRecords(mapForAddRecords);

        processModificationRecords(mapForModifyRecords);

        processDeleteRecords(mapForDeleteRecords);

        bulkUploadErrLogService.writeBulkUploadProcessingSummary(null,
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
            // log error circle not valid
            throw new DataValidationException(
                    MobileAcademyConstants.INCONSISTENT_DATA_MESSAGE,
                    ErrorCategoryConstants.INCONSISTENT_DATA, "Circle");
        }
        if (!masterDataService.isLLCValidInCircle(circle, llc)) {
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
            Map<String, List<CourseRawContent>> mapForModifyRecords) {

        BulkUploadError errorDetail = new BulkUploadError();

        Map<String, List<CourseRawContent>> fileNameChangeRecords = new HashMap<String, List<CourseRawContent>>();

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

                            processError(errorDetail, exc,
                                    courseRawContent.getContentId());

                            bulkUploadErrLogService.writeBulkUploadErrLog(
                                    errorFileName, errorDetail);
                            result.incrementFailureCount();

                            mapForModifyRecords.get(contentName).remove(
                                    courseRawContent);
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
                                CPC.setContentDuration(Integer
                                        .parseInt(courseRawContent
                                                .getContentDuration()));
                                CPC.setContentID(Integer
                                        .parseInt(courseRawContent
                                                .getContentId()));
                                courseProcessedContentService.update(CPC);
                            }
                            result.incrementSuccessCount();
                            mapForModifyRecords.get(contentName).remove(
                                    courseRawContent);
                            courseRawContentService.delete(courseRawContent);
                        }
                    }
                }
            }
        }

        // Start Processing the file change records:
        if (!mapForModifyRecords.isEmpty()) {
            Iterator<String> contentNameIterator = mapForModifyRecords.keySet()
                    .iterator();
            while (contentNameIterator.hasNext()) {
                String contentName = contentNameIterator.next();
                boolean updateContentFile = true;
                List<Integer> listOfExistingLlc = courseProcessedContentService
                        .getListOfAllExistingLLcs();

                List<CourseRawContent> courseRawContents = mapForModifyRecords
                        .get(contentName);
                if (courseRawContents.size() < listOfExistingLlc.size()) {

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

                    mapForModifyRecords.remove(contentName);
                    deleteCourseRawContentsByList(courseRawContents, true);

                    continue;
                }

                String fileName = fileNameChangeRecords.get(contentName).get(0)
                        .getContentFile();

                Iterator<CourseRawContent> fileNameIterator = courseRawContents
                        .iterator();
                while (fileNameIterator.hasNext()) {
                    CourseRawContent courseRawContent = fileNameIterator.next();
                    int LLC = Integer.parseInt(courseRawContent
                            .getLanguageLocationCode());
                    if (!fileName.equals(courseRawContent.getContentFile())) {

                        mapForModifyRecords.remove(contentName);

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

                        deleteCourseRawContentsByList(courseRawContents, true);

                        updateContentFile = false;
                        break;
                    }
                    listOfExistingLlc.remove(LLC);
                }

                if (!updateContentFile) {
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
                        validateRawContent(
                                fileNameChangeRecords.get(contentName).get(0),
                                record);
                    } catch (DataValidationException e) {
                    }

                    determineTypeAndUpdateChapterContent(record);

                    List<CourseRawContent> fileModifyingRecords = fileNameChangeRecords
                            .get(contentName);

                    Iterator<CourseRawContent> fileModifyingRecordsIterator = fileModifyingRecords
                            .iterator();

                    while (fileModifyingRecordsIterator.hasNext()) {
                        CourseRawContent courseRawContent = fileModifyingRecordsIterator
                                .next();
                        int LLC = Integer.parseInt(courseRawContent
                                .getLanguageLocationCode());
                        CourseProcessedContent CPC = courseProcessedContentService
                                .getRecordforModification(
                                        courseRawContent.getCircle(), LLC,
                                        contentName);
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
                        mapForModifyRecords.get(contentName).remove(
                                courseRawContent);
                        courseRawContentService.delete(courseRawContent);
                    }
                }
            }
        }
    }

    private void deleteCourseRawContentsByList(
            List<CourseRawContent> courseRawContents, Boolean hasErrorOccured) {
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
            Map<Integer, List<CourseRawContent>> mapForAddRecords) {
        boolean populateCourseStructure = false;
        CourseFlags courseFlags = new CourseFlags();
        List<Record> answerOptionRecordList = new ArrayList<Record>();
        BulkUploadError errorDetail = new BulkUploadError();

        if (!mapForAddRecords.isEmpty()) {
            Iterator<Integer> distictLLCIterator = mapForAddRecords.keySet()
                    .iterator();
            while (distictLLCIterator.hasNext()) {
                Integer LLC = distictLLCIterator.next();
                List<CourseRawContent> courseRawContents = mapForAddRecords
                        .get(LLC);
                if (CollectionUtils.isNotEmpty(courseRawContents)) {
                    if (courseRawContents.size() != MobileAcademyConstants.MIN_FILES_PER_COURSE) {

                        mapForAddRecords.remove(LLC);
                        deleteCourseRawContentsByList(courseRawContents, true);

                        bulkUploadErrLogService
                                .writeBulkUploadErrLog(
                                        errorFileName,
                                        new BulkUploadError(
                                                "",
                                                ErrorCategoryConstants.INCONSISTENT_DATA,
                                                String.format(
                                                        MobileAcademyConstants.INSUFFICIENT_RECORDS_FOR_ADD,
                                                        LLC)));

                        LOGGER.info(
                                "There must be exact {} records to populate the course corresponding to LLC:{}.",
                                MobileAcademyConstants.MIN_FILES_PER_COURSE,
                                LLC);
                        continue;
                    }

                    CourseUnitState courseState = coursePopulateService
                            .findCourseState();
                    if (courseState == null) {
                        populateCourseStructure = true;
                        coursePopulateService.populateMtrainingCourseData();
                    }

                    courseFlags.resetTheFlags();
                    answerOptionRecordList.clear();

                    List<ChapterContent> chapterContents;
                    if (populateCourseStructure) {
                        // Get a new Course instance
                        chapterContents = createChapterContentPrototype();
                    } else {
                        // Get the current Course instance
                        chapterContents = coursePopulateService
                                .getAllChapterContents();
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

                            mapForAddRecords.remove(LLC);
                            deleteCourseRawContentsByList(courseRawContents,
                                    true);
                        }

                        if (populateCourseStructure) {
                            if (record.getType() == FileType.QUESTION_CONTENT) {
                                answerOptionRecordList.add(record);
                            }
                            checkTypeAndAddToChapterContent(
                                    record,
                                    chapterContents.get(record.getChapterId() - 1),
                                    courseFlags);
                        } else {
                            if (!checkRecordConsistencyAndMarkFlag(
                                    record,
                                    chapterContents.get(record.getChapterId() - 1),
                                    courseFlags)) {

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

                                mapForAddRecords.remove(LLC);
                                deleteCourseRawContentsByList(
                                        courseRawContents, true);
                                break;
                            }
                        }
                    }

                    if (courseFlags.hasCompleteCourseArrived()) {
                        for (CourseRawContent courseRawContent : courseRawContents) {
                            result.incrementSuccessCount();
                            updateRecordInContentProcessedTable(courseRawContent);
                            courseRawContentService.delete(courseRawContent);
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
                        }
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
            Map<Integer, List<CourseRawContent>> mapForDeleteRecords) {

        BulkUploadError errorDetail = new BulkUploadError();

        if (!mapForDeleteRecords.isEmpty()) {
            Iterator<Integer> distictLLCIterator = mapForDeleteRecords.keySet()
                    .iterator();

            while (distictLLCIterator.hasNext()) {
                Integer LLC = distictLLCIterator.next();
                CourseFlags courseFlags = new CourseFlags();
                courseFlags.resetTheFlags();

                List<CourseRawContent> courseRawContents = mapForDeleteRecords
                        .get(LLC);

                if (courseRawContents.size() < MobileAcademyConstants.MIN_FILES_PER_COURSE) {
                    // log error: Size is less than minimum required
                    bulkUploadErrLogService
                            .writeBulkUploadErrLog(
                                    errorFileName,
                                    new BulkUploadError(
                                            "",
                                            ErrorCategoryConstants.INCONSISTENT_DATA,
                                            String.format(
                                                    MobileAcademyConstants.INSUFFICIENT_RECORDS_FOR_DEL,
                                                    LLC)));
                    mapForDeleteRecords.remove(LLC);
                    deleteCourseRawContentsByList(courseRawContents, true);
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
                        // error in particular record
                        processError(errorDetail, exc,
                                courseRawContent.getContentId());

                        bulkUploadErrLogService.writeBulkUploadErrLog(
                                errorFileName, errorDetail);

                        result.incrementFailureCount();
                        courseRawContentService.delete(courseRawContent);
                        mapForDeleteRecords.get(LLC).remove(courseRawContent);
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
                        chapterContentDataService.deleteAll();
                    }
                } else {
                    bulkUploadErrLogService
                            .writeBulkUploadErrLog(
                                    errorFileName,
                                    new BulkUploadError(
                                            "",
                                            ErrorCategoryConstants.INCONSISTENT_DATA,
                                            String.format(
                                                    MobileAcademyConstants.INCOMPLETE_RECORDS_FOR_DEL,
                                                    LLC)));
                }

                mapForDeleteRecords.remove(LLC);
                deleteCourseRawContentsByList(courseRawContents, false);
            }
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
            ChapterContent chapterContent, CourseFlags courseFlags) {
        boolean status = true;
        if (record.getType() == FileType.LESSON_CONTENT) {
            for (LessonContent lessonContent : chapterContent.getLessons()) {
                if (lessonContent.getLessonNumber() == record.getLessonId()
                        && lessonContent.getName().equalsIgnoreCase(
                                MobileAcademyConstants.CONTENT_LESSON)) {
                    if (lessonContent.getAudioFile() != record.getFileName()) {
                        status = false;
                    } else {
                        break;
                    }
                }
            }
            courseFlags.markLessonContent(record.getChapterId(),
                    record.getLessonId());

        } else if (record.getType() == FileType.LESSON_END_MENU) {
            for (LessonContent lessonContent : chapterContent.getLessons()) {
                if ((lessonContent.getLessonNumber() == record.getLessonId())
                        && (lessonContent.getName()
                                .equalsIgnoreCase(MobileAcademyConstants.CONTENT_MENU))) {
                    if (lessonContent.getAudioFile() != record.getFileName()) {
                        status = false;
                    } else {
                        break;
                    }
                }
            }
            courseFlags.markLessonEndMenu(record.getChapterId(),
                    record.getLessonId());

        } else if (record.getType() == FileType.QUIZ_HEADER) {
            QuizContent quiz = chapterContent.getQuiz();
            if ((quiz.getName()
                    .equalsIgnoreCase(MobileAcademyConstants.CONTENT_QUIZ_HEADER))) {
                quiz.setAudioFile(record.getFileName());
            }
            courseFlags.markQuizHeader(record.getChapterId());

        } else if (record.getType() == FileType.QUESTION_CONTENT) {
            for (QuestionContent questionContent : chapterContent.getQuiz()
                    .getQuestions()) {
                if ((questionContent.getQuestionNumber() == record
                        .getQuestionId())
                        && (questionContent.getName()
                                .equalsIgnoreCase(MobileAcademyConstants.CONTENT_QUESTION))) {
                    if (questionContent.getAudioFile() != record.getFileName()) {
                        status = false;
                    } else {
                        break;
                    }
                }
            }
            courseFlags.markQuestionContent(record.getChapterId(),
                    record.getQuestionId());

        } else if (record.getType() == FileType.CORRECT_ANSWER) {
            for (QuestionContent questionContent : chapterContent.getQuiz()
                    .getQuestions()) {
                if ((questionContent.getQuestionNumber() == record
                        .getQuestionId())
                        && (questionContent.getName()
                                .equalsIgnoreCase(MobileAcademyConstants.CONTENT_CORRECT_ANSWER))) {
                    if (questionContent.getAudioFile() != record.getFileName()) {
                        status = false;
                    } else {
                        break;
                    }
                }
            }
            courseFlags.markQuestionCorrectAnswer(record.getChapterId(),
                    record.getQuestionId());

        } else if (record.getType() == FileType.WRONG_ANSWER) {
            for (QuestionContent questionContent : chapterContent.getQuiz()
                    .getQuestions()) {
                if ((questionContent.getQuestionNumber() == record
                        .getQuestionId())
                        && (questionContent.getName()
                                .equalsIgnoreCase(MobileAcademyConstants.CONTENT_WRONG_ANSWER))) {
                    if (questionContent.getAudioFile() != record.getFileName()) {
                        status = false;
                    } else {
                        break;
                    }
                }
            }
            courseFlags.markQuestionWrongAnswer(record.getChapterId(),
                    record.getQuestionId());

        } else if (record.getType() == FileType.CHAPTER_END_MENU) {
            if (chapterContent.getName().equalsIgnoreCase(
                    MobileAcademyConstants.CONTENT_MENU)) {
                if (chapterContent.getAudioFile() != record.getFileName()) {
                    status = false;
                }
                courseFlags.markChapterEndMenu(record.getChapterId());
            }
        } else if (record.getType() == FileType.SCORE) {
            for (ScoreContent scoreContent : chapterContent.getScores()) {
                if (scoreContent
                        .getName()
                        .equalsIgnoreCase(
                                MobileAcademyConstants.SCORE
                                        + String.format(
                                                MobileAcademyConstants.TWO_DIGIT_INTEGER_FORMAT,
                                                record.getScoreID()))) {
                    if (scoreContent.getAudioFile() != record.getFileName()) {
                        status = false;
                    } else {
                        break;
                    }
                }
            }
            courseFlags.markScoreFile(record.getChapterId(),
                    record.getScoreID());
        }
        return status;
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
        courseProcessedContentService.create(new CourseProcessedContent(Integer
                .parseInt(courseRawContent.getContentId()), courseRawContent
                .getCircle().toUpperCase(), Integer.parseInt(courseRawContent
                .getLanguageLocationCode()), courseRawContent.getContentName()
                .toUpperCase(), ContentType.findByName(courseRawContent
                .getContentType()), courseRawContent.getContentFile(), Integer
                .parseInt(courseRawContent.getContentDuration()),
                courseRawContent.getMetaData().toUpperCase()));
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

            if (!metaData.substring(0, metaData.indexOf(':')).equalsIgnoreCase(
                    "CorrectAnswer")) {
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
                || !chapterString.substring(0, chapterString.length() - 2)
                        .equalsIgnoreCase("Chapter")) {
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

        if (!isTypeDeterminable(record, subString)) {
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
            record.setLessonId(index);
            return true;
        } else if (fileType == FileType.LESSON_END_MENU) {
            record.setLessonId(index);
            return true;
        } else if (fileType == FileType.QUESTION_CONTENT) {
            record.setQuestionId(index);
            return true;
        } else if (fileType == FileType.CORRECT_ANSWER) {
            record.setQuestionId(index);
            return true;
        } else if (fileType == FileType.WRONG_ANSWER) {
            record.setQuestionId(index);
            return true;
        } else if (fileType == FileType.SCORE) {
            record.setScoreID(index);
            return true;
        } else {
            return false;
        }
    }

    /*
     * This function checks the file-type to which the record refers and on the
     * basis of that, it populates the chapterContent Prototype Object and marks
     * the flag in courseFlags for successful arrival of the file.
     */
    private void checkTypeAndAddToChapterContent(Record record,
            ChapterContent chapterContent, CourseFlags courseFlags) {

        if (record.getType() == FileType.LESSON_CONTENT) {
            List<LessonContent> lessons = chapterContent.getLessons();
            for (LessonContent lesson : lessons) {
                if ((lesson.getLessonNumber() == record.getLessonId())
                        && (lesson.getName()
                                .equalsIgnoreCase(MobileAcademyConstants.CONTENT_LESSON))) {
                    lesson.setAudioFile(record.getFileName());
                }
            }
            courseFlags.markLessonContent(record.getChapterId(),
                    record.getLessonId());

        } else if (record.getType() == FileType.LESSON_END_MENU) {
            List<LessonContent> lessons = chapterContent.getLessons();
            for (LessonContent lesson : lessons) {
                if ((lesson.getLessonNumber() == record.getLessonId())
                        && (lesson.getName()
                                .equalsIgnoreCase(MobileAcademyConstants.CONTENT_MENU))) {
                    lesson.setAudioFile(record.getFileName());
                }
            }
            courseFlags.markLessonEndMenu(record.getChapterId(),
                    record.getLessonId());

        } else if (record.getType() == FileType.QUIZ_HEADER) {
            QuizContent quiz = chapterContent.getQuiz();
            if ((quiz.getName()
                    .equalsIgnoreCase(MobileAcademyConstants.CONTENT_QUIZ_HEADER))) {
                quiz.setAudioFile(record.getFileName());
            }
            courseFlags.markQuizHeader(record.getChapterId());

        } else if (record.getType() == FileType.QUESTION_CONTENT) {
            List<QuestionContent> questions = chapterContent.getQuiz()
                    .getQuestions();
            for (QuestionContent question : questions) {
                if ((question.getQuestionNumber() == record.getQuestionId())
                        && (question.getName()
                                .equalsIgnoreCase(MobileAcademyConstants.CONTENT_QUESTION))) {
                    question.setAudioFile(record.getFileName());
                }
            }
            courseFlags.markQuestionContent(record.getChapterId(),
                    record.getQuestionId());

        } else if (record.getType() == FileType.CORRECT_ANSWER) {
            List<QuestionContent> questions = chapterContent.getQuiz()
                    .getQuestions();
            for (QuestionContent question : questions) {
                if ((question.getQuestionNumber() == record.getQuestionId())
                        && (question.getName()
                                .equalsIgnoreCase(MobileAcademyConstants.CONTENT_CORRECT_ANSWER))) {
                    question.setAudioFile(record.getFileName());
                }
            }
            courseFlags.markQuestionCorrectAnswer(record.getChapterId(),
                    record.getQuestionId());

        } else if (record.getType() == FileType.WRONG_ANSWER) {
            List<QuestionContent> questions = chapterContent.getQuiz()
                    .getQuestions();
            for (QuestionContent question : questions) {
                if ((question.getQuestionNumber() == record.getQuestionId())
                        && (question.getName()
                                .equalsIgnoreCase(MobileAcademyConstants.CONTENT_WRONG_ANSWER))) {
                    question.setAudioFile(record.getFileName());
                }
            }
            courseFlags.markQuestionWrongAnswer(record.getChapterId(),
                    record.getQuestionId());

        } else if (record.getType() == FileType.CHAPTER_END_MENU) {
            if (chapterContent.getName().equalsIgnoreCase(
                    MobileAcademyConstants.CONTENT_MENU)) {
                chapterContent.setAudioFile(record.getFileName());
            }
            courseFlags.markChapterEndMenu(record.getChapterId());
        } else if (record.getType() == FileType.SCORE) {
            List<ScoreContent> scoreContents = chapterContent.getScores();
            for (ScoreContent scoreContent : scoreContents) {
                if (scoreContent
                        .getName()
                        .equalsIgnoreCase(
                                MobileAcademyConstants.SCORE
                                        + String.format(
                                                MobileAcademyConstants.TWO_DIGIT_INTEGER_FORMAT,
                                                record.getScoreID()))) {
                    scoreContent.setAudioFile(record.getFileName());
                }
            }
            courseFlags.markScoreFile(record.getChapterId(),
                    record.getScoreID());

        }
    }

}