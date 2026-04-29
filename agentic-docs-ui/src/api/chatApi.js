import { apiClient } from './client'
import { CHAT_URL }  from '../constants/apiUrls'

/**
 * Sends a user question to the AI chat backend.
 *
 * @param {string} question
 * @returns {Promise<{ answer: string }>}
 */
export async function sendChatMessage(question) {
  const { data } = await apiClient(CHAT_URL, {
    method: 'POST',
    body:   JSON.stringify({ question }),
  })
  return data
}
