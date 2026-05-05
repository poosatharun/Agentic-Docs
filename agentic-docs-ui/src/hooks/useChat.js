import { useState, useCallback, useRef, useEffect } from 'react'
import { sendChatMessageStream } from '../api/chatApi'

/**
 * Manages AI chat state — messages, loading flag, and the send function.
 *
 * Uses streaming (SSE) to deliver tokens to the UI as they arrive from the LLM,
 * dramatically reducing perceived latency for local Ollama models.
 *
 * @returns {{ messages, loading, sendMessage, abort }}
 */
export function useChat() {
  const [messages, setMessages] = useState([])
  const [loading,  setLoading]  = useState(false)
  const abortRef = useRef(null)

  // Abort any in-flight stream on unmount
  useEffect(() => {
    return () => { abortRef.current?.() }
  }, [])

  const sendMessage = useCallback((question) => {
    // Abort any previous in-flight stream
    abortRef.current?.()

    const now = new Date().toISOString()
    // Add the user message immediately
    setMessages((prev) => [...prev, { role: 'user', content: question, timestamp: now }])
    setLoading(true)

    // Insert a blank assistant placeholder that we'll fill token-by-token
    setMessages((prev) => [...prev, { role: 'assistant', content: '', timestamp: now }])

    const abort = sendChatMessageStream(
      question,
      // onToken — append each token to the last (assistant) message
      (token) => {
        setMessages((prev) => {
          const updated = [...prev]
          const last    = updated[updated.length - 1]
          updated[updated.length - 1] = { ...last, content: last.content + token }
          return updated
        })
      },
      // onDone
      () => {
        setLoading(false)
        // If the model returned nothing, show a fallback message
        setMessages((prev) => {
          const updated = [...prev]
          const last    = updated[updated.length - 1]
          if (!last.content.trim()) {
            updated[updated.length - 1] = {
              ...last,
              content: 'I could not find a relevant endpoint for that. Please check the API Explorer tab.',
            }
          }
          return updated
        })
      },
      // onError
      (errMsg) => {
        setLoading(false)
        setMessages((prev) => {
          const updated = [...prev]
          updated[updated.length - 1] = {
            role: 'assistant',
            content: `**Error:** Could not reach the backend.\n\n\`${errMsg}\``,
          }
          return updated
        })
      }
    )
    abortRef.current = abort
  }, [])

  return { messages, loading, sendMessage }
}
