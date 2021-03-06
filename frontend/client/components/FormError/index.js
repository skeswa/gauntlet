
import classNames from 'classnames'
import React, { Component } from 'react'

import style from './style.css'

const FormError = (props, context) => (
  <div style={props.style} className={style.main}>
    <div className={style.error}>
      <div className={style.icon}>
        <i className="material-icons">error</i>
      </div>
      <div className={style.text}>
        <div className={style.title}>{props.title}</div>
        <div className={
          classNames(style.message, {
            [style.message__limitedHeight]: props.limitHeight,
          })
        }>{props.message}</div>
      </div>
    </div>
  </div>
)

export default FormError
