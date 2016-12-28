
import { routerReducer as routing } from 'react-router-redux'
import { combineReducers } from 'redux'

import category from './category'
import difficulty from './difficulty'
import question from './question'
import quiz from './quiz'
import quizAttempt from './quiz-attempt'
import source from './source'
import take from './take'

export default combineReducers({
  routing,
  category,
  difficulty,
  question,
  quiz,
  quizAttempt,
  source,
  take,
})