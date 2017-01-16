package org.gauntlet.quizzes.generator.defaults.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.gauntlet.core.api.ApplicationException;
import org.gauntlet.core.api.dao.NoSuchModelException;
import org.gauntlet.problems.api.dao.IProblemDAOService;
import org.gauntlet.problems.api.model.Problem;
import org.gauntlet.problems.api.model.ProblemCategory;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.quizzical.backend.security.api.model.user.User;
import org.gauntlet.quizzes.api.dao.IQuizDAOService;
import org.gauntlet.quizzes.api.model.Constants;
import org.gauntlet.quizzes.api.model.Quiz;
import org.gauntlet.quizzes.api.model.QuizProblem;
import org.gauntlet.quizzes.api.model.QuizType;
import org.gauntlet.quizzes.generator.api.IQuizGeneratorService;
import org.gauntlet.quizzes.generator.api.model.QuizGenerationParameters;


public class ByProblemCategoryGeneratorImpl implements IQuizGeneratorService { 
	private static final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
	private static final DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

	private volatile LogService logger;
	
	private volatile BundleContext ctx;

	private volatile IQuizDAOService quizDAOService;
	
	private volatile IProblemDAOService problemDAOService;
	
	@Override
	public Quiz generate(User user, QuizGenerationParameters params) throws ApplicationException {
		ProblemCategory category;
		try {
			category = problemDAOService.getProblemCategoryByPrimary(
					params.getProblemCategoryId());
		} catch (NoSuchModelException e) {
			throw new ApplicationException(String.format(
					"No category with id '%d' could be found: %s.",
					params.getProblemCategoryId(),
					e.getMessage()));
		}
		
		if (category == null) {
			throw new ApplicationException(String.format(
					"No category with id '%d' could be found.",
					params.getProblemCategoryId()));
		}
		
		final QuizType quizType = quizDAOService.provideQuizType(new QuizType(
				Constants.QUIZ_TYPE_GENERATED_NAME,
				Constants.QUIZ_TYPE_GENERATED_CODE));
		final String quizCode = String.format(
				"generated-%s-%d-%d",
				params.getGeneratorType(),
				params.getQuizSize(),
				System.currentTimeMillis());
		final Date quizDateTime = Calendar.getInstance().getTime();
		final String quizName = String.format(
				"Generated by Category \"%s\" at %s on %s",
				category.getName(),
				timeFormat.format(quizDateTime),
				dateFormat.format(quizDateTime));
		
		final Map<Long, Problem> problems = problemDAOService.findByCategory(
				params.getProblemCategoryId(),
				0,
				params.getQuizSize())
				.stream()
				.collect(Collectors.toMap(
						problem -> problem.getId(),
						Function.identity()));
		final List<Problem> orderedProblems = problems
				.entrySet()
				.stream()
				.map(problemEntry -> problemEntry.getValue())
				.collect(Collectors.toList());
		final List<QuizProblem> orderedQuizProblems = IntStream
				.range(0, orderedProblems.size())
				.mapToObj(ordinal -> {
					final Problem problem = orderedProblems.get(ordinal);
					return new QuizProblem(
							"",
							String.format("%s-%s", quizCode, problem.getCode()),
							ordinal,
							problem.getId(),
							problem);
				})
				.collect(Collectors.toList());

		final Quiz quiz = new Quiz();
		quiz.setUserId(user.getId());
		quiz.setCode(quizCode);
		quiz.setName(quizName);
		quiz.setQuizType(quizType);
		quiz.setQuestions(orderedQuizProblems);
		
		final Quiz persistedQuiz = quizDAOService.provide(user, quiz);
		persistedQuiz.getQuestions()
			.stream()
			.forEach(question -> {
				question.setProblem(problems.get(question.getProblemId()));
				question.setQuiz(quiz);
			});
		
		return persistedQuiz;
	}
}