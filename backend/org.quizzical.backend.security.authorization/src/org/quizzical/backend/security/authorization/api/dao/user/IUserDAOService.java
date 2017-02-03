package org.quizzical.backend.security.authorization.api.dao.user;

import java.util.List;

import org.apache.commons.mail.EmailException;
import org.gauntlet.core.api.ApplicationException;
import org.quizzical.backend.security.authorization.api.model.user.User;


public interface IUserDAOService {
	//
	public User get(long id);
	
	public List<User> getAll();
	
	public User getByEmail(String email) throws ApplicationException;
	
	public User getByCode(String code) throws ApplicationException;	

	public User add(User record) throws ApplicationException;

	public void delete(User record);
	
	public void delete(String recordId) throws ApplicationException;

	public User update(User record) throws ApplicationException;
	
	public User activate(User record) throws ApplicationException;
	
	public User deactivate(User record) throws ApplicationException;
	
	public User provide(User record) throws Exception;

	public List<User> getAllActiveUsers();
	
	public User getUserByEmailAndPassword(String email, String password) throws UserNotFoundException, ApplicationException;

	public User getUserByEmail(String email) throws UserNotFoundException, ApplicationException;
	
	//Account management
	public void sendWelcome(String userId, String newPassword, List<String> bccEmails) throws ApplicationException, EmailException;

	void addUser(String userId, String firstName, String newPassword, List<String> bccEmails)
			throws ApplicationException, EmailException;
}