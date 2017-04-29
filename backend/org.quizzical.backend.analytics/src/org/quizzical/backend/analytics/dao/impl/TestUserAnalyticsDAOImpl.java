package org.quizzical.backend.analytics.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.amdatu.jta.Transactional;
import org.gauntlet.core.api.ApplicationException;
import org.gauntlet.core.api.dao.NoSuchModelException;
import org.gauntlet.core.commons.util.Validator;
import org.gauntlet.core.commons.util.jpa.JPAEntityUtil;
import org.gauntlet.core.model.JPABaseEntity;
import org.gauntlet.core.service.impl.BaseServiceImpl;
import org.gauntlet.problems.model.jpa.JPAProblem;
import org.osgi.service.log.LogService;
import org.quizzical.backend.analytics.api.dao.ITestUserAnalyticsDAOService;
import org.quizzical.backend.analytics.api.model.TestCategoryRating;
import org.quizzical.backend.analytics.api.model.TestCategoryRatingSubmission;
import org.quizzical.backend.analytics.api.model.TestUserAnalytics;
import org.quizzical.backend.analytics.model.jpa.JPATestCategoryAttempt;
import org.quizzical.backend.analytics.model.jpa.JPATestCategoryRating;
import org.quizzical.backend.analytics.model.jpa.JPATestCategoryRatingSubmission;
import org.quizzical.backend.analytics.model.jpa.JPATestUserAnalytics;
import org.quizzical.backend.security.authorization.api.model.user.User;


@SuppressWarnings("restriction")
@Transactional
public class TestUserAnalyticsDAOImpl extends BaseServiceImpl implements ITestUserAnalyticsDAOService {
	//TestDesignTemplates
	private volatile LogService logger;
	
	private volatile EntityManager em;
	
	@Override
	public LogService getLogger() {
		return logger;
	}

	public void setLogger(LogService logger) {
		this.logger = logger;
	}

	@Override
	public EntityManager getEm() {
		return em;
	}	
	
	@Override 
	public List<TestCategoryRating> findWeakestCategories(final User user, final Integer categoryLimit) throws ApplicationException {
		List<TestCategoryRating> resultList = null;
		try {
			CriteriaBuilder builder = getEm().getCriteriaBuilder();
			CriteriaQuery<JPATestCategoryRating> query = builder.createQuery(JPATestCategoryRating.class);
			Root<JPATestCategoryRating> rootEntity = query.from(JPATestCategoryRating.class);
			
			final Map<ParameterExpression,Object> pes = new HashMap<>();
			
			//user
			ParameterExpression<Long> userIdParam = builder.parameter(Long.class);
			query.select(rootEntity).where(builder.equal(rootEntity.get("analytics").get("userId"),userIdParam));
			pes.put(userIdParam, user.getId());
			
			//userId
/*			ParameterExpression<Integer> ratingParam = builder.parameter(Integer.class);
			query.select(rootEntity).where(builder.le(rootEntity.get("rating"),ratingParam));
			pes.put(ratingParam, user.getId());*/
			
			query.orderBy(builder.asc(rootEntity.get("rating")));
			
			int adjustedLimit = generateAdjustedLimit(categoryLimit);
			
			if (adjustedLimit < 0) //No limit
				resultList = findWithDynamicQueryAndParams(query,pes);
			else
				resultList = findWithDynamicQueryAndParams(query,pes,0,adjustedLimit);
			
			resultList = JPAEntityUtil.copy(resultList, TestCategoryRating.class);
		}
		catch (Exception e) {
			throw processException(e);
		}
		return resultList;		
	}
	
	@Override 
	public List<TestCategoryRating> findWeakestCategoriesLowerThanRating(final User user, final Integer startRatingCutoffIncl, final Integer endRatingCutoffIncl) throws ApplicationException {
		List<TestCategoryRating> resultList = null;
		try {
			CriteriaBuilder builder = getEm().getCriteriaBuilder();
			
			ParameterExpression<Long> userIdParam = builder.parameter(Long.class);
			ParameterExpression<Integer> startRatingCutoffInclExpr = builder.parameter(Integer.class);
			ParameterExpression<Integer> endRatingCutoffInclExpr = builder.parameter(Integer.class);

			CriteriaBuilder qb =  getEm().getCriteriaBuilder();
			CriteriaQuery<JPATestCategoryRating> cq = qb.createQuery(JPATestCategoryRating.class);
			Root<JPATestCategoryRating> rootEntity = cq.from(JPATestCategoryRating.class);
			cq.select(rootEntity).where(builder.and(
					builder.equal(rootEntity.get("analytics").get("userId"),userIdParam),
					builder.ge(rootEntity.get("rating"),startRatingCutoffInclExpr),
					builder.le(rootEntity.get("rating"),endRatingCutoffInclExpr
							)));
			
			cq.orderBy(builder.asc(rootEntity.get("rating")));
			
			TypedQuery typedQuery = getEm().createQuery(cq);
			typedQuery.setParameter(userIdParam, user.getId());
			typedQuery.setParameter(startRatingCutoffInclExpr, startRatingCutoffIncl);
			typedQuery.setParameter(endRatingCutoffInclExpr, endRatingCutoffIncl);
			
			resultList = typedQuery.getResultList();
			resultList = JPAEntityUtil.copy(resultList, TestCategoryRating.class);
		}
		catch (Exception e) {
			throw processException(e);
		}
		return resultList;		
	}
	
	private Integer generateAdjustedLimit(final Integer categoryLimit)
			throws ApplicationException {
		if (categoryLimit <= org.quizzical.backend.testdesign.api.model.Constants.QUIZ_SMALL_SIZE)
			return 2;//org.quizzical.backend.testdesign.api.model.Constants.QUIZ_SMALL_SIZE;
		else if (categoryLimit > org.quizzical.backend.testdesign.api.model.Constants.QUIZ_SMALL_SIZE &&
				categoryLimit < org.quizzical.backend.testdesign.api.model.Constants.QUIZ_MEDIUM_SIZE)
			return 6;//org.quizzical.backend.testdesign.api.model.Constants.QUIZ_MEDIUM_SIZE;
		else
			return -1;//org.quizzical.backend.testdesign.api.model.Constants.QUIZ_FULL_SIZE;
	}
	
	@Override
	public List<TestUserAnalytics> findAll(int start, int end) throws ApplicationException {
		List<TestUserAnalytics> result = new ArrayList<>();
		try {
			List<JPABaseEntity> resultList = super.findAll(JPATestUserAnalytics.class,start,end);
			result = JPAEntityUtil.copy(resultList, TestUserAnalytics.class);
		}
		catch (Exception e) {
			throw processException(e);
		}
		return result;		
	}
	

	@Override
	public long countAll() throws ApplicationException {
		long res = 0;
		try {
			res = super.countAll(JPATestUserAnalytics.class);
		}
		catch (Exception e) {
			throw processException(e);
		}
		return res;
	}	
	
	@Override
	public TestUserAnalytics getByPrimary(Long pk) throws ApplicationException, NoSuchModelException {
		JPATestUserAnalytics jpaEntity = (JPATestUserAnalytics) super.findByPrimaryKey(JPATestUserAnalytics.class, pk);
		return JPAEntityUtil.copy(jpaEntity, TestUserAnalytics.class);
	}

	@Override
	public TestUserAnalytics provide(TestUserAnalytics record)
			  throws ApplicationException {
		TestUserAnalytics existingTestDesignTemplate = getByCode(record.getCode());
		if (Validator.isNull(existingTestDesignTemplate))
		{
			JPATestUserAnalytics td = toJPAEntity(record);
			JPABaseEntity res = super.add(td);
			existingTestDesignTemplate = toDTO((JPATestUserAnalytics)res);//JPAEntityUtil.copy(res, TestUserAnalytics.class);
		}

		return existingTestDesignTemplate;
	}
	
	private TestUserAnalytics toDTO(JPATestUserAnalytics res) {
		TestUserAnalytics anaDTO = new  TestUserAnalytics(res.getUserId(), res.getCode(), res.getName());
		List<TestCategoryRating> ratings = res.getRatings().stream()
			.map(r -> {
				TestCategoryRating rDTO = JPAEntityUtil.copy(r, TestCategoryRating.class);
				
				List<TestCategoryRatingSubmission> subs = r.getSubmissions().stream()
					.map(s -> {
						return JPAEntityUtil.copy(s, TestCategoryRatingSubmission.class);
					})
					.collect(Collectors.toList());
				
				rDTO.setRatingSubmissions(subs);
				
				return rDTO;
			})
			.collect(Collectors.toList());
		
		anaDTO.setRatings(ratings);
		
		return anaDTO;
	}

	private JPATestUserAnalytics toJPAEntity(TestUserAnalytics record) {
		final TestUserAnalytics recordCopy = new TestUserAnalytics(record.getUserId(),record.getCode(),record.getName());
		final JPATestUserAnalytics jpaTestUserAnalytics = JPAEntityUtil.copy(recordCopy, JPATestUserAnalytics.class);
    	try {
			final List<JPATestCategoryRating> ratings = record.getRatings()
				.stream()
				.map(rating -> {
					final TestCategoryRating ratingRecordCopy = new TestCategoryRating(rating.getRating(), rating.getLastAttemptTestId(),rating.getDateOfLastAttempt(), rating.getCategoryId(), rating.getName());
					JPATestCategoryRating jpaRatingRecord = JPAEntityUtil.copy(ratingRecordCopy, JPATestCategoryRating.class);
			    	final Set<JPATestCategoryRatingSubmission> submissionEnties = rating.getRatingSubmissions()
			        		.stream()
			        		.map(submission -> {
			        			JPATestCategoryRatingSubmission submissionEntity = new JPATestCategoryRatingSubmission(submission.getDateAttempted());
			        			List<JPATestCategoryAttempt> attemptEntities = submission.getAttempts().stream()
			        					.map(a -> {
			        						JPATestCategoryAttempt attempt = new JPATestCategoryAttempt(a);
			        						attempt.setRatingSubmission(submissionEntity);
			        						return attempt;
			        					}).collect(Collectors.toList());
			        			submissionEntity.setAttempts(attemptEntities);
			        			submissionEntity.setRating(jpaRatingRecord);
			    				return submissionEntity;
			    	    	})
			        		.collect(Collectors.toSet());
			    	jpaRatingRecord.setSubmissions(submissionEnties);
			    	jpaRatingRecord.calculateScore();
			    	jpaRatingRecord.setAnalytics(jpaTestUserAnalytics);
					return jpaRatingRecord;
				})
				.collect(Collectors.toList());
			
			jpaTestUserAnalytics.getRatings().addAll(ratings);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return jpaTestUserAnalytics;
	}

	

	@Override
	public TestUserAnalytics update(TestUserAnalytics record) throws ApplicationException {
		JPABaseEntity res = super.update(JPAEntityUtil.copy(record, JPATestUserAnalytics.class));
		TestUserAnalytics dto = JPAEntityUtil.copy(res, TestUserAnalytics.class);
		return dto;	
	}	
	
	@Override
	public TestUserAnalytics delete(Long id) throws ApplicationException, NoSuchModelException {
		JPATestUserAnalytics jpaEntity = (JPATestUserAnalytics) super.findByPrimaryKey(JPATestUserAnalytics.class, id);
		super.remove(jpaEntity);
		return JPAEntityUtil.copy(jpaEntity, TestUserAnalytics.class);
	}
	
	@Override
	public TestUserAnalytics getByCode(String code) throws ApplicationException {
		JPATestUserAnalytics jpaEntity = getByCode_(code);
		return JPAEntityUtil.copy(jpaEntity, TestUserAnalytics.class);
	}

	private JPATestUserAnalytics getByCode_(String code) throws ApplicationException {
		return  (JPATestUserAnalytics) super.findWithAttribute(JPATestUserAnalytics.class, String.class,"code", code);
	}
	
	@Override
	public TestUserAnalytics getByName(String name) throws ApplicationException {
		JPATestUserAnalytics jpaEntity = (JPATestUserAnalytics) super.findWithAttribute(JPATestUserAnalytics.class, String.class,"name", name);
		return JPAEntityUtil.copy(jpaEntity, TestUserAnalytics.class);
	}

	@Override
	public void createDefaults() throws ApplicationException, Exception {
	}
	
	//-- Misc
	@Override
	public void truncate() throws ApplicationException {
		super.truncate("gnt_ana_testuser");
	}
	
	@Override
	public void updateRatings(String code, Map<Long, TestCategoryRating> newCategoryRatingsMap) throws ApplicationException {
		JPATestUserAnalytics analytics = getByCode_(code);
		for (JPATestCategoryRating rating : analytics.getRatings()) {
			final TestCategoryRating newRating = newCategoryRatingsMap.get(rating.getCategoryId());
			if (newRating != null) {
				newRating.getRatingSubmissions().stream()
					.forEach(newSubmission -> {
						final JPATestCategoryRatingSubmission newSubmissionEntity = JPAEntityUtil.copy(newSubmission, JPATestCategoryRatingSubmission.class);
						newSubmissionEntity.getAttempts().stream()
							.forEach(att -> {
								att.setRatingSubmission(newSubmissionEntity);
							});
						newSubmissionEntity.setRating(rating);
						rating.addSubmission(newSubmissionEntity);
					});
				rating.calculateScore();
				System.out.println(String.format("User (%d)/Rating %d in Category (%s)",analytics.getUserId(),rating.getRating(),rating.getName()));
			}
		}
		update(analytics);
	}
}