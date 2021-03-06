
import debug from 'debug'

import { endpointSuffixFor } from 'utils/crud'

const logger = debug('gauntlet:crud:service')
const genericFailureError = 'Sorry. For whatever reason, the server isn\'t co-operating. Let\'s try that again.'
const connectionFailureError = 'Could not reach the server. Make sure you have a good connection.'

export function crudService(entity, extender) {
  const endpoint = `/api/${endpointSuffixFor(entity)}`

  return Object.assign({
    get(id) {
      return fetch(`${endpoint}/${id}`, { method: 'GET' })
        .then(handleSuccess, handleFailure)
    },

    getAll(offset = 0, limit = 50) {
      return fetch(`${endpoint}?start=${offset}&end=${offset + limit}`, { method: 'GET' })
        .then(handleSuccess, handleFailure)
    },

    create(payload) {
      let body, headers = { 'Accept': 'application/json' };
      if (payload instanceof FormData) {
        body = payload
      } else {
        body = JSON.stringify(payload)
        headers['Content-Type'] = 'application/json'
      }

      return fetch(endpoint, { method: 'PUT', headers, body })
        .then(handleSuccess, handleFailure)
    },

    del(id) {
      return fetch(`${endpoint}/${id}`, { method: 'POST' })
        .then(handleSuccess, handleFailure)
    },
  }, extender ? extender(endpoint, handleSuccess, handleFailure) : null)
}

function handleSuccess(response) {
  return response.ok
    ? deserializeResponse(response)
    : Promise.reject(deserializeError(response))
}

function handleFailure(problem) {
  logger('Service request failed:', problem)
  return Promise.reject(connectionFailureError)
}

function deserializeResponse(response) {
  if (response.headers.get('Content-Type') === 'application/json') {
    return response.json()
  }

  return response.text()
}

function deserializeError(response) {
  return deserializeResponse(response)
    .then(data => data ? data : genericFailureError)
}
