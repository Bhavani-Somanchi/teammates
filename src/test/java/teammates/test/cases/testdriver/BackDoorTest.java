package teammates.test.cases.testdriver;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Arrays;
import java.util.HashMap;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import teammates.common.datatransfer.AccountAttributes;
import teammates.common.datatransfer.CourseAttributes;
import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.EvaluationAttributes;
import teammates.common.datatransfer.InstructorAttributes;
import teammates.common.datatransfer.StudentAttributes;
import teammates.common.datatransfer.SubmissionAttributes;
import teammates.common.exception.EnrollException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Const;
import teammates.common.util.StringHelper;
import teammates.common.util.ThreadHelper;
import teammates.common.util.TimeHelper;
import teammates.common.util.Utils;
import teammates.test.cases.BaseTestCase;
import teammates.test.driver.BackDoor;

import com.google.appengine.api.datastore.Text;
import com.google.gson.Gson;

public class BackDoorTest extends BaseTestCase {

    private static Gson gson = Utils.getTeammatesGson();
    private static DataBundle dataBundle = getTypicalDataBundle();
    private static String jsonString = gson.toJson(dataBundle);

    @BeforeClass
    public static void setUp() throws Exception {
        printTestClassHeader();
        dataBundle = getTypicalDataBundle();
    }

    @SuppressWarnings("unused")
    private void ____SYSTEM_level_methods_________________________________() {
    }
    


    @Test
    public void testPersistenceAndDeletion() {
        
        // check persisting
        dataBundle = getTypicalDataBundle();
        while(!BackDoor.restoreDataBundle(dataBundle).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        
        verifyPresentInDatastore(jsonString);

        // ----------deleting Instructor entities-------------------------
        InstructorAttributes instructor1OfCourse1 = dataBundle.instructors.get("instructor1OfCourse1");
        verifyPresentInDatastore(instructor1OfCourse1);
        while(!BackDoor.deleteInstructor(instructor1OfCourse1.courseId, instructor1OfCourse1.email).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        verifyAbsentInDatastore(instructor1OfCourse1);
        
        //try to delete again: should indicate as success because delete fails silently.
        while(!BackDoor.deleteInstructor(instructor1OfCourse1.email, instructor1OfCourse1.courseId).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        
        // ----------deleting Evaluation entities-------------------------

        // check the existence of a submission that will be deleted along with
        // the evaluation
        SubmissionAttributes subInDeletedEvaluation = dataBundle.submissions
                .get("submissionFromS1C1ToS1C1");
        verifyPresentInDatastore(subInDeletedEvaluation);

        // delete the evaluation and verify it is deleted
        EvaluationAttributes evaluation1InCourse1 = dataBundle.evaluations
                .get("evaluation1InCourse1");
        verifyPresentInDatastore(evaluation1InCourse1);
        while(!BackDoor.deleteEvaluation(evaluation1InCourse1.courseId,
                evaluation1InCourse1.name).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        verifyAbsentInDatastore(evaluation1InCourse1);

        // verify that the submission is deleted too
        verifyAbsentInDatastore(subInDeletedEvaluation);

        // try to delete the evaluation again, should succeed
        while(!BackDoor.deleteEvaluation(evaluation1InCourse1.courseId,
                evaluation1InCourse1.name).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }

        // verify that the other evaluation in the same course is intact
        EvaluationAttributes evaluation2InCourse1 = dataBundle.evaluations
                .get("evaluation2InCourse1");
        verifyPresentInDatastore(evaluation2InCourse1);

        // ----------deleting Course entities-------------------------

        // #COURSE 2
        CourseAttributes course2 = dataBundle.courses.get("typicalCourse2");
        verifyPresentInDatastore(course2);
        while(!BackDoor.deleteCourse(course2.id).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS));{
            delay();
        }
        verifyAbsentInDatastore(course2);

        // check if related student entities are also deleted
        StudentAttributes student2InCourse2 = dataBundle.students
                .get("student2InCourse2");
        verifyAbsentInDatastore(student2InCourse2);

        // check if related evaluation entities are also deleted
        EvaluationAttributes evaluation1InCourse2 = dataBundle.evaluations
                .get("evaluation1InCourse1");
        verifyAbsentInDatastore(evaluation1InCourse2);
        
        // #COURSE 1
        CourseAttributes course1 = dataBundle.courses.get("typicalCourse1");
        verifyPresentInDatastore(course1);
        while(!BackDoor.deleteCourse(course1.id).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        verifyAbsentInDatastore(course1);
        
        // check if related student entities are also deleted
        StudentAttributes student1InCourse1 = dataBundle.students
                .get("student1InCourse1");
        verifyAbsentInDatastore(student1InCourse1);
        
        // previously not deleted evaluation should be deleted now since the course has been deleted
        verifyAbsentInDatastore(evaluation2InCourse1);
        
        // #COURSE NO EVALS
        CourseAttributes courseNoEvals = dataBundle.courses.get("courseNoEvals");
        verifyPresentInDatastore(courseNoEvals);
        while(!BackDoor.deleteCourse(courseNoEvals.id).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        verifyAbsentInDatastore(courseNoEvals);
        
        // ----------deleting Feedback Session entities-------------------------
        // TODO: do proper deletion test
        BackDoor.deleteFeedbackSessions(dataBundle);
    }
    
    @SuppressWarnings("unused")
    private void ____ACCOUNT_level_methods_________________________________() {
    }
    
    @Test
    public void testAccounts() throws Exception{
        dataBundle = getTypicalDataBundle();
        while(!BackDoor.restoreDataBundle(dataBundle).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        
        testCreateAccount();
        testGetAccountAsJson();
        testEditAccount();
        testDeleteAccount();
    }
    
    public void testCreateAccount() {
        AccountAttributes newAccount = dataBundle.accounts.get("instructor1OfCourse1");
        while(!BackDoor.deleteAccount(newAccount.googleId).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        verifyAbsentInDatastore(newAccount);
        while(!BackDoor.createAccount(newAccount).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        verifyPresentInDatastore(newAccount);
    }
    
    public void testGetAccountAsJson() {
        AccountAttributes testAccount = dataBundle.accounts.get("instructor1OfCourse1");
        verifyPresentInDatastore(testAccount);
        String actualString = BackDoor.getAccountAsJson(testAccount.googleId);
        AccountAttributes actualAccount = gson.fromJson(actualString, AccountAttributes.class);
        actualAccount.createdAt = testAccount.createdAt;
        assertEquals(gson.toJson(testAccount), gson.toJson(actualAccount));
    }
    
    public void testEditAccount() {
        AccountAttributes testAccount = dataBundle.accounts.get("instructor1OfCourse1");
        verifyPresentInDatastore(testAccount);
        testAccount.name = "New name";
        while(!BackDoor.editAccount(testAccount).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        ThreadHelper.waitFor(1000);
        verifyPresentInDatastore(testAccount);
    }
    
    public void testDeleteAccount() {
        AccountAttributes testAccount = dataBundle.accounts.get("instructor2OfCourse1");
        while(!BackDoor.createAccount(testAccount).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        verifyPresentInDatastore(testAccount);
        while(!BackDoor.deleteAccount(testAccount.googleId).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        verifyAbsentInDatastore(testAccount);
    }

    @SuppressWarnings("unused")
    private void ____INSTRUCTOR_level_methods_________________________________() {
    }

    @Test
    public void testDeleteInstructors() {
        // already tested by testPersistenceAndDeletion
    }

    @Test
    public void testCreateInstructor() {
        // only minimal testing because this is a wrapper method for
        // another well-tested method.

        String instructorId = "tmapitt.tcc.instructor";
        String courseId = "tmapitt.tcc.course";
        String name = "Tmapitt testInstr Name";
        String email = "tmapitt@tci.com";
        InstructorAttributes instructor = new InstructorAttributes(instructorId, courseId, name, email);
        
        // Make sure not already inside
        while(!BackDoor.deleteInstructor(courseId, email).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        verifyAbsentInDatastore(instructor);
        
        // Perform creation
        while(!BackDoor.createInstructor(instructor).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        verifyPresentInDatastore(instructor);
        
        // Clean up
        while(!BackDoor.deleteInstructor(courseId, email).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        verifyAbsentInDatastore(instructor);
    }

    @Test
    public void testGetInstructorAsJson() {
        // already tested by testPersistenceAndDeletion
    }

    @Test
    public void testDeleteInstructor() {
        // already tested by testPersistenceAndDeletion
    }

    @Test
    public void testEditInstructor() {
        // method not implemented
    }

    @Test
    public void testGetCoursesByInstructorId() throws InvalidParametersException {

        // testing for non-existent instructor
        String[] courses = BackDoor.getCoursesByInstructorId("nonExistentInstructor");
        assertEquals("[]", Arrays.toString(courses));
        
        // Create 2 courses for a new instructor
        String course1 = "AST.TGCBCI.course1";
        String course2 = "AST.TGCBCI.course2";
        while(!BackDoor.deleteCourse(course1).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        while(!BackDoor.deleteCourse(course2).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        while(!BackDoor.createCourse(new CourseAttributes(course1, "tmapit tgcbci c1OfInstructor1")).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        while(!BackDoor.createCourse(new CourseAttributes(course2, "tmapit tgcbci c2OfInstructor1")).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        
        // create a fresh instructor with relations for the 2 courses
        String instructor1Id = "AST.TGCBCI.instructor1";
        String instructor1name = "AST TGCBCI Instructor";
        String instructor1email = "instructor1@ast.tgcbi";
        while(!BackDoor.deleteAccount(instructor1Id).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        while(!BackDoor.createInstructor(new InstructorAttributes(instructor1Id, course1, instructor1name, instructor1email)).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        while(!BackDoor.createInstructor(new InstructorAttributes(instructor1Id, course2, instructor1name, instructor1email)).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }

        //============================================================================
        // Don't be confused by the following: it has no relation with the above instructor/course(s)
        
        // add a course that belongs to a different instructor
        String course3 = "AST.TGCBCI.course3";
        while(!BackDoor.deleteCourse(course3).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        while(!BackDoor.createCourse(new CourseAttributes(course3, "tmapit tgcbci c1OfInstructor2")).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }

        courses = BackDoor.getCoursesByInstructorId(instructor1Id);
        assertEquals("[" + course1 + ", " + course2 + "]", Arrays.toString(courses));

        while(!BackDoor.deleteInstructor(instructor1email, course1).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        while(!BackDoor.deleteInstructor(instructor1email, course2).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
    }

    @SuppressWarnings("unused")
    private void ____COURSE_level_methods_________________________________() {
    }

    @Test
    public void testCreateCourse() throws InvalidParametersException {
        // only minimal testing because this is a wrapper method for
        // another well-tested method.

        String courseId = "tmapitt.tcc.course";
        CourseAttributes course = new CourseAttributes(courseId,
                "Name of tmapitt.tcc.instructor");
        
        // Make sure not already inside
        while(!BackDoor.deleteCourse(courseId).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        verifyAbsentInDatastore(course);
        
        // Perform creation
        while(!BackDoor.createCourse(course).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        verifyPresentInDatastore(course);
        
        // Clean up
        while(!BackDoor.deleteCourse(courseId).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        verifyAbsentInDatastore(course);
    }

    @Test
    public void testGetCourseAsJson() {
        // already tested by testPersistenceAndDeletion
    }

    @Test
    public void testEditCourse() {
        // not implemented
    }

    @Test
    public void testDeleteCourse() {
        // already tested by testPersistenceAndDeletion
    }

    @SuppressWarnings("unused")
    private void ____STUDENT_level_methods_________________________________() {
    }

    @Test
    public void testCreateStudent() throws EnrollException {
        // only minimal testing because this is a wrapper method for
        // another well-tested method.

        StudentAttributes student = new StudentAttributes(
                "team name", "name of tcs student", "tcsStudent@gmail.com", "",
                "tmapit.tcs.course");
        BackDoor.deleteStudent(student.course, student.email);
        verifyAbsentInDatastore(student);
        BackDoor.createStudent(student);
        verifyPresentInDatastore(student);
        BackDoor.deleteStudent(student.course, student.email);
        verifyAbsentInDatastore(student);
    }

    @Test
    public void testGetKeyForStudent() throws EnrollException {

        StudentAttributes student = new StudentAttributes("t1", "name of tgsr student", "tgsr@gmail.com", "", "course1");
        while(!BackDoor.createStudent(student).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        String key = BackDoor.getKeyForStudent(student.course, student.email); 

        // The following is the google app engine description about generating
        // keys.
        //
        // A key can be converted to a string by passing the Key object to
        // str(). The string is "urlsafe"—it uses only characters valid for use in URLs. 
        //
        // RFC3986 definition of a safe url pattern
        // Characters that are allowed in a URI but do not have a reserved
        // purpose are called unreserved. 
        // unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
        String pattern = "(\\w|-|~|.)*";

        String errorMessage = key + "[length=" + key.length() + "][reg="
                + StringHelper.isMatching(key, pattern) + "] is not as expected";
        assertTrue(errorMessage, key.length() > 30 && StringHelper.isMatching(key, pattern));

        // clean up student as this is an orphan entity
        BackDoor.deleteStudent(student.course, student.email);
    }

    @Test
    public void testGetStudentAsJson() {
        // already tested by testPersistenceAndDeletion
    }

    @Test
    public void testEditStudent() {

        // check for successful edit
        while(!BackDoor.restoreDataBundle(getTypicalDataBundle()).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        StudentAttributes student = dataBundle.students.get("student1InCourse1");
        String originalEmail = student.email;
        student.name = "New name";
        student.email = "new@gmail.com";
        student.comments = "new comments";
        student.team = "new team";
        while(!BackDoor.editStudent(originalEmail, student).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        ThreadHelper.waitFor(1000);
        verifyPresentInDatastore(student);

        // test for unsuccessful edit
        student.course = "non-existent";
        String status = BackDoor.editStudent(originalEmail, student);
        assertTrue(status.startsWith(Const.StatusCodes.BACKDOOR_STATUS_FAILURE));
        verifyAbsentInDatastore(student);
    }

    @Test
    public void testDeleteStudent() {
        // already tested by testPersistenceAndDeletion
    }

    @SuppressWarnings("unused")
    private void ____EVALUATION_level_methods______________________________() {
    }

    @Test
    public void testCreateEvaluation() throws InvalidParametersException {
        // only minimal testing because this is a wrapper method for
        // another well-tested method.

        EvaluationAttributes e = new EvaluationAttributes();
        e.courseId = "tmapit.tce.course";
        e.name = "Eval for tmapit.tce.course";
        e.instructions = new Text("inst.");
        e.p2pEnabled = true;
        e.startTime = TimeHelper.getDateOffsetToCurrentTime(1);
        e.endTime = TimeHelper.getDateOffsetToCurrentTime(2);
        e.timeZone = 8.0;
        e.gracePeriod = 5;
        while(!BackDoor.deleteEvaluation(e.courseId, e.name).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        verifyAbsentInDatastore(e);
        while(!BackDoor.createEvaluation(e).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        verifyPresentInDatastore(e);
        while(!BackDoor.deleteEvaluation(e.courseId, e.name).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        verifyAbsentInDatastore(e);
    }

    @Test
    public void testGetEvaluationAsJson() {
        // already tested by testPersistenceAndDeletion
    }

    @Test
    public void testEditEvaluation() {

        while(!BackDoor.restoreDataBundle(getTypicalDataBundle()).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }

        // check for successful edit
        EvaluationAttributes e = dataBundle.evaluations
                .get("evaluation1InCourse1");

        e.gracePeriod = e.gracePeriod + 1;
        e.instructions = new Text(e.instructions + "x");
        e.p2pEnabled = (!e.p2pEnabled);
        e.startTime = TimeHelper.getDateOffsetToCurrentTime(-2);
        e.endTime = TimeHelper.getDateOffsetToCurrentTime(-1);
        e.activated = (!e.activated);
        e.published = (!e.published);
        e.timeZone = e.timeZone + 1.0;

        while(!BackDoor.editEvaluation(e).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        ThreadHelper.waitFor(1000);
        verifyPresentInDatastore(e);

        // not testing for unsuccesful edit because this does 
        //  not go through the Logic API (i.e., no error checking done)

    }

    @Test
    public void testDeleteEvaluation() {
        // already tested by testPersistenceAndDeletion
    }

    @SuppressWarnings("unused")
    private void ____SUBMISSION_level_methods______________________________() {
    }

    @Test
    public void testCreateSubmission() {
        // not implemented
    }

    @Test
    public void testGetSubmission() {
        // already tested by testPersistenceAndDeletion
    }

    @Test
    public void testEditSubmission() {

        while(!BackDoor.restoreDataBundle(getTypicalDataBundle()).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }

        // check for successful edit
        SubmissionAttributes submission = dataBundle.submissions
                .get("submissionFromS1C1ToS1C1");
        submission.justification = new Text(submission.justification.getValue()    + "x");
        submission.points = submission.points + 10;
        while(!BackDoor.editSubmission(submission).equals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS)){
            delay();
        }
        ThreadHelper.waitFor(1000);
        verifyPresentInDatastore(submission);

        // test for unsuccessful edit
        submission.reviewer = "non-existent@gmail.com";
        String status = BackDoor.editSubmission(submission);
        assertTrue(status.startsWith(Const.StatusCodes.BACKDOOR_STATUS_FAILURE));
        verifyAbsentInDatastore(submission);
    }

    @Test
    public void testdeleteSubmission() {
        // not implemented
    }

    @SuppressWarnings("unused")
    private void ____helper_methods_________________________________() {
    }

    

    private void verifyAbsentInDatastore(AccountAttributes account) {
        assertEquals("null", BackDoor.getAccountAsJson(account.googleId));
    }
    
    private void verifyAbsentInDatastore(CourseAttributes course) {
        assertEquals("null", BackDoor.getCourseAsJson(course.id));
    }
    
    private void verifyAbsentInDatastore(InstructorAttributes expectedInstructor) {
        assertEquals("null", BackDoor.getInstructorAsJsonByEmail(expectedInstructor.email, expectedInstructor.courseId));
    }

    private void verifyAbsentInDatastore(StudentAttributes student) {
        assertEquals("null",
                BackDoor.getStudentAsJson(student.course, student.email));
    }

    private void verifyAbsentInDatastore(EvaluationAttributes evaluation1InCourse1) {
        assertEquals("null", BackDoor.getEvaluationAsJson(
                evaluation1InCourse1.courseId, evaluation1InCourse1.name));
    }

    private void verifyAbsentInDatastore(SubmissionAttributes subInDeletedEvaluation) {
        String submissionAsJson = BackDoor.getSubmissionAsJson(
                subInDeletedEvaluation.course,
                subInDeletedEvaluation.evaluation,
                subInDeletedEvaluation.reviewer,
                subInDeletedEvaluation.reviewee);
        assertEquals("null", submissionAsJson);
    }

    
    private void verifyPresentInDatastore(String dataBundleJsonString) {
        Gson gson = Utils.getTeammatesGson();

        DataBundle data = gson.fromJson(dataBundleJsonString, DataBundle.class);
        HashMap<String, AccountAttributes> accounts = data.accounts;
        for (AccountAttributes expectedAccount : accounts.values()) {
            verifyPresentInDatastore(expectedAccount);
        }

        HashMap<String, CourseAttributes> courses = data.courses;
        for (CourseAttributes expectedCourse : courses.values()) {
            verifyPresentInDatastore(expectedCourse);
        }
        
        HashMap<String, InstructorAttributes> instructors = data.instructors;
        for (InstructorAttributes expectedInstructor : instructors.values()) {
            verifyPresentInDatastore(expectedInstructor);
        }

        HashMap<String, StudentAttributes> students = data.students;
        for (StudentAttributes expectedStudent : students.values()) {
            verifyPresentInDatastore(expectedStudent);
        }

        HashMap<String, EvaluationAttributes> evaluations = data.evaluations;
        for (EvaluationAttributes expectedEvaluation : evaluations.values()) {
            verifyPresentInDatastore(expectedEvaluation);
        }

        HashMap<String, SubmissionAttributes> submissions = data.submissions;
        for (SubmissionAttributes expectedSubmission : submissions.values()) {
            verifyPresentInDatastore(expectedSubmission);
        }
    }

    private void verifyPresentInDatastore(SubmissionAttributes expectedSubmission) {
        String submissionsJsonString = BackDoor.getSubmissionAsJson(
                expectedSubmission.course, expectedSubmission.evaluation,
                expectedSubmission.reviewer, expectedSubmission.reviewee);
        SubmissionAttributes actualSubmission = gson.fromJson(submissionsJsonString,
                SubmissionAttributes.class);
        assertEquals(gson.toJson(expectedSubmission),
                gson.toJson(actualSubmission));
    }

    private void verifyPresentInDatastore(EvaluationAttributes expectedEvaluation) {
        String evaluationJsonString = BackDoor.getEvaluationAsJson(
                expectedEvaluation.courseId, expectedEvaluation.name);
        EvaluationAttributes actualEvaluation = gson.fromJson(evaluationJsonString,
                EvaluationAttributes.class);
        // equalize id field before comparing (because id field is
        // autogenerated by GAE)
        assertEquals(gson.toJson(expectedEvaluation),
                gson.toJson(actualEvaluation));
    }

    private void verifyPresentInDatastore(StudentAttributes expectedStudent) {
        String studentJsonString = BackDoor.getStudentAsJson(
                expectedStudent.course, expectedStudent.email);
        StudentAttributes actualStudent = gson.fromJson(studentJsonString,
                StudentAttributes.class);
        equalizeIrrelevantData(expectedStudent, actualStudent);
        assertEquals(gson.toJson(expectedStudent), gson.toJson(actualStudent));
    }

    private void verifyPresentInDatastore(CourseAttributes expectedCourse) {
        String courseJsonString = BackDoor.getCourseAsJson(expectedCourse.id);
        CourseAttributes actualCourse = gson.fromJson(courseJsonString,
                CourseAttributes.class);
        // Ignore time field as it is stamped at the time of creation in testing
        actualCourse.createdAt = expectedCourse.createdAt;
        assertEquals(gson.toJson(expectedCourse), gson.toJson(actualCourse));
    }

    private void verifyPresentInDatastore(InstructorAttributes expectedInstructor) {
        String instructorJsonString = BackDoor.getInstructorAsJsonByEmail(expectedInstructor.email, expectedInstructor.courseId);
        InstructorAttributes actualInstructor = gson.fromJson(instructorJsonString, InstructorAttributes.class);
        
        equalizeIrrelevantData(expectedInstructor, actualInstructor);
        assertEquals(gson.toJson(expectedInstructor), gson.toJson(actualInstructor));
    }
    
    private void verifyPresentInDatastore(AccountAttributes expectedAccount) {
        String accountJsonString = BackDoor.getAccountAsJson(expectedAccount.googleId);
        AccountAttributes actualAccount = gson.fromJson(accountJsonString, AccountAttributes.class);
        // Ignore time field as it is stamped at the time of creation in testing
        actualAccount.createdAt = expectedAccount.createdAt;
        assertEquals(gson.toJson(expectedAccount), gson.toJson(actualAccount));
    }
    
    private void equalizeIrrelevantData(
            StudentAttributes expectedStudent,
            StudentAttributes actualStudent) {
        
        // For these fields, we consider null and "" equivalent.
        if ((expectedStudent.googleId == null) && (actualStudent.googleId.equals(""))) {
            actualStudent.googleId = null;
        }
        if ((expectedStudent.team == null) && (actualStudent.team.equals(""))) {
            actualStudent.team = null;
        }
        if ((expectedStudent.comments == null)
                && (actualStudent.comments.equals(""))) {
            actualStudent.comments = null;
        }

        // pretend keys match because the key is generated on the server side
        // and cannot be anticipated
        if ((actualStudent.key != null)) {
            expectedStudent.key = actualStudent.key;
        }
    }
    
    private void equalizeIrrelevantData(
            InstructorAttributes expectedInstructor,
            InstructorAttributes actualInstructor) {
        
        // pretend keys match because the key is generated only before storing into database
        if ((actualInstructor.key != null)) {
            expectedInstructor.key = actualInstructor.key;
        }
    }

    private void delay(){
        ThreadHelper.waitFor((int) Math.random() * 1000);
    }

    @AfterClass
    public static void tearDown() {
        printTestClassFooter();
    }
}
