package org.gauntlet.quizzes.generator.defaults.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gauntlet.core.api.ApplicationException;
import org.gauntlet.problems.api.dao.IProblemDAOService;
import org.gauntlet.problems.api.model.Problem;
import org.gauntlet.problems.api.model.ProblemCategory;
import org.gauntlet.problems.api.model.ProblemDifficulty;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.quizzical.backend.security.authorization.api.model.user.User;
import org.quizzical.backend.testdesign.api.dao.ITestDesignTemplateDAOService;
import org.quizzical.backend.testdesign.api.model.TestDesignTemplate;
import org.quizzical.backend.testdesign.api.model.TestDesignTemplateSection;
import org.gauntlet.quizzes.api.dao.IQuizDAOService;
import org.gauntlet.quizzes.api.model.Constants;
import org.gauntlet.quizzes.api.model.Quiz;
import org.gauntlet.quizzes.api.model.QuizProblem;
import org.gauntlet.quizzes.api.model.QuizProblemType;
import org.gauntlet.quizzes.api.model.QuizType;
import org.gauntlet.quizzes.generator.api.IQuizGeneratorService;
import org.gauntlet.quizzes.generator.api.model.QuizGenerationParameters;


public class DiagnosticTestGeneratorImpl implements IQuizGeneratorService { 
	private static final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
	private static final DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

	private volatile LogService logger;
	
	private volatile BundleContext ctx;

	private volatile IQuizDAOService quizDAOService;
	
	private volatile IProblemDAOService problemDAOService;
	
	private volatile ITestDesignTemplateDAOService testTemplateService;
	
	@Override
	public Quiz generate(User user,  Long problemTypeId, QuizGenerationParameters params) throws ApplicationException {
		TestDesignTemplateSection nonCalSec = null;
		TestDesignTemplateSection calSec = null;

		final TestDesignTemplate pt1 = testTemplateService.getByCode("PracticeTest1");
		final List<TestDesignTemplateSection> sections = pt1.getOrderedSections();
		nonCalSec = sections.get(0);
		calSec = sections.get(1);

		//=== Init params
		final QuizType quizType = quizDAOService.provideQuizType(new QuizType(
				Constants.QUIZ_TYPE_DIAGNOSTIC_CODE,
				Constants.QUIZ_TYPE_DIAGNOSTIC_CODE));

		final Date quizDateTime = Calendar.getInstance().getTime();
		final String quizName = String.format(
				"Generated by TestDesign \"%s\" at %s on %s",
				pt1.getCode(),
				timeFormat.format(quizDateTime),
				dateFormat.format(quizDateTime));
		
		final String quizCode = String.format(
				"generated-%s-%d-%d",
				params.getGeneratorType(),
				58,
				System.currentTimeMillis());
		
		final Counter counterWithInfoCards = new Counter(0);
		final Counter counter = new Counter(0);
		
		//=== NonCalc
		Map<Long,Problem> includedProblemIds = new HashMap<>();

		final Problem startNonCalcProblem = problemDAOService.getByCode(org.gauntlet.problems.api.model.Constants.SYSTEM_PROBLEM_START_NON_CALC_SEC);
		includedProblemIds.put(startNonCalcProblem.getId(),startNonCalcProblem);
		QuizProblem startNonCalcQuizProblem = new QuizProblem(
				quizCode,
				-1,
				nonCalSec.getOrdinal(),
				counterWithInfoCards.incr(),
				startNonCalcProblem.getId(),
				startNonCalcProblem);
		startNonCalcQuizProblem.setType(QuizProblemType.INFORMATIONAL);
		final List<QuizProblem> nonCalcQuizProblems = nonCalSec.getOrderedItems()
				.stream()
        		.map(item -> {
        			QuizProblem qp = null;
        			try {
						ProblemCategory cat = problemDAOService.getProblemCategoryByCode(item.getContentSubType().getCode());
						ProblemDifficulty diff = problemDAOService.getProblemDifficultyByCode(GeneratorUtil.getDifficultyCode(item.getDifficultyType()));
						
						long count = problemDAOService.countByCalcAndDifficultyAndCategoryNotInIn(problemTypeId,false,diff.getId(), cat.getId(), new ArrayList<Long>(includedProblemIds.keySet()));
						if (count < 1)
							throw new RuntimeException(String.format("Test Item %s cannot match a problem with reqCalc=%b cat=%s, diff=%s not in [%s]",item.getCode(),false,cat.getCode(),diff.getCode(),includedProblemIds.keySet()));
						int randomOffset = (int)GeneratorUtil.generateRandowOffset(count);
						
						final List<Problem> problems = problemDAOService.findByDifficultyAndCategoryNotInIn(problemTypeId,false,diff.getId(), cat.getId(), new ArrayList<Long>(includedProblemIds.keySet()),randomOffset,1);
						if (problems.isEmpty())
							throw new RuntimeException(String.format("Test Item %s cannot match a problem with reqCalc=%b cat=%s, diff=%s not in [%s]",item.getCode(),true,cat.getCode(),diff.getCode(),includedProblemIds.keySet()));
						final Problem problem = problems.iterator().next();
						
						includedProblemIds.put(problem.getId(),problem);
						qp = new QuizProblem(
								quizCode,
								counter.incr(),
								item.getSection().getOrdinal(),
								counterWithInfoCards.incr(),
								problem.getId(),
								problem);
					} catch (Exception e) {
						StringWriter sw = new StringWriter();
						e.printStackTrace(new PrintWriter(sw));
						String stacktrace = sw.toString();
						System.out.println(e.getMessage());
						//logger.log(LogService.LOG_ERROR,stacktrace);
						//throw new RuntimeException(String.format("Error processing TestDesign item %s",item.getCode()));
					}
        			
        			return qp;
    	    	})
        		.collect(Collectors.toList());
	
		
		
		//=== Calc
		final Problem startCalcProblem = problemDAOService.getByCode(org.gauntlet.problems.api.model.Constants.SYSTEM_PROBLEM_START_CALC_SEC);
		includedProblemIds.put(startCalcProblem.getId(),startCalcProblem);
		QuizProblem startCalcQuizProblem = new QuizProblem(
				quizCode,
				-1,
				calSec.getOrdinal(),
				counterWithInfoCards.incr(),
				startCalcProblem.getId(),
				startCalcProblem);
		startCalcQuizProblem.setType(QuizProblemType.INFORMATIONAL);
		final List<QuizProblem> calcQuizProblems = calSec.getOrderedItems()
				.stream()
        		.map(item -> {
        			QuizProblem qp = null;
        			long count = 0;
						try {
							final ProblemCategory cat = problemDAOService.getProblemCategoryByCode(item.getContentSubType().getCode());
							final ProblemDifficulty diff = problemDAOService.getProblemDifficultyByCode(GeneratorUtil.getDifficultyCode(item.getDifficultyType()));
							
							count = problemDAOService.countByCalcAndDifficultyAndCategoryNotInIn(problemTypeId,true,diff.getId(), cat.getId(), new ArrayList<Long>(includedProblemIds.keySet()));
							if (count < 1)
								throw new RuntimeException(String.format("Test Item %s cannot match a problem with reqCalc=%b cat=%s, diff=%s not in [%s]",item.getCode(),true,cat.getCode(),diff.getCode(),includedProblemIds.keySet()));
							int randomOffset = (int)GeneratorUtil.generateRandowOffset(count);
							
							final List<Problem> problems = problemDAOService.findByDifficultyAndCategoryNotInIn(problemTypeId,true,diff.getId(), cat.getId(), includedProblemIds.keySet(),randomOffset,1);
							if (problems.isEmpty())
								throw new RuntimeException(String.format("Empty: Test Item %s cannot match a problem with reqCalc=%b cat=%s, diff=%s not in [%s]",item.getCode(),true,cat.getCode(),diff.getCode(),includedProblemIds.keySet()));
							final Problem problem = problems.iterator().next();
							
							includedProblemIds.put(problem.getId(),problem);
							
							qp = new QuizProblem(
									quizCode,
									counter.incr(),
									item.getSection().getOrdinal(),
									counterWithInfoCards.incr(),
									problem.getId(),
									problem); 
						} catch (Exception e) {
							StringWriter sw = new StringWriter();
							e.printStackTrace(new PrintWriter(sw));
							String stacktrace = sw.toString();
							System.out.println(e.getMessage());
							//logger.log(LogService.LOG_ERROR,stacktrace);
							//throw new RuntimeException(String.format("Error processing TestDesign item %s",item.getCode()));
						}
				
        			
        			return qp;
    	    	})
        		.collect(Collectors.toList());	
		final List<QuizProblem> unorderedQuizProblems = Stream.concat(nonCalcQuizProblems.stream(), calcQuizProblems.stream()).collect(Collectors.toList());
		unorderedQuizProblems.add(startNonCalcQuizProblem);
		unorderedQuizProblems.add(startCalcQuizProblem);

		
		final Quiz quiz = new Quiz();
		quiz.setUserId(user.getId());
		quiz.setCode(quizCode);
		quiz.setName(quizName);
		quiz.setQuizType(quizType);
		quiz.setQuestions(unorderedQuizProblems);
		
		final Quiz persistedQuiz = quizDAOService.provide(user, quiz);
		persistedQuiz.getQuestions()
			.stream()
			.forEach(question -> {
				question.setProblem(includedProblemIds.get(question.getProblemId()));
				question.setQuiz(quiz);
			});
		
		Collections.sort(persistedQuiz.getQuestions(), new Comparator<QuizProblem>() {
			@Override
			public int compare(QuizProblem o1, QuizProblem o2) {
				if  (o1.getOrdinal() < o2.getOrdinal())
					return -1;
				else if (o1.getOrdinal() > o2.getOrdinal())
					return  1;
				else 
					return 0;//they must be the same
			}
		});
		
		return persistedQuiz;
	}
}