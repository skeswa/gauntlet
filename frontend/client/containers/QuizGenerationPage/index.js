
import { connect } from 'react-redux'
import RaisedButton from 'material-ui/RaisedButton'
import React, { Component } from 'react'
import { bindActionCreators } from 'redux'

import style from './style.css'
import actions from 'actions'
import PracticeSkeleton from 'components/PracticeSkeleton'
import QuizGenerationForm from 'components/QuizGenerationForm'
import { extractErrorFromResultingActions } from 'utils'

class QuizGenerationPage extends Component {
  static propTypes = {
    actions:            React.PropTypes.object.isRequired,
    categories:         React.PropTypes.array.isRequired,
    dataShouldBeLoaded: React.PropTypes.bool.isRequired,
  }

  static contextTypes = {
    router: React.PropTypes.object.isRequired,
  }

  state = {
    loadingError:             null,
    isDataLoading:            false,
    quizGenerationError:      null,
    quizGenerationInProgress: false,
  }

  componentWillMount() {
    if (this.props.dataShouldBeLoaded) this.loadData()
  }

  loadData() {
    const { loadCategories } = this.props.actions

    // Indicate loading.
    this.setState({ isDataLoading: true, loadingError: null })

    // Re-load everything.
    loadCategories()
      .then(resultingActions => {
        const error = extractErrorFromResultingActions(resultingActions)
        if (error) {
          this.setState({
            loadingError:   error,
            isDataLoading:  false,
          })
        } else {
          this.setState({ isDataLoading: false })
        }
      })
  }

  onStartClicked() {
    this.setState({
      quizGenerationError:      null,
      quizGenerationInProgress: true,
    })

    const formData = this.refs.generationForm.getJSON()
    this.props.actions.generateQuiz(formData)
      .then(resultingAction => {
        if (resultingAction.error) {
          this.setState({
            quizGenerationError:      resultingAction.payload,
            quizGenerationInProgress: false,
          })
        } else {
          this.setState({
            quizGenerationInProgress:     false,
            quizGenerationDialogVisible:  false,
          })

          this.context.router.push(`/quiz/${resultingAction.payload.id}/take`)
        }
      })
  }

  render() {
    const { actions, categories } = this.props
    const {
      isDataLoading,
      quizGenerationError,
      quizGenerationInProgress,
    } = this.state

    return (
      <PracticeSkeleton title="Quizzical" subtitle="Choose your quiz">
        <div className={style.main}>
          <div className={style.middle}>
            <QuizGenerationForm
              ref="generationForm"
              error={quizGenerationError}
              loading={quizGenerationInProgress}
              categories={categories}
              outsidePopup={true} />
          </div>
          <div className={style.bottom}>
            <RaisedButton
              label="Start"
              onClick={::this.onStartClicked}
              disabled={isDataLoading}
              labelColor="#754aec"
              backgroundColor="#ffffff" />
          </div>
        </div>
      </PracticeSkeleton>
    )
  }
}

const reduxify = connect(
  (state, props) => ({
    categories:         state.category.list,
    dataShouldBeLoaded: !state.category.loaded,
  }),
  (dispatch, props) => ({
    actions: Object.assign(
      {},
      bindActionCreators(actions.quiz, dispatch),
      bindActionCreators(actions.category, dispatch))
  }))

export default reduxify(QuizGenerationPage)
