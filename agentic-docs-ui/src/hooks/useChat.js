import { useState, useCallback } from 'react'
import { sendChatMessage } from '../api/chatApi'

/**
 * Manages AI chat state — messages, loading flag, and the send function.
 *
 * Extracted from App.jsx so that:
 *  - The state logic can be tested independently of any rendered output.
 *  - {@link AiChat} becomes a pure presentational component.
 *
 * Data fetching is delegated to {@link sendChatMessage} (src/api/chatApi.js)
 * so this hook only owns UI state, not transport logic.
 *
 * @returns {{ messages, loading, sendMessage, resetMessages }}
 */
export function useChat() {
  const [messages, setMessages] = useState([])
  const [loading,  setLoading]  = useState(false)

  const sendMessage = useCallback(async (question) => {
    setMessages((prev) => [...prev, { role: 'user', content: question }])
    setLoading(true)
    try {
      const data = await sendChatMessage(question)
      setMessages((prev) => [...prev, { role: 'assistant', content: data.answer }])
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: `**Error:** Could not reach the backend.\n\n\`${err.message}\`` },
      ])
    } finally {
      setLoading(false)
    }
  }, [])

  const resetMessages = useCallback(() => setMessages([]), [])

  return { messages, loading, sendMessage, resetMessages }
}
