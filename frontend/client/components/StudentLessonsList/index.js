
import React, { Component, PropTypes } from 'react'
import autobind from 'autobind-decorator'
import StudentLessonCard from './card.js'
import style from './index.css'

class StudentLessonsList extends Component {
  static propTypes = {
    currentLesson: PropTypes.object.isRequired,
    upcomingLessons: PropTypes.array.isRequired,
  }

  static contextTypes = {
    router: React.PropTypes.object.isRequired,
  }

  @autobind
  onLessonStartRequested(contentItemId, lessonTitle) {
    const newWindow = window.open();
    newWindow.location.href = `/api/content/id/${contentItemId}/${lessonTitle}`;
    console.log(`onLessonStartRequested('${contentItemId},${lessonTitle}')`)
  }

  @autobind
  onLessonQuizStartRequested(quizId) {
    this.context.router.history.push(`/quiz/${quizId}`)
    console.log(`onLessonQuizStartRequested('${quizId}')`)
  }

  render() {
    const { currentLesson, upcomingLessons } = this.props

    return (
      <div className={style.main}>
        <div className={style.section}>
          <div className={style.heading}>Current Lesson</div>
          <div className={style.cards}>
            <StudentLessonCard
                key={currentLesson.id}
                lesson={currentLesson}
                onLessonStartRequested={this.onLessonStartRequested}
                onLessonQuizStartRequested={this.onLessonQuizStartRequested} />
          </div>
        </div>
        <div className={style.section}>
          <div className={style.heading}>Upcoming Lessons</div>
          <div className={style.cards}>
          {
            upcomingLessons.map(lesson => (
              <StudentLessonCard
                  key={lesson.id}
                  lesson={lesson}
                  onLessonStartRequested={this.onLessonStartRequested}
                  onLessonQuizStartRequested={this.onLessonQuizStartRequested} />
            ))
          }
          </div>
        </div>
      </div>
    )
  }
}

export default StudentLessonsList
