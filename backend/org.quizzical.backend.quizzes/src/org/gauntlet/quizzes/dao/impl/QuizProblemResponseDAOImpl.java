package org.gauntlet.quizzes.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.amdatu.jta.Transactional;
import org.gauntlet.core.api.ApplicationException;
import org.gauntlet.core.api.dao.NoSuchModelException;
import org.gauntlet.core.commons.util.jpa.JPAEntityUtil;
import org.gauntlet.core.model.JPABaseEntity;
import org.gauntlet.core.service.impl.BaseServiceImpl;
import org.osgi.service.log.LogService;
import org.quizzical.backend.security.authorization.api.model.user.User;
import org.gauntlet.quizzes.api.dao.IQuizProblemResponseDAOService;
import org.gauntlet.quizzes.api.model.QuizProblemResponse;
import org.gauntlet.quizzes.model.jpa.JPAQuizProblem;
import org.gauntlet.quizzes.model.jpa.JPAQuizProblemResponse;


@SuppressWarnings("restriction")
@Transactional
public class QuizProblemResponseDAOImpl extends BaseServiceImpl implements IQuizProblemResponseDAOService {
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
	public List<QuizProblemResponse> findAll() throws ApplicationException {
		List<QuizProblemResponse> result = new ArrayList<>();
		try {
			List<JPABaseEntity> resultList = super.findAll(JPAQuizProblemResponse.class);
			result = JPAEntityUtil.copy(resultList, QuizProblemResponse.class);
		}
		catch (Exception e) {
			throw processException(e);
		}
		return result;		
	}
	
	
	@Override
	public List<Long> getAllUserPracticedProblemIds(Long problemTypeId, User user) throws ApplicationException {
		List<Long> res;
		try {
			CriteriaBuilder builder = getEm().getCriteriaBuilder();
			CriteriaQuery<JPAQuizProblemResponse> query = builder.createQuery(JPAQuizProblemResponse.class);
			Root<JPAQuizProblemResponse> rootEntity = query.from(JPAQuizProblemResponse.class);
			
			final Map<ParameterExpression,Object> pes = new HashMap<>();
			
			//userId
			ParameterExpression<Long> pid = builder.parameter(Long.class);
			query.select(rootEntity).where(builder.equal(rootEntity.get("submission").get("quiz").get("userId"),pid));
			pes.put(pid, user.getId());
			
			List<JPAQuizProblemResponse> resultList = findWithDynamicQueryAndParams(query,pes);
			
			res = resultList.stream()
				.map(qp -> {
					Long pId;
					try {
						JPAQuizProblem qp_ = (JPAQuizProblem)findByPrimaryKey(JPAQuizProblem.class,qp.getQuizProblemId());
						pId = qp_.getProblemId();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					return pId;
					})
				.collect(Collectors.toList());
		}
		catch (Exception e) {
			throw processException(e);
		}
		return res;			
	}
	
	@Override
	public List<Long> getAllUserSkippedOrIncorrectProblemIds(User user, Integer limit) throws ApplicationException {
		List<Long> res;
		try {
			CriteriaBuilder builder = getEm().getCriteriaBuilder();
			CriteriaQuery<JPAQuizProblemResponse> query = builder.createQuery(JPAQuizProblemResponse.class);
			Root<JPAQuizProblemResponse> rootEntity = query.from(JPAQuizProblemResponse.class);
			
			final Map<ParameterExpression,Object> pes = new HashMap<>();
			
			//userId
			ParameterExpression<Boolean> pbool = builder.parameter(Boolean.class);
			ParameterExpression<Long> pid = builder.parameter(Long.class);
			query.select(rootEntity).where(builder.and(
					builder.equal(rootEntity.get("submission").get("quiz").get("userId"),pid),
					builder.or(builder.equal(rootEntity.get("correct"),pbool),builder.equal(rootEntity.get("skipped"),pbool))
					));
			pes.put(pid, user.getId());
			pes.put(pbool, true);
			
			List<JPAQuizProblemResponse> resultList = findWithDynamicQueryAndParams(query,pes,0,limit);
			
			res = resultList.stream()
				.map(qp -> {
					Long pId;
					try {
						pId = ((JPAQuizProblem)findByPrimaryKey(JPAQuizProblem.class,qp.getQuizProblemId())).getProblemId();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					return pId;
					})
				.collect(Collectors.toList());
		}
		catch (Exception e) {
			throw processException(e);
		}
		return res;			
	}
	
	@Override
	public List<Long> getAllUserCorrectAndWithinTimeProblemIds(User user) throws ApplicationException {
		List<Long> res;
		try {
			CriteriaBuilder builder = getEm().getCriteriaBuilder();
			CriteriaQuery<JPAQuizProblemResponse> query = builder.createQuery(JPAQuizProblemResponse.class);
			Root<JPAQuizProblemResponse> rootEntity = query.from(JPAQuizProblemResponse.class);
			
			final Map<ParameterExpression,Object> pes = new HashMap<>();
			
			//userId
			ParameterExpression<Boolean> pbool = builder.parameter(Boolean.class);
			ParameterExpression<Boolean> pskipped = builder.parameter(Boolean.class);
			ParameterExpression<Integer> pelapsed = builder.parameter(Integer.class);
			
			ParameterExpression<Long> pid = builder.parameter(Long.class);
			query.select(rootEntity).where(builder.and(
					builder.equal(rootEntity.get("submission").get("quiz").get("userId"),pid),
					builder.equal(rootEntity.get("skipped"),pskipped),
					builder.equal(rootEntity.get("correct"),pbool),
					builder.le(rootEntity.get("secondsElapsed"),pelapsed)
					));
			pes.put(pid, user.getId());
			pes.put(pskipped, false);
			pes.put(pbool, true);
			pes.put(pelapsed, AVG_RESPONSE_TIME_IN_SECS_CALCULATOR_PER_PROBLEM);
			
			query.orderBy(builder.asc(rootEntity.get("secondsElapsed")));
			
			List<JPAQuizProblemResponse> resultList = findWithDynamicQueryAndParams(query,pes);
			
			res = resultList.stream()
				.map(qp -> {
					Long pId;
					try {
						pId = ((JPAQuizProblem)findByPrimaryKey(JPAQuizProblem.class,qp.getQuizProblemId())).getProblemId();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					return pId;
					})
				.collect(Collectors.toList());
		}
		catch (Exception e) {
			throw processException(e);
		}
		return res;			
	}


	@Override
	public QuizProblemResponse add(QuizProblemResponse record)
			  throws ApplicationException {
		JPAQuizProblemResponse jpaRes = (JPAQuizProblemResponse) super.add(JPAEntityUtil.copy(record, JPAQuizProblemResponse.class));
		return JPAEntityUtil.copy(jpaRes, QuizProblemResponse.class);
	}
	
	@Override
	public QuizProblemResponse update(QuizProblemResponse record) throws ApplicationException {
		JPABaseEntity res = super.update(JPAEntityUtil.copy(record, JPAQuizProblemResponse.class));
		QuizProblemResponse dto = JPAEntityUtil.copy(res, QuizProblemResponse.class);
		return dto;	
	}	
	
	@Override
	public QuizProblemResponse delete(Long id) throws ApplicationException, NoSuchModelException {
		JPAQuizProblemResponse jpaEntity = (JPAQuizProblemResponse) super.findByPrimaryKey(JPAQuizProblemResponse.class, id);
		super.remove(jpaEntity);
		return JPAEntityUtil.copy(jpaEntity, QuizProblemResponse.class);
	}
	
	
	@Override
	public void createDefaults() throws ApplicationException {
	}	
}