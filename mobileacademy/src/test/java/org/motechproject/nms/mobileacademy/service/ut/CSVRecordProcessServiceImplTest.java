package org.motechproject.nms.mobileacademy.service.ut;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.motechproject.nms.mobileacademy.commons.CourseFlags;
import org.motechproject.nms.mobileacademy.commons.FileType;
import org.motechproject.nms.mobileacademy.commons.Record;
import org.motechproject.nms.mobileacademy.domain.ChapterContent;
import org.motechproject.nms.mobileacademy.domain.CourseContentCsv;
import org.motechproject.nms.mobileacademy.domain.LessonContent;
import org.motechproject.nms.mobileacademy.domain.QuestionContent;
import org.motechproject.nms.mobileacademy.domain.QuizContent;
import org.motechproject.nms.mobileacademy.domain.ScoreContent;
import org.motechproject.nms.mobileacademy.repository.ChapterContentDataService;
import org.motechproject.nms.mobileacademy.repository.CourseContentCsvDataService;
import org.motechproject.nms.mobileacademy.service.CourseContentCsvService;
import org.motechproject.nms.mobileacademy.service.CoursePopulateService;
import org.motechproject.nms.mobileacademy.service.CourseProcessedContentService;
import org.motechproject.nms.mobileacademy.service.impl.CSVRecordProcessServiceImpl;
import org.motechproject.nms.util.helper.DataValidationException;
import org.motechproject.nms.util.service.BulkUploadErrLogService;

public class CSVRecordProcessServiceImplTest {

    @InjectMocks
    CSVRecordProcessServiceImpl csvRecordProcessServiceImpl = new CSVRecordProcessServiceImpl();

    @Mock
    private CourseContentCsvService courseContentCsvService;

    @Mock
    private CourseProcessedContentService courseProcessedContentService;

    @Mock
    private ChapterContentDataService chapterContentDataService;

    @Mock
    public CourseContentCsvDataService courseContentCsvDataService;

    @Mock
    private CoursePopulateService coursePopulateService;

    @Mock
    private BulkUploadErrLogService bulkUploadErrLogService;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    /*
     * This test case is used to test the processing of raw records with valid
     * data.
     */
    @SuppressWarnings("serial")
    @Test
    public void testProcessRawRecords() {
        List<CourseContentCsv> courseContentCsvRecords = new ArrayList<CourseContentCsv>() {
        };
        courseContentCsvRecords.add(new CourseContentCsv("100014", "AP", "14",
                "Chapter01_Lesson01", "Content", "ch1_l1.wav", "150", ""));
        courseContentCsvRecords.add(new CourseContentCsv("100015", "AP", "14",
                "Chapter01_Lesson01", "Content", "ch1_l1.wav", "150", ""));
        courseContentCsvRecords.add(new CourseContentCsv("100016", "AP", "14",
                "Chapter01_Lesson01", "Content", "ch1_l1.wav", "150", ""));
        courseContentCsvRecords.add(new CourseContentCsv("100017", "AP", "14",
                "Chapter01_Lesson02", "Content", "ch1_l2.wav", "150", ""));
        assertEquals(csvRecordProcessServiceImpl.processRawRecords(
                courseContentCsvRecords, "Book1.csv"),
                "Records Processed Successfully");
    }

    /*
     * this test case is used to validate schema when provided with invalid
     * ContentId.
     */
    @Test
    public void testValidateSchemaForInvalidContentId() {
        Method method = null;
        Boolean flag = true;
        CourseContentCsv courseContentCsv = new CourseContentCsv("CI14", "AP",
                "14", "Chapter01_Lesson01", "Content", "ch1_l1.wav", "150", "");
        try {
            method = CSVRecordProcessServiceImpl.class.getDeclaredMethod(
                    "validateSchema", new Class[] { CourseContentCsv.class });
            method.setAccessible(true);
            method.invoke(csvRecordProcessServiceImpl,
                    new Object[] { courseContentCsv });
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof DataValidationException) {
                System.out.println(e.getCause());
                flag = false;
            }
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException e) {
        }
        assertFalse(flag);
    }

    /*
     * this test case is used to validate schema when provided with invalid LLC.
     */
    @Test
    public void testValidateSchemaForInvalidLLC() {
        Method method = null;
        Boolean flag = true;
        CourseContentCsv courseContentCsv = new CourseContentCsv("100014",
                "AP", "XX", "Chapter01_Lesson01", "Content", "ch1_l1.wav",
                "150", "");
        try {
            method = CSVRecordProcessServiceImpl.class.getDeclaredMethod(
                    "validateSchema", new Class[] { CourseContentCsv.class });
            method.setAccessible(true);
            method.invoke(csvRecordProcessServiceImpl,
                    new Object[] { courseContentCsv });
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof DataValidationException) {
                System.out.println(e.getCause());
                flag = false;
            }
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException e) {
        }
        assertFalse(flag);
    }

    /*
     * this test case is used to validate schema when provided with Empty
     * ContentName.
     */
    @Test
    public void testValidateSchemaForEmptyContentName() {
        Method method = null;
        Boolean flag = true;
        CourseContentCsv courseContentCsv = new CourseContentCsv("100014",
                "AP", "14", "", "Content", "ch1_l1.wav", "150", "");
        try {
            method = CSVRecordProcessServiceImpl.class.getDeclaredMethod(
                    "validateSchema", new Class[] { CourseContentCsv.class });
            method.setAccessible(true);
            method.invoke(csvRecordProcessServiceImpl,
                    new Object[] { courseContentCsv });
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof DataValidationException) {
                System.out.println(e.getCause());
                flag = false;
            }
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException e) {
        }
        assertFalse(flag);
    }

    /*
     * this test case is used to validate schema when provided with ContentName
     * as NULL.
     */
    @Test
    public void testValidateSchemaForNullContentName() {
        Method method = null;
        Boolean flag = true;
        CourseContentCsv courseContentCsv = new CourseContentCsv("14", "AP",
                "14", "null", "Content", "ch1_l1.wav", "150", "");
        try {
            method = CSVRecordProcessServiceImpl.class.getDeclaredMethod(
                    "validateSchema", new Class[] { CourseContentCsv.class });
            method.setAccessible(true);
            method.invoke(csvRecordProcessServiceImpl,
                    new Object[] { courseContentCsv });
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof DataValidationException) {
                System.out.println(e.getCause());
                flag = false;
            }
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException e) {
        }
        assertFalse(flag);
    }

    /*
     * This test case is used to determine the type of the record and update its
     * Chapter Content accordingly.
     */
    @Test
    public void testDetermineTypeAndUpdateChapterContent() {
        Record record = new Record();
        record.setType(FileType.LESSON_CONTENT);
        record.setChapterId(1);
        record.setLessonId(1);
        record.setFileName("Ch1_l1.wav");
        Method method;
        try {
            method = CSVRecordProcessServiceImpl.class.getDeclaredMethod(
                    "determineTypeAndUpdateChapterContent",
                    new Class[] { Record.class });
            method.setAccessible(true);
            method.invoke(csvRecordProcessServiceImpl, new Object[] { record });
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
        }
    }

    /*
     * This test case is used to test the processing of raw records with Add as
     * Operation.
     */
    @Test
    public void testProcessAddRecords() {
        Map<Integer, List<CourseContentCsv>> mapForAddRecords = new HashMap<Integer, List<CourseContentCsv>>();
        List<CourseContentCsv> listCourseRecords = new ArrayList<>();
        listCourseRecords.add(new CourseContentCsv("100014", "AP", "14",
                "Chapter01_Lesson01", "Content", "ch1_l1.wav", "150", ""));
        listCourseRecords.add(new CourseContentCsv("100015", "AP", "14",
                "Chapter01_Lesson03", "Content", "ch1_l3.wav", "250", ""));
        listCourseRecords.add(new CourseContentCsv("100016", "AP", "14",
                "Chapter01_Lesson04", "Content", "ch1_l4.wav", "150", ""));
        listCourseRecords.add(new CourseContentCsv("100017", "AP", "14",
                "Chapter01_Lesson02", "Content", "ch1_l2.wav", "450", ""));
        mapForAddRecords.put(14, listCourseRecords);
        Method method;
        try {
            method = CSVRecordProcessServiceImpl.class.getDeclaredMethod(
                    "processAddRecords", new Class[] { Map.class });
            method.setAccessible(true);
            method.invoke(csvRecordProcessServiceImpl,
                    new Object[] { mapForAddRecords });
            assertTrue(mapForAddRecords.isEmpty());
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
        }
    }

    /*
     * This test case is used to test the processing of raw records with Delete
     * as Operation.
     */
    @Test
    public void testProcessDeleteRecords() {
        Map<Integer, List<CourseContentCsv>> mapForDeleteRecords = new HashMap<Integer, List<CourseContentCsv>>();
        List<CourseContentCsv> listCourseRecords = new ArrayList<>();
        listCourseRecords.add(new CourseContentCsv("100014", "AP", "14",
                "Chapter01_Lesson01", "Content", "ch1_l1.wav", "150", ""));
        listCourseRecords.add(new CourseContentCsv("100015", "AP", "14",
                "Chapter01_Lesson03", "Content", "ch1_l3.wav", "250", ""));
        listCourseRecords.add(new CourseContentCsv("100016", "AP", "14",
                "Chapter01_Lesson04", "Content", "ch1_l4.wav", "150", ""));
        listCourseRecords.add(new CourseContentCsv("100017", "AP", "14",
                "Chapter01_Lesson02", "Content", "ch1_l2.wav", "450", ""));
        mapForDeleteRecords.put(14, listCourseRecords);
        Method method;
        try {
            method = CSVRecordProcessServiceImpl.class.getDeclaredMethod(
                    "processDeleteRecords", new Class[] { Map.class });
            method.setAccessible(true);
            method.invoke(csvRecordProcessServiceImpl,
                    new Object[] { mapForDeleteRecords });
            assertTrue(mapForDeleteRecords.isEmpty());
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
        }
    }

    /*
     * This test case is used to Check the type of the record and mark the
     * appropriate course flag.
     */
    @Test
    public void testCheckRecordTypeAndMarkCourseFlag() {
        Method method;
        Record record = new Record();
        record.setType(FileType.LESSON_CONTENT);
        record.setChapterId(1);
        record.setLessonId(1);
        record.setFileName("Lesson Content File");
        CourseFlags courseFlags = new CourseFlags();
        try {
            method = CSVRecordProcessServiceImpl.class.getDeclaredMethod(
                    "checkRecordTypeAndMarkCourseFlag", new Class[] {
                            Record.class, CourseFlags.class });
            method.setAccessible(true);
            method.invoke(csvRecordProcessServiceImpl, new Object[] { record,
                    courseFlags });
            assertTrue(courseFlags.flagForLessonFilesOfChapter[0][0][0]);
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
        }

    }

    /*
     * This test case is used to Check the record for consistency and mark the
     * appropriate course flag.
     */
    @SuppressWarnings("serial")
    @Test
    public void testCheckRecordConsistencyAndMarkFlag() {
        Method method;
        Record record = new Record();
        record.setType(FileType.LESSON_CONTENT);
        record.setChapterId(1);
        record.setLessonId(1);
        record.setFileName("Ch1_l1.wav");
        List<QuestionContent> quizContent = new ArrayList<QuestionContent>() {
        };
        quizContent.add(new QuestionContent(1, "question", "ch1_q1.wav"));
        quizContent
                .add(new QuestionContent(1, "correctAnswer", "ch1_q1_ca.wav"));
        quizContent.add(new QuestionContent(2, "question", "ch1_q2.wav"));
        QuizContent quiz = new QuizContent("quiz header", "ch1_qp.wav",
                quizContent);
        List<ScoreContent> scoreContent = new ArrayList<ScoreContent>() {
        };
        scoreContent.add(new ScoreContent("score01", "Ch1_1ca.wav"));
        scoreContent.add(new ScoreContent("score00", "Ch1_0ca.wav"));
        scoreContent.add(new ScoreContent("score02", "Ch1_2ca.wav"));
        List<LessonContent> lessons = new ArrayList<LessonContent>() {
        };
        lessons.add(new LessonContent(1, "lesson", "Ch1_l1.wav"));
        lessons.add(new LessonContent(2, "lesson", "Ch1_l3.wav"));
        lessons.add(new LessonContent(4, "lesson", "Ch1_l4.wav"));
        lessons.add(new LessonContent(3, "lesson", "Ch1_l1.wav"));
        ChapterContent chapterContent = new ChapterContent(1, "content",
                "ch1_l1.wav", lessons, scoreContent, quiz);
        CourseFlags courseFlags = new CourseFlags();
        try {
            method = CSVRecordProcessServiceImpl.class.getDeclaredMethod(
                    "checkRecordConsistencyAndMarkFlag", new Class[] {
                            Record.class, ChapterContent.class,
                            CourseFlags.class });
            method.setAccessible(true);
            method.invoke(csvRecordProcessServiceImpl, new Object[] { record,
                    chapterContent, courseFlags });
            assertTrue(courseFlags.flagForLessonFilesOfChapter[0][0][0]);
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
        }
    }

    /*
     * this test case is used to validate raw content when provided with valid
     * data.
     */
    @Test
    public void testValidateRawContent() {
        CourseContentCsv courseContentCsv = new CourseContentCsv("100014",
                "AP", "14", "Chapter01_Lesson01", "Content", "ch1_l1.wav",
                "150", "");
        Record record = new Record();
        record.setType(FileType.LESSON_CONTENT);
        record.setChapterId(1);
        record.setLessonId(1);
        record.setFileName("Ch1_l1.wav");
        Method method = null;
        Boolean flag = true;
        try {
            method = CSVRecordProcessServiceImpl.class.getDeclaredMethod(
                    "validateRawContent", new Class[] { CourseContentCsv.class,
                            Record.class });
            method.setAccessible(true);
            method.invoke(csvRecordProcessServiceImpl, new Object[] {
                    courseContentCsv, record });
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            flag = false;
        }
        assertTrue(flag);
    }

    /*
     * this test case is used to validate raw content when provided with empty
     * MetaData Field.
     */
    @Test
    public void testValidateRawContentForEmptyMetaData() {
        CourseContentCsv courseContentCsv = new CourseContentCsv("100023",
                "AP", "14", "Chapter01_Question01", "Content", "ch1_q1.wav",
                "20", "");
        Record record = new Record();
        record.setType(FileType.QUESTION_CONTENT);
        record.setChapterId(1);
        record.setLessonId(1);
        record.setFileName("Ch1_l1.wav");
        Method method = null;
        Boolean flag = true;
        try {
            method = CSVRecordProcessServiceImpl.class.getDeclaredMethod(
                    "validateRawContent", new Class[] { CourseContentCsv.class,
                            Record.class });
            method.setAccessible(true);
            method.invoke(csvRecordProcessServiceImpl, new Object[] {
                    courseContentCsv, record });
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            flag = false;
        }
        assertFalse(flag);
    }

    /*
     * this test case is used to validate raw content when provided with no
     * Correct Answer Id.
     */
    @Test
    public void testValidateRawContentForEmptyCorrectAnswer() {
        CourseContentCsv courseContentCsv = new CourseContentCsv("100023",
                "AP", "14", "Chapter01_Question01", "Content", "ch1_q1.wav",
                "20", "CorrectAnswer:");
        Record record = new Record();
        record.setType(FileType.QUESTION_CONTENT);
        record.setChapterId(1);
        record.setLessonId(1);
        record.setFileName("Ch1_l1.wav");
        Method method = null;
        Boolean flag = true;
        try {
            method = CSVRecordProcessServiceImpl.class.getDeclaredMethod(
                    "validateRawContent", new Class[] { CourseContentCsv.class,
                            Record.class });
            method.setAccessible(true);
            method.invoke(csvRecordProcessServiceImpl, new Object[] {
                    courseContentCsv, record });
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            flag = false;
        }
        assertFalse(flag);
    }

    /*
     * this test case is used to validate raw content when provided with Invalid
     * Correct Answer Id.
     */
    @Test
    public void testValidateRawContentForInvalidCorrectAnswer() {
        CourseContentCsv courseContentCsv = new CourseContentCsv("100023",
                "AP", "14", "Chapter01_Question01", "Content", "ch1_q1.wav",
                "20", "CorrectAnswer:X");
        Record record = new Record();
        record.setType(FileType.QUESTION_CONTENT);
        record.setChapterId(1);
        record.setLessonId(1);
        record.setFileName("Ch1_l1.wav");
        Method method = null;
        Boolean flag = true;
        try {
            method = CSVRecordProcessServiceImpl.class.getDeclaredMethod(
                    "validateRawContent", new Class[] { CourseContentCsv.class,
                            Record.class });
            method.setAccessible(true);
            method.invoke(csvRecordProcessServiceImpl, new Object[] {
                    courseContentCsv, record });
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            flag = false;
        }
        assertFalse(flag);
    }

    /*
     * this test case is used to validate raw content when provided with
     * Improper Content Name.
     */
    @Test
    public void testPartialValidateContentName() {
        CourseContentCsv courseContentCsv = new CourseContentCsv("100014",
                "AP", "14", "Chapter01_", "Content", "ch1_l1.wav", "150", "");
        Record record = new Record();
        record.setType(FileType.LESSON_CONTENT);
        record.setChapterId(1);
        record.setLessonId(1);
        record.setFileName("Ch1_l1.wav");
        Method method = null;
        Boolean flag = true;
        try {
            method = CSVRecordProcessServiceImpl.class.getDeclaredMethod(
                    "validateContentName", new Class[] {
                            CourseContentCsv.class, Record.class });
            method.setAccessible(true);
            method.invoke(csvRecordProcessServiceImpl, new Object[] {
                    courseContentCsv, record });
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof DataValidationException) {
                System.out.println(e.getCause());
                flag = false;
            }
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException e) {
            flag = false;
        }
        assertFalse(flag);
    }

    /*
     * this test case is used to validate raw content when provided with
     * ContentName with missing delimeter.
     */
    @Test
    public void testValidateContentNameWithMissingDelimeter() {
        CourseContentCsv courseContentCsv = new CourseContentCsv("100014",
                "AP", "14", "Chapter01Lesson01", "Content", "ch1_l1.wav",
                "150", "");
        Record record = new Record();
        record.setType(FileType.LESSON_CONTENT);
        record.setChapterId(1);
        record.setLessonId(1);
        record.setFileName("Ch1_l1.wav");
        Method method = null;
        Boolean flag = true;
        try {
            method = CSVRecordProcessServiceImpl.class.getDeclaredMethod(
                    "validateContentName", new Class[] {
                            CourseContentCsv.class, Record.class });
            method.setAccessible(true);
            method.invoke(csvRecordProcessServiceImpl, new Object[] {
                    courseContentCsv, record });
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof DataValidationException) {
                System.out.println(e.getCause());
                flag = false;
            }
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException e) {
            flag = false;
        }
        assertFalse(flag);
    }

    /*
     * this test case is used to test whether the type is determinable Or Not
     */
    @Test
    public void testIsTypeDeterminable() {
        Method method;
        Record record = new Record();
        record.setChapterId(1);
        record.setLessonId(1);
        record.setQuestionId(1);
        try {
            method = CSVRecordProcessServiceImpl.class.getDeclaredMethod(
                    "isTypeDeterminable", new Class[] { Record.class,
                            String.class });
            method.setAccessible(true);
            method.invoke(csvRecordProcessServiceImpl, new Object[] { record,
                    "Lesson01" });
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
        }
        assertSame(FileType.LESSON_CONTENT, record.getType());
    }

    /*
     * this test case is used to check the type of record and add to the
     * ChapterContent.
     */
    @SuppressWarnings("serial")
    @Test
    public void testCheckTypeAndAddToChapterContent() {
        Method method;
        Record record = new Record();
        record.setType(FileType.LESSON_CONTENT);
        record.setChapterId(1);
        record.setLessonId(1);
        record.setFileName("Lesson Content File");
        CourseFlags courseFlags = new CourseFlags();
        List<QuestionContent> questionContent = new ArrayList<QuestionContent>() {
        };
        questionContent.add(new QuestionContent(1, "question", "ch1_q1.wav"));
        questionContent.add(new QuestionContent(1, "correctAnswer",
                "ch1_q1_ca.wav"));
        questionContent.add(new QuestionContent(2, "question", "ch1_q2.wav"));
        QuizContent quiz = new QuizContent("quiz header", "ch1_qp.wav",
                questionContent);
        List<ScoreContent> scoreContent = new ArrayList<ScoreContent>() {
        };
        scoreContent.add(new ScoreContent("score01", "Ch1_1ca.wav"));
        scoreContent.add(new ScoreContent("score00", "Ch1_0ca.wav"));
        scoreContent.add(new ScoreContent("score02", "Ch1_2ca.wav"));
        List<LessonContent> lessons = new ArrayList<LessonContent>() {
        };
        lessons.add(new LessonContent(1, "lesson", "Ch1_l1.wav"));
        lessons.add(new LessonContent(2, "lesson", "Ch1_l3.wav"));
        lessons.add(new LessonContent(4, "lesson", "Ch1_l4.wav"));
        lessons.add(new LessonContent(1, "lesson", "Ch1_l1.wav"));
        ChapterContent chapterContent = new ChapterContent(1, "content",
                "ch1_l1.wav", lessons, scoreContent, quiz);
        try {
            method = CSVRecordProcessServiceImpl.class.getDeclaredMethod(
                    "checkTypeAndAddToChapterContent", new Class[] {
                            Record.class, ChapterContent.class,
                            CourseFlags.class });
            method.setAccessible(true);
            method.invoke(csvRecordProcessServiceImpl, new Object[] { record,
                    chapterContent, courseFlags });
            assertTrue(courseFlags.flagForLessonFilesOfChapter[0][0][0]);
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
        }
    }

}
