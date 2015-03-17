package org.motechproject.nms.mobileacademy.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.motechproject.mtraining.domain.Course;
import org.motechproject.mtraining.domain.CourseUnitState;
import org.motechproject.nms.mobileacademy.commons.ContentType;
import org.motechproject.nms.mobileacademy.commons.CourseFlag;
import org.motechproject.nms.mobileacademy.commons.FileType;
import org.motechproject.nms.mobileacademy.commons.MobileAcademyConstants;
import org.motechproject.nms.mobileacademy.commons.Record;
import org.motechproject.nms.mobileacademy.commons.UserDetailsDTO;
import org.motechproject.nms.mobileacademy.domain.ChapterContent;
import org.motechproject.nms.mobileacademy.domain.CourseContentCsv;
import org.motechproject.nms.mobileacademy.domain.CourseProcessedContent;
import org.motechproject.nms.mobileacademy.domain.LessonContent;
import org.motechproject.nms.mobileacademy.domain.QuestionContent;
import org.motechproject.nms.mobileacademy.domain.QuizContent;
import org.motechproject.nms.mobileacademy.domain.ScoreContent;
import org.motechproject.nms.mobileacademy.repository.ChapterContentDataService;
import org.motechproject.nms.mobileacademy.service.CSVRecordProcessService;
import org.motechproject.nms.mobileacademy.service.CourseContentCsvService;
import org.motechproject.nms.mobileacademy.service.CoursePopulateService;
import org.motechproject.nms.mobileacademy.service.CourseProcessedContentService;
import org.motechproject.nms.util.constants.ErrorCategoryConstants;
import org.motechproject.nms.util.domain.BulkUploadError;
import org.motechproject.nms.util.domain.BulkUploadStatus;
import org.motechproject.nms.util.domain.RecordType;
import org.motechproject.nms.util.helper.DataValidationException;
import org.motechproject.nms.util.helper.NmsUtils;
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
    private CourseContentCsvService courseContentCsvService;

    @Autowired
    private CourseProcessedContentService courseProcessedContentService;

    @Autowired
    private ChapterContentDataService chapterContentDataService;

    @Autowired
    private CoursePopulateService coursePopulateService;

    @Autowired
    private BulkUploadErrLogService bulkUploadErrLogService;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(CSVRecordProcessServiceImpl.class);

    @Override
    public String processRawRecords(List<CourseContentCsv> courseContentCsvs,
            String csvFileName) {
        DateTime timeOfUpload = NmsUtils.getCurrentTimeStamp();
        BulkUploadStatus bulkUploadStatus = new BulkUploadStatus("",
                csvFileName, timeOfUpload, 0, 0);

        BulkUploadError bulkUploadError = new BulkUploadError(csvFileName,
                timeOfUpload, RecordType.COURSE_CONTENT, "", "", "");

        Map<Integer, List<CourseContentCsv>> mapForAddRecords = new HashMap<Integer, List<CourseContentCsv>>();
        Map<String, List<CourseContentCsv>> mapForModifyRecords = new HashMap<String, List<CourseContentCsv>>();

        List<Integer> listOfExistingLlc = courseProcessedContentService
                .getListOfAllExistingLlcs();
        UserDetailsDTO userDetailsDTO = new UserDetailsDTO();

        if (CollectionUtils.isNotEmpty(courseContentCsvs)) {
            // set user details from first record
            userDetailsDTO.setCreator(courseContentCsvs.get(0).getCreator());
            bulkUploadStatus.setUploadedBy(userDetailsDTO.getCreator());
            userDetailsDTO.setModifiedBy(courseContentCsvs.get(0)
                    .getModifiedBy());
            userDetailsDTO.setOwner(courseContentCsvs.get(0).getOwner());

            Iterator<CourseContentCsv> recordIterator = courseContentCsvs
                    .iterator();

            while (recordIterator.hasNext()) {
                CourseContentCsv courseContentCsv = recordIterator.next();
                try {
                    validateSchema(courseContentCsv);
                } catch (DataValidationException ex) {
                    processError(bulkUploadError, ex,
                            courseContentCsv.getContentId());
                    bulkUploadErrLogService
                            .writeBulkUploadErrLog(bulkUploadError);
                    bulkUploadStatus.incrementFailureCount();

                    recordIterator.remove();
                    courseContentCsvService.delete(courseContentCsv);
                    LOGGER.warn("Schema Validation failed for Content ID: {}",
                            courseContentCsv.getContentId());
                    continue;
                }

                int languageLocCode = Integer.parseInt(courseContentCsv
                        .getLanguageLocationCode());
                if (listOfExistingLlc.contains(languageLocCode)) {
                    putRecordInModifyMap(mapForModifyRecords, courseContentCsv);
                    LOGGER.debug(
                            "Record moved to Modify Map for Content ID: {}",
                            courseContentCsv.getContentId());
                } else {
                    putRecordInAddMap(mapForAddRecords, courseContentCsv);
                    LOGGER.debug(
                            "Record moved to Addition Map for Content ID: {}",
                            courseContentCsv.getContentId());
                }
                recordIterator.remove();
            }
        }

        processAddRecords(mapForAddRecords, bulkUploadError, bulkUploadStatus,
                userDetailsDTO);
        processModificationRecords(mapForModifyRecords, bulkUploadError,
                bulkUploadStatus, userDetailsDTO);

        bulkUploadErrLogService
                .writeBulkUploadProcessingSummary(bulkUploadStatus);

        LOGGER.info("Finished processing CircleCsv-import success");

        return "Records Processed Successfully";
    }

    /*
     * This function adds the records into a Map having LLC of record as key
     * 
     * The map will be process afterwards for processing ADD Records
     */
    private void putRecordInAddMap(
            Map<Integer, List<CourseContentCsv>> mapForAddRecords,
            CourseContentCsv courseContentCsv) {
        int languageLocCode = Integer.parseInt(courseContentCsv
                .getLanguageLocationCode());
        if (mapForAddRecords.containsKey(languageLocCode)) {
            mapForAddRecords.get(languageLocCode).add(courseContentCsv);
        } else {
            List<CourseContentCsv> listOfRecords = new ArrayList<CourseContentCsv>();
            listOfRecords.add(courseContentCsv);
            mapForAddRecords.put(languageLocCode, listOfRecords);
        }
    }

    /*
     * This function adds the records into a Map having contentName of record as
     * key
     * 
     * The map will be process afterwards for processing "MOD"ify Records
     */
    private void putRecordInModifyMap(
            Map<String, List<CourseContentCsv>> mapForModifyRecords,
            CourseContentCsv courseContentCsv) {
        String key = courseContentCsv.getContentName();
        if (mapForModifyRecords.containsKey(key)) {
            mapForModifyRecords.get(key).add(courseContentCsv);
        } else {
            List<CourseContentCsv> listOfRecords = new ArrayList<CourseContentCsv>();
            listOfRecords.add(courseContentCsv);
            mapForModifyRecords.put(key, listOfRecords);
        }
    }

    /*
     * This function does the schema level validation on a CourseContentCsv
     * Record. In case a erroneous field, throws DataValidationException
     */
    private void validateSchema(CourseContentCsv courseContentCsv)
            throws DataValidationException {

        ParseDataHelper.validateAndParseInt("Content ID", courseContentCsv.getContentId(),
                true);

        ParseDataHelper.validateAndParseInt("Language Location Code",
                courseContentCsv.getLanguageLocationCode(), true);

        ParseDataHelper.validateAndParseString("Contet Name",
                courseContentCsv.getContentName(), true);

        ParseDataHelper.validateAndParseInt("Content Duration",
                courseContentCsv.getContentDuration(), true);

        ParseDataHelper.validateAndParseString("Content File",
                courseContentCsv.getContentFile(), true);
    }

    /*
     * This function takes The Map having CourserawContent Records for the
     * modification and processes them
     */
    private void processModificationRecords(
            Map<String, List<CourseContentCsv>> mapForModifyRecords,
            BulkUploadError bulkUploadError, BulkUploadStatus bulkUploadStatus,
            UserDetailsDTO userDetailsDTO) {

        if (!mapForModifyRecords.isEmpty()) {
            Iterator<String> contentNamesIterator = mapForModifyRecords
                    .keySet().iterator();
            while (contentNamesIterator.hasNext()) {
                String contentName = contentNamesIterator.next();
                List<CourseContentCsv> courseContentCsvs = mapForModifyRecords
                        .get(contentName);
                if (CollectionUtils.isNotEmpty(courseContentCsvs)) {
                    Iterator<CourseContentCsv> courseRawContentsIterator = courseContentCsvs
                            .iterator();

                    while (courseRawContentsIterator.hasNext()) {
                        CourseContentCsv courseContentCsv = courseRawContentsIterator
                                .next();

                        Record record = new Record();
                        try {
                            validateRawContent(courseContentCsv, record);
                        } catch (DataValidationException exc) {
                            LOGGER.warn(
                                    "Record Validation failed for content ID: {}",
                                    courseContentCsv.getContentId());
                            processError(bulkUploadError, exc,
                                    courseContentCsv.getContentId());

                            bulkUploadErrLogService
                                    .writeBulkUploadErrLog(bulkUploadError);
                            bulkUploadStatus.incrementFailureCount();

                            courseRawContentsIterator.remove();
                            courseContentCsvService.delete(courseContentCsv);
                            continue;
                        }

                        if (isRecordChangingTheFileName(record)
                                || isRecordChangingTheAnswerOption(record)) {
                            continue;
                        } else {
                            int languageLocCode = Integer
                                    .parseInt(courseContentCsv
                                            .getLanguageLocationCode());
                            CourseProcessedContent courseProcessedContent = courseProcessedContentService
                                    .getRecordforModification(courseContentCsv
                                            .getCircle(), languageLocCode,
                                            courseContentCsv.getContentName()
                                                    .toUpperCase());

                            if (courseProcessedContent != null) {
                                int contentDuration = Integer
                                        .parseInt(courseContentCsv
                                                .getContentDuration());
                                int contentId = Integer
                                        .parseInt(courseContentCsv
                                                .getContentId());
                                if ((courseProcessedContent
                                        .getContentDuration() != contentDuration)
                                        && (courseProcessedContent
                                                .getContentID() != contentId)) {
                                    LOGGER.info(
                                            "ContentID and duration updated for content name: {}, LLC: {}",
                                            courseContentCsv.getContentName(),
                                            courseContentCsv
                                                    .getLanguageLocationCode());
                                    courseProcessedContent
                                            .setContentDuration(contentDuration);
                                    courseProcessedContent
                                            .setContentID(contentId);
                                    courseProcessedContent
                                            .setModifiedBy(userDetailsDTO
                                                    .getModifiedBy());
                                    courseProcessedContentService
                                            .update(courseProcessedContent);
                                    break;
                                }

                            }
                            bulkUploadStatus.incrementSuccessCount();
                            courseRawContentsIterator.remove();
                            courseContentCsvService.delete(courseContentCsv);
                        }
                    }
                }
                courseContentCsvs = mapForModifyRecords.get(contentName);
                if (CollectionUtils.isEmpty(courseContentCsvs)) {
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
                boolean flagForUpdatingMetaData = false;
                boolean flagForAbortingModification = false;

                // Getting new List as the list return is unmodifiable
                List<Integer> listOfExistingLlc = new ArrayList<Integer>(
                        courseProcessedContentService
                                .getListOfAllExistingLlcs());

                List<CourseContentCsv> courseContentCsvs = mapForModifyRecords
                        .get(contentName);
                if (courseContentCsvs.size() < listOfExistingLlc.size()) {
                    LOGGER.warn(
                            "Records corresponding to all the existing LLCs not received for modification against content name: {}",
                            contentName);

                    bulkUploadError.setRecordDetails(contentName);
                    bulkUploadError
                            .setErrorCategory(ErrorCategoryConstants.INCONSISTENT_DATA);
                    bulkUploadError
                            .setErrorDescription(String
                                    .format(MobileAcademyConstants.INSUFFICIENT_RECORDS_FOR_MODIFY,
                                            contentName));

                    bulkUploadErrLogService
                            .writeBulkUploadErrLog(bulkUploadError);

                    contentNamesIterator.remove();
                    deleteCourseRawContentsByList(courseContentCsvs, true,
                            bulkUploadStatus);

                    continue;
                }

                String fileName = mapForModifyRecords.get(contentName).get(0)
                        .getContentFile();

                String metaData = "";

                int correctAnswerOption = 0;

                /*
                 * This block of code is just being written for the purpose to
                 * know whether this bunch of record refers to questionContent
                 * type or not
                 */
                Record record = new Record();
                try {
                    validateContentName(mapForModifyRecords.get(contentName)
                            .get(0), record);
                } catch (DataValidationException e1) {
                    e1.printStackTrace();
                }

                if (record.getType() == FileType.QUESTION_CONTENT) {
                    metaData = mapForModifyRecords.get(contentName).get(0)
                            .getMetaData();
                    flagForUpdatingMetaData = true;
                }

                Iterator<CourseContentCsv> courseRawContentsIterator = courseContentCsvs
                        .iterator();
                while (courseRawContentsIterator.hasNext()) {
                    CourseContentCsv courseContentCsv = courseRawContentsIterator
                            .next();
                    int languageLocCode = Integer.parseInt(courseContentCsv
                            .getLanguageLocationCode());
                    if (!fileName.equals(courseContentCsv.getContentFile())) {
                        LOGGER.warn(
                                "Content file name does not match for content name: {}, contentID: {}",
                                contentName, courseContentCsv.getContentId());
                        flagForAbortingModification = true;
                    }
                    /*
                     * Check for consistency of metaData only if record
                     * corresponds to question content type
                     */
                    if (flagForUpdatingMetaData) {
                        if (!metaData.equalsIgnoreCase(courseContentCsv
                                .getMetaData())) {
                            LOGGER.warn(
                                    "Correct Answer Option(MetaData) does not match for content name: {}, contentID: {}",
                                    contentName,
                                    courseContentCsv.getContentId());
                            flagForAbortingModification = true;
                        }
                    }
                    if (flagForAbortingModification) {

                        bulkUploadError.setRecordDetails(courseContentCsv
                                .getContentId());
                        bulkUploadError
                                .setErrorCategory(ErrorCategoryConstants.INCONSISTENT_DATA);
                        bulkUploadError
                                .setErrorDescription(String
                                        .format(MobileAcademyConstants.INCONSISTENT_RECORD_FOR_MODIFY,
                                                contentName));
                        bulkUploadErrLogService
                                .writeBulkUploadErrLog(bulkUploadError);

                        deleteCourseRawContentsByList(courseContentCsvs, true,
                                bulkUploadStatus);
                        contentNamesIterator.remove();
                        break;
                    }
                    listOfExistingLlc.remove(Integer.valueOf(languageLocCode));
                }

                if (flagForAbortingModification) {
                    contentNamesIterator.remove();
                    continue;
                }

                // If data has arrived for all the existing LLCS.
                if (CollectionUtils.isEmpty(listOfExistingLlc)) {
                    /*
                     * This is done just to know the type of file to which this
                     * bunch of modification record refers to.
                     */
                    try {
                        if (mapForModifyRecords.get(contentName) != null) {
                            validateRawContent(
                                    mapForModifyRecords.get(contentName).get(0),
                                    record);
                        }
                    } catch (DataValidationException e) {
                        LOGGER.debug(e.getMessage(), e);
                    }

                    if (isRecordChangingTheFileName(record)) {
                        determineTypeAndUpdateChapterContent(record,
                                userDetailsDTO);
                        LOGGER.info(
                                "Audio file name has been changed for contentName: {}",
                                contentName);
                    }

                    if (flagForUpdatingMetaData
                            && isRecordChangingTheAnswerOption(record)) {
                        correctAnswerOption = record.getAnswerId();
                        coursePopulateService
                                .updateCorrectAnswer(
                                        MobileAcademyConstants.CHAPTER
                                                + String.format(
                                                        MobileAcademyConstants.TWO_DIGIT_INTEGER_FORMAT,
                                                        record.getChapterId()),
                                        MobileAcademyConstants.QUESTION
                                                + String.format(
                                                        MobileAcademyConstants.TWO_DIGIT_INTEGER_FORMAT,
                                                        record.getChapterId()),
                                        Integer.toString(correctAnswerOption),
                                        userDetailsDTO);
                        LOGGER.info(
                                "Correct Answer Option for contentName: {} has been changed to :{}",
                                contentName, correctAnswerOption);
                    }

                    List<CourseContentCsv> fileModifyingRecords = mapForModifyRecords
                            .get(contentName);

                    Iterator<CourseContentCsv> fileModifyingRecordsIterator = fileModifyingRecords
                            .iterator();

                    while (fileModifyingRecordsIterator.hasNext()) {
                        CourseContentCsv courseContentCsv = fileModifyingRecordsIterator
                                .next();
                        int languageLocCode = Integer.parseInt(courseContentCsv
                                .getLanguageLocationCode());
                        CourseProcessedContent courseProcessedContent = courseProcessedContentService
                                .getRecordforModification(courseContentCsv
                                        .getCircle().toUpperCase(),
                                        languageLocCode, contentName
                                                .toUpperCase());
                        if (courseProcessedContent != null) {
                            courseProcessedContent.setContentFile(fileName);
                            courseProcessedContent.setContentDuration(Integer
                                    .parseInt(courseContentCsv
                                            .getContentDuration()));
                            courseProcessedContent.setContentID(Integer
                                    .parseInt(courseContentCsv.getContentId()));
                            courseProcessedContent.setModifiedBy(userDetailsDTO
                                    .getModifiedBy());
                            if (flagForUpdatingMetaData) {
                                courseProcessedContent
                                        .setMetadata(MobileAcademyConstants.CONTENT_CORRECT_ANSWER
                                                + ":"
                                                + Integer
                                                        .toString(correctAnswerOption));
                            }
                            courseProcessedContentService
                                    .update(courseProcessedContent);
                        }

                        bulkUploadStatus.incrementSuccessCount();
                        fileModifyingRecordsIterator.remove();
                        courseContentCsvService.delete(courseContentCsv);
                    }
                    LOGGER.warn("Course modified for content name: {}",
                            contentName);
                } else { // Not sufficient records for a course
                    LOGGER.warn("Course not modified for content name: {}",
                            contentName);
                    LOGGER.warn("Records for all exisiting LLCs not recieved");

                    bulkUploadError.setRecordDetails(contentName);
                    bulkUploadError
                            .setErrorCategory(ErrorCategoryConstants.INCONSISTENT_DATA);
                    bulkUploadError
                            .setErrorDescription(String
                                    .format(MobileAcademyConstants.INSUFFICIENT_RECORDS_FOR_MODIFY,
                                            contentName));

                    bulkUploadErrLogService
                            .writeBulkUploadErrLog(bulkUploadError);

                    deleteCourseRawContentsByList(
                            mapForModifyRecords.get(contentName), true,
                            bulkUploadStatus);
                }
            }
        }
    }

    private boolean isRecordChangingTheAnswerOption(Record record) {
        if (record.getType() != FileType.QUESTION_CONTENT) {
            return false;
        } else {
            if (coursePopulateService.getCorrectAnswerOption(
                    record.getChapterId(), record.getQuestionId()) != record
                    .getAnswerId()) {
                return true;
            }
        }
        return false;
    }

    private void deleteCourseRawContentsByList(
            List<CourseContentCsv> courseContentCsvs, Boolean hasErrorOccured,
            BulkUploadStatus bulkUploadStatus) {
        if (CollectionUtils.isNotEmpty(courseContentCsvs)) {

            for (CourseContentCsv courseContentCsv : courseContentCsvs) {
                if (hasErrorOccured) {
                    bulkUploadStatus.incrementFailureCount();
                } else {
                    bulkUploadStatus.incrementSuccessCount();
                }
                courseContentCsvService.delete(courseContentCsv);
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
    private void determineTypeAndUpdateChapterContent(Record record,
            UserDetailsDTO userDetailsDTO) {
        if (record.getType() == FileType.LESSON_CONTENT) {
            coursePopulateService.setLessonContent(record.getChapterId(),
                    record.getLessonId(),
                    MobileAcademyConstants.CONTENT_LESSON,
                    record.getFileName(), userDetailsDTO);
        } else if (record.getType() == FileType.LESSON_END_MENU) {
            coursePopulateService.setLessonContent(record.getChapterId(),
                    record.getLessonId(), MobileAcademyConstants.CONTENT_MENU,
                    record.getFileName(), userDetailsDTO);
        } else if (record.getType() == FileType.QUESTION_CONTENT) {
            coursePopulateService.setQuestionContent(record.getChapterId(),
                    record.getQuestionId(),
                    MobileAcademyConstants.CONTENT_QUESTION,
                    record.getFileName(), userDetailsDTO);
        } else if (record.getType() == FileType.CORRECT_ANSWER) {
            coursePopulateService.setQuestionContent(record.getChapterId(),
                    record.getQuestionId(),
                    MobileAcademyConstants.CONTENT_CORRECT_ANSWER,
                    record.getFileName(), userDetailsDTO);
        } else if (record.getType() == FileType.WRONG_ANSWER) {
            coursePopulateService.setQuestionContent(record.getChapterId(),
                    record.getQuestionId(),
                    MobileAcademyConstants.CONTENT_WRONG_ANSWER,
                    record.getFileName(), userDetailsDTO);
        } else if (record.getType() == FileType.CHAPTER_END_MENU) {
            coursePopulateService.setChapterContent(record.getChapterId(),
                    MobileAcademyConstants.CONTENT_MENU, record.getFileName(),
                    userDetailsDTO);
        } else if (record.getType() == FileType.QUIZ_HEADER) {
            coursePopulateService.setQuizContent(record.getChapterId(),
                    MobileAcademyConstants.CONTENT_QUIZ_HEADER,
                    record.getFileName(), userDetailsDTO);
        } else if (record.getType() == FileType.SCORE) {
            coursePopulateService.setScore(record.getChapterId(),
                    record.getScoreID(), MobileAcademyConstants.SCORE,
                    record.getFileName(), userDetailsDTO);
        }
    }

    /*
     * This checks if a modify record is also changing the name of the file
     * currently existing the system. If yes, it returns true.
     */
    private boolean isRecordChangingTheFileName(Record record) {
        boolean status = false;
        List<ChapterContent> chapterContents = coursePopulateService
                .getAllChapterContents();

        if (record.getType() == FileType.LESSON_CONTENT) {
            LessonContent lessonContent = coursePopulateService
                    .getLessonContent(chapterContents, record.getChapterId(),
                            record.getLessonId(),
                            MobileAcademyConstants.CONTENT_LESSON);
            if (!lessonContent.getAudioFile().equals(record.getFileName())) {
                status = true;
            }
        } else if (record.getType() == FileType.LESSON_END_MENU) {
            LessonContent lessonContent = coursePopulateService
                    .getLessonContent(chapterContents, record.getChapterId(),
                            record.getLessonId(),
                            MobileAcademyConstants.CONTENT_MENU);
            if (!lessonContent.getAudioFile().equals(record.getFileName())) {
                status = true;
            }
        } else if (record.getType() == FileType.QUESTION_CONTENT) {
            QuestionContent questionContent = coursePopulateService
                    .getQuestionContent(chapterContents, record.getChapterId(),
                            record.getQuestionId(),
                            MobileAcademyConstants.CONTENT_QUESTION);
            if (!questionContent.getAudioFile().equals(record.getFileName())) {
                status = true;
            }
        } else if (record.getType() == FileType.CORRECT_ANSWER) {
            QuestionContent questionContent = coursePopulateService
                    .getQuestionContent(chapterContents, record.getChapterId(),
                            record.getQuestionId(),
                            MobileAcademyConstants.CONTENT_CORRECT_ANSWER);
            if (!questionContent.getAudioFile().equals(record.getFileName())) {
                status = true;
            }
        } else if (record.getType() == FileType.WRONG_ANSWER) {
            QuestionContent questionContent = coursePopulateService
                    .getQuestionContent(chapterContents, record.getChapterId(),
                            record.getQuestionId(),
                            MobileAcademyConstants.CONTENT_WRONG_ANSWER);
            if (!questionContent.getAudioFile().equals(record.getFileName())) {
                status = true;
            }
        } else if (record.getType() == FileType.CHAPTER_END_MENU) {
            ChapterContent chapterContent = coursePopulateService
                    .getChapterContent(chapterContents, record.getChapterId(),
                            MobileAcademyConstants.CONTENT_MENU);
            if (!chapterContent.getAudioFile().equals(record.getFileName())) {
                status = true;
            }
        } else if (record.getType() == FileType.QUIZ_HEADER) {
            QuizContent quizContent = coursePopulateService.getQuizContent(
                    chapterContents, record.getChapterId(),
                    MobileAcademyConstants.CONTENT_QUIZ_HEADER);
            if (!quizContent.getAudioFile().equals(record.getFileName())) {
                status = true;
            }
        } else if (record.getType() == FileType.SCORE) {
            ScoreContent scoreContent = coursePopulateService.getScore(
                    chapterContents, record.getChapterId(),
                    record.getScoreID(), MobileAcademyConstants.SCORE);
            if (!scoreContent.getAudioFile().equals(record.getFileName())) {
                status = true;
            }
        }
        return status;
    }

    /*
     * This function takes the list of CourseContentCsv records against which
     * the file need to be added into the course
     */
    private void processAddRecords(
            Map<Integer, List<CourseContentCsv>> mapForAddRecords,
            BulkUploadError bulkUploadError, BulkUploadStatus bulkUploadStatus,
            UserDetailsDTO userDetailsDTO) {

        boolean populateCourseStructure = false;

        List<Record> answerOptionRecordList = new ArrayList<Record>();
        boolean abortAdditionProcess = false;

        if (!mapForAddRecords.isEmpty()) {
            Iterator<Integer> distictLLCIterator = mapForAddRecords.keySet()
                    .iterator();
            while (distictLLCIterator.hasNext()) {
                CourseFlag courseFlag = new CourseFlag();
                abortAdditionProcess = false;
                populateCourseStructure = false;
                Integer languageLocCode = distictLLCIterator.next();
                List<CourseContentCsv> courseContentCsvs = mapForAddRecords
                        .get(languageLocCode);
                if (CollectionUtils.isNotEmpty(courseContentCsvs)) {
                    if (courseContentCsvs.size() != MobileAcademyConstants.MIN_FILES_PER_COURSE) {
                        LOGGER.warn(
                                "There must be exact {} records to populate the course corresponding to LLC:{}.",
                                MobileAcademyConstants.MIN_FILES_PER_COURSE,
                                languageLocCode);

                        deleteCourseRawContentsByList(courseContentCsvs, true,
                                bulkUploadStatus);
                        bulkUploadError.setRecordDetails(languageLocCode
                                .toString());
                        bulkUploadError
                                .setErrorCategory(ErrorCategoryConstants.INCONSISTENT_DATA);
                        bulkUploadError
                                .setErrorDescription(String
                                        .format(MobileAcademyConstants.INSUFFICIENT_RECORDS_FOR_ADD,
                                                languageLocCode));
                        bulkUploadErrLogService
                                .writeBulkUploadErrLog(bulkUploadError);

                        distictLLCIterator.remove();
                        continue;
                    }

                    Course course = coursePopulateService.getMtrainingCourse();
                    if (course == null) {
                        course = coursePopulateService
                                .populateMtrainingCourseData(userDetailsDTO);
                        populateCourseStructure = true;
                    } else if (coursePopulateService.findCourseState() == CourseUnitState.Inactive) {
                        populateCourseStructure = true;
                    }

                    answerOptionRecordList.clear();

                    List<ChapterContent> chapterContents = coursePopulateService
                            .getAllChapterContents();

                    if (CollectionUtils.isEmpty(chapterContents)) {
                        chapterContents = createChapterContentPrototype();
                    }

                    Iterator<CourseContentCsv> courseRawContentsIterator = courseContentCsvs
                            .iterator();
                    while (courseRawContentsIterator.hasNext()) {
                        CourseContentCsv courseContentCsv = courseRawContentsIterator
                                .next();
                        Record record = new Record();
                        try {
                            validateRawContent(courseContentCsv, record);
                        } catch (DataValidationException exc) {

                            abortAdditionProcess = true;

                            LOGGER.warn(
                                    "Record validation failed for content ID: {}",
                                    courseContentCsv.getContentId());

                            processError(bulkUploadError, exc,
                                    courseContentCsv.getContentId());

                            bulkUploadErrLogService
                                    .writeBulkUploadErrLog(bulkUploadError);

                            bulkUploadError.setRecordDetails(courseContentCsv
                                    .getContentId());
                            bulkUploadError
                                    .setErrorCategory(ErrorCategoryConstants.INCONSISTENT_DATA);
                            bulkUploadError
                                    .setErrorDescription(String
                                            .format(MobileAcademyConstants.INCONSISTENT_RECORDS_FOR_ADD,
                                                    languageLocCode));
                            bulkUploadErrLogService
                                    .writeBulkUploadErrLog(bulkUploadError);

                            deleteCourseRawContentsByList(courseContentCsvs,
                                    true, bulkUploadStatus);
                            distictLLCIterator.remove();
                            break;
                        }

                        if (populateCourseStructure) {
                            if (record.getType() == FileType.QUESTION_CONTENT) {
                                answerOptionRecordList.add(record);
                            }
                            checkTypeAndAddToChapterContent(record,
                                    chapterContents, courseFlag);
                        } else {
                            if (!checkRecordConsistencyAndMarkFlag(record,
                                    chapterContents, courseFlag)) {
                                LOGGER.warn(
                                        "Record with content ID: {} is not consistent with the data already existing in the system",
                                        courseContentCsv.getContentId());

                                bulkUploadError
                                        .setRecordDetails(courseContentCsv
                                                .getContentId());
                                bulkUploadError
                                        .setErrorCategory(ErrorCategoryConstants.INCONSISTENT_DATA);
                                bulkUploadError
                                        .setErrorDescription(String
                                                .format(MobileAcademyConstants.INCONSISTENT_RECORDS_FOR_ADD,
                                                        languageLocCode));
                                bulkUploadErrLogService
                                        .writeBulkUploadErrLog(bulkUploadError);

                                distictLLCIterator.remove();
                                deleteCourseRawContentsByList(
                                        courseContentCsvs, true,
                                        bulkUploadStatus);
                                abortAdditionProcess = true;
                                break;
                            }
                        }
                    }
                    if (abortAdditionProcess) {
                        continue;
                    }

                    if (courseFlag.hasCompleteCourseArrived()) {
                        courseRawContentsIterator = courseContentCsvs
                                .iterator();
                        while (courseRawContentsIterator.hasNext()) {
                            CourseContentCsv courseContentCsv = courseRawContentsIterator
                                    .next();
                            bulkUploadStatus.incrementSuccessCount();
                            updateRecordInContentProcessedTable(
                                    courseContentCsv, userDetailsDTO);
                            courseContentCsvService.delete(courseContentCsv);
                            courseRawContentsIterator.remove();
                        }
                        // Update Course
                        if (populateCourseStructure) {
                            for (int chapterCounter = 0; chapterCounter < MobileAcademyConstants.NUM_OF_CHAPTERS; chapterCounter++) {
                                ChapterContent chapterContent = chapterContents
                                        .get(chapterCounter);
                                updateChapterContentForUserDetails(
                                        chapterContent, userDetailsDTO);
                                chapterContentDataService
                                        .create(chapterContent);
                            }
                            // Update AnswerOptionList
                            // Change the state to Active
                            processListOfAnswerOptionRecords(
                                    answerOptionRecordList, userDetailsDTO);
                            coursePopulateService.updateCourseState(
                                    CourseUnitState.Active, userDetailsDTO);
                            LOGGER.info(
                                    "Course Added successfully for LLC: {}",
                                    languageLocCode);
                        }
                    } else {
                        bulkUploadError.setRecordDetails(languageLocCode
                                .toString());
                        bulkUploadError
                                .setErrorCategory(ErrorCategoryConstants.INCONSISTENT_DATA);
                        bulkUploadError
                                .setErrorDescription(String
                                        .format(MobileAcademyConstants.INSUFFICIENT_RECORDS_FOR_ADD,
                                                languageLocCode));

                        bulkUploadErrLogService
                                .writeBulkUploadErrLog(bulkUploadError);

                        LOGGER.warn(
                                "Record for complete course haven't arrived to add the course for LLC: {}",
                                languageLocCode);
                        deleteCourseRawContentsByList(courseContentCsvs, true,
                                bulkUploadStatus);
                    }

                }
            }
        }

    }

    /**
     * update Chapter Content For User Details
     * 
     * @param chapterContent
     * @param userDetailsDTO
     */
    private void updateChapterContentForUserDetails(
            ChapterContent chapterContent, UserDetailsDTO userDetailsDTO) {
        for (LessonContent lessonContent : chapterContent.getLessons()) {
            lessonContent.setCreator(userDetailsDTO.getCreator());
            lessonContent.setModifiedBy(userDetailsDTO.getModifiedBy());
            lessonContent.setOwner(userDetailsDTO.getOwner());
        }
        for (ScoreContent scoreContent : chapterContent.getScores()) {
            scoreContent.setCreator(userDetailsDTO.getCreator());
            scoreContent.setModifiedBy(userDetailsDTO.getModifiedBy());
            scoreContent.setOwner(userDetailsDTO.getOwner());
        }

        QuizContent quiz = chapterContent.getQuiz();
        for (QuestionContent questionContent : quiz.getQuestions()) {
            questionContent.setCreator(userDetailsDTO.getCreator());
            questionContent.setModifiedBy(userDetailsDTO.getModifiedBy());
            questionContent.setOwner(userDetailsDTO.getOwner());
        }
        quiz.setCreator(userDetailsDTO.getCreator());
        quiz.setModifiedBy(userDetailsDTO.getModifiedBy());
        quiz.setOwner(userDetailsDTO.getOwner());

        chapterContent.setCreator(userDetailsDTO.getCreator());
        chapterContent.setModifiedBy(userDetailsDTO.getModifiedBy());
        chapterContent.setOwner(userDetailsDTO.getOwner());

    }

    /*
     * this function updates the correct option for different questions in the
     * mTraining module.
     */
    private void processListOfAnswerOptionRecords(
            List<Record> answerOptionRecordList, UserDetailsDTO userDetailsDTO) {
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
                            String.valueOf(answerRecord.getAnswerId()),
                            userDetailsDTO);
        }
    }

    /*
     * This function checks whether a ADD record is having the same file Name
     * for a file which is currently existing in the system. In positive
     * scenarios, it also marks for successful arrival of the file in the course
     * flags
     */
    private boolean checkRecordConsistencyAndMarkFlag(Record record,
            List<ChapterContent> chapterContents, CourseFlag courseFlag) {
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
        } else {
            if ((record.getType() == FileType.LESSON_CONTENT)
                    || (record.getType() == FileType.LESSON_END_MENU)) {
                status = checkRecordConsistencyAndMarkFlagForLesson(record,
                        chapterContent, courseFlag, status);

            } else if ((record.getType() == FileType.QUESTION_CONTENT)
                    || (record.getType() == FileType.CORRECT_ANSWER)
                    || (record.getType() == FileType.WRONG_ANSWER)) {
                status = checkRecordConsistencyAndMarkFlagForQuestion(record,
                        chapterContent, courseFlag, status);
            } else if (record.getType() == FileType.QUIZ_HEADER) {
                QuizContent quizContent = chapterContent.getQuiz();
                if ((MobileAcademyConstants.CONTENT_QUIZ_HEADER
                        .equalsIgnoreCase(quizContent.getName()))) {
                    if (!quizContent.getAudioFile()
                            .equals(record.getFileName())) {
                        LOGGER.debug(
                                MobileAcademyConstants.LOG_MSG_ORIGINAL_FILE_NAME,
                                quizContent.getAudioFile());
                        LOGGER.debug(
                                MobileAcademyConstants.LOG_MSG_NEW_FILE_NAME,
                                record.getFileName());
                        status = false;
                    } else {
                        courseFlag.markQuizHeader(record.getChapterId());
                    }
                }
            } else if (record.getType() == FileType.CHAPTER_END_MENU) {
                if (MobileAcademyConstants.CONTENT_MENU
                        .equalsIgnoreCase(chapterContent.getName())) {
                    if (!chapterContent.getAudioFile().equals(
                            record.getFileName())) {
                        LOGGER.debug(
                                MobileAcademyConstants.LOG_MSG_ORIGINAL_FILE_NAME,
                                chapterContent.getAudioFile());
                        LOGGER.debug(
                                MobileAcademyConstants.LOG_MSG_NEW_FILE_NAME,
                                record.getFileName());
                        status = false;
                    } else {
                        courseFlag.markChapterEndMenu(record.getChapterId());
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
                            LOGGER.debug(
                                    MobileAcademyConstants.LOG_MSG_ORIGINAL_FILE_NAME,
                                    scoreContent.getAudioFile());
                            LOGGER.debug(
                                    MobileAcademyConstants.LOG_MSG_NEW_FILE_NAME,
                                    record.getFileName());
                            status = false;
                        } else {
                            courseFlag.markScoreFile(record.getChapterId(),
                                    record.getScoreID());
                        }
                        break;
                    }
                }
            }
        }
        return status;
    }

    private boolean checkRecordConsistencyAndMarkFlagForLesson(Record record,
            ChapterContent chapterContent, CourseFlag courseFlag, boolean status) {
        if (record.getType() == FileType.LESSON_CONTENT) {
            for (LessonContent lessonContent : chapterContent.getLessons()) {
                if (lessonContent.getLessonNumber() == record.getLessonId()
                        && MobileAcademyConstants.CONTENT_LESSON
                                .equalsIgnoreCase(lessonContent.getName())) {
                    if (!lessonContent.getAudioFile().equals(
                            record.getFileName())) {
                        LOGGER.debug(
                                MobileAcademyConstants.LOG_MSG_ORIGINAL_FILE_NAME,
                                lessonContent.getAudioFile());
                        LOGGER.debug(
                                MobileAcademyConstants.LOG_MSG_NEW_FILE_NAME,
                                record.getFileName());
                        status = false;
                    } else {
                        courseFlag.markLessonContent(record.getChapterId(),
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
                        LOGGER.debug(
                                MobileAcademyConstants.LOG_MSG_ORIGINAL_FILE_NAME,
                                lessonContent.getAudioFile());
                        LOGGER.debug(
                                MobileAcademyConstants.LOG_MSG_NEW_FILE_NAME,
                                record.getFileName());
                        status = false;
                    } else {
                        courseFlag.markLessonEndMenu(record.getChapterId(),
                                record.getLessonId());
                    }
                    break;
                }
            }
        }
        return status;
    }

    private boolean checkRecordConsistencyAndMarkFlagForQuestion(Record record,
            ChapterContent chapterContent, CourseFlag courseFlag, boolean status) {
        if (record.getType() == FileType.QUESTION_CONTENT) {
            for (QuestionContent questionContent : chapterContent.getQuiz()
                    .getQuestions()) {
                if ((questionContent.getQuestionNumber() == record
                        .getQuestionId())
                        && (MobileAcademyConstants.CONTENT_QUESTION
                                .equalsIgnoreCase(questionContent.getName()))) {
                    if ((!questionContent.getAudioFile().equals(
                            record.getFileName()))
                            || !answerOptionMatcher(record)) {
                        LOGGER.debug("Correct Answer Option or fileName doesn't match");
                        LOGGER.debug(
                                MobileAcademyConstants.LOG_MSG_ORIGINAL_FILE_NAME,
                                questionContent.getAudioFile());
                        LOGGER.debug(
                                MobileAcademyConstants.LOG_MSG_NEW_FILE_NAME,
                                record.getFileName());
                        status = false;
                    } else {
                        courseFlag.markQuestionContent(record.getChapterId(),
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
                        LOGGER.debug(
                                MobileAcademyConstants.LOG_MSG_ORIGINAL_FILE_NAME,
                                questionContent.getAudioFile());
                        LOGGER.debug(
                                MobileAcademyConstants.LOG_MSG_NEW_FILE_NAME,
                                record.getFileName());
                        status = false;
                    } else {
                        courseFlag.markQuestionCorrectAnswer(
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
                        LOGGER.debug(
                                MobileAcademyConstants.LOG_MSG_ORIGINAL_FILE_NAME,
                                questionContent.getAudioFile());
                        LOGGER.debug(
                                MobileAcademyConstants.LOG_MSG_NEW_FILE_NAME,
                                record.getFileName());
                        status = false;
                    } else {
                        courseFlag.markQuestionWrongAnswer(
                                record.getChapterId(), record.getQuestionId());
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

        return (answerNo == coursePopulateService.getCorrectAnswerOption(
                chapterNo, questionNo));
    }

    /*
     * This function is used to create the static course data in the content
     * tables.
     */
    private List<ChapterContent> createChapterContentPrototype() {
        List<ChapterContent> listOfChapters = new ArrayList<ChapterContent>();

        for (int chapterCount = 1; chapterCount <= MobileAcademyConstants.NUM_OF_CHAPTERS; chapterCount++) {
            List<LessonContent> lessons = createListOfLessons();
            List<QuestionContent> questions = createListOfQuestions();
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
    private List<QuestionContent> createListOfQuestions() {
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
    private List<LessonContent> createListOfLessons() {
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
            CourseContentCsv courseContentCsv, UserDetailsDTO userDetailsDTO) {
        String metaData = "";
        ContentType contentType = ContentType.CONTENT;

        if (StringUtils.isNotEmpty(courseContentCsv.getMetaData())) {
            metaData = courseContentCsv.getMetaData().toUpperCase();
        }
        if (StringUtils.isNotEmpty(courseContentCsv.getContentType())) {
            contentType = ContentType.findByName(courseContentCsv
                    .getContentType());
        }
        CourseProcessedContent courseProcessedContent = new CourseProcessedContent(
                Integer.parseInt(courseContentCsv.getContentId()),
                courseContentCsv.getCircle().toUpperCase(),
                Integer.parseInt(courseContentCsv.getLanguageLocationCode()),
                courseContentCsv.getContentName().toUpperCase(), contentType,
                courseContentCsv.getContentFile(),
                Integer.parseInt(courseContentCsv.getContentDuration()),
                metaData);
        courseProcessedContent.setCreator(userDetailsDTO.getCreator());
        courseProcessedContent.setModifiedBy(userDetailsDTO.getModifiedBy());
        courseProcessedContent.setOwner(userDetailsDTO.getOwner());
        courseProcessedContentService.create(courseProcessedContent);
    }

    /*
     * This function validates the CourseContentCsv record and returns the
     * record object, populated on the basis of contentName in the raw record.
     * In case of error in the record, it returns null.
     */
    private void validateRawContent(CourseContentCsv courseContentCsv,
            Record record) throws DataValidationException {

        validateContentName(courseContentCsv, record);

        if (record.getType() == FileType.QUESTION_CONTENT) {
            String metaData = ParseDataHelper.validateAndParseString("METADETA",
                    courseContentCsv.getMetaData(), true);

            if (!("CorrectAnswer").equalsIgnoreCase(metaData.substring(0,
                    metaData.indexOf(':')))) {
                throw new DataValidationException(
                        null,
                        ErrorCategoryConstants.INCONSISTENT_DATA,
                        String.format(
                                MobileAcademyConstants.INCONSISTENT_DATA_MESSAGE,
                                courseContentCsv.getContentId()), "METADETA");
            } else {
                int answerNo = ParseDataHelper.validateAndParseInt("",
                        metaData.substring(metaData.indexOf(':') + 1), true);
                if (verifyRange(answerNo, 1, 2)) {
                    record.setAnswerId(answerNo);
                } else {
                    throw new DataValidationException(
                            null,
                            ErrorCategoryConstants.INCONSISTENT_DATA,
                            String.format(
                                    MobileAcademyConstants.INCONSISTENT_DATA_MESSAGE,
                                    courseContentCsv.getContentId()),
                            "METADETA");
                }
            }
        }

        record.setFileName(courseContentCsv.getContentFile());
    }

    /*
     * This function validates the content Name in a CourseContentCsv Record. In
     * case of Sunny Scenario, it sets the indices in the record object and
     * return true. while in case of any error in the content name field, it
     * returns false.
     */
    private void validateContentName(CourseContentCsv courseContentCsv,
            Record record) throws DataValidationException {
        String contentName = courseContentCsv.getContentName().trim();
        boolean recordDataValidation = true;
        if (contentName.indexOf('_') == -1) {
            throw new DataValidationException(null,
                    ErrorCategoryConstants.INCONSISTENT_DATA, String.format(
                            MobileAcademyConstants.INCONSISTENT_DATA_MESSAGE,
                            courseContentCsv.getContentId()),
                    MobileAcademyConstants.CONTENT_NAME);
        }

        String chapterString = contentName.substring(0,
                contentName.indexOf('_'));
        String subString = contentName.substring(1 + contentName.indexOf('_'));

        if (StringUtils.isBlank(subString)
                || !("Chapter").equalsIgnoreCase(chapterString.substring(0,
                        chapterString.length() - 2))) {
            throw new DataValidationException(null,
                    ErrorCategoryConstants.INCONSISTENT_DATA, String.format(
                            MobileAcademyConstants.INCONSISTENT_DATA_MESSAGE,
                            courseContentCsv.getContentId()),
                    MobileAcademyConstants.CONTENT_NAME);
        }

        try {
            record.setChapterId(Integer.parseInt(chapterString
                    .substring(chapterString.length() - 2)));
        } catch (NumberFormatException exception) {
            LOGGER.debug(exception.getMessage(), exception);
            throw new DataValidationException(null,
                    ErrorCategoryConstants.INCONSISTENT_DATA, String.format(
                            MobileAcademyConstants.INCONSISTENT_DATA_MESSAGE,
                            courseContentCsv.getContentId()),
                    MobileAcademyConstants.CONTENT_NAME);
        }

        if (!verifyRange(record.getChapterId(), 1,
                MobileAcademyConstants.NUM_OF_CHAPTERS)) {
            recordDataValidation = false;
        }

        if ((!recordDataValidation) || (!isTypeDeterminable(record, subString))) {
            throw new DataValidationException(null,
                    ErrorCategoryConstants.INCONSISTENT_DATA, String.format(
                            MobileAcademyConstants.INCONSISTENT_DATA_MESSAGE,
                            courseContentCsv.getContentId()),
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
            LOGGER.debug(exception.getMessage());
            return false;
        }
        return determineTypeForLessonQuesScore(record, type, index);
    }

    /**
     * If the type is other than "QuizHeader" and "EndMenu", it will be
     * determined.
     * 
     * @param record
     * @param type
     * @param index
     * @return boolean
     */
    private boolean determineTypeForLessonQuesScore(Record record, String type,
            int index) {
        FileType fileType;
        fileType = FileType.getFor(type);
        record.setType(fileType);
        if ((fileType == FileType.LESSON_CONTENT)
                || (fileType == FileType.LESSON_END_MENU)) {
            if (!verifyRange(index, 1, MobileAcademyConstants.NUM_OF_LESSONS)) {
                return false;
            }
            record.setLessonId(index);
            return true;
        } else if ((fileType == FileType.QUESTION_CONTENT)
                || (fileType == FileType.CORRECT_ANSWER)
                || (fileType == FileType.WRONG_ANSWER)) {
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
            List<ChapterContent> chapterContents, CourseFlag courseFlag) {
        ChapterContent chapterContent = null;
        for (ChapterContent chapter : chapterContents) {
            if (chapter.getChapterNumber() == record.getChapterId()) {
                chapterContent = chapter;
                break;
            }
        }
        if (chapterContent == null) {
            return;
        } else {
            if ((record.getType() == FileType.LESSON_CONTENT)
                    || (record.getType() == FileType.LESSON_END_MENU)) {
                checkTypeAndAddToChapterContentForLesson(record, courseFlag,
                        chapterContent);

            } else if ((record.getType() == FileType.QUESTION_CONTENT)
                    || (record.getType() == FileType.CORRECT_ANSWER)
                    || (record.getType() == FileType.WRONG_ANSWER)) {
                checkTypeAndAddToChapterContentForQuestion(record, courseFlag,
                        chapterContent);

            } else if (record.getType() == FileType.QUIZ_HEADER) {
                QuizContent quiz = chapterContent.getQuiz();
                if ((MobileAcademyConstants.CONTENT_QUIZ_HEADER
                        .equalsIgnoreCase(quiz.getName()))) {
                    quiz.setAudioFile(record.getFileName());
                }
                courseFlag.markQuizHeader(record.getChapterId());

            } else if (record.getType() == FileType.CHAPTER_END_MENU) {
                if (MobileAcademyConstants.CONTENT_MENU
                        .equalsIgnoreCase(chapterContent.getName())) {
                    chapterContent.setAudioFile(record.getFileName());
                }
                courseFlag.markChapterEndMenu(record.getChapterId());
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
                courseFlag.markScoreFile(record.getChapterId(),
                        record.getScoreID());
            }
        }
    }

    private void checkTypeAndAddToChapterContentForQuestion(Record record,
            CourseFlag courseFlag, ChapterContent chapterContent) {
        if (record.getType() == FileType.QUESTION_CONTENT) {
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
            courseFlag.markQuestionContent(record.getChapterId(),
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
            courseFlag.markQuestionCorrectAnswer(record.getChapterId(),
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
            courseFlag.markQuestionWrongAnswer(record.getChapterId(),
                    record.getQuestionId());

        }

    }

    private void checkTypeAndAddToChapterContentForLesson(Record record,
            CourseFlag courseFlag, ChapterContent chapterContent) {
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
            courseFlag.markLessonContent(record.getChapterId(),
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
            courseFlag.markLessonEndMenu(record.getChapterId(),
                    record.getLessonId());

        }
    }

}
